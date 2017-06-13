/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.specmodels.generator;

import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests {@link StateGenerator}
 */
public class StateGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private static class TestWithStateSpec<T extends CharSequence> {
    @OnCreateLayout
    public void onCreateLayout(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3,
        @State(canUpdateLazily = true) boolean arg4) {
    }

    @OnEvent(Object.class)
    public void testEventMethod2(
        @Prop boolean arg0,
        @State int arg1) {}

    @OnUpdateState
    void updateCurrentState() {
    }
  }

  private static class TestWithoutStateSpec<T extends CharSequence> {

    @OnCreateLayout
    public void onCreateLayout() {
    }
  }

  private SpecModel mSpecModelWithState;
  private SpecModel mSpecModelWithoutState;

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    TypeElement typeElementWithState =
        elements.getTypeElement(TestWithStateSpec.class.getCanonicalName());
    mSpecModelWithState = LayoutSpecModelFactory.create(elements, typeElementWithState, null);
    TypeElement typeElementWithoutState =
        elements.getTypeElement(TestWithoutStateSpec.class.getCanonicalName());
    mSpecModelWithoutState = LayoutSpecModelFactory.create(elements, typeElementWithoutState, null);
  }

  @Test
  public void testGenerateHasState() {
    TypeSpecDataHolder dataHolder = StateGenerator.generateHasState(mSpecModelWithState);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@java.lang.Override\n" +
            "protected boolean hasState() {\n" +
            "  return true;\n" +
            "}\n");
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
            "@java.lang.Override\n" +
            "protected void transferState(com.facebook.litho.ComponentContext context,\n" +
            "    com.facebook.litho.ComponentLifecycle.StateContainer prevStateContainer,\n" +
            "    com.facebook.litho.Component component) {\n" +
            "  TestWithStateStateContainerImpl prevStateContainerImpl = (TestWithStateStateContainerImpl) prevStateContainer;\n" +
            "  TestWithStateImpl componentImpl = (TestWithStateImpl) component;\n" +
            "  componentImpl.mStateContainerImpl.arg1 = prevStateContainerImpl.arg1;\n" +
            "  componentImpl.mStateContainerImpl.arg4 = prevStateContainerImpl.arg4;\n" +
            "}\n");
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

    assertThat(dataHolder.getMethodSpecs()).hasSize(2);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "protected static void updateCurrentStateAsync(com.facebook.litho.ComponentContext c) {\n" +
            "  com.facebook.litho.Component _component = c.getComponentScope();\n" +
            "  if (_component == null) {\n" +
            "    return;\n" +
            "  }\n" +
            "  TestWithState.UpdateCurrentStateStateUpdate _stateUpdate = ((TestWithState.TestWithStateImpl) _component).createUpdateCurrentStateStateUpdate();\n" +
            "  c.updateStateAsync(_stateUpdate);\n" +
            "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "protected static void updateCurrentState(com.facebook.litho.ComponentContext c) {\n" +
            "  com.facebook.litho.Component _component = c.getComponentScope();\n" +
            "  if (_component == null) {\n" +
            "    return;\n" +
            "  }\n" +
            "  TestWithState.UpdateCurrentStateStateUpdate _stateUpdate = ((TestWithState.TestWithStateImpl) _component).createUpdateCurrentStateStateUpdate();\n" +
            "  c.updateState(_stateUpdate);\n" +
            "}\n");
  }

  @Test
  public void testGenerateStateUpdateClasses() {
    TypeSpecDataHolder dataHolder = StateGenerator.generateStateUpdateClasses(mSpecModelWithState);

    assertThat(dataHolder.getTypeSpecs()).hasSize(1);

    assertThat(dataHolder.getTypeSpecs().get(0).toString())
        .isEqualTo(
            "private static class UpdateCurrentStateStateUpdate implements com.facebook.litho.ComponentLifecycle.StateUpdate {\n" +
            "  UpdateCurrentStateStateUpdate() {\n" +
            "  }\n" +
            "\n" +
            "  public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer stateContainer,\n" +
            "      com.facebook.litho.Component newComponent) {\n" +
            "    TestWithStateStateContainerImpl stateContainerImpl = (TestWithStateStateContainerImpl) stateContainer;\n" +
            "    TestWithStateImpl newComponentStateUpdate = (TestWithStateImpl) newComponent;\n" +
            "    TestWithStateSpec.updateCurrentState();\n" +
            "  }\n" +
            "\n" +
            "  public boolean isLazyStateUpdate() {\n" +
            "    return false;\n" +
            "  }\n" +
            "}\n");
  }

  @Test
  public void testGenerateLazyStateUpdateMethods() {
    TypeSpecDataHolder dataHolder =
        StateGenerator.generateLazyStateUpdateMethods(mSpecModelWithState);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "protected static void lazyUpdateArg4(com.facebook.litho.ComponentContext c,\n" +
            "    final boolean lazyUpdateValue) {\n" +
            "  com.facebook.litho.Component _component = c.getComponentScope();\n" +
            "  if (_component == null) {\n" +
            "    return;\n" +
            "  }\n" +
            "  com.facebook.litho.ComponentLifecycle.StateUpdate _stateUpdate = new com.facebook.litho.ComponentLifecycle.StateUpdate() {\n" +
            "    public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer stateContainer,\n" +
            "        com.facebook.litho.Component newComponent) {\n" +
            "      TestWithState.TestWithStateImpl newComponentStateUpdate = (TestWithState.TestWithStateImpl) newComponent;\n" +
            "      com.facebook.litho.StateValue<java.lang.Boolean> arg4 = new com.facebook.litho.StateValue<java.lang.Boolean>();\n" +
            "      arg4.set(lazyUpdateValue);\n" +
            "      newComponentStateUpdate.mStateContainerImpl.arg4 = arg4.get();\n" +
            "    }\n" +
            "\n" +
            "    public boolean isLazyStateUpdate() {\n" +
            "      return true;\n" +
            "    }\n" +
            "  };\n" +
            "  c.updateStateLazy(_stateUpdate);\n" +
            "}\n");
  }
}
