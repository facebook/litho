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
import com.facebook.litho.intellij.completion.ComponentGenerateUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import java.util.Map;

public class NewSpecTemplateAction extends CreateFileFromTemplateAction {

  private static final String TITLE = "New Litho Component";

  public NewSpecTemplateAction() {
    super(null, TITLE, null);
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return "NewSpecTemplateAction";
  }

  @Override
  protected void buildDialog(
      Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder
        .setTitle(TITLE)
        .addKind("Layout Component", AllIcons.Nodes.AbstractClass, "LayoutSpec")
        .addKind("GroupSection Component", AllIcons.Nodes.AbstractClass, "GroupSectionSpec")
        .addKind("Mount Component", AllIcons.Nodes.AbstractClass, "MountSpec");
  }

  @Override
  protected void postProcess(
      PsiFile createdElement, String templateName, Map<String, String> customProperties) {
    super.postProcess(createdElement, templateName, customProperties);
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_NEW_TEMPLATE + "." + templateName);
    LithoPluginUtils.getFirstClass(createdElement, LithoPluginUtils::isLayoutSpec)
        .ifPresent(ComponentGenerateUtils::updateLayoutComponent);
  }
}
