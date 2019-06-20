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

import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;

public class NewComponentTemplateAction extends CreateFileFromTemplateAction {

  private static final String TITLE = "New Litho Component";

  public NewComponentTemplateAction() {
    super(null, TITLE, null);
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return "NewComponentTemplateAction";
  }

  @Override
  protected void buildDialog(
      Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_NEW_TEMPLATE);

    builder
        .setTitle(TITLE)
        .addKind("Layout Component", AllIcons.Nodes.AbstractClass, "LayoutSpec")
        .addKind("GroupSection Component", AllIcons.Nodes.AbstractClass, "GroupSectionSpec")
        .addKind("Mount Component", AllIcons.Nodes.AbstractClass, "MountSpec");
  }
}
