/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import static org.assertj.core.api.Java6Assertions.assertThat;

import androidx.annotation.UiThread;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.Output;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
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
        mMountSpecModelFactory.create(
            elements,
            types,
            elements.getTypeElement(MountTestSpec.class.getCanonicalName()),
            mMessager,
            RunMode.normal(),
            null,
            null);
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
                + "  java.lang.Integer color;\n"
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

    assertThat(methodNames.contains("createInterStagePropsContainer")).isTrue();
    assertThat(methodNames.contains("getInterStagePropsContainerImpl")).isTrue();
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
            "private MountTestInterStagePropsContainer getInterStagePropsContainerImpl(com.facebook.litho.ComponentContext c) {\n"
                + "  return (MountTestInterStagePropsContainer) super.getInterStagePropsContainer(c);\n"
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
                + "  component.setInterStagePropsContainer(createInterStagePropsContainer());\n"
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

    assertThat(holder.getMethodSpecs()).hasSize(2);

    MethodSpec onPrepareMethod = null;
    MethodSpec onBindMethod = null;

    for (int i = 0, size = holder.getMethodSpecs().size(); i < size; i++) {
      final MethodSpec methodSpec = holder.getMethodSpecs().get(i);
      if (methodSpec.name.equals("onPrepare")) {
        onPrepareMethod = methodSpec;
      }

      if (methodSpec.name.equals("onBind")) {
        onBindMethod = methodSpec;
      }
    }

    assertThat(onPrepareMethod).isNotNull();
    assertThat(onBindMethod).isNotNull();

    assertThat(onPrepareMethod.toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void onPrepare(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Output<java.lang.Integer> colorTmp = new Output<>();\n"
                + "  MountTestSpec.onPrepare(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (com.facebook.litho.Output<java.lang.Integer>) colorTmp);\n"
                + "  getInterStagePropsContainerImpl(c).color = colorTmp.get();\n"
                + "}\n");

    assertThat(onBindMethod.toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void onBind(com.facebook.litho.ComponentContext c, java.lang.Object lithoView) {\n"
                + "  MountTestSpec.onBind(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (com.facebook.litho.LithoView) lithoView,\n"
                + "    (java.lang.Integer) getInterStagePropsContainerImpl(c).color);\n"
                + "}\n");
  }

  @MountSpec
  static class MountTestSpec {

    @OnPrepare
    static void onPrepare(ComponentContext c, Output<Integer> color) {}

    @UiThread
    @OnBind
    static void onBind(ComponentContext c, LithoView lithoView, @FromPrepare Integer color) {}
  }
}
