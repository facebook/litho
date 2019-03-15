/*
 * Copyright 2004-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij.inspections;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UppercaseStatePropInspection extends AbstractBaseJavaLocalInspectionTool {

  @Nullable
  @Override
  public ProblemDescriptor[] checkMethod(
      @NotNull PsiMethod method, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!LithoPluginUtils.isLithoSpec(method.getContainingClass())) {
      return ProblemDescriptor.EMPTY_ARRAY;
    }
    return Optional.of(method)
        .map(PsiMethod::getParameterList)
        .map(PsiParameterList::getParameters)
        .map(
            psiParameters ->
                Stream.of(psiParameters)
                    .filter(LithoPluginUtils::isPropOrState)
                    .filter(UppercaseStatePropInspection::isFirstLetterCapital)
                    .map(PsiParameter::getNameIdentifier)
                    .filter(Objects::nonNull)
                    .map(identifier -> createWarning(identifier, manager, isOnTheFly))
                    .toArray(ProblemDescriptor[]::new))
        .orElse(ProblemDescriptor.EMPTY_ARRAY);
  }

  @Nullable
  @Override
  public String getStaticDescription() {
    return "Litho @Prop and @State names should begin with a lowercase letter "
        + "to avoid confusion when `Type name` and `Type Name` represent different elements.";
  }

  private static boolean isFirstLetterCapital(PsiParameter psiParameter) {
    return isFirstLetterCapital(psiParameter.getName());
  }

  private static boolean isFirstLetterCapital(@Nullable String aText) {
    return Optional.ofNullable(aText)
        .filter(text -> text.length() > 0)
        .map(text -> text.charAt(0))
        .filter(Character::isUpperCase)
        .isPresent();
  }

  private static ProblemDescriptor createWarning(
      PsiElement element, InspectionManager manager, boolean isOnTheFly) {
    return manager.createProblemDescriptor(
        element,
        "Should not be capitalized: " + element.getText(),
        true /* show tooltip */,
        ProblemHighlightType.ERROR,
        isOnTheFly);
  }
}
