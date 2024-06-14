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

package com.facebook.litho

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class LithoFileVisitor(
    private val file: KtFile,
    private val bindingContext: BindingContext,
    private val reporter: (String, CompilerMessageSourceLocation) -> Unit
) : KtVisitorVoid() {
  override fun visitElement(element: PsiElement) {
    element.acceptChildren(this)
  }

  override fun visitCallExpression(expression: KtCallExpression) {
    if (expression.text.contains("test")) {
      expression.report("Test function called")
    }
  }

  private fun KtElement.report(message: String) {
    location()?.let { reporter(message, it) }
  }

  private fun KtElement.location(): CompilerMessageLocation? {
    val lineAndColumn =
        PsiDiagnosticUtils.offsetToLineAndColumn(file.viewProvider.document, startOffset)
    return CompilerMessageLocation.create(
        file.virtualFilePath, lineAndColumn.line, lineAndColumn.column, lineAndColumn.lineContent)
  }
}
