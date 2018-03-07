/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.MockSpecModel;
import com.squareup.javapoet.ClassName;
import java.util.List;
import org.junit.Test;

public class TagValidationTest {

  private final Object mRepresentedObject = new Object();

  @Test
  public void testNotEmpty() throws Exception {
    final SpecModel model =
        MockSpecModel.newBuilder()
            .tags(ImmutableList.of(new TagModel(ClassName.OBJECT, false, true, mRepresentedObject)))
            .build();
    List<SpecModelValidationError> validationErrors = TagValidation.validate(model);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject);
    assertThat(validationErrors.get(0).message).isEqualTo(TagValidation.NON_EMPTY_ERROR_MESSAGE);
  }

  @Test
  public void testExtendInterface() throws Exception {
    final SpecModel model =
        MockSpecModel.newBuilder()
            .tags(ImmutableList.of(new TagModel(ClassName.OBJECT, true, false, mRepresentedObject)))
            .build();
    List<SpecModelValidationError> validationErrors = TagValidation.validate(model);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(TagValidation.EXTEND_INTERFACE_ERROR_MESSAGE);
  }
}
