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

package com.facebook.litho.intellij.completion;

import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.specmodels.processor.PsiEventDeclarationsExtractor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.Collection;

/**
 * Helper class for Litho {@literal @OnEvent} method related code generation. Details about this
 * method in the <a href="https://fblitho.com/docs/events-overview">docs</a>
 */
public class OnEventGenerateUtils {
  private static final String CONTEXT_PARAMETER_NAME = "c";

  private OnEventGenerateUtils() {}

  /**
   * Creates new @OnEvent method.
   *
   * <p>Example:
   *
   * <pre><code>{@literal @OnEvent(ColorChangedEvent.class)}
   * static void onColorChangedEvent(
   *       ComponentContext c,
   *      {@literal @FromEvent} int color,
   *      {@literal @Prop} String someProp) {
   *
   *      }
   * }</code></pre>
   *
   * @param context Context to create event for. Created event contains either SectionContext or
   *     ComponentContext depending on this context.
   * @param eventClass Class defines method name and method parameters. Parameters derived from this
   *     class are created with the {@literal @FromEvent} annotation. This method doesn't verify if
   *     the provided class is {@link Event} class.
   * @param additionalParameters Additional parameters added to the method 'as is'.
   * @return New PsiMethod describing Litho event handler.
   */
  public static PsiMethod createOnEventMethod(
      PsiClass context, PsiClass eventClass, Collection<PsiParameter> additionalParameters) {

    final Project project = context.getProject();
    final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

    final PsiType methodReturnType = PsiEventDeclarationsExtractor.getReturnPsiType(eventClass);
    String methodName = "on" + eventClass.getName();
    int postfix = 1;
    while (context.findMethodsByName(methodName).length > 0) {
      methodName = "on" + eventClass.getName() + postfix;
      postfix++;
    }

    final PsiMethod method = factory.createMethod(methodName, methodReturnType, context);

    final PsiParameterList parameterList = method.getParameterList();
    final PsiParameter contextParameter =
        factory.createParameter(
            CONTEXT_PARAMETER_NAME,
            PsiType.getTypeByName(
                getContextClassName(context), project, GlobalSearchScope.allScope(project)),
            context);
    parameterList.add(contextParameter);

    final PsiField[] eventFields = eventClass.getFields();
    for (PsiField field : eventFields) {
      final String fieldName = field.getName();
      if (fieldName == null) {
        continue;
      }
      final PsiParameter parameter = factory.createParameter(fieldName, field.getType());
      final PsiModifierList parameterModifierList = parameter.getModifierList();
      if (parameterModifierList == null) {
        continue;
      }
      parameterModifierList.addAnnotation(FromEvent.class.getName());
      parameterList.add(parameter);
    }
    for (PsiParameter parameter : additionalParameters) {
      parameterList.add(parameter);
    }

    final PsiModifierList methodModifierList = method.getModifierList();
    methodModifierList.addAnnotation(
        OnEvent.class.getName() + "(" + eventClass.getQualifiedName() + ".class)");

    methodModifierList.setModifierProperty(PsiModifier.PACKAGE_LOCAL, true);
    methodModifierList.setModifierProperty(PsiModifier.STATIC, true);

    return method;
  }

  /**
   * @return String representation of the @OnEvent method first line.
   *     <p>Example:
   *     <pre><code>{@literal @OnEvent(ColorChangedEvent.class)}</code></pre>
   */
  static String createOnEventLookupString(PsiClass eventClass) {
    return "@" + OnEvent.class.getSimpleName() + "(" + eventClass.getName() + ".class)";
  }

  private static String getContextClassName(PsiClass context) {
    return LithoPluginUtils.hasLithoSectionSpecAnnotation(context)
        ? LithoClassNames.SECTION_CONTEXT_CLASS_NAME
        : LithoClassNames.COMPONENT_CONTEXT_CLASS_NAME;
  }

  /**
   * Adds comment to the given method "// An event handler ContextClassName.methodName(c,
   * parameterName)
   */
  public static void addComment(PsiClass contextClass, PsiMethod method) {
    final Project project = contextClass.getProject();
    final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

    final StringBuilder builder =
        new StringBuilder("// An event handler ")
            .append(LithoPluginUtils.getLithoComponentNameFromSpec(contextClass.getName()))
            .append(".")
            .append(method.getName())
            .append("(")
            .append(CONTEXT_PARAMETER_NAME);
    for (PsiParameter parameter : method.getParameterList().getParameters()) {
      if (LithoPluginUtils.isParam(parameter)) {
        builder.append(", ").append(parameter.getName());
      }
    }

    builder.append(")");
    final PsiComment comment = factory.createCommentFromText(builder.toString(), method);
    method.addBefore(comment, method.getModifierList());
  }
}
