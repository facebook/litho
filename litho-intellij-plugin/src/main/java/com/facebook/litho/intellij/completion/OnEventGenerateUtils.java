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
package com.facebook.litho.intellij.completion;

import com.facebook.litho.intellij.LithoClassNames;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
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
   * @param context Context for creating PsiElements.
   * @param eventClass Class defines method name and method parameters. Parameters derived from this
   *     class are created with the {@literal @FromEvent} annotation.
   * @param additionalParameters Additional parameters added to the method 'as is'.
   * @return New PsiMethod describing Litho event handler.
   */
  public static PsiMethod createOnEventMethod(
      PsiElement context, PsiClass eventClass, Collection<PsiParameter> additionalParameters) {

    final Project project = context.getProject();
    final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

    final PsiType methodReturnType = PsiType.VOID;
    final String methodName = "on" + eventClass.getName();
    final PsiMethod method = factory.createMethod(methodName, methodReturnType, context);

    final PsiParameterList parameterList = method.getParameterList();
    final PsiParameter contextParameter =
        factory.createParameter(
            CONTEXT_PARAMETER_NAME,
            PsiType.getTypeByName(
                LithoClassNames.COMPONENT_CONTEXT_CLASS_NAME,
                project,
                GlobalSearchScope.allScope(project)),
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
      parameterModifierList.addAnnotation(LithoClassNames.FROM_EVENT_ANNOTATION_NAME);
      parameterList.add(parameter);
    }
    for (PsiParameter parameter : additionalParameters) {
      parameterList.add(parameter);
    }

    final PsiModifierList methodModifierList = method.getModifierList();
    methodModifierList.addAnnotation(
        LithoClassNames.ON_EVENT_ANNOTATION_NAME + "(" + eventClass.getQualifiedName() + ".class)");

    methodModifierList.setModifierProperty(PsiModifier.PACKAGE_LOCAL, true);
    methodModifierList.setModifierProperty(PsiModifier.STATIC, true);

    return method;
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
