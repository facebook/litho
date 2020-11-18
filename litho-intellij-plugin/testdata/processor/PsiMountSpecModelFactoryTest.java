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

static class TestTreeProp {

  private final long mValue;

  public TestTreeProp(long value) {
    mValue = value;
  }

  public long getValue() {
    return mValue;
  }

  @Override
  public boolean equals(java.lang.Object obj) {
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

@com.facebook.litho.annotations.Event(returnType = java.lang.String.class)
static class TestTriggerEvent {
  int integer;
}

@com.facebook.litho.annotations.MountSpec(value = "TestMountComponentName", isPublic = false, isPureRender = true, events = { TestTriggerEvent.class })
static class TestMountSpecWithExplicitMountType {

  @com.facebook.litho.annotations.OnCreateMountContent(mountingType = MountingType.DRAWABLE)
  static android.graphics.drawable.ColorDrawable onCreateMountContent(android.content.Context context) {
    return new android.graphics.drawable.ColorDrawable();
  }

  @com.facebook.litho.annotations.OnCreateInitialState
  static void createInitialState(@com.facebook.litho.annotations.Prop int prop1) {}

  @com.facebook.litho.annotations.OnCreateTreeProp
  static TestTreeProp onCreateFeedPrefetcherProp(@com.facebook.litho.annotations.Prop long prop2) {
    return new TestTreeProp(prop2);
  }

  @com.facebook.litho.annotations.OnBoundsDefined
  static void onBoundsDefined(
          @com.facebook.litho.annotations.Prop java.lang.Object prop3, @com.facebook.litho.annotations.Prop char[] prop4, @com.facebook.litho.annotations.FromMeasure java.lang.Long measureOutput) {}

  @com.facebook.litho.annotations.OnMount
  static <S extends java.lang.Object> void onMount(
          @com.facebook.litho.annotations.Prop(optional = true) boolean prop5,
          @com.facebook.litho.annotations.State(canUpdateLazily = true) long state1,
          @com.facebook.litho.annotations.State S state2,
          @com.facebook.litho.annotations.TreeProp TestTreeProp treeProp) {}

  @com.facebook.litho.annotations.OnUnmount
  static void onUnmount() {}

  @com.facebook.litho.annotations.OnTrigger(TestTriggerEvent.class)
  static String testTrigger(@com.facebook.litho.annotations.Prop java.lang.Object prop6, @com.facebook.litho.annotations.FromTrigger int integer) {
    return "";
  }

  @com.facebook.litho.annotations.ShouldAlwaysRemeasure
  static boolean shouldAlwaysRemeasure(@com.facebook.litho.annotations.Prop boolean prop7) {
    return prop7;
  }

  @com.facebook.litho.annotations.OnAttached
  static void onAttached(com.facebook.litho.ComponentContext c, @com.facebook.litho.annotations.Prop java.lang.Object prop8, @com.facebook.litho.annotations.State java.lang.Object state3) {}

  @com.facebook.litho.annotations.OnDetached
  static void onDetached(com.facebook.litho.ComponentContext c, @com.facebook.litho.annotations.Prop java.lang.Object prop9, @com.facebook.litho.annotations.State java.lang.Object state4) {}

  @com.facebook.litho.annotations.OnBindDynamicValue
  static void onBindDynamicValue(android.graphics.drawable.ColorDrawable colorDrawable, @com.facebook.litho.annotations.Prop(dynamic = true) int prop10) {}

  @com.facebook.litho.annotations.OnUpdateStateWithTransition
  static com.facebook.litho.Transition onUpdateStateWithTransition(com.facebook.litho.StateValue<java.lang.Object> stateValue) {
    return null;
  }
}

@com.facebook.litho.annotations.MountSpec
static class TestMountSpecWithImplicitMountType {

  @com.facebook.litho.annotations.OnCreateMountContent
  static android.graphics.drawable.Drawable onCreateMountContent(android.content.Context context) {
    return android.graphics.drawable.ColorDrawable.createFromPath("test");
  }
}

@com.facebook.litho.annotations.MountSpec
static class TestMountSpecWithoutMountType {

  @com.facebook.litho.annotations.OnCreateMountContent
  static void onCreateMountContent(android.content.Context context) {}
}
