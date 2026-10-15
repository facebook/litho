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

package com.facebook.litho.intellij.navigation

import com.facebook.litho.annotations.PropSetter
import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.intellij.PsiSearchUtils
import com.facebook.litho.intellij.extensions.EventLogger
import com.facebook.litho.intellij.logging.DebounceEventLogger
import com.facebook.litho.intellij.services.ComponentGenerateService
import com.facebook.litho.specmodels.processor.PsiAnnotationProxyUtils
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiTreeUtil
import java.lang.annotation.Annotation as JavaAnnotation
import java.util.Arrays
import java.util.Objects

/** Navigates from Component method to Spec elements. */
class ComponentsMethodDeclarationHandler : GotoDeclarationHandlerBase() {

  override fun getGotoDeclarationTarget(sourceElement: PsiElement?, editor: Editor): PsiElement? {
    val targets = getGotoDeclarationTargets(sourceElement, 0, editor)
    if (targets.isEmpty()) {
      return null
    }
    return targets[0]
  }

  override fun getGotoDeclarationTargets(
      sourceElement: PsiElement?,
      offset: Int,
      editor: Editor,
  ): Array<PsiElement> {
    // Exclusions
    if (
        sourceElement == null ||
            PsiTreeUtil.getParentOfType(sourceElement, PsiImportStatement::class.java) != null
    ) {
      return PsiElement.EMPTY_ARRAY
    }
    val project = sourceElement.project
    return BaseLithoComponentsDeclarationHandler.resolve(sourceElement)
        .filter { it is PsiMethod }
        .map { it as PsiMethod }
        .flatMap { componentMethod ->
          val specCls = getSpecParent(componentMethod, project) ?: return@flatMap null
          @Suppress("UNCHECKED_CAST")
          val propSetter =
              PsiAnnotationProxyUtils.findAnnotationInHierarchy(
                  componentMethod,
                  PropSetter::class.java as Class<JavaAnnotation>,
              ) as? PropSetter
          val result: Array<PsiElement> =
              if (propSetter != null) {
                findSpecProps(propSetter, specCls)
              } else {
                findSpecMethods(componentMethod, specCls)
              }
          if (result.isEmpty()) {
            return@flatMap null
          }
          log(specCls, result)
          Arrays.stream(result)
        }
        .filter(Objects::nonNull)
        .toArray { size -> arrayOfNulls<PsiElement>(size) }
  }

  companion object {
    private val LOGGER: EventLogger = DebounceEventLogger(4_000)

    private fun log(specCls: PsiClass, result: Array<PsiElement>) {
      val data = HashMap<String, String>()
      data[EventLogger.KEY_TARGET] =
          if (result[0] is PsiMethod) {
            EventLogger.VALUE_NAVIGATION_TARGET_METHOD
          } else {
            EventLogger.VALUE_NAVIGATION_TARGET_PARAMETER
          }
      data[EventLogger.KEY_TYPE] = EventLogger.VALUE_NAVIGATION_TYPE_GOTO
      val cls: String =
          if (LithoPluginUtils.isComponentClass(specCls)) {
            EventLogger.VALUE_NAVIGATION_CLASS_COMPONENT
          } else if (LithoPluginUtils.isSectionClass(specCls)) {
            EventLogger.VALUE_NAVIGATION_CLASS_SECTION
          } else {
            "other"
          }
      data[EventLogger.KEY_CLASS] = cls
      LOGGER.log(EventLogger.EVENT_NAVIGATION, data)
    }

    private fun getSpecParent(componentMethod: PsiMethod, project: Project): PsiClass? {
      val containingCls =
          LithoPluginUtils.getFirstClass(
              componentMethod.containingFile,
              LithoPluginUtils::isGeneratedClass,
          )
      if (containingCls == null) {
        return null
      }

      // For Unit testing we don't care about package
      val containingClsName =
          if (ApplicationManager.getApplication().isUnitTestMode) {
            containingCls.name
          } else {
            containingCls.qualifiedName
          }
      val specName =
          LithoPluginUtils.getLithoComponentSpecNameFromComponent(containingClsName) ?: return null
      return PsiSearchUtils.getInstance().findOriginalClass(project, specName)
    }

    private fun findSpecMethods(
        componentMethod: PsiMethod,
        specCls: PsiClass,
    ): Array<PsiElement> {
      if (componentMethod.modifierList.hasModifierProperty(PsiModifier.STATIC)) {
        @Suppress("UNCHECKED_CAST")
        return specCls.findMethodsByName(componentMethod.name, false) as Array<PsiElement>
      }
      @Suppress("UNCHECKED_CAST")
      return PsiMethod.EMPTY_ARRAY as Array<PsiElement>
    }

    private fun findSpecProps(propSetter: PropSetter, specCls: PsiClass): Array<PsiElement> {
      val specModel = ComponentGenerateService.getInstance().getOrCreateSpecModel(specCls)
      if (specModel == null) {
        return PsiElement.EMPTY_ARRAY
      }
      val prop = propSetter.value
      return specModel.props
          .filter { propModel -> prop == propModel.name }
          .mapNotNull { propModel -> propModel.representedObject as? PsiElement }
          .toTypedArray()
    }
  }
}
