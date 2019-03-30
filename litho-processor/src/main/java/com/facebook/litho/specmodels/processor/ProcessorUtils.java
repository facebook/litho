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

import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;

/**
 * Utility class for processing specs.
 */
public class ProcessorUtils {

  /**
   * Gets an annotation parameter from an annotation. Usually you can just get the parameter
   * directly, but if the parameter has type {@link Class} it doesn't work, because javac doesn't
   * load classes in the normal manner.
   *
   * @see <a
   *     href="https://area-51.blog/2009/02/13/getting-class-values-from-annotations-in-an-annotationprocessor">this
   *     article</a> for more details.
   */
  public static @Nullable <T> T getAnnotationParameter(
      Elements elements,
      Element element,
      Class<?> annotationType,
      String parameterName,
      Class<? extends T> expectedReturnType) {
    List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

    AnnotationMirror mirror = null;
    for (AnnotationMirror m : annotationMirrors) {
      if (m.getAnnotationType().toString().equals(annotationType.getCanonicalName())) {
        mirror = m;
        break;
      }
    }

    if (mirror == null) {
      return null;
    }

    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        elements.getElementValuesWithDefaults(mirror).entrySet()) {
      if (parameterName.equals(entry.getKey().getSimpleName().toString())) {
        try {
          return expectedReturnType.cast(entry.getValue().getValue());
        } catch (ClassCastException e) {
          throw new ComponentsProcessingException(
              element,
              mirror,
              String.format(
                  "Error processing the annotation '%s'. Are your imports set up correctly? The causing error was: %s",
                  annotationType.getCanonicalName(), e));
        }
      }
    }

    return null;
  }

  /**
   * Creates printable exceptions for the validation errors found while running the annotation
   * processor for the given specmodel and throws a {@link MultiPrintableException} if any such
   * errors are found.
   */
  public static final void validate(SpecModel specModel, EnumSet<RunMode> runMode) {
    List<SpecModelValidationError> validationErrors = specModel.validate(runMode);

    if (validationErrors == null || validationErrors.isEmpty()) {
      return;
    }

    final List<PrintableException> printableExceptions = new ArrayList<>();
    for (SpecModelValidationError validationError : validationErrors) {
      printableExceptions.add(
          new ComponentsProcessingException(
              (Element) validationError.element,
              (AnnotationMirror) validationError.annotation,
              validationError.message));
    }

    throw new MultiPrintableException(printableExceptions);
  }

  public static String getPackageName(String qualifiedName) {
    int lastDotIndex = qualifiedName.lastIndexOf('.');
    if (lastDotIndex == -1) {
      throw new IllegalArgumentException(
          "Your class " + qualifiedName + " has no package declaration.");
    }
    return qualifiedName.substring(0, lastDotIndex);
  }

  public static String getPackageName(TypeName typeName) {
    return getPackageName(typeName.toString());
  }
}
