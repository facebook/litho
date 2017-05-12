/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.StateParamModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;

import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.ClassName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ComponentImplGenerator}
 */
public class ComponentImplGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  @LayoutSpec
  static class TestSpec {
    @PropDefault protected static boolean arg0 = true;

    @OnCreateLayout
    public void testDelegateMethod(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3) {
    }

    @OnEvent(Object.class)
    public void testEventMethod(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @TreeProp long arg3) {}

    @OnUpdateState
    public void testUpdateStateMethod() {}
  }

  private SpecModel mSpecModelDI;

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    mSpecModelDI = LayoutSpecModelFactory.create(elements, typeElement, null);
  }

  @Test
  public void testGenerateStateContainerImpl() {
    assertThat(ComponentImplGenerator.generateStateContainerImpl(mSpecModelDI).toString())
        .isEqualTo(
            "private static class TestStateContainerImpl implements com.facebook.litho.ComponentLifecycle.StateContainer {\n" +
            "  @com.facebook.litho.annotations.State\n" +
            "  int arg1;\n" +
            "}\n");
  }

  @Test
  public void testGetStateContainerImplClassName() {
    assertThat(ComponentImplGenerator.getStateContainerImplClassName(mSpecModelDI))
        .isEqualTo("TestStateContainerImpl");
  }

  @Test
  public void testGenerateStateContainerGetter() {
    assertThat(ComponentImplGenerator.generateStateContainerGetter(
        ClassNames.STATE_CONTAINER_COMPONENT).toString())
        .isEqualTo(
            "@java.lang.Override\n" +
            "protected com.facebook.litho.ComponentLifecycle.StateContainer getStateContainer() {\n" +
            "  return mStateContainerImpl;\n" +
            "}\n");
  }

  @Test
  public void testGenerateProps() {
    TypeSpecDataHolder dataHolder = ComponentImplGenerator.generateProps(mSpecModelDI);
    assertThat(dataHolder.getFieldSpecs()).hasSize(1);
    assertThat(dataHolder.getFieldSpecs().get(0).toString())
        .isEqualTo(
            "@com.facebook.litho.annotations.Prop(\n" +
            "    resType = com.facebook.litho.annotations.ResType.NONE,\n" +
            "    optional = false\n" +
            ")\n" +
            "boolean arg0 = TestSpec.arg0;\n");
  }

  @Test
  public void testGenerateTreeProps() {
    TypeSpecDataHolder dataHolder = ComponentImplGenerator.generateTreeProps(mSpecModelDI);
    assertThat(dataHolder.getFieldSpecs()).hasSize(1);
    assertThat(dataHolder.getFieldSpecs().get(0).toString()).isEqualTo("long arg3;\n");
  }

  @Test
  public void testGenerateInterStageInputs() {
    TypeSpecDataHolder dataHolder = ComponentImplGenerator.generateInterStageInputs(mSpecModelDI);
    assertThat(dataHolder.getFieldSpecs()).hasSize(0);
  }

  @Test
  public void testGenerateEventDeclarations() {
    SpecModel specModel = mock(SpecModel.class);
    when(specModel.getEventDeclarations()).thenReturn(
        ImmutableList.of(
            new EventDeclarationModel(
                ClassName.OBJECT,
                ClassName.OBJECT,
                ImmutableList.<EventDeclarationModel.FieldModel>of(),
                null)));

    TypeSpecDataHolder dataHolder = ComponentImplGenerator.generateEventHandlers(specModel);
    assertThat(dataHolder.getFieldSpecs()).hasSize(1);
    assertThat(dataHolder.getFieldSpecs().get(0).toString())
        .isEqualTo("com.facebook.litho.EventHandler objectHandler;\n");
  }

  @Test
  public void testGenerateGetSimpleName() {
    assertThat(ComponentImplGenerator.generateGetSimpleName(mSpecModelDI).toString())
        .isEqualTo(
            "@java.lang.Override\n" +
            "public java.lang.String getSimpleName() {\n" +
            "  return \"Test\";\n" +
            "}\n");
  }

  @Test
  public void testGenerateEqualsMethod() {
    assertThat(ComponentImplGenerator.generateEqualsMethod(mSpecModelDI, true).toString())
        .isEqualTo(
            "@java.lang.Override\n" +
            "public boolean equals(java.lang.Object other) {\n" +
            "  if (this == other) {\n" +
            "    return true;\n" +
            "  }\n" +
            "  if (other == null || getClass() != other.getClass()) {\n" +
            "    return false;\n" +
            "  }\n" +
            "  TestImpl testImpl = (TestImpl) other;\n" +
            "  if (this.getId() == testImpl.getId()) {\n" +
            "    return true;\n" +
            "  }\n" +
            "  if (arg0 != testImpl.arg0) {\n" +
            "    return false;\n" +
            "  }\n" +
            "  if (mStateContainerImpl.arg1 != testImpl.mStateContainerImpl.arg1) {\n" +
            "    return false;\n" +
            "  }\n" +
            "  if (arg3 != testImpl.arg3) {\n" +
            "    return false;\n" +
            "  }\n" +
            "  return true;\n" +
            "}\n");
  }

  @Test
  public void testOnUpdateStateMethods() {
    TypeSpecDataHolder dataHolder =
        ComponentImplGenerator.generateOnUpdateStateMethods(mSpecModelDI);
    assertThat(dataHolder.getMethodSpecs()).hasSize(1);
    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "private TestUpdateStateMethodStateUpdate createTestUpdateStateMethodStateUpdate() {\n" +
            "  return new TestUpdateStateMethodStateUpdate();\n" +
            "}\n");
  }

  @Test
  public void testGenerateStateParamImplAccessor() {
    StateParamModel stateParamModel = mock(StateParamModel.class);
    when(stateParamModel.getName()).thenReturn("stateParam");
    assertThat(ComponentImplGenerator.getImplAccessor(mSpecModelDI, stateParamModel))
        .isEqualTo("mStateContainerImpl.stateParam");
  }

  @Test
  public void testGeneratePropParamImplAccessor() {
    PropModel propModel = mock(PropModel.class);
    when(propModel.getName()).thenReturn("propParam");
    assertThat(ComponentImplGenerator.getImplAccessor(mSpecModelDI, propModel))
        .isEqualTo("propParam");
  }
}
