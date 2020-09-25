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

package com.facebook.litho.intellij.actions;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.completion.OnEventGenerateUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.codeInsight.generation.PsiGenerationInfo;
import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.ide.util.TreeJavaClassChooserDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Generates a method handling Litho event in the Litho Spec.
 * https://fblitho.com/docs/events-overview
 */
public class OnEventGenerateAction extends BaseGenerateAction {

  public interface EventChooser {
    PsiClass choose(PsiClass context, Project project);
  }

  @Nullable
  public interface OnEventRefactorer {
    PsiMethod changeSignature(Project project, PsiMethod originalOnEventMethod, PsiClass context);
  }

  public interface OnEventGeneratedListener {
    void onGenerated(PsiMethod onEvent);
  }

  public static CodeInsightActionHandler createHandler(
      EventChooser eventChooser, OnEventGeneratedListener onEventGeneratedListener) {
    return new OnEventGenerateHandler(
        eventChooser,
        (project, originalOnEventMethod, context) -> {
          if (ApplicationManager.getApplication().isUnitTestMode()) {
            return originalOnEventMethod;
          }
          final OnEventChangeSignatureDialog onEventMethodSignatureChooser =
              new OnEventChangeSignatureDialog(project, originalOnEventMethod, context);
          onEventMethodSignatureChooser.show();
          return onEventMethodSignatureChooser.getMethod();
        },
        onEventGeneratedListener);
  }

  public OnEventGenerateAction() {
    super(
        createHandler(
            (context, project) -> {
              // Choose event to generate method for
              final TreeJavaClassChooserDialog chooseEventDialog =
                  new TreeJavaClassChooserDialog(
                      "Choose Event",
                      project,
                      GlobalSearchScope.allScope(project),
                      LithoPluginUtils::isEvent,
                      context /* Any initial class */);
              chooseEventDialog.show();
              return chooseEventDialog.getSelected();
            },
            onEventMethod ->
                LithoLoggerProvider.getEventLogger()
                    .log(EventLogger.EVENT_ON_EVENT_GENERATION + ".success")));
  }

  @Override
  public void update(AnActionEvent e) {
    // Applies visibility of the Generate Action group
    super.update(e);
    final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    if (!LithoPluginUtils.isLithoSpec(file)) {
      e.getPresentation().setEnabledAndVisible(false);
    }
  }

  /**
   * Generates Litho event method. Prompts the user for additional data: choose Event class and
   * method signature customisation.
   *
   * @see com.facebook.litho.intellij.completion.MethodGenerateHandler
   */
  static class OnEventGenerateHandler extends GenerateMembersHandlerBase {
    private final EventChooser eventChooser;
    private final OnEventGeneratedListener onEventGeneratedListener;
    private final OnEventRefactorer onEventRefactorer;

    OnEventGenerateHandler(
        EventChooser eventChooser,
        OnEventRefactorer onEventRefactorer,
        OnEventGeneratedListener onEventGeneratedListener) {
      super("");
      this.eventChooser = eventChooser;
      this.onEventGeneratedListener = onEventGeneratedListener;
      this.onEventRefactorer = onEventRefactorer;
    }

    /** @return method based on user choice. */
    @Override
    protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project) {
      return Optional.ofNullable(eventChooser.choose(aClass, project))
          .map(
              eventClass ->
                  OnEventGenerateUtils.createOnEventMethod(
                      aClass, eventClass, Collections.emptyList()))
          .map(
              customMethod -> {
                OnEventGenerateUtils.addComment(aClass, customMethod);
                onEventGeneratedListener.onGenerated(customMethod);
                return new ClassMember[] {new PsiMethodMember(customMethod)};
              })
          .orElse(ClassMember.EMPTY_ARRAY);
    }

    @Override
    protected GenerationInfo[] generateMemberPrototypes(PsiClass psiClass, ClassMember classMember)
        throws IncorrectOperationException {
      return generateMemberPrototypes(psiClass, new ClassMember[] {classMember})
          .toArray(GenerationInfo.EMPTY_ARRAY);
    }

    /** @return a list of objects to insert into generated code. */
    @NotNull
    @Override
    protected List<? extends GenerationInfo> generateMemberPrototypes(
        PsiClass aClass, ClassMember[] members) throws IncorrectOperationException {
      final List<GenerationInfo> prototypes = new ArrayList<>();
      for (ClassMember member : members) {
        if (member instanceof PsiMethodMember) {
          PsiMethodMember methodMember = (PsiMethodMember) member;
          prototypes.add(new PsiGenerationInfo<>(methodMember.getElement()));
        }
      }
      return prototypes;
    }

    @Override
    protected ClassMember[] getAllOriginalMembers(PsiClass psiClass) {
      return ClassMember.EMPTY_ARRAY;
    }
  }
}
