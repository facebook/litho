/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.MockSpecModel;
import com.squareup.javapoet.ClassName;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TagValidationTest {

  private final Object mRepresentedObject = new Object();

  @Test
  public void testNotEmpty() throws Exception {
    final SpecModel model =
        MockSpecModel.newBuilder()
            .specName("MyTestSpec")
            .tags(ImmutableList.of(new TagModel(ClassName.OBJECT, false, true, mRepresentedObject)))
            .build();
    List<SpecModelValidationError> validationErrors = TagValidation.validate(model);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "MyTestSpec: Spec classes use interfaces as component tags. Tags cannot be non-empty interfaces like 'java.lang.Object'.");
  }

  @Test
  public void testExtendInterface() throws Exception {
    final SpecModel model =
        MockSpecModel.newBuilder()
            .specName("MyTestSpec")
            .tags(ImmutableList.of(new TagModel(ClassName.OBJECT, true, false, mRepresentedObject)))
            .build();
    List<SpecModelValidationError> validationErrors = TagValidation.validate(model);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "MyTestSpec: Spec classes use interfaces as component tags. Tags cannot extend other interfaces like 'java.lang.Object'.");
  }
}
