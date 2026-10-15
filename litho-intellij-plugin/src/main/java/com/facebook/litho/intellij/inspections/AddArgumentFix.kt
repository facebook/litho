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

package com.facebook.litho.intellij.inspections

import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.util.IncorrectOperationException

/** Fix replaces argument list in the given method call. */
class AddArgumentFix
private constructor(
    private val originalCall: PsiCall,
    private val newArgumentList: PsiExpressionList,
    description: String,
) : BaseIntentionAction(), HighPriorityAction {

  init {
    text = description
  }

  override fun getFamilyName(): String {
    return MethodCallAnnotator.FIX_FAMILY_NAME
  }

  override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
    // Copied from PermuteArgumentsFix
    return !project.isDisposed &&
        originalCall.isValid &&
        originalCall.manager.isInProject(originalCall) &&
        originalCall.argumentList != null
  }

  @Throws(IncorrectOperationException::class)
  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    val argumentList = originalCall.argumentList
    if (argumentList != null) {
      argumentList.replace(newArgumentList)
      val offset = argumentList.lastChild.textOffset - 1
      // Move cursor before the ')'.
      editor.caretModel.moveToOffset(offset)
      LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_FIX_EVENT_HANDLER)
    }
  }

  companion object {
    /** Creates new fix, that adds static method call as an argument to the originalMethodCall. */
    @JvmStatic
    fun createAddMethodCallFix(
        originalMethodCall: PsiMethodCallExpression,
        clsName: String,
        methodName: String,
        elementFactory: PsiElementFactory,
    ): IntentionAction {
      val newArgumentList =
          createArgumentList(originalMethodCall.context, clsName, methodName, elementFactory)
      val fixDescription = "Add .$methodName() ${getCapitalizedMethoName(originalMethodCall)}"
      return AddArgumentFix(originalMethodCall, newArgumentList, fixDescription)
    }

    @JvmStatic
    fun getCapitalizedMethoName(methodCall: PsiMethodCallExpression): String {
      return StringUtil.capitalize(methodCall.methodExpression.referenceName.orEmpty())
    }

    @JvmStatic
    fun createArgumentList(
        context: PsiElement?,
        clsName: String,
        methodName: String,
        elementFactory: PsiElementFactory,
    ): PsiExpressionList {
      val stub =
          elementFactory.createExpressionFromText("methodName($clsName.$methodName())", context)
              as PsiMethodCallExpression
      return stub.argumentList
    }
  }
}
