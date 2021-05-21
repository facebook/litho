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

@com.facebook.litho.annotations.Event
static class TestTriggerEvent {
  int testTriggerVar;
}

@com.facebook.litho.sections.annotations.GroupSectionSpec
static class TestGroupSectionSpec {
  @com.facebook.litho.annotations.OnCreateInitialState
  static void onCreateInitialState(@com.facebook.litho.annotations.Prop int prop1, @com.facebook.litho.annotations.State java.lang.Integer state1) {}
  
  @com.facebook.litho.annotations.OnUpdateState
  static void onUpdateState1(java.lang.Integer state1) {}
  @com.facebook.litho.annotations.OnUpdateState
  static void onUpdateState2(java.lang.Integer state2) {}
  
  @com.facebook.litho.sections.annotations.OnCreateChildren
  static Component onCreateChildren(com.facebook.litho.sections.SectionContext c, @com.facebook.litho.annotations.Prop int prop2, @com.facebook.litho.annotations.Prop int prop3, @com.facebook.litho.annotations.State java.lang.Integer state1) {
    return null;
  }
  
  @com.facebook.litho.sections.annotations.OnCreateService
  static java.lang.String onCreateService(com.facebook.litho.sections.SectionContext c, @com.facebook.litho.annotations.Prop int prop4, @com.facebook.litho.annotations.State java.lang.Integer state2) {
    return "Test";
  }
  @com.facebook.litho.sections.annotations.OnBindService
  static void onBindService(com.facebook.litho.sections.SectionContext c, java.lang.String service, @com.facebook.litho.annotations.Prop java.lang.String prop5) {}

  @com.facebook.litho.annotations.OnTrigger(TestTriggerEvent.class)
  static void onTestTriggerEvent(com.facebook.litho.sections.SectionContext c, @com.facebook.litho.annotations.FromTrigger int testTriggerVar) {}

  @com.facebook.litho.annotations.OnEvent(com.facebook.litho.ClickEvent.class)
  static void onClickEvent(com.facebook.litho.sections.SectionContext c) {}
}

