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

import com.facebook.litho.intellij.actions.OnEventGenerateAction;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.util.IncorrectOperationException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.Nls;

/** Fix generates new OnEvent method, and replaces argument list in the given method call. */
class OnEventCreateFix extends BaseIntentionAction implements HighPriorityAction {
  private final PsiClass event;
  private final PsiCall methodCall;
  private final String clsName;
  private final PsiClass layoutCls;

  OnEventCreateFix(
      PsiCall methodCall,
      String clsName,
      PsiClass event,
      PsiClass layoutSpecToUpdate,
      String fixDescription) {
    this.event = event;
    this.clsName = clsName;
    this.methodCall = methodCall;
    this.layoutCls = layoutSpecToUpdate;
    setText(fixDescription);
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
        && methodCall.isValid()
        && methodCall.getManager().isInProject(methodCall)
        && methodCall.getArgumentList() != null;
  }

  @Override
  public void invoke(Project project, Editor editor, PsiFile file)
      throws IncorrectOperationException {
    final AtomicReference<PsiMethod> eventMethodRef = new AtomicReference<>();
    final Runnable generateOnEvent =
        () ->
            OnEventGenerateAction.createHandler(
                    (context, eventProject) -> event, eventMethodRef::set)
                .invoke(project, editor, file);
    final Runnable updateArgumentList =
        () ->
            Optional.ofNullable(eventMethodRef.get())
                .map(
                    eventMethod ->
                        AddArgumentFix.createArgumentList(
                            methodCall,
                            clsName,
                            eventMethod.getName(),
                            JavaPsiFacade.getInstance(project).getElementFactory()))
                .ifPresent(argumentList -> methodCall.getArgumentList().replace(argumentList));
    final Runnable action =
        () -> {
          TransactionGuard.getInstance().submitTransactionAndWait(generateOnEvent);
          WriteCommandAction.runWriteCommandAction(project, updateArgumentList);
          LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_FIX_EVENT_HANDLER + ".new");
        };
    final Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode()) {
      action.run();
    } else {
      application.invokeLater(action);
    }
  }
}
