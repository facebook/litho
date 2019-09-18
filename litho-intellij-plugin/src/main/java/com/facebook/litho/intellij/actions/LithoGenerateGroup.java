/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.litho.intellij.actions;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.psi.PsiFile;
import java.util.Optional;

public final class LithoGenerateGroup extends DefaultActionGroup {

  @Override
  public void update(AnActionEvent e) {
    Optional<PsiFile> component =
        Optional.of(e)
            .map(event -> event.getData(CommonDataKeys.PSI_FILE))
            .filter(LithoPluginUtils::isLithoSpec);
    e.getPresentation().setEnabledAndVisible(component.isPresent());
  }
}
