/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.specmodels.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.PropDefaultModel;

class PropDefaultsExtractorTestHelper {
  static void assertFieldPropDefaultExtraction(ImmutableList<PropDefaultModel> propDefaults) {
    assertPropDefaultsExtraction(propDefaults);

    final PropDefaultModel propDefault = propDefaults.get(0);
    assertThat(propDefault.isGetterMethodAccessor()).isFalse();
  }

  static void assertGetterPropDefaultExtraction(ImmutableList<PropDefaultModel> propDefaults) {
    assertPropDefaultsExtraction(propDefaults);

    final PropDefaultModel propDefault = propDefaults.get(0);
    assertThat(propDefault.isGetterMethodAccessor()).isTrue();
  }

  private static void assertPropDefaultsExtraction(ImmutableList<PropDefaultModel> propDefaults) {
    assertThat(propDefaults).hasSize(1);

    final PropDefaultModel propDefault = propDefaults.get(0);
    assertThat(propDefault.getName()).isEqualTo("title");
  }
}
