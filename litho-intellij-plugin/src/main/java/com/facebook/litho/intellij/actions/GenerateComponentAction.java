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

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.completion.ComponentGenerateUtils;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiClass;
import java.util.Optional;

/**
 * Updates Component file for the given Spec file. Update logic is defined by the {@link
 * ComponentGenerateUtils}. Currently works with the {@link LayoutSpec} only.
 */
public class GenerateComponentAction extends AnAction {

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    Optional<PsiClass> specCls = getValidSpec(e);
    e.getPresentation().setEnabledAndVisible(specCls.isPresent());

    specCls.ifPresent(
        cls ->
            e.getPresentation()
                .setText(
                    "Regenerate "
                        + LithoPluginUtils.getLithoComponentNameFromSpec(cls.getName())
                        + " Component"));
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    getValidSpec(e).ifPresent(ComponentGenerateUtils::updateLayoutComponent);
  }

  /**
   * @return {@link Optional} with {@link PsiClass} from {@link AnActionEvent} if it's a valid class
   *     for which GenerateComponent action is enabled. Or empty {@link Optional} otherwise.
   */
  @VisibleForTesting
  static Optional<PsiClass> getValidSpec(AnActionEvent e) {
    return Optional.of(e)
        .map(event -> event.getData(CommonDataKeys.PSI_FILE))
        .flatMap(LithoPluginUtils::getFirstLayoutSpec);
  }
}
