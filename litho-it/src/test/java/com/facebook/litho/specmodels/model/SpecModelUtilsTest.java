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
package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.TypeElement;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
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
