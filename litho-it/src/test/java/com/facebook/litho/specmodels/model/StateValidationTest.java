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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.Param;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link StateValidation}
 */
public class StateValidationTest {
  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final PropModel mPropModel = mock(PropModel.class);
  private final StateParamModel mStateParamModel = mock(StateParamModel.class);
  private final Object mRepresentedObject1 = new Object();
  private final Object mRepresentedObject2 = new Object();
  private final Object mRepresentedObject3 = new Object();
  private final Object mRepresentedObject4 = new Object();
  private final Object mRepresentedObject5 = new Object();

  @Before
  public void setup() {
    when(mSpecModel.getProps()).thenReturn(ImmutableList.<PropModel>of());
    when(mSpecModel.getStateValues()).thenReturn(ImmutableList.<StateParamModel>of());
  }

  @Test
  public void testTwoStateValuesWithSameNameButDifferentType() {
    StateParamModel stateValue1 = mock(StateParamModel.class);
    StateParamModel stateValue2 = mock(StateParamModel.class);
    when(stateValue1.getName()).thenReturn("sameName");
    when(stateValue2.getName()).thenReturn("sameName");
    when(stateValue1.getType()).thenReturn(TypeName.BOOLEAN);
    when(stateValue2.getType()).thenReturn(TypeName.INT);
    when(stateValue2.getRepresentedObject()).thenReturn(mRepresentedObject2);
    when(mSpecModel.getStateValues()).thenReturn(ImmutableList.of(stateValue1, stateValue2));

    List<SpecModelValidationError> validationErrors =
        StateValidation.validateStateValues(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "State values with the same name must have the same type.");
  }

  @Test
  public void testTwoStateValuesWithSameNameButDifferentCanUpdateLazily() {
    StateParamModel stateValue1 = mock(StateParamModel.class);
    StateParamModel stateValue2 = mock(StateParamModel.class);
    when(stateValue1.getName()).thenReturn("sameName");
    when(stateValue2.getName()).thenReturn("sameName");
    when(stateValue1.getType()).thenReturn(TypeName.BOOLEAN);
    when(stateValue2.getType()).thenReturn(TypeName.BOOLEAN);
    when(stateValue1.canUpdateLazily()).thenReturn(false);
    when(stateValue1.canUpdateLazily()).thenReturn(true);
    when(stateValue2.getRepresentedObject()).thenReturn(mRepresentedObject2);
    when(mSpecModel.getStateValues()).thenReturn(ImmutableList.of(stateValue1, stateValue2));

    List<SpecModelValidationError> validationErrors =
        StateValidation.validateStateValues(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "State values with the same name must have the same annotated value for " +
            "canUpdateLazily().");
  }

