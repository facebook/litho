/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

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
