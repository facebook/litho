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

class RequiredPropAnnotatorTest {

  static com.facebook.litho.Component onCreateLayout(com.facebook.litho.ComponentContext c) {
    RequiredPropAnnotatorComponent.create(c);

    RequiredPropAnnotatorComponent.create(c).requiredProp();

    RequiredPropAnnotatorComponent.create(c).requiredProp().sameRequiredProp();

    com.facebook.litho.Component component = RequiredPropAnnotatorComponent.create(c).build();

    return RequiredPropAnnotatorComponent.create(c)
        .build();
  }
}
