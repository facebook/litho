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
import com.squareup.javapoet.TypeName;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Tests {@link SimpleNameDelegateValidation} */
public class SimpleNameDelegateValidationTest {

  private LayoutSpecModel mSpecModel;
  private PropModel mPropModel;

  @Before
  public void setup() {
    mSpecModel = mock(LayoutSpecModel.class);
    mPropModel = mock(PropModel.class);
    when(mSpecModel.getProps()).thenReturn(ImmutableList.<PropModel>of(mPropModel));
    when(mSpecModel.getSpecElementType()).thenReturn(SpecElementType.JAVA_CLASS);
    when(mSpecModel.getStateValues()).thenReturn(ImmutableList.<StateParamModel>of());
  }

  @Test
  public void testNoDelegate() {
    when(mPropModel.getName()).thenReturn("child");
    when(mPropModel.getTypeName()).thenReturn(ClassNames.COMPONENT);
    when(mSpecModel.getSimpleNameDelegate()).thenReturn("");

    List<SpecModelValidationError> validationErrors =
        SimpleNameDelegateValidation.validate(mSpecModel);
    assertThat(validationErrors).isEmpty();
  }

  @Test
  public void testCorrectUsage() {
    when(mPropModel.getName()).thenReturn("child");
    when(mPropModel.getTypeName()).thenReturn(ClassNames.COMPONENT);
    when(mSpecModel.getSimpleNameDelegate()).thenReturn("child");

    List<SpecModelValidationError> validationErrors =
        SimpleNameDelegateValidation.validate(mSpecModel);
    assertThat(validationErrors).isEmpty();
  }

  @Test
  public void testMissingProp() {
    when(mPropModel.getName()).thenReturn("delegate");
    when(mPropModel.getTypeName()).thenReturn(ClassNames.COMPONENT);
    when(mSpecModel.getSimpleNameDelegate()).thenReturn("child");

    List<SpecModelValidationError> validationErrors =
        SimpleNameDelegateValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).message).contains("Did not find a @Prop named 'child'");
  }

  @Test
  public void testIncorrectPropType() {
    when(mPropModel.getName()).thenReturn("child");
    when(mPropModel.getTypeName()).thenReturn(TypeName.INT);
    when(mSpecModel.getSimpleNameDelegate()).thenReturn("child");

    List<SpecModelValidationError> validationErrors =
        SimpleNameDelegateValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).message).contains("@Prop 'child' has type int");
  }
}
