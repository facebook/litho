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

import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.OnUpdateStateWithTransition;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import java.util.List;
import java.util.function.Function;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests {@link StateContainerGenerator} */
@RunWith(JUnit4.class)
public class StateContainerGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();
  @Mock private Messager mMessager;

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  private SpecModel mSpecModelWithState;
  private SpecModel mSpecModelWithStateWithTransition;
  private SpecModel mSpecModelWithBothMethods;
  private SpecModel mSpecModelWithSameGenericMultipleTimes;
  private SpecModel mSpecModelWithMultipleGenerics;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    final Elements elements = mCompilationRule.getElements();
    final Types types = mCompilationRule.getTypes();

    final TypeElement typeElementWithState =
        elements.getTypeElement(TestWithStateSpec.class.getCanonicalName());
    mSpecModelWithState =
        mLayoutSpecModelFactory.create(
            elements, types, typeElementWithState, mMessager, RunMode.normal(), null, null);

    final TypeElement typeElementWithTransition =
        elements.getTypeElement(TestWithStateWithTransitionSpec.class.getCanonicalName());
    mSpecModelWithStateWithTransition =
        mLayoutSpecModelFactory.create(
            elements, types, typeElementWithTransition, mMessager, RunMode.normal(), null, null);

    final TypeElement typeElementWithBothMethods =
        elements.getTypeElement(TestWithBothMethodsSpec.class.getCanonicalName());
    mSpecModelWithBothMethods =
        mLayoutSpecModelFactory.create(
            elements, types, typeElementWithBothMethods, mMessager, RunMode.normal(), null, null);

    final TypeElement typeElementWithSameGenericMultipleTimes =
        elements.getTypeElement(TestWithSameGenericMultipleTimesSpec.class.getCanonicalName());
    mSpecModelWithSameGenericMultipleTimes =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithSameGenericMultipleTimes,
            mMessager,
            RunMode.normal(),
            null,
            null);

    final TypeElement typeElementWithMultipleGenerics =
        elements.getTypeElement(TestWithMultipleGenericsSpec.class.getCanonicalName());
    mSpecModelWithMultipleGenerics =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithMultipleGenerics,
            mMessager,
            RunMode.normal(),
            null,
            null);
  }

  @Test
  public void testGetStateContainerClassName() {
    assertThat(StateContainerGenerator.getStateContainerClassName(mSpecModelWithState))
        .isEqualTo("TestWithStateStateContainer");
  }

  @Test
  public void testGenerateStateContainerImpl() {
    assertThat(StateContainerGenerator.generate(mSpecModelWithState, RunMode.normal()).toString())
        .isEqualTo(
            "@androidx.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "@com.facebook.litho.annotations.Generated\n"
                + "static class TestWithStateStateContainer<T extends java.lang.CharSequence> extends com.facebook.litho.StateContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 3\n"
                + "  )\n"
                + "  int arg1;\n"
                + "\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 3\n"
                + "  )\n"
                + "  boolean arg4;\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void applyStateUpdate(com.facebook.litho.StateContainer.StateUpdate stateUpdate) {\n"
                + "    com.facebook.litho.StateValue<java.lang.Integer> arg1;\n"
                + "    com.facebook.litho.StateValue<java.lang.Boolean> arg4;\n"
                + "\n"
                + "    final java.lang.Object[] params = stateUpdate.params;\n"
                + "    switch (stateUpdate.type) {\n"
                + "      case 0:\n"
                + "        TestWithStateSpec.testUpdateState();\n"
                + "        break;\n"
                + "\n"
                + "      case -2147483648:\n"
                + "        this.arg4 = (boolean) params[0];\n"
                + "        break;\n"
                + "    }\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateContainerWithTransitionImpl() {
    assertThat(
            StateContainerGenerator.generate(mSpecModelWithStateWithTransition, RunMode.normal())
                .toString())
        .isEqualTo(
            "@androidx.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "@com.facebook.litho.annotations.Generated\n"
                + "static class TestWithStateWithTransitionStateContainer<T extends java.lang.CharSequence> extends com.facebook.litho.StateContainer implements com.facebook.litho.Component.TransitionContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 3\n"
                + "  )\n"
                + "  int arg1;\n"
                + "\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 3\n"
                + "  )\n"
                + "  boolean arg4;\n"
                + "\n"
                + "  com.facebook.litho.Transition _transition;\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  @androidx.annotation.Nullable\n"
                + "  public com.facebook.litho.Transition consumeTransition() {\n"
                + "    com.facebook.litho.Transition transitionCopy = _transition;\n"
                + "    _transition = null;\n"
                + "    return transitionCopy;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void applyStateUpdate(com.facebook.litho.StateContainer.StateUpdate stateUpdate) {\n"
                + "    com.facebook.litho.StateValue<java.lang.Integer> arg1;\n"
                + "    com.facebook.litho.StateValue<java.lang.Boolean> arg4;\n"
                + "\n"
                + "    final java.lang.Object[] params = stateUpdate.params;\n"
                + "    switch (stateUpdate.type) {\n"
                + "      case 0:\n"
                + "        _transition = TestWithStateWithTransitionSpec.testUpdateStateWithTransition();\n"
                + "        break;\n"
                + "\n"
                + "      case -2147483648:\n"
                + "        this.arg4 = (boolean) params[0];\n"
                + "        break;\n"
                + "    }\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateContainerWithBothMethodsImpl() {
    assertThat(
            StateContainerGenerator.generate(mSpecModelWithBothMethods, RunMode.normal())
                .toString())
        .isEqualTo(
            "@androidx.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "@com.facebook.litho.annotations.Generated\n"
                + "static class TestWithBothMethodsStateContainer<T extends java.lang.CharSequence> extends com.facebook.litho.StateContainer implements com.facebook.litho.Component.TransitionContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 3\n"
                + "  )\n"
                + "  int arg1;\n"
                + "\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 3\n"
                + "  )\n"
                + "  boolean arg4;\n"
                + "\n"
                + "  com.facebook.litho.Transition _transition;\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  @androidx.annotation.Nullable\n"
                + "  public com.facebook.litho.Transition consumeTransition() {\n"
                + "    com.facebook.litho.Transition transitionCopy = _transition;\n"
                + "    _transition = null;\n"
                + "    return transitionCopy;\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void applyStateUpdate(com.facebook.litho.StateContainer.StateUpdate stateUpdate) {\n"
                + "    com.facebook.litho.StateValue<java.lang.Integer> arg1;\n"
                + "    com.facebook.litho.StateValue<java.lang.Boolean> arg4;\n"
                + "\n"
                + "    final java.lang.Object[] params = stateUpdate.params;\n"
                + "    switch (stateUpdate.type) {\n"
                + "      case 0:\n"
                + "        TestWithBothMethodsSpec.testUpdateState();\n"
                + "        break;\n"
                + "\n"
                + "      case 1:\n"
                + "        _transition = TestWithBothMethodsSpec.testUpdateStateWithTransition();\n"
                + "        break;\n"
                + "\n"
                + "      case -2147483648:\n"
                + "        this.arg4 = (boolean) params[0];\n"
                + "        break;\n"
                + "    }\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateContainerWithSameGenericMultipleTimesImpl() {
    assertThat(
            StateContainerGenerator.generate(
                    mSpecModelWithSameGenericMultipleTimes, RunMode.normal())
                .toString())
        .isEqualTo(
            "@androidx.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "@com.facebook.litho.annotations.Generated\n"
                + "static class TestWithSameGenericMultipleTimesStateContainer<T> extends com.facebook.litho.StateContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 5\n"
                + "  )\n"
                + "  java.util.List<T> values;\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void applyStateUpdate(com.facebook.litho.StateContainer.StateUpdate stateUpdate) {\n"
                + "    com.facebook.litho.StateValue<java.util.List<T>> values;\n"
                + "\n"
                + "    final java.lang.Object[] params = stateUpdate.params;\n"
                + "    switch (stateUpdate.type) {\n"
                + "      case 0:\n"
                + "        values = new com.facebook.litho.StateValue<java.util.List<T>>();\n"
                + "        values.set(this.values);\n"
                + "        TestWithSameGenericMultipleTimesSpec.updateValues(values, (java.util.List<T>) params[0]);\n"
                + "        this.values = values.get();\n"
                + "        break;\n"
                + "    }\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateContainerWithMultipleGenericsImpl() {
    assertThat(
            StateContainerGenerator.generate(mSpecModelWithMultipleGenerics, RunMode.normal())
                .toString())
        .isEqualTo(
            "@androidx.annotation.VisibleForTesting(\n"
                + "    otherwise = 2\n"
                + ")\n"
                + "@com.facebook.litho.annotations.Generated\n"
                + "static class TestWithMultipleGenericsStateContainer<T, E, D> extends com.facebook.litho.StateContainer {\n"
                + "  @com.facebook.litho.annotations.State\n"
                + "  @com.facebook.litho.annotations.Comparable(\n"
                + "      type = 13\n"
                + "  )\n"
                + "  java.util.function.Function<E, D> functions;\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void applyStateUpdate(com.facebook.litho.StateContainer.StateUpdate stateUpdate) {\n"
                + "    com.facebook.litho.StateValue<java.util.function.Function<E, D>> functions;\n"
                + "\n"
                + "    final java.lang.Object[] params = stateUpdate.params;\n"
                + "    switch (stateUpdate.type) {\n"
                + "      case 0:\n"
                + "        functions = new com.facebook.litho.StateValue<java.util.function.Function<E, D>>();\n"
                + "        functions.set(this.functions);\n"
                + "        TestWithMultipleGenericsSpec.updateValues(functions, (java.util.function.Function<T, D>) params[0]);\n"
                + "        this.functions = functions.get();\n"
                + "        break;\n"
                + "    }\n"
                + "  }\n"
                + "}\n");
  }

  @LayoutSpec
  private static class TestWithStateSpec<T extends CharSequence> {
    @OnCreateLayout
    public void onCreateLayout(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @State(canUpdateLazily = true) boolean arg4) {}

    @OnUpdateState
    void testUpdateState() {}
  }

  @LayoutSpec
  private static class TestWithStateWithTransitionSpec<T extends CharSequence> {
    @OnCreateLayout
    public void onCreateLayout(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @State(canUpdateLazily = true) boolean arg4) {}

    @OnUpdateStateWithTransition
    Transition testUpdateStateWithTransition() {
      return null;
    }
  }

  @LayoutSpec
  private static class TestWithBothMethodsSpec<T extends CharSequence> {
    @OnCreateLayout
    public void onCreateLayout(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @State(canUpdateLazily = true) boolean arg4) {}

    @OnUpdateState
    void testUpdateState() {}

    @OnUpdateStateWithTransition
    Transition testUpdateStateWithTransition() {
      return Transition.create(Transition.TransitionKeyType.GLOBAL, "key")
          .animate(AnimatedProperties.X);
    }
  }

  @LayoutSpec
  private static class TestWithSameGenericMultipleTimesSpec<T> {

    @OnCreateLayout
    static <T> void onCreateLayout(@State List<T> values) {}

    @OnUpdateState
    static <T> void updateValues(StateValue<List<T>> values, @Param List<T> foos) {}
  }

  @LayoutSpec
  private static class TestWithMultipleGenericsSpec<T, E, D> {

    @OnCreateLayout
    static <E, D> void onCreateLayout(@State Function<E, D> functions) {}

    @OnUpdateState
    static <T, E, D> void updateValues(
        StateValue<Function<E, D>> functions, @Param Function<T, D> foos) {}
  }
}
