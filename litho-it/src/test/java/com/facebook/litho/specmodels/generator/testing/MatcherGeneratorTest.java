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

package com.facebook.litho.specmodels.generator.testing;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.InjectPropModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.testing.TestSpecModel;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.facebook.litho.testing.specmodels.MockSpecModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.assertj.core.api.Condition;
import org.assertj.core.data.Index;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link MatcherGenerator} */
@RunWith(JUnit4.class)
public class MatcherGeneratorTest {
  public static class DummyContext {}

  private final SpecModel sEnclosedSpec =
      MockSpecModel.newBuilder()
          .componentName("MyEnclosedComponentSpec")
          .componentTypeName(ClassName.bestGuess("com.facebook.litho.MyEnclosedComponentSpec"))
          .build();

  @Test
  public void testEmptyGenerate() {
    final MockSpecModel specModel =
        MockSpecModel.newBuilder()
            .contextClass(ClassName.bestGuess(DummyContext.class.getName()))
            .enclosedSpecModel(sEnclosedSpec)
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
    final MockSpecModel specModel =
        MockSpecModel.newBuilder()
            .contextClass(ClassName.bestGuess(DummyContext.class.getName()))
            .enclosedSpecModel(sEnclosedSpec)
            .props(
                ImmutableList.of(
                    new PropModel(
                        MockMethodParamModel.newBuilder()
                            .name("prop1")
                            .type(TypeName.get(String.class))
                            .build(),
                        false,
                        false,
                        false,
                        false,
                        ResType.STRING,
                        ""),
                    new PropModel(
                        MockMethodParamModel.newBuilder()
                            .name("prop2")
                            .type(TypeName.FLOAT)
                            .build(),
                        true,
                        false,
                        false,
                        false,
                        ResType.DIMEN_SIZE,
                        "")))
            .build();

    final TypeSpecDataHolder holder = MatcherGenerator.generate(specModel);

    assertThat(holder.getFieldSpecs()).isEmpty();

    assertThat(holder.getTypeSpecs()).hasSize(1);

    final TypeSpec matcherSpec = holder.getTypeSpecs().get(0);

    assertThat(matcherSpec.name).isEqualTo("Matcher");
    assertThat(matcherSpec.fieldSpecs).hasSize(3);

    assertThat(matcherSpec.methodSpecs.stream().map(m -> m.name).toArray())
        .hasSameElementsAs(
            ImmutableList.of(
                "<init>",
                "prop1",
                "prop1",
                "prop1Res",
                "prop1Res",
                "prop1Attr",
                "prop1Attr",
                "prop2",
                "prop2Px",
                "prop2Res",
                "prop2Attr",
                "prop2Attr",
                "prop2Dip",
                "build",
                "getThis"));
  }

  @Test
  public void testGenerationWithInjectProps() {
    final MockSpecModel mockSpecModel =
        MockSpecModel.newBuilder()
            .contextClass(ClassName.bestGuess(DummyContext.class.getName()))
            .enclosedSpecModel(sEnclosedSpec)
            .props(
                ImmutableList.of(
                    new PropModel(
                        MockMethodParamModel.newBuilder()
                            .name("prop1")
                            .type(TypeName.get(String.class))
                            .build(),
                        false,
                        false,
                        false,
                        false,
                        ResType.STRING,
                        "")))
            .injectProps(
                ImmutableList.of(
                    new InjectPropModel(
                        MockMethodParamModel.newBuilder()
                            .name("injectProp1")
                            .type(TypeName.get(MatcherGenerator.class))
                            .build(),
                        true)))
            .build();
    final TestSpecModel specModel = new TestSpecModel(mockSpecModel, null, sEnclosedSpec);

    final TypeSpecDataHolder holder = MatcherGenerator.generate(specModel);

    assertThat(holder.getFieldSpecs()).isEmpty();

    assertThat(holder.getTypeSpecs()).hasSize(1);

    final TypeSpec matcherSpec = holder.getTypeSpecs().get(0);

    assertThat(matcherSpec.name).isEqualTo("Matcher");
    assertThat(matcherSpec.fieldSpecs).hasSize(3);

    assertThat(matcherSpec.methodSpecs.stream().map(m -> m.name).toArray())
        .hasSameElementsAs(
            ImmutableList.of(
                "<init>",
                "prop1",
                "prop1",
                "prop1Res",
                "prop1Res",
                "prop1Attr",
                "prop1Attr",
                "injectProp1",
                "build",
                "getThis"));
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
    return new PropModel(paramModel, false, false, false, false, ResType.INT, "");
  }

}
