/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.specmodels.generator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.annotations.LayoutSpec;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests {@link StateGenerator}
 */
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

  @LayoutSpec
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
    mSpecModelWithState =
        mLayoutSpecModelFactory.create(elements, typeElementWithState, null, null);
    TypeElement typeElementWithoutState =
        elements.getTypeElement(TestWithoutStateSpec.class.getCanonicalName());
    mSpecModelWithoutState =
        mLayoutSpecModelFactory.create(elements, typeElementWithoutState, null, null);
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
            "    com.facebook.litho.ComponentLifecycle.StateContainer _prevStateContainer,\n" +
            "    com.facebook.litho.Component _component) {\n" +
            "  TestWithStateStateContainer prevStateContainer = (TestWithStateStateContainer) _prevStateContainer;\n" +
            "  TestWithState component = (TestWithState) _component;\n" +
            "  component.mStateContainer.arg1 = prevStateContainer.arg1;\n" +
            "  component.mStateContainer.arg4 = prevStateContainer.arg4;\n" +
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
            "  TestWithState.UpdateCurrentStateStateUpdate _stateUpdate = ((TestWithState) _component).createUpdateCurrentStateStateUpdate();\n" +
            "  c.updateStateAsync(_stateUpdate);\n" +
            "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "protected static void updateCurrentState(com.facebook.litho.ComponentContext c) {\n" +
            "  com.facebook.litho.Component _component = c.getComponentScope();\n" +
            "  if (_component == null) {\n" +
            "    return;\n" +
            "  }\n" +
            "  TestWithState.UpdateCurrentStateStateUpdate _stateUpdate = ((TestWithState) _component).createUpdateCurrentStateStateUpdate();\n" +
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
            "  public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer _stateContainer,\n" +
            "      com.facebook.litho.Component newComponent) {\n" +
            "    TestWithStateStateContainer stateContainer = (TestWithStateStateContainer) _stateContainer;\n" +
            "    TestWithState newComponentStateUpdate = (TestWithState) newComponent;\n" +
            "    TestWithStateSpec.updateCurrentState();\n" +
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
            "    public void updateState(com.facebook.litho.ComponentLifecycle.StateContainer _stateContainer,\n" +
            "        com.facebook.litho.Component newComponent) {\n" +
            "      com.facebook.litho.specmodels.generator.StateGeneratorTest.TestWithState newComponentStateUpdate = (com.facebook.litho.specmodels.generator.StateGeneratorTest.TestWithState) newComponent;\n" +
            "      com.facebook.litho.StateValue<java.lang.Boolean> arg4 = new com.facebook.litho.StateValue<java.lang.Boolean>();\n" +
            "      arg4.set(lazyUpdateValue);\n" +
            "      newComponentStateUpdate.mStateContainer.arg4 = arg4.get();\n" +
            "    }\n" +
            "  };\n" +
            "  c.updateStateLazy(_stateUpdate);\n" +
            "}\n");
  }
}
