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

import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.TestMethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link PureRenderValidation}
 */
public class PureRenderValidationTest {
  public interface PureRenderSpecModel extends SpecModel, HasPureRender {}

  private final PureRenderSpecModel mSpecModel = mock(PureRenderSpecModel.class);
  private final PropModel mPropModel = mock(PropModel.class);
  private final MethodParamModel mMethodParamModel = mock(MethodParamModel.class);
  private final Object mDelegateMethodRepresentedObject1 = new Object();
  private final Object mParamRepresentedObject1 = new Object();
  private final Object mParamRepresentedObject2 = new Object();
  private final Object mParamRepresentedObject3 = new Object();
  private final Object mParamRepresentedObject4 = new Object();
  private DelegateMethodModel mDelegateMethod;

  @Before
  public void setup() {
    when(mSpecModel.isPureRender()).thenReturn(true);
    when(mSpecModel.getProps()).thenReturn(ImmutableList.of(mPropModel));
    mDelegateMethod = new DelegateMethodModel(
        ImmutableList.<Annotation>of(new Annotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return ShouldUpdate.class;
          }
        }),
        ImmutableList.<Modifier>of(),
        "method",
        TypeName.BOOLEAN,
        ImmutableList.<MethodParamModel>of(),
        mDelegateMethodRepresentedObject1);
    when(mSpecModel.getDelegateMethods()).thenReturn(ImmutableList.of(mDelegateMethod));
  }

  @Test
  public void testShouldUpdateDefinedButNotPureRender() {
    when(mSpecModel.isPureRender()).thenReturn(false);

    List<SpecModelValidationError> validationErrors = PureRenderValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Specs defining a method annotated with @ShouldUpdate should also set " +
            "isPureRender = true in the top-level spec annotation.");
  }

  @Test
  public void testNoPropWithSameName() {
    when(mPropModel.getName()).thenReturn("name1");
    when(mMethodParamModel.getName()).thenReturn("name2");
    when(mMethodParamModel.getRepresentedObject()).thenReturn(mParamRepresentedObject2);
    mDelegateMethod = new DelegateMethodModel(
        mDelegateMethod.annotations,
        mDelegateMethod.modifiers,
        mDelegateMethod.name,
        mDelegateMethod.returnType,
        ImmutableList.of(mMethodParamModel),
        mDelegateMethod.representedObject);
    when(mSpecModel.getDelegateMethods()).thenReturn(ImmutableList.of(mDelegateMethod));

    List<SpecModelValidationError> validationErrors = PureRenderValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mParamRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Names of parameters for a method annotated with @ShouldUpdate should match a " +
            "declared Prop of the same name.");
  }

  @Test
  public void testShouldUpdateParamTypeNotValid() {
    when(mPropModel.getName()).thenReturn("name");
    when(mPropModel.getType()).thenReturn(TypeName.INT);
    MethodParamModel methodParamModel1 =
        TestMethodParamModel.newBuilder()
            .type(TypeName.INT)
            .name("name")
            .representedObject(mParamRepresentedObject1)
            .build();
    MethodParamModel methodParamModel2 =
        TestMethodParamModel.newBuilder()
            .type(ParameterizedTypeName.get(
                ClassName.bestGuess("com.facebook.litho.Output"),
                TypeVariableName.get("T")))
            .name("name")
            .representedObject(mParamRepresentedObject2)
            .build();
    MethodParamModel methodParamModel3 =
        TestMethodParamModel.newBuilder()
            .type(ParameterizedTypeName.get(
                ClassNames.DIFF,
                TypeVariableName.get("S"),
                TypeVariableName.get("T")))
            .name("name")
            .representedObject(mParamRepresentedObject3)
            .build();
    MethodParamModel methodParamModel4 =
        TestMethodParamModel.newBuilder()
            .type(ParameterizedTypeName.get(
                ClassNames.DIFF,
                WildcardTypeName.subtypeOf(ClassName.bestGuess("java.lang.Object"))))
            .name("name")
            .representedObject(mParamRepresentedObject4)
            .build();
    MethodParamModel methodParamModel5 =
        TestMethodParamModel.newBuilder()
            .type(ParameterizedTypeName.get(
                ClassNames.DIFF,
                ClassName.get("java.lang", "Integer")))
            .name("name")
            .build();

    mDelegateMethod =
        new DelegateMethodModel(
            mDelegateMethod.annotations,
            mDelegateMethod.modifiers,
            mDelegateMethod.name,
            mDelegateMethod.returnType,
            ImmutableList.of(
                methodParamModel1,
                methodParamModel2,
                methodParamModel3,
                methodParamModel4,
                methodParamModel5),
            mDelegateMethodRepresentedObject1);
    when(mSpecModel.getDelegateMethods()).thenReturn(ImmutableList.of(mDelegateMethod));

    List<SpecModelValidationError> validationErrors = PureRenderValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(4);

    assertThat(validationErrors.get(0).element).isSameAs(mParamRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("Types of parameters for a method annotated with @ShouldUpdate should be " +
            "Diff<T>, where T is the type of the declared Prop of the same name.");

    assertThat(validationErrors.get(1).element).isSameAs(mParamRepresentedObject2);
    assertThat(validationErrors.get(1).message)
        .isEqualTo("Types of parameters for a method annotated with @ShouldUpdate should be " +
            "Diff<T>, where T is the type of the declared Prop of the same name.");

    assertThat(validationErrors.get(2).element).isSameAs(mParamRepresentedObject3);
    assertThat(validationErrors.get(2).message)
        .isEqualTo("Types of parameters for a method annotated with @ShouldUpdate should be " +
            "Diff<T>, where T is the type of the declared Prop of the same name.");

    assertThat(validationErrors.get(3).element).isSameAs(mParamRepresentedObject4);
    assertThat(validationErrors.get(3).message)
        .isEqualTo("Types of parameters for a method annotated with @ShouldUpdate should be " +
            "Diff<T>, where T is the type of the declared Prop of the same name.");
  }
}
