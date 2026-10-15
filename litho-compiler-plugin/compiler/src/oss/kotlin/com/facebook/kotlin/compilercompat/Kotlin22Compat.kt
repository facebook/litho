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

// The internal build gets these from a compat library that spans several Kotlin versions. The open
// source build pins Kotlin 2.2, so it implements them directly. Buck never compiles this file.
@file:OptIn(
    org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi::class,
    org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class,
)

package com.facebook.kotlin.compilercompat

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrorsDefaultMessages
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.FqName

abstract class CompilerPluginRegistrarCompat : CompilerPluginRegistrar() {
  abstract val pluginIdCompat: String
}

abstract class FirFunctionCallCheckerCompat : FirFunctionCallChecker(MppCheckerKind.Common) {
  abstract override fun check(
      expression: FirFunctionCall,
      context: CheckerContext,
      reporter: DiagnosticReporter,
  )
}

// The plugin names this type through the compat API, where it differs between Kotlin versions.
// ast-grep-ignore: common/kotlin/no-new-typealias-warn
typealias FirNamedFunctionCompat = FirSimpleFunction

abstract class KtDiagnosticsContainerCompat {
  abstract fun getRendererFactory(): BaseDiagnosticRendererFactory

  init {
    RootDiagnosticRendererFactory.registerFactory(getRendererFactory())
  }
}

// Kotlin 2.2 validates `-Xwarning-level` names against the registered renderer factories, and the
// built-in one only registers itself on first use.
fun registerDefaultDiagnosticsCompat() {
  RootDiagnosticRendererFactory.registerFactory(FirErrorsDefaultMessages)
}

fun buildKtDiagnosticFactoryToRendererMap(
    name: String,
    init: (KtDiagnosticFactoryToRendererMap) -> Unit,
): Lazy<KtDiagnosticFactoryToRendererMap> = lazy {
  KtDiagnosticFactoryToRendererMap(name).also(init)
}

val IrCall.compatExtensionReceiver: IrExpression?
  get() = extensionReceiver

val IrClass.compatFqNameForIrSerialization: FqName
  get() = fqNameForIrSerialization

val IrFunction.compatValueParameters: List<IrValueParameter>
  get() = valueParameters

fun <S : IrSymbol> IrMemberAccessExpression<S>.compatPutValueArgument(
    index: Int,
    valueArgument: IrExpression?,
) {
  putValueArgument(index, valueArgument)
}
