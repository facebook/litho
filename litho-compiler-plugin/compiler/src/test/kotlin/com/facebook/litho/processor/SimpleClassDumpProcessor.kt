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

package com.facebook.litho.processor

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.Name

@OptIn(UnsafeDuringIrConstructionAPI::class)
class SimpleClassDumpProcessor : AbstractTestProcessor() {

  override fun StringBuilder.analyse(data: AnalysisData) {
    data.file.declarations.filterIsInstance<IrClass>().forEach { analyseClass(it) }
  }

  private fun StringBuilder.analyseClass(clas: IrClass, indentLevel: Int = 0) {
    appendIndent(indentLevel).appendLine("class ${clas.name}")
    clas.declarations
        .sortedBy { (it as? IrDeclarationWithName)?.name ?: Name.identifier("") }
        .filterNot { it.isFakeOverride }
        .forEach { declaration ->
          when (declaration) {
            is IrSimpleFunction ->
                appendIndent(indentLevel + 1)
                    .append("fun ${declaration.name.identifier}(): ")
                    .appendLine(declaration.returnType.classOrNull?.owner?.name)
            is IrProperty ->
                appendIndent(indentLevel + 1)
                    .append(if (declaration.isVar) "var" else "val")
                    .append(" ")
                    .append(declaration.name)
                    .append(": ")
                    .appendLine(
                        declaration.backingField?.type?.classOrNull?.owner?.name
                            ?: declaration.getter?.returnType?.classOrNull?.owner?.name)
            is IrClass -> analyseClass(declaration, indentLevel + 1)
            else -> Unit
          }
        }
  }

  private fun StringBuilder.appendIndent(level: Int): StringBuilder = apply {
    if (level > 0) append(" ".repeat(level * 2))
  }
}
