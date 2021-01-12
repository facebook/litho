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
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.MountSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.MethodSpec;
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
  public void test_generate_field() {
    final TypeSpecDataHolder holder =
        ComponentBodyGenerator.generate(mInterstagePropsMountSpecModel, null, RunMode.testing());

    assertThat(holder.getFieldSpecs()).hasSize(1);
    assertThat(holder.getFieldSpecs().get(0).toString()).isEqualTo("java.lang.Integer color;\n");
  }

  @Test
  public void test_generate_copyInterstageImpl() {
    final TypeSpecDataHolder holder =
        ComponentBodyGenerator.generateCopyInterStageImpl(mInterstagePropsMountSpecModel);
    assertThat(holder.getMethodSpecs()).hasSize(1);
    assertThat(holder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void copyInterStageImpl(com.facebook.litho.Component component) {\n"
                + "  MountTest mountTestRef = (MountTest) component;\n"
                + "  color = mountTestRef.color;\n"
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
                + "  component.color = null;\n"
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
                + "  color = colorTmp.get();\n"
                + "}\n");

    assertThat(onBindMethod.toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void onBind(com.facebook.litho.ComponentContext c, java.lang.Object lithoView) {\n"
                + "  MountTestSpec.onBind(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (com.facebook.litho.LithoView) lithoView,\n"
                + "    (java.lang.Integer) color);\n"
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
