/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SpecModelValidation}
 */
public class SpecModelValidationTest {
  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final MountSpecModel mMountSpecModel = mock(MountSpecModel.class);
  private final Object mModelRepresentedObject = new Object();
  private final Object mMountSpecModelRepresentedObject = new Object();

  @Before
  public void setup() {
    when(mSpecModel.getRepresentedObject()).thenReturn(mModelRepresentedObject);
    when(mMountSpecModel.getRepresentedObject()).thenReturn(mMountSpecModelRepresentedObject);
  }

  @Test
  public void testNameValidation() {
    when(mSpecModel.getSpecName()).thenReturn("testNotEndingWithSpecXXXX");
    List<SpecModelValidationError> validationErrors = SpecModelValidation.validateName(mSpecModel);

    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isSameAs(mModelRepresentedObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "You must suffix the class name of your spec with \"Spec\" e.g. a \"MyComponentSpec\" " +
            "class name generates a component named \"MyComponent\".");
  }

  @Test
  public void testMountTypeValidation() {
    when(mMountSpecModel.getMountType()).thenReturn(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_NONE);
    List<SpecModelValidationError> validationErrors =
        SpecModelValidation.validateGetMountType(mMountSpecModel);

    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isSameAs(mMountSpecModelRepresentedObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "onCreateMountContent's return type should be either a View or a Drawable subclass.");
  }

  @Test
  public void testDisplayListValidation() {
    when(mMountSpecModel.shouldUseDisplayList()).thenReturn(true);
    when(mMountSpecModel.getMountType()).thenReturn(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_VIEW);
    List<SpecModelValidationError> validationErrors =
        SpecModelValidation.validateShouldUseDisplayLists(mMountSpecModel);

    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isSameAs(mMountSpecModelRepresentedObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "shouldUseDisplayList = true can only be used on MountSpecs that mount a drawable.");
  }
}
