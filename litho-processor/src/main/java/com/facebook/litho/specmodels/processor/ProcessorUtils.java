/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  public static <T> T getAnnotationParameter(
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
  public static final void validate(SpecModel specModel, RunMode runMode) {
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
    return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
  }

  public static String getPackageName(TypeName typeName) {
    return getPackageName(typeName.toString());
  }
}
