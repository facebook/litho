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

import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
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

/** Tests {@link TriggerGenerator} */
@RunWith(JUnit4.class)
public class TriggerGeneratorTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final LayoutSpecModelFactory mLayoutSpecModelFactory = new LayoutSpecModelFactory();

  @LayoutSpec
  static class TestSpec<T extends CharSequence> {
    @PropDefault protected static final boolean arg0 = true;

    @OnTrigger(TestEvent.class)
    public Object testTriggerMethod1(
        @Prop boolean arg0,
        @State int arg1,
        @Param Object arg2,
        @Param T arg3,
        @FromTrigger long arg4) {

      return null;
    }

    @OnTrigger(Object.class)
    public void testTriggerMethod2(@Prop boolean arg0, @State int arg1) {}
  }

  @Event(returnType = Object.class)
  static class TestEvent {
    public long arg4;
  }

  private SpecModel mSpecModel;

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement = elements.getTypeElement(TestSpec.class.getCanonicalName());
    mSpecModel =
        mLayoutSpecModelFactory.create(
            elements, types, typeElement, mock(Messager.class), RunMode.normal(), null, null);
  }

  @Test
  public void testGenerateAcceptTriggerEvent() {
    assertThat(TriggerGenerator.generateAcceptTriggerEvent(mSpecModel).toString())
        .isEqualTo(
            "@java.lang.Override\n"
                + "public java.lang.Object acceptTriggerEvent(final com.facebook.litho.EventTrigger eventTrigger,\n"
                + "    final java.lang.Object eventState, final java.lang.Object[] params) {\n"
                + "  int id = eventTrigger.mId;\n"
                + "  switch(id) {\n"
                + "    case -773082596: {\n"
                + "      com.facebook.litho.specmodels.generator.TriggerGeneratorTest.TestEvent _event = (com.facebook.litho.specmodels.generator.TriggerGeneratorTest.TestEvent) eventState;\n"
                + "      return testTriggerMethod1(\n"
                + "            eventTrigger.mTriggerTarget,\n"
                + "            (java.lang.Object) params[0],\n"
                + "            (T) params[1],\n"
                + "            _event.arg4);\n"
                + "    }\n"
                + "    case 969727739: {\n"
                + "      java.lang.Object _event = (java.lang.Object) eventState;\n"
                + "      testTriggerMethod2(\n"
                + "            eventTrigger.mTriggerTarget);\n"
                + "      return null;\n"
                + "    }\n"
                + "    default:\n"
                + "        return null;\n"
                + "  }\n"
                + "}\n");
  }

  @Test
  public void testGenerateOnTriggerMethods() {
    TypeSpecDataHolder dataHolder = TriggerGenerator.generateOnTriggerMethodDelegates(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(2);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "private java.lang.Object testTriggerMethod1(com.facebook.litho.EventTriggerTarget _abstract,\n"
                + "    java.lang.Object arg2, T arg3, long arg4) {\n"
                + "  Test _ref = (Test) _abstract;\n"
                + "  java.lang.Object _result = (java.lang.Object) TestSpec.testTriggerMethod1(\n"
                + "    (boolean) _ref.arg0,\n"
                + "    (int) _ref.mStateContainer.arg1,\n"
                + "    arg2,\n"
                + "    arg3,\n"
                + "    arg4);\n"
                + "  return _result;\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "private void testTriggerMethod2(com.facebook.litho.EventTriggerTarget _abstract) {\n"
                + "  Test _ref = (Test) _abstract;\n"
                + "  TestSpec.testTriggerMethod2(\n"
                + "    (boolean) _ref.arg0,\n"
                + "    (int) _ref.mStateContainer.arg1);\n"
                + "}\n");
  }

  @Test
  public void testGenerateStaticTriggerMethod() {
    TypeSpecDataHolder dataHolder = TriggerGenerator.generateStaticTriggerMethods(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(8);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "public static <T extends java.lang.CharSequence> java.lang.Object testTriggerMethod1(com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.Handle handle, java.lang.Object arg2, T arg3, long arg4) {\n"
                + "  int methodId = -773082596;\n"
                + "  com.facebook.litho.EventTrigger trigger = getEventTrigger(c, methodId, handle);\n"
                + "  if (trigger == null) {\n"
                + "    return null;\n"
                + "  }\n"
                + "  com.facebook.litho.specmodels.generator.TriggerGeneratorTest.TestEvent _eventState = new com.facebook.litho.specmodels.generator.TriggerGeneratorTest.TestEvent();\n"
                + "  _eventState.arg4 = arg4;\n"
                + "  return (java.lang.Object) trigger.dispatchOnTrigger(_eventState, new Object[] {\n"
                + "        arg2,\n"
                + "        arg3,\n"
                + "      });\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "public static <T extends java.lang.CharSequence> java.lang.Object testTriggerMethod1(com.facebook.litho.ComponentContext c,\n"
                + "    java.lang.String key, java.lang.Object arg2, T arg3, long arg4) {\n"
                + "  int methodId = -773082596;\n"
                + "  com.facebook.litho.EventTrigger trigger = getEventTrigger(c, methodId, key);\n"
                + "  if (trigger == null) {\n"
                + "    return null;\n"
                + "  }\n"
                + "  com.facebook.litho.specmodels.generator.TriggerGeneratorTest.TestEvent _eventState = new com.facebook.litho.specmodels.generator.TriggerGeneratorTest.TestEvent();\n"
                + "  _eventState.arg4 = arg4;\n"
                + "  return (java.lang.Object) trigger.dispatchOnTrigger(_eventState, new Object[] {\n"
                + "        arg2,\n"
                + "        arg3,\n"
                + "      });\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(2).toString())
        .isEqualTo(
            "public static <T extends java.lang.CharSequence> java.lang.Object testTriggerMethod1(com.facebook.litho.EventTrigger trigger,\n"
                + "    java.lang.Object arg2, T arg3, long arg4) {\n"
                + "  com.facebook.litho.specmodels.generator.TriggerGeneratorTest.TestEvent _eventState = new com.facebook.litho.specmodels.generator.TriggerGeneratorTest.TestEvent();\n"
                + "  _eventState.arg4 = arg4;\n"
                + "  return (java.lang.Object) trigger.dispatchOnTrigger(_eventState, new Object[] {\n"
                + "        arg2,\n"
                + "        arg3,\n"
                + "      });\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(3).toString())
        .isEqualTo(
            "static <T extends java.lang.CharSequence> java.lang.Object testTriggerMethod1(com.facebook.litho.ComponentContext c,\n"
                + "    java.lang.Object arg2, T arg3, long arg4) {\n"
                + "  Test component = (Test) c.getComponentScope();\n"
                + "  return component.testTriggerMethod1(\n"
                + "      (com.facebook.litho.EventTriggerTarget) component,\n"
                + "      arg2,\n"
                + "      arg3,\n"
                + "      arg4);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(4).toString())
        .isEqualTo(
            "public static void testTriggerMethod2(com.facebook.litho.ComponentContext c,\n"
                + "    com.facebook.litho.Handle handle) {\n"
                + "  int methodId = 969727739;\n"
                + "  com.facebook.litho.EventTrigger trigger = getEventTrigger(c, methodId, handle);\n"
                + "  if (trigger == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  java.lang.Object _eventState = new java.lang.Object();\n"
                + "  trigger.dispatchOnTrigger(_eventState, new Object[] {\n"
                + "      });\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(5).toString())
        .isEqualTo(
            "public static void testTriggerMethod2(com.facebook.litho.ComponentContext c, java.lang.String key) {\n"
                + "  int methodId = 969727739;\n"
                + "  com.facebook.litho.EventTrigger trigger = getEventTrigger(c, methodId, key);\n"
                + "  if (trigger == null) {\n"
                + "    return;\n"
                + "  }\n"
                + "  java.lang.Object _eventState = new java.lang.Object();\n"
                + "  trigger.dispatchOnTrigger(_eventState, new Object[] {\n"
                + "      });\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(6).toString())
        .isEqualTo(
            "public static void testTriggerMethod2(com.facebook.litho.EventTrigger trigger) {\n"
                + "  java.lang.Object _eventState = new java.lang.Object();\n"
                + "  trigger.dispatchOnTrigger(_eventState, new Object[] {\n"
                + "      });\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(7).toString())
        .isEqualTo(
            "static void testTriggerMethod2(com.facebook.litho.ComponentContext c) {\n"
                + "  Test component = (Test) c.getComponentScope();\n"
                + "  component.testTriggerMethod2(\n"
                + "      (com.facebook.litho.EventTriggerTarget) component);\n"
                + "}\n");
  }

  @Test
  public void testGenerateStaticGetTriggerMethods() {
    TypeSpecDataHolder dataHolder = TriggerGenerator.generateStaticGetTriggerMethods(mSpecModel);

    assertThat(dataHolder.getMethodSpecs()).hasSize(4);

    assertThat(dataHolder.getMethodSpecs().get(0).toString())
        .isEqualTo(
            "private static com.facebook.litho.EventTrigger testTriggerMethod1Trigger(com.facebook.litho.ComponentContext c,\n"
                + "    java.lang.String key, com.facebook.litho.Handle handle) {\n"
                + "  int methodId = -773082596;\n"
                + "  return newEventTrigger(c, key, methodId, handle);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(1).toString())
        .isEqualTo(
            "/**\n"
                + " * @Deprecated Do not use this method to trigger events. */\n"
                + "@java.lang.Deprecated\n"
                + "public static com.facebook.litho.EventTrigger testTriggerMethod1Trigger(com.facebook.litho.ComponentContext c,\n"
                + "    java.lang.String key) {\n"
                + "  return testTriggerMethod1Trigger(c, key, null);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(2).toString())
        .isEqualTo(
            "private static com.facebook.litho.EventTrigger testTriggerMethod2Trigger(com.facebook.litho.ComponentContext c,\n"
                + "    java.lang.String key, com.facebook.litho.Handle handle) {\n"
                + "  int methodId = 969727739;\n"
                + "  return newEventTrigger(c, key, methodId, handle);\n"
                + "}\n");

    assertThat(dataHolder.getMethodSpecs().get(3).toString())
        .isEqualTo(
            "/**\n"
                + " * @Deprecated Do not use this method to trigger events. */\n"
                + "@java.lang.Deprecated\n"
                + "public static com.facebook.litho.EventTrigger testTriggerMethod2Trigger(com.facebook.litho.ComponentContext c,\n"
                + "    java.lang.String key) {\n"
                + "  return testTriggerMethod2Trigger(c, key, null);\n"
                + "}\n");
  }
}
