/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.intellij.actions.templates

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.extensions.ActionPostProcess
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.extensions.TemplateProvider
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import javax.swing.Icon

class LithoTemplateAction
internal constructor(
    private val templateName: String,
    private val classNameSuffix: String,
    private val templatePriority: Int,
) : CreateFileFromTemplateAction(templateName, templateName, ICON) {

  private val templateMap: MutableMap<String, FileTemplate> = HashMap(2)

  override fun getActionName(
      directory: PsiDirectory,
      newName: String,
      templateName: String,
  ): String {
    return "Creating $templateName"
  }

  override fun buildDialog(
      project: Project,
      directory: PsiDirectory,
      builder: CreateFileFromTemplateDialog.Builder,
  ) {
    builder.setTitle("New $templateName")
    val templateManager = FileTemplateManager.getInstance(project)
    addKind(builder, templateManager, ".java", "Java")
    addKind(builder, templateManager, ".kt", "Kotlin")
  }

  private fun addKind(
      builder: CreateFileFromTemplateDialog.Builder,
      templateManager: FileTemplateManager,
      extension: String,
      kind: String,
  ) {
    val fullTemplateName = templateName + extension
    templateMap.putIfAbsent(
        fullTemplateName,
        templateManager.findInternalTemplate(fullTemplateName),
    )
    if (templateMap[fullTemplateName] != null) {
      builder.addKind(kind, ICON, fullTemplateName)
    }
  }

  override fun createFile(name: String, templateName: String, dir: PsiDirectory): PsiFile {
    // Template adds Spec suffix, avoiding SpecSpec
    var adjustedName = name
    if (classNameSuffix.isNotEmpty() && name.endsWith(classNameSuffix)) {
      adjustedName = name.substring(0, name.length - classNameSuffix.length)
    } else if (LithoPluginUtils.isSpecName(name)) {
      // Fallback to original name if getLithoComponentNameFromSpec returns null.
      // isSpecName() guards this path so null is unlikely, but these are separate methods
      // with no contract guaranteeing non-null. Using ?: keeps behavior close to the original
      // Java which would silently assign null (vs requireNotNull which would crash).
      adjustedName = LithoPluginUtils.getLithoComponentNameFromSpec(name) ?: name
    }
    // templateMap is populated in #buildDialog step.
    // templateMap[templateName] may return null — this matches the original Java behavior
    // (NULLSAFE_FIXME[Parameter Not Nullable]). The template is always present in practice
    // because buildDialog populates the map before createFile is called by the framework.
    val file = super.createFileFromTemplate(adjustedName, templateMap[templateName], dir)
    // Kotlin class isn't adjusted to be the same as Kotlin filename, which Java is. Hence, we need
    // to make the class and file names the same
    if (templateName.endsWith(".kt")) {
      val nameWithoutSuffix = adjustedName
      // file is a platform type (Java return) — null would NPE here same as the original Java.
      // createFileFromTemplate should not return null in practice since the template exists.
      WriteAction.run<RuntimeException> {
        file.setName(nameWithoutSuffix + classNameSuffix + ".kt")
      }
    }
    return file
  }

  /** Generates component for createdElement */
  override fun postProcess(
      createdElement: PsiFile,
      templateName: String,
      customProperties: Map<String, String>,
  ) {
    super.postProcess(createdElement, templateName, customProperties)
    PostProcessesHolder.postProcesses(createdElement, templateName)
    val data = HashMap<String, String>()
    data[EventLogger.KEY_TYPE] = templateName
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_NEW_TEMPLATE, data)
  }

  private object ActionsHolder {
    val EP_NAME: ExtensionPointName<TemplateProvider> =
        ExtensionPointName.create("com.facebook.litho.intellij.templateProvider")

    val ACTIONS: Array<AnAction> = getActionsArray()

    private fun getActionsArray(): Array<AnAction> {
      val actions =
          EP_NAME.extensions
              .map { provider ->
                LithoTemplateAction(
                    provider.getTemplateName(),
                    provider.getClassNameSuffix(),
                    provider.getTemplatePriority(),
                )
              }
              .sortedBy { it.templatePriority }

      return addDivider(actions)
    }

    private fun addDivider(actions: List<LithoTemplateAction>): Array<AnAction> {
      val actionsList = ArrayList<AnAction>(actions)
      var indexToAdd = 0
      for (action in actionsList) {
        if ((action as LithoTemplateAction).templatePriority > 0) {
          break
        }
        indexToAdd++
      }
      actionsList.add(indexToAdd, Separator.create())
      return actionsList.toTypedArray()
    }
  }

  private object PostProcessesHolder {
    val EP_NAME: ExtensionPointName<ActionPostProcess> =
        ExtensionPointName.create("com.facebook.litho.intellij.actionPostProcess")

    fun postProcesses(createdElement: PsiFile, templateName: String) {
      EP_NAME.extensions.forEach { provider -> provider.postProcess(createdElement, templateName) }
    }
  }

  companion object {
    private val ICON: Icon = AllIcons.Nodes.AbstractClass

    @JvmStatic
    fun getTemplateActions(): Array<AnAction> {
      return ActionsHolder.ACTIONS
    }
  }
}
