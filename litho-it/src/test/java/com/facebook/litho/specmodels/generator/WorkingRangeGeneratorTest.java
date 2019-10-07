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
import static org.mockito.Mockito.mock;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnExitedRange;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WorkingRangeGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  @LayoutSpec
  static class TestSpec<T extends CharSequence> {
    @PropDefault protected static final boolean arg0 = true;

    @OnEnteredRange(name = "enter")
    public void testEnteredRangeMethod(ComponentContext c, @Prop boolean arg0, @State int arg1) {}

    @OnExitedRange(name = "exit")
    public void testExitedRangeMethod(ComponentContext c, @Prop T arg2, @TreeProp int arg3) {}

    @OnEnteredRange(name = "prefetch")
    public void testEnteredPrefetchMethod(
        ComponentContext c, @Prop boolean arg0, @State int arg1) {}

    @OnExitedRange(name = "prefetch")
    public void testExitedPrefetchMethod(ComponentContext c, @Prop T arg2, @TreeProp int arg3) {}
  }

  private SpecModel mSpecModel;

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement =
        elements.getTypeElement(WorkingRangeGeneratorTest.TestSpec.class.getCanonicalName());
    mSpecModel =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mock(Messager.class), RunMode.normal(), null, null);
  }

  @Test
  public void testGenerateDispatchOnEnteredRange() {
    assertThat(WorkingRangeGenerator.generateDispatchOnEnteredRangeMethod(mSpecModel).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "public void dispatchOnEnteredRange(java.lang.String name) {\n"
                + "  com.facebook.litho.ComponentContext c = getScopedContext();\n"
                + "  switch (name) {\n"
                + "    case \"enter\": {\n"
                + "      testEnteredRangeMethod(c);\n"
                + "      return;\n"
                + "    }\n"
                + "    case \"prefetch\": {\n"
                + "      testEnteredPrefetchMethod(c);\n"
                + "      return;\n"
                + "    }\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateDispatchOnExitedRange() {
    assertThat(WorkingRangeGenerator.generateDispatchOnExitedRangeMethod(mSpecModel).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "public void dispatchOnExitedRange(java.lang.String name) {\n"
                + "  com.facebook.litho.ComponentContext c = getScopedContext();\n"
                + "  switch (name) {\n"
                + "    case \"exit\": {\n"
                + "      testExitedRangeMethod(c);\n"
                + "      return;\n"
                + "    }\n"
                + "    case \"prefetch\": {\n"
                + "      testExitedPrefetchMethod(c);\n"
                + "      return;\n"
                + "    }\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateWorkingRangeMethodDelegates() {
    TypeSpecDataHolder dataHolder =
        WorkingRangeGenerator.generateWorkingRangeMethodDelegates(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(4);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "private void testEnteredRangeMethod(com.facebook.litho.ComponentContext c) {\n"
                + "  TestSpec.testEnteredRangeMethod(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (boolean) arg0,\n"
                + "    (int) mStateContainer.arg1);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "private void testExitedRangeMethod(com.facebook.litho.ComponentContext c) {\n"
                + "  TestSpec.testExitedRangeMethod(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (T) arg2,\n"
                + "    (int) arg3);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(2).toString())
        .isEqualTo(
            "private void testEnteredPrefetchMethod(com.facebook.litho.ComponentContext c) {\n"
                + "  TestSpec.testEnteredPrefetchMethod(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (boolean) arg0,\n"
                + "    (int) mStateContainer.arg1);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(3).toString())
        .isEqualTo(
            "private void testExitedPrefetchMethod(com.facebook.litho.ComponentContext c) {\n"
                + "  TestSpec.testExitedPrefetchMethod(\n"
                + "    (com.facebook.litho.ComponentContext) c,\n"
                + "    (T) arg2,\n"
                + "    (int) arg3);\n"
                + "}\n");
  }

  @Test
  public void testGenerateStaticRegisterMethods() {
    TypeSpecDataHolder dataHolder = WorkingRangeGenerator.generateStaticRegisterMethods(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(3);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "static void registerEnterWorkingRange(com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.WorkingRange workingRange) {\n"
                + "  if (workingRange == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.Component component = c.getComponentScope();\n"
                + "  registerWorkingRange(\"enter\", workingRange, component);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "static void registerExitWorkingRange(com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.WorkingRange workingRange) {\n"
                + "  if (workingRange == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.Component component = c.getComponentScope();\n"
                + "  registerWorkingRange(\"exit\", workingRange, component);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(2).toString())
        .isEqualTo(
            "static void registerPrefetchWorkingRange(com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.WorkingRange workingRange) {\n"
                + "  if (workingRange == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.Component component = c.getComponentScope();\n"
                + "  registerWorkingRange(\"prefetch\", workingRange, component);\n"
                + "}\n");
  }
}
