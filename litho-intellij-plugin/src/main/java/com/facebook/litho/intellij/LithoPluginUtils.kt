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

package com.facebook.litho.intellij

import com.facebook.litho.annotations.Event
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.State
import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.infos.MethodCandidateInfo
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil

class LithoPluginUtils {

  companion object {
    private val NOTIFICATION_GROUP: NotificationGroup = NotificationGroup.balloonGroup("Litho")
    private const val SPEC_SUFFIX = "Spec"
    private const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"

    @JvmStatic
    fun isComponentClass(psiClass: PsiClass?): Boolean {
      return psiClass != null &&
          psiClass.superClass != null &&
          ("SpecGeneratedComponent" == psiClass.superClass?.name ||
              "com.facebook.litho.Component" == psiClass.superClass?.qualifiedName)
    }

    @JvmStatic
    fun isSectionClass(psiClass: PsiClass?): Boolean {
      return psiClass?.superClass?.qualifiedName == LithoClassNames.SECTION_CLASS_NAME
    }

    @JvmStatic
    fun isGeneratedClass(psiClass: PsiClass?): Boolean {
      return isComponentClass(psiClass) || isSectionClass(psiClass)
    }

    @JvmStatic
    fun isLithoSpec(psiFile: PsiFile?): Boolean {
      if (psiFile == null) {
        return false
      }
      return getFirstClass(psiFile) { isLithoSpec(it) } != null
    }

    @JvmStatic
    fun isLithoSpec(psiClass: PsiClass?): Boolean {
      return psiClass != null &&
          (hasLithoComponentSpecAnnotation(psiClass) || hasLithoSectionSpecAnnotation(psiClass))
    }

    @JvmStatic
    fun isLayoutSpec(psiClass: PsiClass?): Boolean {
      return psiClass != null && hasAnnotation(psiClass, equals(LayoutSpec::class.java.name))
    }

    @JvmStatic
    fun isMountSpec(psiClass: PsiClass?): Boolean {
      return psiClass != null && hasAnnotation(psiClass, equals(MountSpec::class.java.name))
    }

    @JvmStatic
    fun hasLithoComponentSpecAnnotation(psiClass: PsiClass?): Boolean {
      return psiClass != null &&
          isSpecName(psiClass.name) &&
          (isLayoutSpec(psiClass) || isMountSpec(psiClass))
    }

    @JvmStatic
    fun hasLithoSectionSpecAnnotation(psiClass: PsiClass?): Boolean {
      return psiClass != null &&
          isSpecName(psiClass.name) &&
          hasAnnotation(psiClass, startsWith("com.facebook.litho.sections.annotations"))
    }

    /** @return true if given name ends with "Spec". */
    @JvmStatic
    fun isSpecName(clsName: String?): Boolean {
      return clsName != null && clsName.endsWith(SPEC_SUFFIX)
    }

    @JvmStatic
    @VisibleForTesting
    fun hasAnnotation(
        modifierListOwner: PsiModifierListOwner,
        nameFilter: (String) -> Boolean,
    ): Boolean {
      val annotations = modifierListOwner.annotations
      for (annotation in annotations) {
        val qualifiedName = annotation.qualifiedName ?: continue
        if (nameFilter(qualifiedName)) {
          return true
        }
      }
      return false
    }

    @JvmStatic
    @VisibleForTesting
    fun startsWith(prefix: String): (String) -> Boolean {
      return { name -> name.startsWith(prefix) }
    }

    private fun equals(text: String): (String) -> Boolean {
      return { name -> name == text }
    }

    @JvmStatic
    fun isPropOrState(parameter: PsiParameter): Boolean {
      return isProp(parameter) || isState(parameter)
    }

    @JvmStatic
    fun isProp(parameter: PsiParameter): Boolean {
      return hasAnnotation(parameter, equals(Prop::class.java.name))
    }

    @JvmStatic
    fun isState(parameter: PsiParameter): Boolean {
      return hasAnnotation(parameter, equals(State::class.java.name))
    }

    @JvmStatic
    fun isParam(parameter: PsiParameter): Boolean {
      return hasAnnotation(parameter, equals(Param::class.java.name))
    }

    @JvmStatic
    fun isPropDefault(field: PsiField): Boolean {
      return hasAnnotation(field, equals(PropDefault::class.java.name))
    }

    @JvmStatic
    fun isEvent(psiClass: PsiClass): Boolean {
      return hasAnnotation(psiClass, equals(Event::class.java.name))
    }

    @JvmStatic
    fun getLithoComponentNameFromSpec(specName: String?): String? {
      if (specName != null && isSpecName(specName)) {
        return specName.substring(0, specName.length - SPEC_SUFFIX.length)
      }
      return null
    }

    @JvmStatic
    fun getLithoComponentSpecNameFromComponent(componentName: String?): String? {
      if (componentName != null) {
        return componentName + SPEC_SUFFIX
      }
      return null
    }

    /** @return the list of unique parameters from all methods excluding current method. */
    @JvmStatic
    fun getPsiParameterStream(
        currentMethod: PsiMethod?,
        allMethods: Array<PsiMethod>,
    ): List<PsiParameter> {
      val seen = ContainerUtil.newConcurrentSet<Any>()
      return allMethods
          .filter { psiMethod -> psiMethod != currentMethod }
          .flatMap { it.parameterList.parameters.toList() }
          .filter { parameter ->
            val name = parameter.name ?: return@filter false
            seen.add(name)
          }
    }

    /** Finds LayoutSpec class in the given file. */
    @JvmStatic
    fun getFirstLayoutSpec(psiFile: PsiFile): PsiClass? {
      return getFirstClass(psiFile) { isLayoutSpec(it) }
    }

    @JvmStatic
    fun getFirstClass(psiFile: PsiFile, classFilter: (PsiClass) -> Boolean): PsiClass? {
      return PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java).firstOrNull {
        it != null && classFilter(it)
      }
    }

