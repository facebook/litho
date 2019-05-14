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

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.facebook.litho.testing.specmodels.MockSpecModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link JavadocGenerator} */
@RunWith(JUnit4.class)
public class JavadocGeneratorTest {
  @Test
  public void testGenerateJavadocProps() {
    final MethodParamModel requiredMethodParam =
        MockMethodParamModel.newBuilder().name("propName1").type(TypeName.INT).build();
    final MethodParamModel optionalMethodParam =
        MockMethodParamModel.newBuilder().name("propName2").type(TypeName.BOOLEAN).build();
    final SpecModel specModel =
        MockSpecModel.newBuilder()
            .classJavadoc("Test Javadoc")
            .propJavadocs(
                ImmutableList.of(
                    new PropJavadocModel("propName1", "test prop1 javadoc"),
                    new PropJavadocModel("propName2", "test prop2 javadoc")))
            .props(
                ImmutableList.of(
                    new PropModel(requiredMethodParam, false, false, false, false, ResType.INT, ""),
                    new PropModel(
                        optionalMethodParam, true, false, false, false, ResType.BOOL, "")))
            .build();

    final TypeSpecDataHolder dataHolder = JavadocGenerator.generate(specModel);
    assertThat(dataHolder.getJavadocSpecs()).hasSize(4);

    assertThat(getJavadocsString(dataHolder))
        .isEqualTo(
            "Test Javadoc<p>\n"
                + "@prop-required propName1 int test prop1 javadoc\n"
                + "@prop-optional propName2 boolean test prop2 javadoc\n");
  }

  @Test
  public void testGenerateJavadocSeeReference() {
    final SpecModel specModel =
        MockSpecModel.newBuilder()
            .classJavadoc("Test Javadoc")
            // Any class with a well-known path is fine here.
            .specTypeName(ClassName.get(TypeSpecDataHolder.class))
            .build();

    final TypeSpecDataHolder dataHolder = JavadocGenerator.generate(specModel);
    assertThat(dataHolder.getJavadocSpecs()).hasSize(3);
    assertThat(getJavadocsString(dataHolder))
        .isEqualTo(
            "Test Javadoc<p>\n"
                + "\n"
                + "@see com.facebook.litho.specmodels.generator.TypeSpecDataHolder\n");
  }

  private final String getJavadocsString(TypeSpecDataHolder dataHolder) {
    return dataHolder
        .getJavadocSpecs()
        .stream()
        .reduce("", (s, javadocSpec) -> s + javadocSpec, (s, s2) -> s + s2);
  }
}
