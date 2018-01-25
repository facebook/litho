/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.TypeElement;
import org.junit.Rule;
import org.junit.Test;

public class SpecModelUtilsTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  @Test
  public void testGenerateTypeSpecForRegularType() {
    final TypeElement typeElement =
        mCompilationRule.getElements().getTypeElement(SpecModelUtilsTest.class.getCanonicalName());

    final TypeSpec typeSpec = SpecModelUtils.generateTypeSpec(typeElement.asType());
    assertThat(typeSpec.isValid()).isTrue();
  }
}
