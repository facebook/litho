/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.lang.annotation.Annotation;
import java.util.List;

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DiffValidation}
 */
public class DiffValidationTest {

  private SpecModel mSpecModel;
  private StateParamModel mStateModel;
  private PropModel mPropModel;
  private DiffModel mDiffModel;
  private Object mDiffRepresentedObject;

  @Before
  public void setup() {
    mSpecModel = mock(SpecModel.class);
    mStateModel = mock(StateParamModel.class);
    mPropModel = mock(PropModel.class);
    mDiffModel = mock(DiffModel.class);
    mDiffRepresentedObject = new Object();

    when(mSpecModel.getProps()).thenReturn(ImmutableList.<PropModel>of(mPropModel));
    when(mSpecModel.getStateValues()).thenReturn(ImmutableList.of(mStateModel));
    when(mSpecModel.getDiffs()).thenReturn(ImmutableList.of(mDiffModel));

    when(mStateModel.getName()).thenReturn("stateName");
    when(mStateModel.getType()).thenReturn(TypeName.INT);

    when(mPropModel.getName()).thenReturn("propName");
    when(mPropModel.getType()).thenReturn(TypeName.INT);

    when(mDiffModel.getName()).thenReturn("stateName");
    when(mDiffModel.getType()).thenReturn(
        ParameterizedTypeName.get(ClassNames.DIFF, TypeName.INT.box())
            .annotated(AnnotationSpec.builder(State.class).build()));
    when(mDiffModel.getRepresentedObject()).thenReturn(mDiffRepresentedObject);
    when(mDiffModel.getAnnotations()).thenReturn(ImmutableList.of(annotation(State.class)));
  }

  @Test
  public void testNameDoesntExist() {
    when(mDiffModel.getName()).thenReturn("doesNotExist");

    List<SpecModelValidationError> validationErrors = DiffValidation.validate(mSpecModel);
    assertSingleError(validationErrors, DiffValidation.STATE_MISMATCH_ERROR);
  }

  @Test
  public void testDiffModelHasNoTypeParameter() {
    when(mDiffModel.getType()).thenReturn(
        ClassNames.DIFF.annotated(AnnotationSpec.builder(State.class).build()));

    List<SpecModelValidationError> validationErrors = DiffValidation.validate(mSpecModel);
    assertSingleError(validationErrors, DiffValidation.MISSING_TYPE_PARAMETER_ERROR);
  }

  @Test
  public void testDiffModelHasDifferentParameterFromState() {
    when(mDiffModel.getType()).thenReturn(
        ParameterizedTypeName.get(ClassNames.DIFF, TypeName.BOOLEAN.box())
            .annotated(AnnotationSpec.builder(State.class).build()));

    List<SpecModelValidationError> validationErrors = DiffValidation.validate(mSpecModel);
    assertSingleError(validationErrors, DiffValidation.STATE_MISMATCH_ERROR);
  }

  @Test
  public void testDiffModelHasDifferentParameterFromProp() {
    when(mDiffModel.getName()).thenReturn("propName");
    when(mDiffModel.getType()).thenReturn(
        ParameterizedTypeName.get(ClassNames.DIFF, TypeName.BOOLEAN.box())
            .annotated(AnnotationSpec.builder(Prop.class).build()));
    when(mDiffModel.getAnnotations()).thenReturn(ImmutableList.of(annotation(Prop.class)));

    List<SpecModelValidationError> validationErrors = DiffValidation.validate(mSpecModel);
    assertSingleError(validationErrors, DiffValidation.PROP_MISMATCH_ERROR);
  }

  @Test
  public void testNoErrorState() {
    List<SpecModelValidationError> validationErrors = DiffValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(0);
  }

  @Test
  public void testNoErrorProp() {
    when(mDiffModel.getName()).thenReturn("propName");
    when(mDiffModel.getType()).thenReturn(
        ParameterizedTypeName.get(ClassNames.DIFF, TypeName.INT.box())
            .annotated(AnnotationSpec.builder(Prop.class).build()));
    when(mDiffModel.getAnnotations()).thenReturn(ImmutableList.of(annotation(Prop.class)));

    List<SpecModelValidationError> validationErrors = DiffValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(0);
  }

  private void assertSingleError(
      List<SpecModelValidationError> validationErrors,
      String expectedError) {
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mDiffRepresentedObject);
    assertThat(validationErrors.get(0).message).isEqualTo(expectedError);
  }

  private static Annotation annotation(final Class<? extends Annotation> cls) {
    return new Annotation() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return cls;
      }
    };
  }
}
