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

import com.facebook.litho.intellij.LithoPluginUtils
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

class UppercaseStatePropInspection : AbstractBaseJavaLocalInspectionTool() {

  override fun checkMethod(
      method: PsiMethod,
      manager: InspectionManager,
      isOnTheFly: Boolean,
  ): Array<ProblemDescriptor>? {
    if (!LithoPluginUtils.isLithoSpec(method.containingClass)) {
      return ProblemDescriptor.EMPTY_ARRAY
    }
    val problems =
        method.parameterList.parameters
            .filter { LithoPluginUtils.isPropOrState(it) }
            .filter { isFirstLetterCapital(it) }
            .mapNotNull { it.nameIdentifier }
            .map { createWarning(it, manager, isOnTheFly) }
            .toTypedArray()
    return if (problems.isEmpty()) {
      ProblemDescriptor.EMPTY_ARRAY
    } else {
      problems
    }
  }

  override fun getStaticDescription(): String {
    return "Litho @Prop and @State names should begin with a lowercase letter " +
        "to avoid confusion when `Type name` and `Type Name` represent different elements."
  }

  companion object {
    private fun isFirstLetterCapital(parameter: PsiParameter): Boolean {
      return isFirstLetterCapital(parameter.name)
    }

    private fun isFirstLetterCapital(text: String?): Boolean {
      if (text.isNullOrEmpty()) {
        return false
      }
      return text[0].isUpperCase()
    }

    private fun createWarning(
        element: PsiElement,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): ProblemDescriptor {
      return manager.createProblemDescriptor(
          element,
          "Should not be capitalized: ${element.text}",
          true, /* show tooltip */
          ProblemHighlightType.ERROR,
          isOnTheFly,
      )
    }
  }
}
