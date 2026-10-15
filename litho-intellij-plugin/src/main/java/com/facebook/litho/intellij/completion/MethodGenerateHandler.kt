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

package com.facebook.litho.intellij.completion

import com.facebook.litho.intellij.services.ComponentGenerateService
import com.facebook.litho.sections.specmodels.model.GroupSectionSpecModel
import com.facebook.litho.specmodels.model.SpecModel
import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase
import com.intellij.codeInsight.generation.GenerationInfo
import com.intellij.codeInsight.generation.TemplateGenerationInfo
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.impl.TextExpression
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Generates method. Doesn't prompt the user for additional data, uses pre-defined method instead.
 */
internal class MethodGenerateHandler(
    private val generatedMethods: List<PsiMethod>,
    private val specClass: PsiClass,
    private val document: Document,
    private val project: Project,
) : GenerateMembersHandlerBase("") {

  protected override fun generateMemberPrototypes(
      aClass: PsiClass,
      members: Array<ClassMember>,
  ): List<GenerationInfo> {
    return generatedMethods.map { generatedMethod ->
      MethodTemplateGenerationInfo(generatedMethod, specClass, document, project)
    }
  }

  protected override fun chooseOriginalMembers(
      aClass: PsiClass,
      project: Project,
      editor: Editor,
  ): Array<ClassMember> {
    return ClassMember.EMPTY_ARRAY
  }

  protected override fun getAllOriginalMembers(aClass: PsiClass): Array<ClassMember> {
    return ClassMember.EMPTY_ARRAY
  }

  protected override fun generateMemberPrototypes(
      aClass: PsiClass,
      originalMember: ClassMember,
  ): Array<GenerationInfo> {
    return GenerationInfo.EMPTY_ARRAY
  }

  internal class MethodTemplateGenerationInfo(
      element: PsiMethod,
      private val specClass: PsiClass,
      private val document: Document,
      private val project: Project,
  ) : TemplateGenerationInfo(element, null) {

    protected override fun getTemplateElement(method: PsiMethod): PsiElement? {
      return null
    }

    override fun getTemplate(): Template {
      PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)

      val psiMethodWithContext = requireNotNull(psiMember)
      val templateBuilder = TemplateBuilderImpl(psiMethodWithContext)
      fillTemplate(psiMethodWithContext, templateBuilder, specClass)
      val template = templateBuilder.buildTemplate()
      template.isToReformat = true
      return template
    }
  }

  companion object {
    @JvmStatic
    internal fun fillTemplate(
        psiMethodWithContext: PsiMethod,
        templateBuilder: TemplateBuilderImpl,
        specClass: PsiClass,
    ) {
      PsiTreeUtil.processElements(psiMethodWithContext) { psiElement ->
        val elementText = psiElement.text
        if (psiElement is PsiTypeElement && elementText == "placeholder_type") {
          templateBuilder.replaceElement(
              psiElement,
              TextExpression(getFirstStateTypeAndName(specClass).first),
          )
        } else if (psiElement is PsiIdentifier) {
          if (elementText == "onClickEvent") {
            val methodName = getNextMethodName("onClickEvent", specClass)
            templateBuilder.replaceElement(psiElement, methodName)
          } else if (elementText == "onTriggerEvent") {
            val methodName = getNextMethodName("onTriggerEvent", specClass)
            templateBuilder.replaceElement(psiElement, methodName)
          } else if (elementText == "placeholder_name") {
            templateBuilder.replaceElement(
                psiElement,
                TextExpression(getFirstStateTypeAndName(specClass).second),
            )
          } else if (psiElement.parent is PsiMethod) {
            // Method name
            templateBuilder.replaceElement(psiElement, TextExpression(elementText))
          } else if (elementText == "placeholder_annotation_class") {
            templateBuilder.replaceElement(psiElement, "ClickEvent")
          } else if (elementText == "placeholder_from_event_parameter") {
            templateBuilder.replaceElement(psiElement, "@FromEvent")
          } else if (elementText == "placeholder_from_trigger_parameter") {
            templateBuilder.replaceElement(psiElement, "@FromTrigger")
          }
        } else if (psiElement is PsiTypeElement && elementText == "placeholder_service_type") {
          templateBuilder.replaceElement(psiElement, TextExpression(getServiceType(specClass)))
        } else if (psiElement is PsiTypeElement && elementText == "placeholder_tree_prop_type") {
          templateBuilder.replaceElement(psiElement, "TreePropType")
        } else if (psiElement is PsiComment) {
          templateBuilder.replaceElement(psiElement, TextExpression(elementText))
          templateBuilder.setEndVariableAfter(psiElement)
        }
        true
      }
    }

    private fun getNextMethodName(methodName: String, specClass: PsiClass): String {
      var newMethodName = methodName
      var postfix = 1
      while (specClass.findMethodsByName(newMethodName).isNotEmpty()) {
        newMethodName = methodName + postfix
        postfix++
      }
      return newMethodName
    }

    private fun getFirstStateTypeAndName(specClass: PsiClass): Pair<String, String> {
      val specModel: SpecModel? =
          ComponentGenerateService.getInstance().getOrCreateSpecModel(specClass)
      if (specModel != null && specModel.stateValues.isNotEmpty()) {
        val stateParamModel = specModel.stateValues[0]
        var typename = stateParamModel.typeName
        if (typename.isPrimitive) {
          typename = typename.box()
        }
        return Pair(typename.toString(), stateParamModel.name)
      }
      return Pair("StateType", "stateName")
    }

    private fun getServiceType(specClass: PsiClass): String {
      val specModel: SpecModel? =
          ComponentGenerateService.getInstance().getOrCreateSpecModel(specClass, false)
      if (specModel != null && specModel is GroupSectionSpecModel) {
        val serviceParam = specModel.serviceParam
        if (serviceParam != null) {
          return serviceParam.typeName.toString()
        }
      }
      return "ServiceType"
    }
  }
}
