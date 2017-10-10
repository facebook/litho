/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional gr
 * of patent rights can be found in the PATENTS file in the same directory.
 *
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.SpecElementType;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class SpecElementTypeDeterminator {
  static boolean isKotlinSingleton(TypeElement element) {
    return element.getKind() == ElementKind.CLASS
        && element
            .getEnclosedElements()
            .stream()
            .anyMatch(
                e -> {
                  final CharSequence instanceFieldName = "INSTANCE";
                  return e.getSimpleName().contentEquals(instanceFieldName)
                      && e.asType().toString().equals(((Symbol.ClassSymbol) element).className())
                      && e.getModifiers()
                          .containsAll(
                              ImmutableList.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL));
                });
  }

  public static SpecElementType determine(TypeElement element) {
    if (isKotlinSingleton(element)) {
      return SpecElementType.KOTLIN_SINGLETON;
    }

    return SpecElementType.JAVA_CLASS;
  }
}
