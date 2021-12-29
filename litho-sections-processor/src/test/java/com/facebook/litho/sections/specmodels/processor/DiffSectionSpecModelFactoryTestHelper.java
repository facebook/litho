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

package com.facebook.litho.sections.specmodels.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.sections.specmodels.model.DiffSectionSpecModel;

public class DiffSectionSpecModelFactoryTestHelper {

  public static void create_forDiffSectionSpec_populateGenericSpecInfo(
      DiffSectionSpecModel diffSectionSpecModel) {
    assertThat(diffSectionSpecModel.getSpecName()).isEqualTo("TestDiffSectionSpec");
    assertThat(diffSectionSpecModel.getDelegateMethods()).hasSize(1);
    assertThat(diffSectionSpecModel.getDelegateMethods().get(0).annotations.get(0).toString())
        .isEqualTo("@com.facebook.litho.sections.annotations.OnDiff()");
    assertThat(diffSectionSpecModel.getProps()).hasSize(2);
  }
}
