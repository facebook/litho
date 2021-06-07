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

package com.facebook.litho.intellij.redsymbols;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.facebook.litho.intellij.services.ComponentGenerateService;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Updates Component file for the given Spec file. Update logic is defined by the {@link
 * ComponentGenerateService}. Works with {@link LayoutSpec}, {@link MountSpec}, and {@link
 * GroupSectionSpec}.
 */
class GenerateComponentAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    Optional<PsiClass> specCls = getValidSpec(e);
    e.getPresentation().setEnabledAndVisible(specCls.isPresent());
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Map<String, String> eventMetadata = new HashMap<>();
    getValidSpec(e)
        .ifPresent(
            cls -> {
              final Project project = cls.getProject();
              final Runnable job =
                  () -> {
                    // Force model update
                    ComponentGenerateService.getInstance().getOrCreateSpecModel(cls, false);
                    final PsiClass component = FileGenerateUtils.generateClass(cls);
                    if (component != null) {
                      LithoPluginUtils.showInfo(component.getName() + " was regenerated", project);
                    }
                  };
              DumbService.getInstance(project).smartInvokeLater(job);
              final PsiJavaFile file = (PsiJavaFile) cls.getContainingFile();
              eventMetadata.put(EventLogger.KEY_FILE, file.getPackageName() + "." + file.getName());
            });
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_GENERATE_COMPONENT, eventMetadata);
  }

  /**
   * @return {@link Optional} with {@link PsiClass} from {@link AnActionEvent} if it's a valid class
   *     for which GenerateComponent action is enabled. Or empty {@link Optional} otherwise.
   */
  @VisibleForTesting
  static Optional<PsiClass> getValidSpec(AnActionEvent e) {
    return Optional.of(e)
        .map(event -> event.getData(CommonDataKeys.PSI_FILE))
        .flatMap(psiFile -> LithoPluginUtils.getFirstClass(psiFile, LithoPluginUtils::isLithoSpec));
  }
}