    @JvmStatic
    fun showInfo(infoMessage: String, project: Project?) {
      showNotification(infoMessage, NotificationType.INFORMATION, project)
    }

    @JvmStatic
    fun showWarning(infoMessage: String, project: Project?) {
      showNotification(infoMessage, NotificationType.WARNING, project)
    }

    private fun showNotification(infoMessage: String, type: NotificationType, project: Project?) {
      NOTIFICATION_GROUP.createNotification(infoMessage, type).notify(project)
    }

    /**
     * Tries to guess if the given methodCall requires event handler.
     *
     * @return Qualified name of the handled Event or null, if methodCall neither accepts event
     *   handler, nor require fix.
     */
    @JvmStatic
    fun resolveEventName(methodCall: PsiMethodCallExpression): String? {
      val results = methodCall.methodExpression.multiResolve(true)
      if (results.size != 1) {
        return null
      }
      val info = results[0] as? MethodCandidateInfo ?: return null
      if (!info.isTypeArgumentsApplicable) {
        return null
      }
      if (info.isApplicable || info.isValidResult) {
        return null
      }
      val parameters = info.element.parameterList.parameters
      if (parameters.isEmpty()) {
        return null
      }
      val parameterType = parameters[0].type as? PsiClassType ?: return null
      val fullName = parameterType.canonicalText
      val genericIndex = fullName.indexOf('<')
      if (genericIndex <= 0) {
        return null
      }
      val className = fullName.substring(0, genericIndex)
      if (LithoClassNames.EVENT_HANDLER_CLASS_NAME != className) {
        return null
      }
      val generics = parameterType.parameters
      if (generics.size != 1) {
        return null
      }
      return generics[0].canonicalText
    }

    @JvmStatic
    fun getVirtualFile(file: PsiFile): VirtualFile {
      val vf = file.virtualFile
      if (vf != null) {
        return vf
      }
      return file.viewProvider.virtualFile
    }

    @JvmStatic
    fun isKotlinPluginAvailable(): Boolean {
      return PluginManagerCore.getPlugin(PluginId.findId(KOTLIN_PLUGIN_ID)) != null
    }
  }
}
