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

import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.intellij.LithoClassNames
import com.facebook.litho.intellij.LithoPluginUtils
import com.facebook.litho.specmodels.processor.PsiEventDeclarationsExtractor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

/**
 * Helper class for Litho @OnEvent method related code generation. Details about this method in the
 * [docs](https://fblitho.com/docs/events-overview)
 */
object OnEventGenerateUtils {
  private const val CONTEXT_PARAMETER_NAME = "c"

  /**
   * Creates new @OnEvent method.
   *
   * Example:
   * ```
   * @OnEvent(ColorChangedEvent.class)
   * static void onColorChangedEvent(
   *       ComponentContext c,
   *       @FromEvent int color,
   *       @Prop String someProp) {
   *       }
   * ```
   *
   * @param context Context to create event for. Created event contains either SectionContext or
   *   ComponentContext depending on this context.
   * @param eventClass Class defines method name and method parameters. Parameters derived from this
   *   class are created with the @FromEvent annotation. This method doesn't verify if the provided
   *   class is [com.facebook.litho.annotations.Event] class.
   * @param additionalParameters Additional parameters added to the method 'as is'.
   * @return New PsiMethod describing Litho event handler.
   */
  @JvmStatic
  fun createOnEventMethod(
      context: PsiClass,
      eventClass: PsiClass,
      additionalParameters: Collection<PsiParameter>,
  ): PsiMethod {
    val project = context.project
    val factory = JavaPsiFacade.getElementFactory(project)

    val methodReturnType = PsiEventDeclarationsExtractor.getReturnPsiType(eventClass)
    var methodName = "on" + eventClass.name
    var postfix = 1
    while (context.findMethodsByName(methodName).isNotEmpty()) {
      methodName = "on" + eventClass.name + postfix
      postfix++
    }

    val method = factory.createMethod(methodName, methodReturnType, context)

    val parameterList = method.parameterList
    val contextParameter =
        factory.createParameter(
            CONTEXT_PARAMETER_NAME,
            PsiType.getTypeByName(
                getContextClassName(context),
                project,
                GlobalSearchScope.allScope(project),
            ),
            context,
        )
    parameterList.add(contextParameter)

    val eventFields = eventClass.fields
    for (field in eventFields) {
      val fieldName = field.name ?: continue
      val parameter = factory.createParameter(fieldName, field.type)
      val parameterModifierList = parameter.modifierList ?: continue
      parameterModifierList.addAnnotation(FromEvent::class.java.name)
      parameterList.add(parameter)
    }
    for (parameter in additionalParameters) {
      parameterList.add(parameter)
    }

    val methodModifierList = method.modifierList
    methodModifierList.addAnnotation(
        OnEvent::class.java.name + "(" + eventClass.qualifiedName + ".class)",
    )

    methodModifierList.setModifierProperty(PsiModifier.PACKAGE_LOCAL, true)
    methodModifierList.setModifierProperty(PsiModifier.STATIC, true)

    return method
  }

  /**
   * @return String representation of the @OnEvent method first line.
   *
   * Example:
   * ```
   * @OnEvent(ColorChangedEvent.class)
   * ```
   */
  @JvmStatic
  internal fun createOnEventLookupString(eventClass: PsiClass): String {
    return "@" + OnEvent::class.java.simpleName + "(" + eventClass.name + ".class)"
  }

  private fun getContextClassName(context: PsiClass): String {
    return if (LithoPluginUtils.hasLithoSectionSpecAnnotation(context)) {
      LithoClassNames.SECTION_CONTEXT_CLASS_NAME
    } else {
      LithoClassNames.COMPONENT_CONTEXT_CLASS_NAME
    }
  }

  /**
   * Adds comment to the given method "// An event handler ContextClassName.methodName(c,
   * parameterName)
   */
  @JvmStatic
  fun addComment(contextClass: PsiClass, method: PsiMethod) {
    val project = contextClass.project
    val factory = JavaPsiFacade.getElementFactory(project)

    val builder =
        StringBuilder("// An event handler ")
            .append(LithoPluginUtils.getLithoComponentNameFromSpec(contextClass.name))
            .append(".")
            .append(method.name)
            .append("(")
            .append(CONTEXT_PARAMETER_NAME)
    for (parameter in method.parameterList.parameters) {
      if (LithoPluginUtils.isParam(parameter)) {
        builder.append(", ").append(parameter.name)
      }
    }

    builder.append(")")
    val comment = factory.createCommentFromText(builder.toString(), method)
    method.addBefore(comment, method.modifierList)
  }
}
