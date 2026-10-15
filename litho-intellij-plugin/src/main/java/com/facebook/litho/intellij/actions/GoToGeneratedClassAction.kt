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

package com.facebook.litho.intellij.actions

import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.PsiSearchUtils
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.LithoLoggerProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiClass

/**
 * User initiated action available for the Spec file. When invoked navigates user to the generated
 * class.
 */
class GoToGeneratedClassAction : AnAction() {

  override fun update(e: AnActionEvent) {
    val component = getNavigatableComponent(e)
    e.presentation.isEnabledAndVisible = component != null
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val component = getNavigatableComponent(e) ?: return
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_GOTO_GENERATED)
    component.navigate(true)
  }

  companion object {
    private fun getNavigatableComponent(e: AnActionEvent): PsiClass? {
      val project = e.project ?: return null
      val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return null
      val specClass =
          LithoPluginUtils.getFirstClass(psiFile, LithoPluginUtils::isLithoSpec) ?: return null
      val qualifiedName = specClass.qualifiedName ?: return null
      val componentName =
          LithoPluginUtils.getLithoComponentNameFromSpec(qualifiedName) ?: return null
      val component =
          PsiSearchUtils.getInstance().findOriginalClass(project, componentName) ?: return null
      if (!component.canNavigate()) {
        return null
      }
      return component
    }
  }
}
