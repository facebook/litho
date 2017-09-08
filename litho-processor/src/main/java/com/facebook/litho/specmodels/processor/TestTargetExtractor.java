/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.annotations.TestSpec;
import com.sun.tools.javac.code.Type;
import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

/** Utility class for extracting the target value of a {@link TestSpec}. */
public class TestTargetExtractor {
  @Nullable
  public static TypeElement getTestSpecValue(TypeElement element) {
    try {
      element.getAnnotation(TestSpec.class).value();
    } catch (MirroredTypeException e) {
      final TypeMirror typeMirror = e.getTypeMirror();
      return (TypeElement) ((Type.ClassType) typeMirror).asElement();
    }

    return null;
  }
}
