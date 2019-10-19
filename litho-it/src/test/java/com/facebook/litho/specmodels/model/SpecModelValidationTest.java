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

package com.facebook.litho.specmodels.model;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link SpecModelValidation} */
@RunWith(JUnit4.class)
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
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "You must suffix the class name of your spec with \"Spec\" e.g. a \"MyComponentSpec\" "
                + "class name generates a component named \"MyComponent\".");
  }

  @Test
  public void testMountTypeValidation() {
    when(mMountSpecModel.getMountType()).thenReturn(ClassNames.COMPONENT_LIFECYCLE_MOUNT_TYPE_NONE);
    List<SpecModelValidationError> validationErrors =
        SpecModelValidation.validateGetMountType(mMountSpecModel);

    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isSameAs(mMountSpecModelRepresentedObject);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "onCreateMountContent's return type should be either a View or a Drawable subclass.");
  }
}
