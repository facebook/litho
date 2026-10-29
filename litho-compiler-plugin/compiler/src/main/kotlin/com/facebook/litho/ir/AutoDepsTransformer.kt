/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.ir

import com.facebook.kotlin.compilercompat.compatValueParameters
import com.facebook.kotlin.compilercompat.getValueArgumentCompat
import com.facebook.kotlin.compilercompat.putValueArgumentCompat
import com.facebook.litho.common.AutoDepsConfig
import com.facebook.litho.common.LithoNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getArrayElementType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

/**
 * Fills in the omitted dependency list of every `@AutoDeps` call site from the closure of its
 * `@DepsFrom` arguments.
 *
 * ```
 * // Author writes                    Transformer emits
 * Layout(style = style) { c ->        Layout(deps = arrayOf(text, count), style = style) { c ->
 *   Text("$text : $count").measure(c)   Text("$text : $count").measure(c)
 * }                                   }
 * ```
 *
 * Only a call that *omits* the `@Deps` argument is rewritten. Every call site that passes
 * dependencies today compiles to exactly the bytes it compiles to now -- passing them is the
 * opt-out, and `Unit` (vararg) or `emptyArray()` (array) is the way to say "no dependencies".
 *
 * An inferred dependency is evaluated eagerly at the call site, as a hand-written one always has
 * been, even when the lambda would have skipped the read or never run. That is why a Level 1 path
 * is made only of field reads and default getters no override can replace, which run none of the
 * author's code: a custom getter lifted to the call could throw where the lambda guarded it.
 *
 * Runs before [HookIdentityTransformer] so that it analyses source-shaped IR. Both orders produce
 * identical output today, but "the analysis reads every value reference the developer wrote" is
 * only literally true of IR nothing else has rewritten.
 */
