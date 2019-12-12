/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.intellij.inspections;

import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;

/** Fix replaces argument list in the given method call. */
public class AddArgumentFix extends BaseIntentionAction implements HighPriorityAction {
  private final PsiExpressionList newArgumentList;
  private final PsiCall originalCall;

  private AddArgumentFix(
      PsiCall originalCall, PsiExpressionList newArgumentList, String description) {
    this.originalCall = originalCall;
    this.newArgumentList = newArgumentList;
    setText(description);
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @Override
  public String getFamilyName() {
    return EventHandlerAnnotator.FIX_FAMILY_NAME;
  }

  @Override
  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    // Copied from PermuteArgumentsFix
    return !project.isDisposed()
        && originalCall.isValid()
        && originalCall.getManager().isInProject(originalCall)
        && originalCall.getArgumentList() != null;
  }

  @Override
  public void invoke(Project project, Editor editor, PsiFile file)
      throws IncorrectOperationException {
    originalCall.getArgumentList().replace(newArgumentList);
    int offset = originalCall.getArgumentList().getLastChild().getTextOffset() - 1;
    // Move cursor before the ')'.
    editor.getCaretModel().moveToOffset(offset);
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_FIX_EVENT_HANDLER);
  }

  /** Creates new fix, that adds static method call as an argument to the originalMethodCall. */
  static IntentionAction createAddMethodCallFix(
      PsiMethodCallExpression originalMethodCall,
      String clsName,
      String methodName,
      PsiElementFactory elementFactory) {
    PsiExpressionList newArgumentList =
        createArgumentList(originalMethodCall.getContext(), clsName, methodName, elementFactory);
    String fixDescription =
        "Add ." + methodName + "() " + getCapitalizedMethoName(originalMethodCall);
    return new AddArgumentFix(originalMethodCall, newArgumentList, fixDescription);
  }

  /**
   * Creates new fix, that generates OnEvent method and adds static method call as an argument to
   * the originalMethodCall.
   */
  static IntentionAction createNewMethodCallFix(
      PsiMethodCallExpression originalMethodCall,
      String clsName,
      PsiClass event,
      PsiClass parentLayoutSpec) {
    String fixDescription = "Create new " + getCapitalizedMethoName(originalMethodCall);
    return new OnEventCreateFix(
        originalMethodCall, clsName, event, parentLayoutSpec, fixDescription);
  }

  static String getCapitalizedMethoName(PsiMethodCallExpression methodCall) {
    return StringUtil.capitalize(methodCall.getMethodExpression().getReferenceName());
  }

  static PsiExpressionList createArgumentList(
      PsiElement context, String clsName, String methodName, PsiElementFactory elementFactory) {
    final PsiMethodCallExpression stub =
        (PsiMethodCallExpression)
            elementFactory.createExpressionFromText(
                "methodName(" + clsName + "." + methodName + "())", context);
    return stub.getArgumentList();
  }
}
