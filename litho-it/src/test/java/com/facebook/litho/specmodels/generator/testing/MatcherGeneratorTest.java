/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator.testing;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.facebook.litho.testing.specmodels.MockSpecModel;
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

  private final SpecModel sEnclosedSpec =
      MockSpecModel.newBuilder()
          .componentName("MyEnclosedComponentSpec")
          .componentTypeName(ClassName.bestGuess("com.facebook.litho.MyEnclosedComponentSpec"))
          .build();

  @Test
  public void testEmptyGenerate() {
    final SpecModel specModel =
        MockSpecModel.newBuilder()
            .contextClass(ClassName.bestGuess(DummyContext.class.getName()))
            .representedObject(sEnclosedSpec)
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
            .representedObject(sEnclosedSpec)
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

  @Test
  public void testPropMatcherNameGenerator() {
    assertThat(MatcherGenerator.getPropMatcherName(makePropModel("myProp")))
        .isEqualTo("mMyPropMatcher");
    assertThat(MatcherGenerator.getPropMatcherName(makePropModel("\u2020Prop")))
        .isEqualTo("m\u2020PropMatcher");
    // Small letter o with stroke to capital O with stroke.
    assertThat(MatcherGenerator.getPropMatcherName(makePropModel("\u00F8de")))
        .isEqualTo("m\u00D8deMatcher");
    // Phabricator doesn't want poo in the source. :(
    assertThat(MatcherGenerator.getPropMatcherName(makePropModel("\uD83D\uDCA9")))
        .isEqualTo("m\uD83D\uDCA9Matcher");
  }

  public static PropModel makePropModel(String name) {
    final MockMethodParamModel paramModel = MockMethodParamModel.newBuilder().name(name).build();
    return new PropModel(paramModel, false, ResType.INT, "");
  }

}