class AutoDepsTransformer(
    private val pluginContext: IrPluginContext,
    config: AutoDepsConfig,
    private val messages: MessageCollector,
) : IrElementTransformerVoid() {

  private val analysis = CaptureAnalysis(pluginContext, config, messages)

  private var currentFile: IrFile? = null
  private var currentFunction: IrFunction? = null

  /**
   * Receivers of the objects whose construction the walk is inside: a constructor, an `init` block,
   * or a property initializer. Scopes nested in them count too, since an inline lambda there runs
   * during construction.
   */
  private val constructing = mutableListOf<IrValueSymbol>()

  override fun visitFile(declaration: IrFile): IrFile {
    currentFile = declaration
    val result = super.visitFile(declaration)
    currentFile = null
    return result
  }

  override fun visitFunction(declaration: IrFunction): IrStatement {
    val previous = currentFunction
    currentFunction = declaration
    val result = super.visitFunction(declaration)
    currentFunction = previous
    return result
  }

  override fun visitConstructor(declaration: IrConstructor): IrStatement =
      whileConstructing(declaration.parentAsClass) { super.visitConstructor(declaration) }

  override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement =
      whileConstructing(declaration.parentAsClass) { super.visitAnonymousInitializer(declaration) }

  override fun visitField(declaration: IrField): IrStatement =
      whileConstructing(declaration.parent as? IrClass) { super.visitField(declaration) }

  private inline fun <T> whileConstructing(irClass: IrClass?, visit: () -> T): T {
    val receiver = irClass?.thisReceiver?.symbol ?: return visit()
    constructing += receiver
    try {
      return visit()
    } finally {
      constructing.removeAt(constructing.lastIndex)
    }
  }

  override fun visitCall(expression: IrCall): IrExpression {
    val transformed = super.visitCall(expression) as IrCall
    val callee = transformed.symbol.owner
    val declaration = autoDepsDeclaration(callee)
    if (declaration != null) inferDeps(transformed, callee, declaration)
    return transformed
  }

  /**
   * The `@AutoDeps` function [callee] is or overrides, if any.
   *
   * An override does not inherit annotations, so a call that resolves to one -- written out, or the
   * fake override a subclass inherits -- would otherwise skip inference and pass an empty array.
   * Value parameters line up along an override chain, so the declaration's `@Deps` and `@DepsFrom`
   * positions are the call's.
   */
  private fun autoDepsDeclaration(callee: IrSimpleFunction): IrSimpleFunction? {
    val seen = mutableSetOf<IrSimpleFunctionSymbol>()
    val pending = ArrayDeque(listOf(callee))
    while (pending.isNotEmpty()) {
      val function = pending.removeFirst()
      if (!seen.add(function.symbol)) continue
      if (function.hasAnnotation(AUTO_DEPS)) return function
      function.overriddenSymbols.mapTo(pending) { it.owner }
    }
    return null
  }

  override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
    val transformed = super.visitConstructorCall(expression) as IrConstructorCall
    val callee = transformed.symbol.owner
    // The class carries @AutoDeps; its constructor parameters carry @Deps and @DepsFrom.
    if (callee.parentAsClass.hasAnnotation(AUTO_DEPS)) inferDeps(transformed, callee)
    return transformed
  }

  /**
   * `this(…)` and `super(…)`, which are a separate IR node from an ordinary constructor call.
   *
   * A delegation that omits `@Deps` is a call site like any other, and leaving it out would let a
   * secondary constructor or a subclass take the parameter's default -- an empty array, silently,
   * which is the stale-content bug the inference exists to prevent.
   */
  override fun visitDelegatingConstructorCall(
      expression: IrDelegatingConstructorCall,
  ): IrExpression {
    val transformed =
        super.visitDelegatingConstructorCall(expression) as IrDelegatingConstructorCall
    val callee = transformed.symbol.owner
    if (callee.parentAsClass.hasAnnotation(AUTO_DEPS)) inferDeps(transformed, callee)
    return transformed
  }

  /**
   * Infers the omitted `@Deps` argument of [call], which resolved to [callee] and takes its
   * contract from [declaration] -- the same function unless [callee] overrides it.
   *
   * The declaration checker rejects a malformed contract, but only in the compilation that declares
   * it. A declaration compiled without the plugin arrives here unchecked, and skipping it would
   * leave the parameter's empty default in place, so each way it can be malformed is an error at
   * the call instead.
   */
  private fun inferDeps(
      call: IrMemberAccessExpression<*>,
      callee: IrFunction,
      declaration: IrFunction = callee,
  ) {
    val name = declaration.name
    val parameters = declaration.compatValueParameters
    val depsIndex = parameters.indexOfFirst { it.hasAnnotation(DEPS) }
    if (depsIndex < 0) {
      reportMalformed(call, "'$name' is annotated @AutoDeps but has no @Deps parameter")
      return
    }
    if (call.getValueArgumentCompat(depsIndex) != null) return

    // The slot written into is the callee's, which an override may declare differently.
    val slot = callee.compatValueParameters[depsIndex]
    val isVararg = slot.varargElementType != null
    val elementType =
        slot.varargElementType
            ?: slot.type.takeIf { it.isArray() }?.getArrayElementType(pluginContext.irBuiltIns)
    if (elementType?.isNullableAny() != true) {
      reportMalformed(
          call,
          "the @Deps parameter of '$name' is neither 'vararg Any?' nor 'Array<Any?>', so the " +
              "inferred values cannot be written into it",
      )
      return
    }

    val sources = parameters.indices.filter { parameters[it].hasAnnotation(DEPS_FROM) }
    if (sources.isEmpty()) {
      reportMalformed(call, "'$name' has no @DepsFrom parameter to read the dependencies from")
      return
    }
    val omitted = sources.filter { call.getValueArgumentCompat(it) == null }
    if (omitted.isNotEmpty()) {
      reportMalformed(
          call,
          "this call omits @DepsFrom parameter '${parameters[omitted.first()].name}' of '$name', " +
              "whose default value is not here to read",
      )
      return
    }

    val captures =
        analysis.analyse(sources.mapNotNull(call::getValueArgumentCompat), constructing.toSet())
    // A parameter that keeps the default when its argument has no closure to read leaves the call
    // as written: the declaration tells its default apart and compares the argument instead.
    // Every other problem is still reported, from any of the call's arguments.
    val keptDefault =
        captures.problems.filter { problem ->
          problem.kind == CaptureProblem.Kind.NON_LITERAL_TARGET &&
              sources.any { index ->
                parameters[index].keepsDefaultIfNotLiteral &&
                    call.getValueArgumentCompat(index) === problem.element
              }
        }
    captures.problems.filterNot { it in keptDefault }.forEach(::report)
    if (keptDefault.isNotEmpty() || captures.hasErrors) return

    val builder = DeclarationIrBuilder(
        pluginContext,
        currentFunction?.symbol ?: callee.symbol,
        call.startOffset,
        call.endOffset,
    )
    // The dependency expressions are nodes lifted out of the lambda body; the same node cannot
    // appear twice in one tree.
    val elements = captures.deps.map { it.deepCopyWithSymbols() }
    call.putValueArgumentCompat(
        depsIndex,
        if (isVararg) builder.irVararg(pluginContext.irBuiltIns.anyNType, elements)
        else builder.irArrayOfAnyN(elements),
    )
  }

  /** Whether this `@DepsFrom` parameter is declared with `keepsDefaultIfNotLiteral = true`. */
  private val IrValueParameter.keepsDefaultIfNotLiteral: Boolean
    get() = (getAnnotation(DEPS_FROM)?.getValueArgumentCompat(0) as? IrConst)?.value == true

  /**
   * `arrayOf<Any?>(...)`. An [org.jetbrains.kotlin.ir.expressions.IrVararg] is only valid in a
   * vararg slot, so a `@Deps` parameter declared `Array<Any?>` needs the array built explicitly.
   */
  private fun IrBuilderWithScope.irArrayOfAnyN(elements: List<IrExpression>): IrExpression {
    val elementType = pluginContext.irBuiltIns.anyNType
    return irCall(
        pluginContext.irBuiltIns.arrayOf,
        pluginContext.irBuiltIns.arrayClass.typeWith(elementType),
        listOf(elementType),
    )
        .apply { putValueArgumentCompat(0, irVararg(elementType, elements)) }
  }

  private fun reportMalformed(call: IrElement, reason: String) {
    report(
        CaptureProblem(
            CaptureProblem.Kind.MALFORMED_DECLARATION,
            call,
            "$reason. The declaration was compiled without the checks that reject this; fix it " +
                "there, or pass the dependencies explicitly.",
        ),
    )
  }

  private fun report(problem: CaptureProblem) {
    val severity =
        if (problem.kind.isError) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING
    messages.report(severity, "${problem.kind.id}: ${problem.detail}", location(problem.element))
  }

  /**
   * A synthesised node -- an implicit receiver read, anything the frontend generated -- carries a
   * negative offset, which several [IrFileEntry] implementations index with directly. Any negative
   * value is rejected rather than the two named constants: `UNDEFINED_OFFSET` and
   * `SYNTHETIC_OFFSET` are not the only ones the compiler produces, and reporting a problem against
   * the file is always better than crashing the compiler for a position.
   */
  private fun location(element: IrElement): CompilerMessageSourceLocation? {
    val entry = currentFile?.fileEntry ?: return null
    if (element.startOffset < 0) return null
    return CompilerMessageLocation.create(
        entry.name,
        entry.getLineNumber(element.startOffset) + 1,
        entry.getColumnNumber(element.startOffset) + 1,
        null,
    )
  }

  private companion object {
    val AUTO_DEPS: FqName = LithoNames.AutoDeps.asSingleFqName()
    val DEPS: FqName = LithoNames.Deps.asSingleFqName()
    val DEPS_FROM: FqName = LithoNames.DepsFrom.asSingleFqName()
  }
}
