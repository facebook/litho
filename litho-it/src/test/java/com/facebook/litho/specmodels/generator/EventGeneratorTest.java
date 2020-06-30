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
import static org.mockito.Mockito.when;

import androidx.annotation.Nullable;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.FieldModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link EventGenerator} */
@RunWith(JUnit4.class)
public class EventGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  @LayoutSpec
  static class TestSpec<T extends CharSequence> {
    @PropDefault protected static boolean arg0 = true;

    @OnEvent(Object.class)
    public void testEventMethod1(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @Param T arg3,
        @FromEvent long arg4,
        @Param @Nullable T arg6) {}

    @OnEvent(Object.class)
    public void testEventMethod2(
        @Prop boolean arg0, @State int arg1, @State(canUpdateLazily = true) long arg5) {}
  }

  private SpecModel mSpecModel;
  private final SpecModel mMockSpecModel = mock(SpecModel.class);

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    mSpecModel =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mock(Messager.class), RunMode.normal(), null, null);

    EventDeclarationModel eventDeclarationModel =
        new EventDeclarationModel(
            ClassName.OBJECT,
            ClassName.OBJECT,
            ImmutableList.of(
                new FieldModel(
                    FieldSpec.builder(TypeName.INT, "field1", Modifier.PUBLIC).build(),
                    new Object()),
                new FieldModel(
                    FieldSpec.builder(TypeName.INT, "field2", Modifier.PUBLIC).build(),
                    new Object())),
            new Object());
    when(mMockSpecModel.getEventDeclarations()).thenReturn(ImmutableList.of(eventDeclarationModel));
    when(mMockSpecModel.getContextClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
    when(mMockSpecModel.getComponentName()).thenReturn("Test");
    when(mMockSpecModel.getScopeMethodName()).thenReturn("getComponentScope");
  }

  @Test
  public void testGenerateEventMethods() {
    TypeSpecDataHolder dataHolder = EventGenerator.generateEventMethods(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(2);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "private void testEventMethod1(com.facebook.litho.HasEventDispatcher _abstract,\n"
                + "    java.lang.Object arg2, T arg3, @androidx.annotation.Nullable T arg6) {\n"
                + "  Test _ref = (Test) _abstract;\n"
                + "  TestSpec.testEventMethod1(\n"
                + "    (boolean) _ref.arg0,\n"
                + "    (int) _ref.mStateContainer.arg1,\n"
                + "    arg2,\n"
                + "    arg3,\n"
                + "    (long) _ref.arg4,\n"
                + "    arg6);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "private void testEventMethod2(com.facebook.litho.HasEventDispatcher _abstract) {\n"
                + "  Test _ref = (Test) _abstract;\n"
                + "  TestStateContainer stateContainer = getStateContainerWithLazyStateUpdatesApplied(c, _ref);\n"
                + "  TestSpec.testEventMethod2(\n"
                + "    (boolean) _ref.arg0,\n"
                + "    (int) stateContainer.arg1,\n"
                + "    (long) stateContainer.arg5);\n"
                + "}\n");
  }

  @Test
  public void testGenerateEventHandlerFactories() {
    TypeSpecDataHolder dataHolder = EventGenerator.generateEventHandlerFactories(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(2);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "public static <T extends java.lang.CharSequence> com.facebook.litho.EventHandler<java.lang.Object> testEventMethod1(com.facebook.litho.ComponentContext c,\n"
                + "    java.lang.Object arg2, T arg3, @androidx.annotation.Nullable T arg6) {\n"
                + "  return newEventHandler(Test.class, \"Test\", c, -1400079064, new Object[] {\n"
                + "        c,\n"
                + "        arg2,\n"
                + "        arg3,\n"
                + "        arg6,\n"
                + "      });\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "public static com.facebook.litho.EventHandler<java.lang.Object> testEventMethod2(com.facebook.litho.ComponentContext c) {\n"
                + "  return newEventHandler(Test.class, \"Test\", c, -1400079063, new Object[] {\n"
                + "        c,\n"
                + "      });\n"
                + "}\n");
  }

  @Test
  public void testGenerateDispatchOnEvent() {
    assertThat(EventGenerator.generateDispatchOnEvent(mSpecModel).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "public java.lang.Object dispatchOnEvent(final com.facebook.litho.EventHandler eventHandler,\n"
                + "    final java.lang.Object eventState) {\n"
                + "  int id = eventHandler.id;\n"
                + "  switch (id) {\n"
                + "    case -1400079064: {\n"
                + "      java.lang.Object _event = (java.lang.Object) eventState;\n"
                + "      testEventMethod1(\n"
                + "            eventHandler.mHasEventDispatcher,\n"
                + "            (java.lang.Object) eventHandler.params[0],\n"
                + "            (T) eventHandler.params[1],\n"
                + "            (T) eventHandler.params[2]);\n"
                + "      return null;\n"
                + "    }\n"
                + "    case -1400079063: {\n"
                + "      java.lang.Object _event = (java.lang.Object) eventState;\n"
                + "      testEventMethod2(\n"
                + "            eventHandler.mHasEventDispatcher);\n"
                + "      return null;\n"
                + "    }\n"
                + "    case -1048037474: {\n"
                + "      dispatchErrorEvent((com.facebook.litho.ComponentContext) eventHandler.params[0], (com.facebook.litho.ErrorEvent) eventState);\n"
                + "      return null;\n"
                + "    }\n"
                + "    default:\n"
                + "        return null;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGetEventHandlerMethods() {
    TypeSpecDataHolder dataHolder = EventGenerator.generateGetEventHandlerMethods(mMockSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);
    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "@androidx.annotation.Nullable\n"
                + "public static com.facebook.litho.EventHandler getObjectHandler(com.facebook.litho.ComponentContext context) {\n"
                + "  if (context.getComponentScope() == null) {\n"
                + "    return null;\n"
                + "  }\n"
                + "  return ((Test) context.getComponentScope()).objectHandler;\n"
                + "}\n");
  }

  @Test
  public void testGenerateEventDispatchers() {
    TypeSpecDataHolder dataHolder = EventGenerator.generateEventDispatchers(mMockSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(1);
    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "static java.lang.Object dispatchObject(com.facebook.litho.EventHandler _eventHandler, int field1,\n"
                + "    int field2) {\n"
                + "  final java.lang.Object _eventState = new java.lang.Object();\n"
                + "  _eventState.field1 = field1;\n"
                + "  _eventState.field2 = field2;\n"
                + "  com.facebook.litho.EventDispatcher _lifecycle = _eventHandler.mHasEventDispatcher.getEventDispatcher();\n"
                + "  return (java.lang.Object) _lifecycle.dispatchOnEvent(_eventHandler, _eventState);\n"
                + "}\n");
  }

  @Test
  public void testGenerateEventDispatchersIgnoresGenericTypesInFields() {
    FieldModel fieldWithTypeArguments =
        new FieldModel(
            FieldSpec.builder(
                    ParameterizedTypeName.get(List.class, String.class), "field1", Modifier.PUBLIC)
                .build(),
            new Object());

    EventDeclarationModel eventDeclarationModel =
        new EventDeclarationModel(
            ClassName.OBJECT,
            ClassName.OBJECT,
            ImmutableList.of(fieldWithTypeArguments),
            new Object());
    when(mMockSpecModel.getEventDeclarations()).thenReturn(ImmutableList.of(eventDeclarationModel));

    TypeSpecDataHolder dataHolder = EventGenerator.generateEventDispatchers(mMockSpecModel);
    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "static java.lang.Object dispatchObject(com.facebook.litho.EventHandler _eventHandler,\n"
                + "    java.util.List field1) {\n"
                + "  final java.lang.Object _eventState = new java.lang.Object();\n"
                + "  _eventState.field1 = field1;\n"
                + "  com.facebook.litho.EventDispatcher _lifecycle = _eventHandler.mHasEventDispatcher.getEventDispatcher();\n"
                + "  return (java.lang.Object) _lifecycle.dispatchOnEvent(_eventHandler, _eventState);\n"
                + "}\n");
  }
}
