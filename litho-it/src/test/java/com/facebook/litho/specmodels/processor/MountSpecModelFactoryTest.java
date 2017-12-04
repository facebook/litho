/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.EventDeclarationModel;
import com.facebook.litho.specmodels.model.EventMethod;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests {@link MountSpecModelFactory}
 */
public class MountSpecModelFactoryTest {
  @Rule public CompilationRule mCompilationRule = new CompilationRule();

  private final DependencyInjectionHelper mDependencyInjectionHelper =
      mock(DependencyInjectionHelper.class);

  private final MountSpecModelFactory mFactory = new MountSpecModelFactory();

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
    static void createInitialState(
        @Prop int prop1) {
    }

    @OnCreateTreeProp
    static TestTreeProp onCreateFeedPrefetcherProp(
        @Prop long prop2) {
      return new TestTreeProp(prop2);
    }

    @OnBoundsDefined
    static void onBoundsDefined(
        @Prop Object prop3,
        @Prop char[] prop4,
        @FromMeasure Long measureOutput) {}

    @OnMount
    static <S extends Object> void onMount(
        @Prop(optional = true) boolean prop5,
        @State(canUpdateLazily = true) long state1,
        @State S state2,
        @TreeProp TestTreeProp treeProp) {}

    @OnUnmount
    static void onUnmount() {}

    @OnTrigger(TestTriggerEvent.class)
    static String testTrigger(@FromTrigger int integer, @Prop Object prop3) {
      return "";
    }
  }

  @Test
  public void testCreate() {
    Elements elements = mCompilationRule.getElements();
    TypeElement typeElement =
        elements.getTypeElement(MountSpecModelFactoryTest.TestMountSpec.class.getCanonicalName());

    MountSpecModel mountSpecModel =
        mFactory.create(elements, typeElement, mDependencyInjectionHelper, null);

    assertThat(mountSpecModel.getSpecName()).isEqualTo("TestMountSpec");
    assertThat(mountSpecModel.getComponentName()).isEqualTo("TestMountComponentName");
    assertThat(mountSpecModel.getSpecTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.MountSpecModelFactoryTest.TestMountSpec");
    assertThat(mountSpecModel.getComponentTypeName().toString())
        .isEqualTo(
            "com.facebook.litho.specmodels.processor.MountSpecModelFactoryTest." +
                "TestMountComponentName");

    assertThat(mountSpecModel.getDelegateMethods()).hasSize(5);

    assertThat(mountSpecModel.getProps()).hasSize(5);
    assertThat(mountSpecModel.getStateValues()).hasSize(2);
    assertThat(mountSpecModel.getInterStageInputs()).hasSize(1);
    assertThat(mountSpecModel.getTreeProps()).hasSize(1);

    assertThat(mountSpecModel.isPublic()).isFalse();
    assertThat(mountSpecModel.isPureRender()).isTrue();

    assertThat(mountSpecModel.hasInjectedDependencies()).isTrue();
    assertThat(mountSpecModel.getDependencyInjectionHelper()).isSameAs(mDependencyInjectionHelper);

    assertThat(mountSpecModel.getTriggerMethods()).hasSize(1);
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethodModel =
        mountSpecModel.getTriggerMethods().get(0);
    assertThat(triggerMethodModel.name).isEqualToIgnoringCase("testTrigger");
    assertThat(triggerMethodModel.methodParams).hasSize(2);
  }
}
