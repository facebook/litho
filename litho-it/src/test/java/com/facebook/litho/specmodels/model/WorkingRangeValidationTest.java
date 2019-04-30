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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.specmodels.internal.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link WorkingRangeValidation} */
@RunWith(JUnit4.class)
public class WorkingRangeValidationTest {

  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final Object mRepresentedObject1 = new Object();
  private final Object mRepresentedObject2 = new Object();
  private final Object mRepresentedObject3 = new Object();
  private final Object mRepresentedObject4 = new Object();
  private final Object mRepresentedObject5 = new Object();
  private final Object mRepresentedObject6 = new Object();
  private final Object mRepresentedObject7 = new Object();
  private final Object mRepresentedObject8 = new Object();

  @Before
  public void setup() {
    when(mSpecModel.getWorkingRangeMethods())
        .thenReturn(ImmutableList.<WorkingRangeMethodModel>of());
  }

  @Test
  public void testValidateNoRegisterMethod() {
    final WorkingRangeMethodModel prefetchModel = new WorkingRangeMethodModel("prefetch");
    prefetchModel.enteredRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject2)
            .typeModel(new WorkingRangeDeclarationModel("prefetch", mRepresentedObject4))
            .build();
    prefetchModel.exitedRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject3)
            .typeModel(new WorkingRangeDeclarationModel("prefetch", mRepresentedObject5))
            .build();
    when(mSpecModel.getWorkingRangeMethods()).thenReturn(ImmutableList.of(prefetchModel));

    SpecModelValidationError validationError =
        WorkingRangeValidation.validateNoRegisterMethod(mSpecModel);
    assertThat(validationError).isNotNull();
    assertThat(validationError.element).isEqualTo(null);
    assertThat(validationError.message)
        .isEqualTo(
            "You need to have a method annotated with @OnRegisterRanges in your spec since "
                + "there are @OnEnteredRange/@OnExitedRange annotated methods.");
  }

  @Test
  public void testValidateEmptyName() {
    final WorkingRangeMethodModel emptyModel = new WorkingRangeMethodModel("");
    emptyModel.enteredRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject1)
            .typeModel(new WorkingRangeDeclarationModel("", mRepresentedObject5))
            .build();
    emptyModel.exitedRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject2)
            .typeModel(new WorkingRangeDeclarationModel("", mRepresentedObject6))
            .build();
    final WorkingRangeMethodModel prefetchModel = new WorkingRangeMethodModel("prefetch");
    prefetchModel.enteredRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject3)
            .typeModel(new WorkingRangeDeclarationModel("prefetch", mRepresentedObject7))
            .build();
    prefetchModel.exitedRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject4)
            .typeModel(new WorkingRangeDeclarationModel("prefetch", mRepresentedObject8))
            .build();
    when(mSpecModel.getWorkingRangeMethods())
        .thenReturn(ImmutableList.of(emptyModel, prefetchModel));

    List<SpecModelValidationError> validationErrors =
        WorkingRangeValidation.validateEmptyName(mSpecModel);
    assertThat(validationErrors).hasSize(2);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(1).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).annotation).isEqualTo(mRepresentedObject5);
    assertThat(validationErrors.get(1).annotation).isEqualTo(mRepresentedObject6);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("The name in @OnEnteredRange cannot be empty.");
  }

  @Test
  public void testValidateDuplicateName() {
    final WorkingRangeMethodModel model1 = new WorkingRangeMethodModel("prefetch");
    model1.enteredRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject1)
            .typeModel(new WorkingRangeDeclarationModel("prefetch", mRepresentedObject5))
            .build();

    final WorkingRangeMethodModel model2 = new WorkingRangeMethodModel("prefetch");
    model2.enteredRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject2)
            .typeModel(new WorkingRangeDeclarationModel("prefetch", mRepresentedObject6))
            .build();

    final WorkingRangeMethodModel model3 = new WorkingRangeMethodModel("purge");
    model3.exitedRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject3)
            .typeModel(new WorkingRangeDeclarationModel("purge", mRepresentedObject7))
            .build();

    final WorkingRangeMethodModel model4 = new WorkingRangeMethodModel("purge");
    model4.exitedRangeModel =
        SpecMethodModel.<EventMethod, WorkingRangeDeclarationModel>builder()
            .representedObject(mRepresentedObject4)
            .typeModel(new WorkingRangeDeclarationModel("purge", mRepresentedObject8))
            .build();

    when(mSpecModel.getWorkingRangeMethods())
        .thenReturn(ImmutableList.of(model1, model2, model3, model4));

    List<SpecModelValidationError> validationErrors =
        WorkingRangeValidation.validateDuplicateName(mSpecModel);
    assertThat(validationErrors).hasSize(4);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(1).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(2).element).isEqualTo(mRepresentedObject3);
    assertThat(validationErrors.get(3).element).isEqualTo(mRepresentedObject4);
    assertThat(validationErrors.get(0).annotation).isEqualTo(mRepresentedObject5);
    assertThat(validationErrors.get(1).annotation).isEqualTo(mRepresentedObject6);
    assertThat(validationErrors.get(2).annotation).isEqualTo(mRepresentedObject7);
    assertThat(validationErrors.get(3).annotation).isEqualTo(mRepresentedObject8);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "The name \"prefetch\" is duplicated, it's must be unique across @OnEnteredRange methods.");
    assertThat(validationErrors.get(1).message)
        .isEqualTo(
            "The name \"prefetch\" is duplicated, it's must be unique across @OnEnteredRange methods.");
    assertThat(validationErrors.get(2).message)
        .isEqualTo(
            "The name \"purge\" is duplicated, it's must be unique across @OnExitedRange methods.");
    assertThat(validationErrors.get(3).message)
        .isEqualTo(
            "The name \"purge\" is duplicated, it's must be unique across @OnExitedRange methods.");
  }
}
