/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.MockMethodParamModel;
import com.facebook.litho.specmodels.internal.MockSpecModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.assertj.core.api.Condition;
import org.assertj.core.data.Index;
import org.junit.Test;

/** Tests {@link MatcherGenerator} */
public class MatcherGeneratorTest {
  public static class DummyContext {}

  @Test
  public void testEmptyGenerate() {
    final SpecModel specModel =
        MockSpecModel.newBuilder()
            .contextClass(ClassName.bestGuess(DummyContext.class.getName()))
            .build();

    final TypeSpecDataHolder holder = MatcherGenerator.generate(specModel);

    assertThat(holder.getFieldSpecs()).isEmpty();

    assertThat(holder.getMethodSpecs())
        .hasSize(1)
        .has(
            new Condition<MethodSpec>() {
              @Override
              public boolean matches(MethodSpec value) {
                return value.name.equals("matcher")
                    && value.returnType.toString().equals("Matcher");
              }
            },
            Index.atIndex(0));
  }

  @Test
  public void testGenerationWithProps() {
    final SpecModel specModel =
        MockSpecModel.newBuilder()
            .contextClass(ClassName.bestGuess(DummyContext.class.getName()))
            .props(
                ImmutableList.of(
                    new PropModel(
                        MockMethodParamModel.newBuilder()
                            .name("prop1")
                            .type(TypeName.get(String.class))
                            .build(),
                        false,
                        ResType.STRING,
                        ""),
                    new PropModel(
                        MockMethodParamModel.newBuilder()
                            .name("prop2")
                            .type(TypeName.FLOAT)
                            .build(),
                        true,
                        ResType.DIMEN_SIZE,
                        "")))
            .build();

    final TypeSpecDataHolder holder = MatcherGenerator.generate(specModel);

    assertThat(holder.getFieldSpecs()).isEmpty();

    assertThat(holder.getTypeSpecs()).hasSize(1);

    final TypeSpec matcherSpec = holder.getTypeSpecs().get(0);

    assertThat(matcherSpec.name).isEqualTo("Matcher");
    assertThat(matcherSpec.fieldSpecs).hasSize(2);

    assertThat(matcherSpec.methodSpecs.stream().map(m -> m.name).toArray())
        .hasSameElementsAs(
            ImmutableList.of(
                "<init>",
                "prop1",
                "prop1Res",
                "prop1Res",
                "prop1Attr",
                "prop1Attr",
                "prop2Px",
                "prop2Res",
                "prop2Attr",
                "prop2Attr",
                "prop2Dip",
                "build"));
  }
}
