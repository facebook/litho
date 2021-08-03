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

package com.facebook.litho.intellij.actions.templates;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.extensions.ActionPostProcess;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.extensions.TemplateProvider;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;

public class LithoTemplateAction extends CreateFileFromTemplateAction {
  private static final Icon ICON = AllIcons.Nodes.AbstractClass;
  private final String templateName;
  private final String classNameSuffix;
  private final Map<String, FileTemplate> templateMap = new HashMap<>(2);

  static AnAction[] getTemplateActions() {
    return ActionsHolder.ACTIONS;
  }

  LithoTemplateAction(String templateName, String classNameSuffix) {
    super(templateName, templateName, ICON);
    this.templateName = templateName;
    this.classNameSuffix = classNameSuffix;
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return "Creating " + templateName;
  }

  @Override
  protected void buildDialog(
      Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder.setTitle("New " + templateName);
    final FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
    addKind(builder, templateManager, ".java", "Java");
    addKind(builder, templateManager, ".kt", "Kotlin");
  }

  private void addKind(
      CreateFileFromTemplateDialog.Builder builder,
      FileTemplateManager templateManager,
      String extension,
      String kind) {
    final String templateName = this.templateName + extension;
    templateMap.putIfAbsent(templateName, templateManager.findInternalTemplate(templateName));
    if (templateMap.get(templateName) != null) {
      builder.addKind(kind, ICON, templateName);
    }
  }

  @Override
  protected PsiFile createFile(String name, String templateName, PsiDirectory dir) {
    // Template adds Spec suffix, avoiding SpecSpec
    if (!classNameSuffix.isEmpty() && name.endsWith(classNameSuffix)) {
      name = name.substring(0, name.length() - classNameSuffix.length());
    } else if (LithoPluginUtils.isSpecName(name)) {
      name = LithoPluginUtils.getLithoComponentNameFromSpec(name);
    }
    // templateMap is populated in #buildDialog step
    final PsiFile file = super.createFileFromTemplate(name, templateMap.get(templateName), dir);
    // Kotlin class isn't adjusted to be the same as Kotlin filename, which Java is. Hence, we need
    // to make the class and file names the same
    if (templateName.endsWith(".kt")) {
      final String nameWithoutSuffix = name;
      WriteAction.run(
          () -> {
            file.setName(nameWithoutSuffix.concat(classNameSuffix).concat(".kt"));
          });
    }
    return file;
  }

  /** Generates component for createdElement */
  @Override
  protected void postProcess(
      PsiFile createdElement, String templateName, Map<String, String> customProperties) {
    super.postProcess(createdElement, templateName, customProperties);
    PostProcessesHolder.postProcesses(createdElement, templateName);
    final Map<String, String> data = new HashMap<>();
    data.put(EventLogger.KEY_TYPE, templateName);
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_NEW_TEMPLATE, data);
  }

  private static class ActionsHolder {
    private static final ExtensionPointName<TemplateProvider> EP_NAME =
        ExtensionPointName.create("com.facebook.litho.intellij.templateProvider");
    private static final AnAction[] ACTIONS =
        Arrays.stream(EP_NAME.getExtensions())
            .map(
                provider ->
                    new LithoTemplateAction(
                        provider.getTemplateName(), provider.getClassNameSuffix()))
            .toArray(AnAction[]::new);
  }

  private static class PostProcessesHolder {
    private static final ExtensionPointName<ActionPostProcess> EP_NAME =
        ExtensionPointName.create("com.facebook.litho.intellij.actionPostProcess");

    private static void postProcesses(PsiFile createdElement, String templateName) {
      Arrays.stream(EP_NAME.getExtensions())
          .forEach(provider -> provider.postProcess(createdElement, templateName));
    }
  }
}