  @Test
  public void testOnUpdateStateParamSameNameAsPropAndState() {
    MethodParamModel methodParamModel1 =
        MockMethodParamModel.newBuilder()
            .name("propName")
            .annotations(ImmutableList.of(Param.class))
            .representedObject(mRepresentedObject1)
            .build();
    MethodParamModel methodParamModel2 =
        MockMethodParamModel.newBuilder()
            .name("stateName")
            .annotations(ImmutableList.of(Param.class))
            .representedObject(mRepresentedObject2)
            .build();

    UpdateStateMethodModel updateStateMethodModel =
        new UpdateStateMethodModel(
            null,
            ImmutableList.of(Modifier.STATIC),
            null,
            null,
            ImmutableList.of(),
            ImmutableList.of(methodParamModel1, methodParamModel2),
            mRepresentedObject3,
            null);

    when(mPropModel.getName()).thenReturn("propName");
    when(mStateParamModel.getName()).thenReturn("stateName");
    when(mSpecModel.getProps()).thenReturn(ImmutableList.of(mPropModel));
    when(mSpecModel.getStateValues()).thenReturn(ImmutableList.of(mStateParamModel));

    List<SpecModelValidationError> validationErrors =
        StateValidation.validateOnUpdateStateMethod(mSpecModel, updateStateMethodModel);
    assertThat(validationErrors).hasSize(2);
    assertThat(validationErrors.get(0).element).isSameAs(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("Parameters annotated with @Param should not have the same name as a @Prop.");
    assertThat(validationErrors.get(1).element).isSameAs(mRepresentedObject2);
    assertThat(validationErrors.get(1).message)
        .isEqualTo("Parameters annotated with @Param should not have the same name as a @State " +
            "value.");
  }

  @Test
  public void testOnUpdateStateStateParamsNotValid() {
    MethodParamModel methodParamModel1 =
        MockMethodParamModel.newBuilder()
            .type(TypeName.INT)
            .name("name1")
            .representedObject(mRepresentedObject1)
            .build();
    MethodParamModel methodParamModel2 =
        MockMethodParamModel.newBuilder()
            .type(
                ParameterizedTypeName.get(
                    ClassName.bestGuess("com.facebook.litho.Output"), TypeVariableName.get("T")))
            .name("name2")
            .representedObject(mRepresentedObject2)
            .build();
    MethodParamModel methodParamModel3 =
        MockMethodParamModel.newBuilder()
            .type(
                ParameterizedTypeName.get(
                    ClassNames.STATE_VALUE, TypeVariableName.get("S"), TypeVariableName.get("T")))
            .name("name3")
            .representedObject(mRepresentedObject3)
            .build();
    MethodParamModel methodParamModel4 =
        MockMethodParamModel.newBuilder()
            .type(
                ParameterizedTypeName.get(
                    ClassNames.STATE_VALUE,
                    WildcardTypeName.subtypeOf(ClassName.bestGuess("java.lang.Object"))))
            .name("name4")
            .representedObject(mRepresentedObject4)
            .build();

    UpdateStateMethodModel updateStateMethodModel =
        new UpdateStateMethodModel(
            null,
            ImmutableList.of(Modifier.STATIC),
            "methodName",
            null,
            ImmutableList.of(),
            ImmutableList.of(
                methodParamModel1, methodParamModel2, methodParamModel3, methodParamModel4),
            mRepresentedObject5,
            null);

    List<SpecModelValidationError> validationErrors =
        StateValidation.validateOnUpdateStateMethod(mSpecModel, updateStateMethodModel);
    assertThat(validationErrors).hasSize(4);
    assertThat(validationErrors.get(0).element).isSameAs(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("Only state parameters and parameters annotated with @Param are permitted in " +
            "@OnUpdateState method, and all state parameters must be of type " +
            "com.facebook.litho.StateValue, but name1 is of type int.");
    assertThat(validationErrors.get(1).element).isSameAs(mRepresentedObject2);
    assertThat(validationErrors.get(1).message)
        .isEqualTo("Only state parameters and parameters annotated with @Param are permitted in " +
            "@OnUpdateState method, and all state parameters must be of type " +
            "com.facebook.litho.StateValue, but name2 is of " +
            "type com.facebook.litho.Output<T>.");
    assertThat(validationErrors.get(2).element).isSameAs(mRepresentedObject3);
    assertThat(validationErrors.get(2).message)
        .isEqualTo("All parameters of type com.facebook.litho.StateValue must define a type " +
            "argument, name3 in method methodName does not.");
    assertThat(validationErrors.get(3).element).isSameAs(mRepresentedObject4);
    assertThat(validationErrors.get(3).message)
        .isEqualTo("All parameters of type com.facebook.litho.StateValue must define a type " +
            "argument, name4 in method methodName does not.");
  }

  @Test
  public void testOnUpdateStateStateParamsNotDefinedElsewhere() {
    MethodParamModel methodParamModel4 =
        MockMethodParamModel.newBuilder()
            .type(
                ParameterizedTypeName.get(
                    ClassNames.STATE_VALUE, ClassName.bestGuess("java.lang.Object")))
            .name("name")
            .representedObject(mRepresentedObject1)
            .build();

    UpdateStateMethodModel updateStateMethodModel =
        new UpdateStateMethodModel(
            null,
            ImmutableList.of(Modifier.STATIC),
            "methodName",
            null,
            ImmutableList.of(),
            ImmutableList.of(methodParamModel4),
            mRepresentedObject2,
            null);

    List<SpecModelValidationError> validationErrors =
        StateValidation.validateOnUpdateStateMethod(mSpecModel, updateStateMethodModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isSameAs(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("Names of parameters of type StateValue must match the name and type of a " +
            "parameter annotated with @State.");
  }

  @Test
  public void testOnUpdateStateNotStatic() {
    UpdateStateMethodModel updateStateMethodModel =
        new UpdateStateMethodModel(
            null,
            ImmutableList.<Modifier>of(),
            "methodName",
            null,
            ImmutableList.of(),
            ImmutableList.<MethodParamModel>of(),
            mRepresentedObject1,
            null);

    List<SpecModelValidationError> validationErrors =
        StateValidation.validateOnUpdateStateMethod(mSpecModel, updateStateMethodModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isSameAs(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("Methods in a spec that doesn't have dependency injection must be static.");
  }
}
