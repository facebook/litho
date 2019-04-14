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

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnAttached;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnDetached;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ShouldAlwaysRemeasure;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.google.testing.compile.CompilationRule;
import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests {@link MountSpecModelFactory} */
public class MountSpecModelFactoryTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private final MountSpecModelFactory mFactory = new MountSpecModelFactory();

  private MountSpecModel mMountSpecModel;

  static class TestTreeProp {

    private final long mValue;

    public TestTreeProp(long value) {
      mValue = value;
    }

    public long getValue() {
      return mValue;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      TestTreeProp other = (TestTreeProp) obj;
      return other.getValue() == mValue;
    }

    @Override
    public int hashCode() {
      return (int) mValue;
    }
  }

  @Event(returnType = String.class)
  static class TestTriggerEvent {
    int integer;
  }

  @MountSpec(value = "TestMountComponentName", isPublic = false, isPureRender = true)
  static class TestMountSpec {

    @OnCreateInitialState
    static void createInitialState(@Prop int prop1) {}

    @OnCreateTreeProp
    static TestTreeProp onCreateFeedPrefetcherProp(@Prop long prop2) {
      return new TestTreeProp(prop2);
    }

    @OnBoundsDefined
    static void onBoundsDefined(
        @Prop Object prop3, @Prop char[] prop4, @FromMeasure Long measureOutput) {}

    @OnMount
    static <S extends Object> void onMount(
        @Prop(optional = true) boolean prop5,
        @State(canUpdateLazily = true) long state1,
        @State S state2,
        @TreeProp TestTreeProp treeProp) {}

    @OnUnmount
    static void onUnmount() {}

    @OnTrigger(TestTriggerEvent.class)
    static String testTrigger(@Prop Object prop6, @FromTrigger int integer) {
      return "";
    }

    @ShouldAlwaysRemeasure
    static boolean shouldAlwaysRemeasure(@Prop boolean prop7) {
      return prop7;
    }

    @OnAttached
    static void onAttached(ComponentContext c, @Prop Object prop8, @State Object state3) {}

    @OnDetached
    static void onDetached(ComponentContext c, @Prop Object prop9, @State Object state4) {}
  }

  @Before
  public void setUp() {
    Elements elements = mCompilationRule.getElements();
    Types types = mCompilationRule.getTypes();
    TypeElement typeElement =
        elements.getTypeElement(MountSpecModelFactoryTest.TestMountSpec.class.getCanonicalName());

    mMountSpecModel =
        mFactory.create(
            elements,
            types,
            typeElement,
            mock(Messager.class),
            RunMode.normal(),
            mDependencyInjectionHelper,
            null);
  }

  @Test
  public void testCreate() {
    assertThat(mMountSpecModel.getSpecName()).isEqualTo("TestMountSpec");
    assertThat(mMountSpecModel.getComponentName()).isEqualTo("TestMountComponentName");
    assertThat(mMountSpecModel.getSpecTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.MountSpecModelFactoryTest.TestMountSpec");
    assertThat(mMountSpecModel.getComponentTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.MountSpecModelFactoryTest."
                + "TestMountComponentName");

    assertThat(mMountSpecModel.getDelegateMethods()).hasSize(8);

    final SpecMethodModel<DelegateMethod, Void> shouldRemeasureMethod =
        mMountSpecModel.getDelegateMethods().get(5);
    assertThat(shouldRemeasureMethod.name).isEqualToIgnoringCase("shouldAlwaysRemeasure");
    assertThat(shouldRemeasureMethod.methodParams).hasSize(1);

    assertThat(mMountSpecModel.getProps()).hasSize(9);
    assertThat(mMountSpecModel.getStateValues()).hasSize(4);
    assertThat(mMountSpecModel.getInterStageInputs()).hasSize(1);
    assertThat(mMountSpecModel.getTreeProps()).hasSize(1);

    assertThat(mMountSpecModel.isPublic()).isFalse();
    assertThat(mMountSpecModel.isPureRender()).isTrue();

    assertThat(mMountSpecModel.hasInjectedDependencies()).isTrue();
    assertThat(mMountSpecModel.getDependencyInjectionHelper()).isSameAs(mDependencyInjectionHelper);

    assertThat(mMountSpecModel.getTriggerMethods()).hasSize(1);
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethodModel =
        mMountSpecModel.getTriggerMethods().get(0);
    assertThat(triggerMethodModel.name).isEqualToIgnoringCase("testTrigger");
    assertThat(triggerMethodModel.methodParams).hasSize(2);
  }

  @Test
  public void testOnAttached() {
    final SpecMethodModel<DelegateMethod, Void> onAttached =
        mMountSpecModel.getDelegateMethods().get(6);
    assertThat(onAttached.name).isEqualToIgnoringCase("onAttached");
    assertThat(onAttached.methodParams).hasSize(3);
  }

  @Test
  public void testOnDetached() {
    final SpecMethodModel<DelegateMethod, Void> onDetached =
        mMountSpecModel.getDelegateMethods().get(7);
    assertThat(onDetached.name).isEqualToIgnoringCase("onDetached");
    assertThat(onDetached.methodParams).hasSize(3);
  }
}
