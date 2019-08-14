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

import com.facebook.litho.annotations.TestSpec;
import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
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
      return (TypeElement) ((DeclaredType) typeMirror).asElement();
    }

    return null;
  }
}
