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

import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.LayoutSpec;
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
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link StateGenerator} */
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
      return Transition.create("key").animate(AnimatedProperties.X);
    }
  }

  private SpecModel mSpecModelWithState;
  private SpecModel mSpecModelWithoutState;
  private SpecModel mSpecModelWithStateWithTransition;
  private SpecModel mSpecModelWithBothStates;

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
            RunMode.NORMAL,
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
            RunMode.NORMAL,
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
            RunMode.NORMAL,
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
            RunMode.NORMAL,
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
                + "protected void transferState(com.facebook.litho.ComponentContext context,\n"
                + "    com.facebook.litho.ComponentLifecycle.StateContainer _prevStateContainer) {\n"
                + "  TestWithStateStateContainer prevStateContainer = (TestWithStateStateContainer) _prevStateContainer;\n"
                + "  mStateContainer.arg1 = prevStateContainer.arg1;\n"
                + "  mStateContainer.arg4 = prevStateContainer.arg4;\n"
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
                + "protected void transferState(com.facebook.litho.ComponentContext context,\n"
                + "    com.facebook.litho.ComponentLifecycle.StateContainer _prevStateContainer) {\n"
                + "  TestWithStateWithTransitionStateContainer prevStateContainer = (TestWithStateWithTransitionStateContainer) _prevStateContainer;\n"
                + "  mStateContainer.arg1 = prevStateContainer.arg1;\n"
                + "  mStateContainer.arg4 = prevStateContainer.arg4;\n"
                + "  mStateContainer._transitions = prevStateContainer._transitions;\n"
                + "}\n");
  }

  @Test
  public void testDoNotGenerateTransferState() {
    TypeSpecDataHolder dataHolder = StateGenerator.generateTransferState(mSpecModelWithoutState);

    assertThat(dataHolder.getMethodSpecs()).isEmpty();
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
                + "  TestWithState.UpdateCurrentStateStateUpdate _stateUpdate = ((TestWithState) _component).createUpdateCurrentStateStateUpdate();\n"
                + "  c.updateStateSync(_stateUpdate);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "protected static void updateCurrentStateAsync(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  TestWithState.UpdateCurrentStateStateUpdate _stateUpdate = ((TestWithState) _component).createUpdateCurrentStateStateUpdate();\n"
                + "  c.updateStateAsync(_stateUpdate);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(2).toString())
        .isEqualTo(
            "protected static void updateCurrentStateSync(com.facebook.litho.ComponentContext c) {\n"
                + "  com.facebook.litho.Component _component = c.getComponentScope();\n"
                + "  if (_component == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  TestWithState.UpdateCurrentStateStateUpdate _stateUpdate = ((TestWithState) _component).createUpdateCurrentStateStateUpdate();\n"
                + "  c.updateStateSync(_stateUpdate);\n"
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
                + "  TestWithStateWithTransition.UpdateCurrentStateStateUpdate _stateUpdate = ((TestWithStateWithTransition) _component).createUpdateCurrentStateStateUpdate();\n"
                + "  c.updateStateWithTransition(_stateUpdate);\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateUpdateClasses() {
    TypeSpecDataHolder dataHolder = StateGenerator.generateStateUpdateClasses(mSpecModelWithState);

    assertThat(dataHolder.getTypeSpecs()).hasSize(1);

    assertThat(dataHolder.getTypeSpecs().get(0).toString())
        .isEqualTo(
            "private static class UpdateCurrentStateStateUpdate implements com.facebook.litho.ComponentLifecycle.StateUpdate {\n"
                + "  UpdateCurrentStateStateUpdate() {\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer _stateContainer,\n"
                + "      com.facebook.litho.Component newComponent) {\n"
                + "    TestWithStateStateContainer stateContainer = (TestWithStateStateContainer) _stateContainer;\n"
                + "    TestWithState newComponentStateUpdate = (TestWithState) newComponent;\n"
                + "    TestWithStateSpec.updateCurrentState();\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateUpdateWithTransitionClasses() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateStateUpdateClasses(mSpecModelWithStateWithTransition);

    assertThat(dataHolder.getTypeSpecs()).hasSize(1);

    assertThat(dataHolder.getTypeSpecs().get(0).toString())
        .isEqualTo(
            "private static class UpdateCurrentStateStateUpdate implements com.facebook.litho.ComponentLifecycle.StateUpdate {\n"
                + "  UpdateCurrentStateStateUpdate() {\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer _stateContainer,\n"
                + "      com.facebook.litho.Component newComponent) {\n"
                + "    TestWithStateWithTransitionStateContainer stateContainer = (TestWithStateWithTransitionStateContainer) _stateContainer;\n"
                + "    TestWithStateWithTransition newComponentStateUpdate = (TestWithStateWithTransition) newComponent;\n"
                + "    com.facebook.litho.Transition transition = TestWithStateWithTransitionSpec.updateCurrentState();\n"
                + "    if (transition != null) {\n"
                + "      newComponentStateUpdate.mStateContainer._transitions.add(transition);\n"
                + "    }\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateStateUpdateWithBothTransitionAndRegularClasses() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateStateUpdateClasses(mSpecModelWithBothStates);

    assertThat(dataHolder.getTypeSpecs()).hasSize(2);

    assertThat(dataHolder.getTypeSpecs().get(0).toString())
        .isEqualTo(
            "private static class UpdateCurrentStateStateUpdate implements com.facebook.litho.ComponentLifecycle.StateUpdate {\n"
                + "  UpdateCurrentStateStateUpdate() {\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer _stateContainer,\n"
                + "      com.facebook.litho.Component newComponent) {\n"
                + "    TestWithBothStatesStateContainer stateContainer = (TestWithBothStatesStateContainer) _stateContainer;\n"
                + "    TestWithBothStates newComponentStateUpdate = (TestWithBothStates) newComponent;\n"
                + "    TestWithBothStatesSpec.updateCurrentState();\n"
                + "  }\n"
                + "}\n");

    assertThat(dataHolder.getTypeSpecs().get(1).toString())
        .isEqualTo(
            "private static class UpdateCurrentStateWithTransitionStateUpdate implements com.facebook.litho.ComponentLifecycle.StateUpdate {\n"
                + "  UpdateCurrentStateWithTransitionStateUpdate() {\n"
                + "  }\n"
                + "\n"
                + "  @java.lang.Override\n"
                + "  public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer _stateContainer,\n"
                + "      com.facebook.litho.Component newComponent) {\n"
                + "    TestWithBothStatesStateContainer stateContainer = (TestWithBothStatesStateContainer) _stateContainer;\n"
                + "    TestWithBothStates newComponentStateUpdate = (TestWithBothStates) newComponent;\n"
                + "    com.facebook.litho.Transition transition = TestWithBothStatesSpec.updateCurrentStateWithTransition();\n"
                + "    if (transition != null) {\n"
                + "      newComponentStateUpdate.mStateContainer._transitions.add(transition);\n"
                + "    }\n"
                + "  }\n"
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
                + "  com.facebook.litho.ComponentLifecycle.StateUpdate _stateUpdate = new com.facebook.litho.ComponentLifecycle.StateUpdate() {\n"
                + "    @java.lang.Override\n"
                + "    public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer _stateContainer,\n"
                + "        com.facebook.litho.Component newComponent) {\n"
                + "      com.facebook.litho.specmodels.generator.StateGeneratorTest.TestWithState newComponentStateUpdate = (com.facebook.litho.specmodels.generator.StateGeneratorTest.TestWithState) newComponent;\n"
                + "      com.facebook.litho.StateValue<java.lang.Boolean> arg4 = new com.facebook.litho.StateValue<java.lang.Boolean>();\n"
                + "      arg4.set(lazyUpdateValue);\n"
                + "      newComponentStateUpdate.mStateContainer.arg4 = arg4.get();\n"
                + "    }\n"
                + "  };\n"
                + "  c.updateStateLazy(_stateUpdate);\n"
                + "}\n");
  }
}
