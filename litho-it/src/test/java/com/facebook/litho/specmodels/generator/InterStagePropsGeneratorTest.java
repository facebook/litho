/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.LithoView;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.InterStageInputParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelFactory;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.MountSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class InterStagePropsGeneratorTest {
  public @Rule CompilationRule mCompilationRule = new CompilationRule();
  private @Mock Messager mMessager;

  private SpecModel mInterstagePropsMountSpecModel;
  private final MountSpecModelFactory mMountSpecModelFactory = new MountSpecModelFactory();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();

    mInterstagePropsMountSpecModel =
        spy(
            mMountSpecModelFactory.create(
                elements,
                types,
                elements.getTypeElement(MountTestSpec.class.getCanonicalName()),
                mMessager,
                RunMode.normal(),
                null,
                null));

    InterStageInputParamModel props =
        new InterStageInputParamModel(
            MethodParamModelFactory.createSimpleMethodParamModel(
                new com.facebook.litho.specmodels.model.TypeSpec(ClassNames.STRING),
                "stringOutput",
                new Object()));

    when(mInterstagePropsMountSpecModel.getInterStageInputs()).thenReturn(ImmutableList.of(props));
  }

  @Test
  public void test_interStageProps_containerClassName() {
    assertThat(
            InterStagePropsContainerGenerator.getInterStagePropsContainerClassName(
                mInterstagePropsMountSpecModel))
        .isEqualTo("MountTestInterStagePropsContainer");
  }

  @Test
  public void test_generate_container_impl() {
    final TypeSpec container =
        InterStagePropsContainerGenerator.generate(mInterstagePropsMountSpecModel);

    assertThat(container.toString())
        .isEqualTo(
            "@com.facebook.litho.annotations.Generated\n"
                + "static class MountTestInterStagePropsContainer implements com.facebook.litho.InterStagePropsContainer {\n"
                + "  java.lang.String stringOutput;\n"
                + "}\n");
  }

  @Test
  public void test_generate_getAndCreate_container() {
    final TypeSpecDataHolder holder =
        ComponentBodyGenerator.generate(mInterstagePropsMountSpecModel, null, RunMode.testing());
    final List<String> methodNames = new ArrayList<>();
    for (MethodSpec methodSpec : holder.getMethodSpecs()) {
      methodNames.add(methodSpec.name);
    }

    assertThat(methodNames.contains("createPrepareInterStagePropsContainer")).isTrue();
    assertThat(methodNames.contains("getPrepareInterStagePropsContainerImpl")).isTrue();
  }

  @Test
  public void test_create_container() {
    MethodSpec methodSpec =
        ComponentBodyGenerator.generateInterStagePropsContainerCreator(
            ClassName.bestGuess(
                InterStagePropsContainerGenerator.getInterStagePropsContainerClassName(
                    mInterstagePropsMountSpecModel)));

    assertThat(methodSpec.toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected MountTestInterStagePropsContainer createInterStagePropsContainer() {\n"
                + "  return new MountTestInterStagePropsContainer();\n"
                + "}\n");
  }

  @Test
  public void test_get_container_impl() {
    MethodSpec methodSpec =
        ComponentBodyGenerator.generateInterstagePropsContainerImplGetter(
            mInterstagePropsMountSpecModel,
            ClassName.bestGuess(
                InterStagePropsContainerGenerator.getInterStagePropsContainerClassName(
                    mInterstagePropsMountSpecModel)));

    assertThat(methodSpec.toString())
        .isEqualTo(
            "private MountTestInterStagePropsContainer getInterStagePropsContainerImpl(\n"
                + "    com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.InterStagePropsContainer interStageProps) {\n"
                + "  return (MountTestInterStagePropsContainer) super.getInterStagePropsContainer(c, interStageProps);\n"
                + "}\n");
  }

  @Test
  public void test_makeShallowCopy_clear() {
    final TypeSpecDataHolder holder =
        ComponentBodyGenerator.generateMakeShallowCopy(mInterstagePropsMountSpecModel, false);
    assertThat(holder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "public MountTest makeShallowCopy() {\n"
                + "  MountTest component = (MountTest) super.makeShallowCopy();\n"
                + "  return component;\n"
                + "}\n");
  }

  @Test
  public void test_usages() {
    TypeSpecDataHolder holder =
        DelegateMethodGenerator.generateDelegates(
            mInterstagePropsMountSpecModel,
            DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP,
            RunMode.testing());

    assertThat(holder.getMethodSpecs()).hasSize(4);

    MethodSpec onPrepareMethod = null;
    MethodSpec onBindMethod = null;
    MethodSpec onMeasureMethod = null;

    for (int i = 0, size = holder.getMethodSpecs().size(); i < size; i++) {
      final MethodSpec methodSpec = holder.getMethodSpecs().get(i);
      if (methodSpec.name.equals("onPrepare")) {
        onPrepareMethod = methodSpec;
      }

      if (methodSpec.name.equals("onMeasure")) {
        onMeasureMethod = methodSpec;
      }

      if (methodSpec.name.equals("onBind")) {
        onBindMethod = methodSpec;
      }
    }

    assertThat(onPrepareMethod).isNotNull();
    assertThat(onMeasureMethod).isNotNull();
    assertThat(onBindMethod).isNotNull();

    assertThat(onPrepareMethod.toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void onPrepare(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Output<java.lang.Integer> colorTmp = new Output<>();\n"
                + "  MountTestSpec.onPrepare(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (com.facebook.litho.Output<java.lang.Integer>) colorTmp);\n"
                + "  getPrepareInterStagePropsContainerImpl(c).color = colorTmp.get();\n"
                + "}\n");

    assertThat(onMeasureMethod.toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void onMeasure(com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.ComponentLayout layout, int widthSpec, int heightSpec,\n"
                + "    com.facebook.litho.Size size, com.facebook.litho.InterStagePropsContainer _5) {\n"
                + "  com.facebook.litho.InterStagePropsContainer _interStageProps = _5;\n"
                + "  com.facebook.litho.Output<java.lang.String> stringOutputTmp = new Output<>();\n"
                + "  MountTestSpec.onMeasure(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (com.facebook.litho.ComponentLayout) layout,\n"
                + "    (int) widthSpec,\n"
                + "    (int) heightSpec,\n"
                + "    (com.facebook.litho.Size) size,\n"
                + "    (com.facebook.litho.Output<java.lang.String>) stringOutputTmp);\n"
                + "  getInterStagePropsContainerImpl(c, _interStageProps).stringOutput = stringOutputTmp.get();\n"
                + "}\n");

    assertThat(onBindMethod.toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void onBind(com.facebook.litho.ComponentContext c, java.lang.Object lithoView,\n"
                + "    com.facebook.litho.InterStagePropsContainer _2) {\n"
                + "  com.facebook.litho.InterStagePropsContainer _interStageProps = _2;\n"
                + "  com.facebook.litho.Output<java.lang.String> stringOutputTmp = new Output<>();\n"
                + "  MountTestSpec.onBind(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (com.facebook.litho.LithoView) lithoView,\n"
                + "    (java.lang.Integer) getPrepareInterStagePropsContainerImpl(c).color,\n"
                + "    (com.facebook.litho.Output<java.lang.String>) stringOutputTmp);\n"
                + "  getInterStagePropsContainerImpl(c, _interStageProps).stringOutput = stringOutputTmp.get();\n"
                + "}\n");
  }

  @MountSpec
  static class MountTestSpec {

    @OnPrepare
    static void onPrepare(ComponentContext c, Output<Integer> color) {}

    @OnMeasure
    static void onMeasure(
        ComponentContext c,
        ComponentLayout layout,
        int widthSpec,
        int heightSpec,
        Size size,
        Output<String> stringOutput) {}

    @UiThread
    @OnBind
    static void onBind(
        ComponentContext c,
        LithoView lithoView,
        @FromPrepare Integer color,
        @FromMeasure Output<String> stringOutput) {}
  }
}
