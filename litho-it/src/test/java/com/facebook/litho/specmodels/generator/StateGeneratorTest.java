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
import static org.mockito.Mockito.mock;

import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
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

/** Tests {@link StateGenerator} */
@RunWith(JUnit4.class)
public class StateGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  @LayoutSpec
  private static class TestWithStateSpec<T extends CharSequence> {
    @OnCreateLayout
    public void onCreateLayout(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @State(canUpdateLazily = true) boolean arg4) {}

    @OnEvent(Object.class)
    public void testEventMethod2(@Prop boolean arg0, @State int arg1) {}

    @OnUpdateState
    void updateCurrentState() {}
  }

  @LayoutSpec
  private static class TestWithoutStateSpec<T extends CharSequence> {

    @OnCreateLayout
    public void onCreateLayout() {}
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

    @OnEvent(Object.class)
    public void testEventMethod2(@Prop boolean arg0, @State int arg1) {}

    @OnUpdateStateWithTransition
    void updateCurrentState() {}
  }

  @LayoutSpec
  private static class TestWithBothStatesSpec<T extends CharSequence> {
    @OnCreateLayout
    public void onCreateLayout(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @State(canUpdateLazily = true) boolean arg4) {}

    @OnEvent(Object.class)
    public void testEventMethod2(@Prop boolean arg0, @State int arg1) {}

    @OnUpdateState
    void updateCurrentState() {}

    @OnUpdateStateWithTransition
    Transition updateCurrentStateWithTransition() {
      return Transition.create(Transition.TransitionKeyType.GLOBAL, "key").animate(AnimatedProperties.X);
    }
  }

  @LayoutSpec
  private static class TestWithLazyGeneric<T extends CharSequence> {
    @OnCreateLayout
    public void onCreateLayout(@State(canUpdateLazily = true) T arg0) {}

    @OnUpdateState
    void updateCurrentState() {}
  }

  @LayoutSpec
  private static class TestWithLazyMethodGeneric {
    @OnCreateLayout
    public void onCreateLayout(@Prop int arg0) {}

    @OnCreateInitialState
    static <T> void onCreateInitialState(@State(canUpdateLazily = true) T arg1) {}
  }

  @LayoutSpec
  private static class TestWithSameGenericMultipleTimes<T> {

    @OnCreateLayout
    static <T> void onCreateLayout(@State List<T> values) {}

    @OnUpdateState
    static <T> void updateValues(StateValue<List<T>> values, @Param List<T> foos) {}
  }

  @LayoutSpec
  private static class TestWithMultipleGenerics<T, E, D> {

    @OnCreateLayout
    static <E, D> void onCreateLayout(@State Function<E, D> functions) {}

    @OnUpdateState
    static <T, E, D> void updateValues(
        StateValue<Function<E, D>> functions, @Param Function<T, D> foos) {}
  }

  private SpecModel mSpecModelWithState;
  private SpecModel mSpecModelWithoutState;
  private SpecModel mSpecModelWithStateWithTransition;
  private SpecModel mSpecModelWithBothStates;
  private SpecModel mSpecModelWithLazyGeneric;
  private SpecModel mSpecModelWithLazyMethodGeneric;
  private SpecModel mSpecWithSameGenericMultipleTimes;
  private SpecModel mSpecWithMultipleGenerics;

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElementWithState =
        elements.getTypeElement(TestWithStateSpec.class.getCanonicalName());
    mSpecModelWithState =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithState,
            mock(Messager.class),
            RunMode.normal(),
            null,
            null);
    TypeElement typeElementWithoutState =
        elements.getTypeElement(TestWithoutStateSpec.class.getCanonicalName());
    mSpecModelWithoutState =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithoutState,
            mock(Messager.class),
            RunMode.normal(),
            null,
            null);

    TypeElement typeElementWithStateWithTransition =
        elements.getTypeElement(TestWithStateWithTransitionSpec.class.getCanonicalName());
    mSpecModelWithStateWithTransition =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithStateWithTransition,
            mock(Messager.class),
            RunMode.normal(),
            null,
            null);

    TypeElement typeElementWithBothStates =
        elements.getTypeElement(TestWithBothStatesSpec.class.getCanonicalName());
    mSpecModelWithBothStates =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithBothStates,
            mock(Messager.class),
            RunMode.normal(),
            null,
            null);

    final TypeElement typeElementWithLazyGeneric =
        elements.getTypeElement(TestWithLazyGeneric.class.getCanonicalName());
    mSpecModelWithLazyGeneric =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithLazyGeneric,
            mock(Messager.class),
            RunMode.normal(),
            null,
            null);

    final TypeElement typeElementWithLazyMethodGeneric =
        elements.getTypeElement(TestWithLazyMethodGeneric.class.getCanonicalName());
    mSpecModelWithLazyMethodGeneric =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithLazyMethodGeneric,
            mock(Messager.class),
            RunMode.normal(),
            null,
            null);

    final TypeElement typeElementWithSameGenericMultipleTimes =
        elements.getTypeElement(TestWithSameGenericMultipleTimes.class.getCanonicalName());
    mSpecWithSameGenericMultipleTimes =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithSameGenericMultipleTimes,
            mock(Messager.class),
            RunMode.normal(),
            null,
            null);

    final TypeElement typeElementWithMultipleGenerics =
        elements.getTypeElement(TestWithMultipleGenerics.class.getCanonicalName());
    mSpecWithMultipleGenerics =
        mLayoutSpecModelFactory.create(
            elements,
            types,
            typeElementWithMultipleGenerics,
            mock(Messager.class),
            RunMode.normal(),
            null,
            null);
  }

  @Test
  public void testGenerateHasState() {
    TypeSpecDataHolder dataHolder = StateGenerator.generateHasState(mSpecModelWithState);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected boolean hasState() {\n"
                + "  return true;\n"
                + "}\n");
  }

  @Test
  public void testDoNotGenerateState() {
    TypeSpecDataHolder dataHolder = StateGenerator.generateHasState(mSpecModelWithoutState);

    assertThat(dataHolder.getMethodSpecs()).isEmpty();
  }

  @Test
  public void testGenerateTransferState() {
    TypeSpecDataHolder dataHolder = StateGenerator.generateTransferState(mSpecModelWithState);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void transferState(com.facebook.litho.StateContainer _prevStateContainer,\n"
                + "    com.facebook.litho.StateContainer _nextStateContainer) {\n"
                + "  TestWithStateStateContainer<T> prevStateContainer = (TestWithStateStateContainer<T>) _prevStateContainer;\n"
                + "  TestWithStateStateContainer<T> nextStateContainer = (TestWithStateStateContainer<T>) _nextStateContainer;\n"
                + "  nextStateContainer.arg1 = prevStateContainer.arg1;\n"
                + "  nextStateContainer.arg4 = prevStateContainer.arg4;\n"
                + "}\n");
  }

  @Test
  public void testGenerateTransferStateWithTransition() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateTransferState(mSpecModelWithStateWithTransition);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "protected void transferState(com.facebook.litho.StateContainer _prevStateContainer,\n"
                + "    com.facebook.litho.StateContainer _nextStateContainer) {\n"
                + "  TestWithStateWithTransitionStateContainer<T> prevStateContainer = (TestWithStateWithTransitionStateContainer<T>) _prevStateContainer;\n"
                + "  TestWithStateWithTransitionStateContainer<T> nextStateContainer = (TestWithStateWithTransitionStateContainer<T>) _nextStateContainer;\n"
                + "  nextStateContainer.arg1 = prevStateContainer.arg1;\n"
                + "  nextStateContainer.arg4 = prevStateContainer.arg4;\n"
                + "  synchronized (prevStateContainer._transitions) {\n"
                + "    nextStateContainer._transitions = new ArrayList<>(prevStateContainer._transitions);\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testDoNotGenerateTransferState() {
    TypeSpecDataHolder dataHolder = StateGenerator.generateTransferState(mSpecModelWithoutState);

    assertThat(dataHolder.getMethodSpecs()).isEmpty();
  }

  @Test
  public void testGenerateStateContainerWithLazyStateUpdatesApplied_noLazyState() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateGetStateContainerWithLazyStateUpdatesApplied(mSpecModelWithoutState);

    assertThat(dataHolder.getMethodSpecs()).isEmpty();
  }

  @Test
  public void testGenerateStateContainerWithLazyStateUpdatesApplied_withLazyState() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateGetStateContainerWithLazyStateUpdatesApplied(mSpecModelWithState);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "private TestWithStateStateContainer getStateContainerWithLazyStateUpdatesApplied(com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.specmodels.generator.StateGeneratorTest.TestWithState component) {\n"
                + "  TestWithStateStateContainer _stateContainer = new TestWithStateStateContainer();\n"
                + "  transferState(component.mStateContainer, _stateContainer);\n"
                + "  c.applyLazyStateUpdatesForContainer(_stateContainer);\n"
                + "  return _stateContainer;\n"
                + "}\n");
  }

  @Test
  public void testGenerateOnStateUpdateMethods() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateOnStateUpdateMethods(mSpecModelWithState);

    assertThat(dataHolder.getMethodSpecs()).hasSize(3);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "protected static void updateCurrentState(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.StateContainer.StateUpdate _stateUpdate = new com.facebook.litho.StateContainer.StateUpdate(0);\n"
                + "  c.updateStateAsync(_stateUpdate, \"TestWithState.updateCurrentState\");\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "protected static void updateCurrentStateAsync(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.StateContainer.StateUpdate _stateUpdate = new com.facebook.litho.StateContainer.StateUpdate(0);\n"
                + "  c.updateStateAsync(_stateUpdate, \"TestWithState.updateCurrentState\");\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(2).toString())
        .isEqualTo(
            "protected static void updateCurrentStateSync(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.StateContainer.StateUpdate _stateUpdate = new com.facebook.litho.StateContainer.StateUpdate(0);\n"
                + "  c.updateStateSync(_stateUpdate, \"TestWithState.updateCurrentState\");\n"
                + "}\n");
  }

  @Test
  public void testGenerateOnStateUpdateWithTransitionMethods() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateOnStateUpdateMethods(mSpecModelWithStateWithTransition);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "protected static void updateCurrentStateWithTransition(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.StateContainer.StateUpdate _stateUpdate = new com.facebook.litho.StateContainer.StateUpdate(0);\n"
                + "  c.updateStateWithTransition(_stateUpdate, \"TestWithStateWithTransition.updateCurrentState\");\n"
                + "}\n");
  }

  @Test
  public void testGenerateLazyStateUpdateMethods() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateLazyStateUpdateMethods(mSpecModelWithState);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "protected static void lazyUpdateArg4(com.facebook.litho.ComponentContext c,\n"
                + "    final boolean lazyUpdateValue) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.StateContainer.StateUpdate _stateUpdate = new com.facebook.litho.StateContainer.StateUpdate(-2147483648, lazyUpdateValue);\n"
                + "  c.updateStateLazy(_stateUpdate);\n"
                + "}\n");
  }

  @Test
  public void testGenerateLazyStateUpdateMethodsForGenerics() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateLazyStateUpdateMethods(mSpecModelWithLazyGeneric);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "protected static <T extends java.lang.CharSequence> void lazyUpdateArg0(com.facebook.litho.ComponentContext c,\n"
                + "    final T lazyUpdateValue) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.StateContainer.StateUpdate _stateUpdate = new com.facebook.litho.StateContainer.StateUpdate(-2147483648, lazyUpdateValue);\n"
                + "  c.updateStateLazy(_stateUpdate);\n"
                + "}\n");
  }

  @Test
  public void testGenerateLazyStateUpdateMethodsForGenericMethod() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateLazyStateUpdateMethods(mSpecModelWithLazyMethodGeneric);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "protected static <T> void lazyUpdateArg1(com.facebook.litho.ComponentContext c,\n"
                + "    final T lazyUpdateValue) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  com.facebook.litho.StateContainer.StateUpdate _stateUpdate = new com.facebook.litho.StateContainer.StateUpdate(-2147483648, lazyUpdateValue);\n"
                + "  c.updateStateLazy(_stateUpdate);\n"
                + "}\n");
  }
}
