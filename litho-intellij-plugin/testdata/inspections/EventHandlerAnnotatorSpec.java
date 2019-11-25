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

@com.facebook.litho.annotations.LayoutSpec
class EventHandlerAnnotatorSpec {

  static void methodName(ComponentContext c) {
    Component.create(c).testEventHandler();
  }

  @com.facebook.litho.annotations.OnEvent(TestEvent.class)
  static void handler1() {}

  @com.facebook.litho.annotations.OnEvent(TestEvent.class)
  static void handlerTwo() {}

  @com.facebook.litho.annotations.OnEvent(TestEvent.class)
  static void thirdHandler() {}

  static class Component {

    static Component create(ComponentContext c) {
      return new Component();
    }

    void testEventHandler(com.facebook.litho.EventHandler<TestEvent> e) {}

    void otherEventHandler(com.facebook.litho.EventHandler<OtherEvent> e) {}
  }
}
