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

@com.facebook.litho.annotations.LayoutSpec(value = "TestLayoutComponentName")
static class TestLayoutSpec {
  @com.facebook.litho.annotations.OnCreateInitialState
  static void createInitialState(@com.facebook.litho.annotations.Prop int prop1) {}

  @com.facebook.litho.annotations.OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @com.facebook.litho.annotations.Prop int prop2) {
    return null;
  }

  @com.facebook.litho.annotations.OnAttached
  static void onAttached(ComponentContext c, @com.facebook.litho.annotations.Prop Object prop3, @com.facebook.litho.annotations.State Object state1) {}

  @com.facebook.litho.annotations.OnDetached
  static void onDetached(ComponentContext c, @com.facebook.litho.annotations.Prop Object prop4, @com.facebook.litho.annotations.State Object state2) {}
}
