// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.Map;

import com.squareup.javapoet.ClassName;

public class ProcessorUtils {

  /**
   * @return the AnnotationMirror of class clazz if typeElement is annotated with that class.
   */
  public static AnnotationMirror getAnnotationMirror(TypeElement typeElement, ClassName className) {
    final String classSimpleName = className.simpleName();
    for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
      if (m.getAnnotationType().asElement().getSimpleName().toString().equals(classSimpleName)) {
        return m;
      }
    }

    return null;
  }

  /**
   * @return the AnnotationValue related to key from the provided AnnotationMirror
   */
  public static AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String key) {
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        annotationMirror.getElementValues().entrySet() ) {
      if (entry.getKey().getSimpleName().toString().equals(key)) {
        return entry.getValue();
      }
    }

    return null;
  }

  /**
   * Convenience method to simplify extracting an AnnotationValue from an AnnotationMirror on a
   * TypeElement
   */
  public static AnnotationValue getAnnotationValue(
      TypeElement typeElement,
      ClassName className,
      String key) {
    final AnnotationMirror mirror = getAnnotationMirror(typeElement, className);
    if (mirror == null) {
      return null;
    }

    return getAnnotationValue(mirror, key);
  }
}
