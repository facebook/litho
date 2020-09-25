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
public class TestEvent {}

@com.facebook.litho.annotations.Event(returnType = boolean.class)
public class TestEventWithReturnTypeAndParam {
  public int number;
}

public class TestClassWithoutOnEventMethod {}

public class TestClassWithOnEventMethod {
  @com.facebook.litho.annotations.OnEvent(TestEvent.class)
  static void onTestEvent(com.facebook.litho.ComponentContext c) {}
}

public class TestClassWithCustomOnEventMethod {
  @com.facebook.litho.annotations.OnEvent(TestEvent.class)
  static void customMethodName(com.facebook.litho.ComponentContext c) {}
}

public class TestClassWithMultipleOnEventMethods {
  @com.facebook.litho.annotations.OnEvent(TestEvent.class)
  static void onTestEvent(com.facebook.litho.ComponentContext c) {}

  @com.facebook.litho.annotations.OnEvent(TestEvent.class)
  static void onTestEvent1(com.facebook.litho.ComponentContext c) {}

  @com.facebook.litho.annotations.OnEvent(TestEvent.class)
  static void onTestEvent3(com.facebook.litho.ComponentContext c) {}
}
