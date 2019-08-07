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

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link DiffValidation} */
@RunWith(JUnit4.class)
public class DiffValidationTest {

  private SpecModel mSpecModel;
  private StateParamModel mStateModel;
  private PropModel mPropModel;
  private RenderDataDiffModel mDiffModel;
  private Object mDiffRepresentedObject;

  @Before
  public void setup() {
    mSpecModel = mock(SpecModel.class);
    mStateModel = mock(StateParamModel.class);
    mPropModel = mock(PropModel.class);
    mDiffModel = mock(RenderDataDiffModel.class);
    mDiffRepresentedObject = new Object();

    when(mSpecModel.getProps()).thenReturn(ImmutableList.<PropModel>of(mPropModel));
    when(mSpecModel.getStateValues()).thenReturn(ImmutableList.of(mStateModel));
    when(mSpecModel.getRenderDataDiffs()).thenReturn(ImmutableList.of(mDiffModel));

    when(mStateModel.getName()).thenReturn("stateName");
    when(mStateModel.getTypeName()).thenReturn(TypeName.INT);

    when(mPropModel.getName()).thenReturn("propName");
    when(mPropModel.getTypeName()).thenReturn(TypeName.INT);

    when(mDiffModel.getName()).thenReturn("stateName");
    when(mDiffModel.getTypeName()).thenReturn(
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
    when(mDiffModel.getTypeName()).thenReturn(
        ClassNames.DIFF.annotated(AnnotationSpec.builder(State.class).build()));

    List<SpecModelValidationError> validationErrors = DiffValidation.validate(mSpecModel);
    assertSingleError(validationErrors, DiffValidation.MISSING_TYPE_PARAMETER_ERROR);
  }

  @Test
  public void testDiffModelHasDifferentParameterFromState() {
    when(mDiffModel.getTypeName()).thenReturn(
        ParameterizedTypeName.get(ClassNames.DIFF, TypeName.BOOLEAN.box())
            .annotated(AnnotationSpec.builder(State.class).build()));

    List<SpecModelValidationError> validationErrors = DiffValidation.validate(mSpecModel);
    assertSingleError(validationErrors, DiffValidation.STATE_MISMATCH_ERROR);
  }

  @Test
  public void testDiffModelHasDifferentParameterFromProp() {
    when(mDiffModel.getName()).thenReturn("propName");
    when(mDiffModel.getTypeName()).thenReturn(
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
    when(mDiffModel.getTypeName()).thenReturn(
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
