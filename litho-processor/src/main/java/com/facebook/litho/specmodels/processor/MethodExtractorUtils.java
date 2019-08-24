/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.model.SpecModelUtils.generateTypeSpec;

import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelFactory;
import com.facebook.litho.specmodels.model.TypeSpec;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

/** Extracts methods from the given input. */
public final class MethodExtractorUtils {
  public static final String COMPONENTS_PACKAGE = "com.facebook.litho";

  private MethodExtractorUtils() {}

  /** @return a list of params for a method. */
  static List<MethodParamModel> getMethodParams(
      ExecutableElement method,
      Messager messager,
      List<Class<? extends Annotation>> permittedAnnotations,
      List<Class<? extends Annotation>> permittedInterStageInputAnnotations,
      List<Class<? extends Annotation>> delegateMethodAnnotationsThatSkipDiffModels) {

    final List<MethodParamModel> methodParamModels = new ArrayList<>();
    final List<Name> savedParameterNames = getSavedParameterNames(method);
    final List<? extends VariableElement> params = method.getParameters();

    for (int i = 0, size = params.size(); i < size; i++) {
      final VariableElement param = params.get(i);
      final String paramName =
          savedParameterNames == null
              ? param.getSimpleName().toString()
              : savedParameterNames.get(i).toString();

      try {
        final TypeSpec typeSpec = generateTypeSpec(param.asType());

        if (!typeSpec.isValid()) {
          messager.printMessage(
              Diagnostic.Kind.WARNING,
              String.format(
                  "The type of '%s' cannot be fully determined at compile time. "
                      + "This can cause issues if the target referenced is from a different "
                      + "package. "
                      + "Learn more at https://fburl.com/fblitho-cross-package-error.",
                  param.getSimpleName()),
              param);
        }

        methodParamModels.add(
            MethodParamModelFactory.create(
                typeSpec,
                paramName,
                getLibraryAnnotations(param, permittedAnnotations),
                getExternalAnnotations(param),
                permittedInterStageInputAnnotations,
                canCreateDiffModels(method, delegateMethodAnnotationsThatSkipDiffModels),
                param));
      } catch (Exception e) {
        throw new ComponentsProcessingException(
            param,
            String.format(
                "Error processing the param '%s'. Are your imports set up correctly? The causing error was: %s",
                param, e));
      }
    }

    return methodParamModels;
  }

  private static boolean canCreateDiffModels(
      ExecutableElement method,
      List<Class<? extends Annotation>> delegateMethodAnnotationsThatSkipDiffModels) {

    for (Class<? extends Annotation> delegate : delegateMethodAnnotationsThatSkipDiffModels) {
      if (method.getAnnotation(delegate) != null) {
        return false;
      }
    }

    return true;
  }

  /**
   * Attempt to recover saved parameter names for a method. This will likely only work for code
   * compiled with javac >= 8, but it's often the only chance to get named parameters as opposed to
   * 'arg0', 'arg1', ...
   */
  @Nullable
  private static List<Name> getSavedParameterNames(ExecutableElement method) {
    if (method instanceof Symbol.MethodSymbol) {
      final Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) method;
      try {
        //noinspection unchecked
        return (List<Name>)
            Symbol.MethodSymbol.class.getField("savedParameterNames").get(methodSymbol);
      } catch (NoSuchFieldError | IllegalAccessException | NoSuchFieldException ignored) {
        // This can happen on JVM versions >= 10. However, we need to keep this workaround for JVM
        // versions < 8 which do not provide the '-parameters' javac option which is the Right Way
        // to achieve this.
        return null;
      }
    }
    return null;
  }

  private static List<Annotation> getLibraryAnnotations(
      VariableElement param, List<Class<? extends Annotation>> permittedAnnotations) {
    List<Annotation> paramAnnotations = new ArrayList<>();
    for (Class<? extends Annotation> possibleMethodParamAnnotation : permittedAnnotations) {
      final Annotation paramAnnotation = param.getAnnotation(possibleMethodParamAnnotation);
      if (paramAnnotation != null) {
        paramAnnotations.add(paramAnnotation);
      }
    }

    return paramAnnotations;
  }

  private static List<AnnotationSpec> getExternalAnnotations(VariableElement param) {
    final List<? extends AnnotationMirror> annotationMirrors = param.getAnnotationMirrors();
    final List<AnnotationSpec> annotations = new ArrayList<>();

    for (AnnotationMirror annotationMirror : annotationMirrors) {
      if (annotationMirror.getAnnotationType().toString().startsWith(COMPONENTS_PACKAGE)) {
        continue;
      }

      final AnnotationSpec.Builder annotationSpec =
          AnnotationSpec.builder(
              ClassName.bestGuess(annotationMirror.getAnnotationType().toString()));

      Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues =
          annotationMirror.getElementValues();
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> elementValue :
          elementValues.entrySet()) {
        annotationSpec.addMember(
            elementValue.getKey().getSimpleName().toString(), elementValue.getValue().toString());
      }

      annotations.add(annotationSpec.build());
    }

    return annotations;
  }

  static List<TypeVariableName> getTypeVariables(ExecutableElement method) {
    final List<TypeVariableName> typeVariables = new ArrayList<>();
    for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
      typeVariables.add(
          TypeVariableName.get(
              typeParameterElement.getSimpleName().toString(), getBounds(typeParameterElement)));
    }

    return typeVariables;
  }

  private static TypeName[] getBounds(TypeParameterElement typeParameterElement) {
    final TypeName[] bounds = new TypeName[typeParameterElement.getBounds().size()];
    for (int i = 0, size = typeParameterElement.getBounds().size(); i < size; i++) {
      bounds[i] = TypeName.get(typeParameterElement.getBounds().get(i));
    }

    return bounds;
  }
}
