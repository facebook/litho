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

@com.facebook.litho.sections.annotations.GroupSectionSpec
class TestGroupSectionSpec {

  @com.facebook.litho.sections.annotations.OnCreateChildren
  static com.facebook.litho.sections.Children OnCreateChildren(com.facebook.litho.sections.SectionContext c, @com.facebook.litho.annotations.Prop int prop) {}

  void test1() {
    TestGroupSection.method1();
  }

  void test2() {
    TestGroupSection.prop(1);
  }

  static void method1() {}

  class TestGroupSection extends ComponentLifecycle {
    static void method1() {}

    @com.facebook.litho.annotations.PropSetter( value = "prop", required = true)
    static void prop(int prop) {}
  }

  static class ComponentLifecycle {}
}
