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

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.TestMethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link EventValidation}
 */
public class EventValidationTest {
  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final Object mRepresentedObject1 = new Object();
  private final Object mRepresentedObject2 = new Object();
  private final Object mRepresentedObject3 = new Object();
  private final Object mRepresentedObject4 = new Object();
  private final Object mRepresentedObject5 = new Object();

  @Before
  public void setup() {
    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.<EventMethodModel>of());
    when(mSpecModel.getEventDeclarations()).thenReturn(ImmutableList.<EventDeclarationModel>of());
    when(mSpecModel.getContextClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
  }

  @Test
  public void testNonPublicFields() {
    EventDeclarationModel.FieldModel fieldModel1 =
        new EventDeclarationModel.FieldModel(
            FieldSpec.builder(TypeName.INT, "field1", Modifier.PRIVATE).build(),
            mRepresentedObject1);
    EventDeclarationModel.FieldModel fieldModel2 =
        new EventDeclarationModel.FieldModel(
            FieldSpec.builder(TypeName.INT, "field2", Modifier.PROTECTED).build(),
            mRepresentedObject2);
    EventDeclarationModel.FieldModel fieldModel3 =
        new EventDeclarationModel.FieldModel(
            FieldSpec.builder(TypeName.INT, "field3").build(),
            mRepresentedObject3);
    EventDeclarationModel.FieldModel fieldModel4 =
        new EventDeclarationModel.FieldModel(
            FieldSpec.builder(TypeName.INT, "field4", Modifier.PUBLIC).build(),
            mRepresentedObject4);

    when(mSpecModel.getEventDeclarations()).thenReturn(
        ImmutableList.of(
            new EventDeclarationModel(
                ClassName.OBJECT,
                ClassName.OBJECT,
                ImmutableList.of(fieldModel1, fieldModel2, fieldModel3, fieldModel4),
                mRepresentedObject5)));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(3);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(1).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(2).element).isEqualTo(mRepresentedObject3);

    for (int i = 0; i < 3; i++) {
      assertThat(validationErrors.get(i).message).isEqualTo(
          "Event fields must be declared as public non-final.");
    }
  }

  @Test
  public void testFinalFields() {
    EventDeclarationModel.FieldModel fieldModel1 =
        new EventDeclarationModel.FieldModel(
            FieldSpec.builder(TypeName.INT, "field1", Modifier.PUBLIC, Modifier.FINAL).build(),
            mRepresentedObject1);

    when(mSpecModel.getEventDeclarations()).thenReturn(
        ImmutableList.of(
            new EventDeclarationModel(
                ClassName.OBJECT,
                ClassName.OBJECT,
                ImmutableList.of(fieldModel1),
                mRepresentedObject5)));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Event fields must be declared as public non-final.");
  }

  @Test
  public void testNullReturnType() {
    when(mSpecModel.getEventDeclarations()).thenReturn(
        ImmutableList.of(
            new EventDeclarationModel(
                ClassName.OBJECT,
                null,
                ImmutableList.<EventDeclarationModel.FieldModel>of(),
                mRepresentedObject5)));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject5);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Event declarations must be annotated with @Event.");
  }

  @Test
  public void testEventMethodsWithSameName() {
    MethodParamModel methodParam = TestMethodParamModel.newBuilder()
        .type(ClassNames.COMPONENT_CONTEXT)
        .build();
    EventMethodModel eventMethod1 = new EventMethodModel(
        new EventDeclarationModel(
            ClassName.OBJECT,
            TypeName.INT,
            ImmutableList.<EventDeclarationModel.FieldModel>of(),
            mRepresentedObject1),
        ImmutableList.of(Modifier.STATIC),
        "sameName",
        TypeName.INT,
        ImmutableList.<TypeVariableName>of(),
        ImmutableList.of(methodParam),
        mRepresentedObject2);
    EventMethodModel eventMethod2 = new EventMethodModel(
        new EventDeclarationModel(
            ClassName.OBJECT,
            TypeName.INT,
            ImmutableList.<EventDeclarationModel.FieldModel>of(),
            mRepresentedObject3),
        ImmutableList.of(Modifier.STATIC),
        "sameName",
        TypeName.INT,
        ImmutableList.<TypeVariableName>of(),
        ImmutableList.of(methodParam),
        mRepresentedObject4);

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod1, eventMethod2));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Two methods annotated with @OnEvent should not have the same name (sameName).");
  }

  @Test
  public void testEventMethodsWithWrongReturnType() {
    MethodParamModel methodParam = TestMethodParamModel.newBuilder()
        .type(ClassNames.COMPONENT_CONTEXT)
        .build();
    EventMethodModel eventMethod = new EventMethodModel(
        new EventDeclarationModel(
            ClassName.OBJECT,
            TypeName.BOOLEAN,
            ImmutableList.<EventDeclarationModel.FieldModel>of(),
            mRepresentedObject1),
        ImmutableList.of(Modifier.STATIC),
        "name",
        TypeName.INT,
        ImmutableList.<TypeVariableName>of(),
        ImmutableList.of(methodParam),
        mRepresentedObject2);

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Method must return boolean since that is what java.lang.Object expects.");
  }

  @Test
  public void testEventMethodsWithPrimitiveReturnType() {
    MethodParamModel methodParam =
        TestMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).build();
    EventMethodModel eventMethod =
        new EventMethodModel(
            new EventDeclarationModel(
                ClassName.OBJECT,
                TypeName.INT.box(),
                ImmutableList.<EventDeclarationModel.FieldModel>of(),
                mRepresentedObject1),
            ImmutableList.of(Modifier.STATIC),
            "name",
            TypeName.INT,
            ImmutableList.<TypeVariableName>of(),
            ImmutableList.of(methodParam),
            mRepresentedObject2);

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors = EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(0);
  }

  @Test
  public void testEventMethodsWithWrongFirstParam() {
    MethodParamModel methodParam = TestMethodParamModel.newBuilder()
        .type(TypeName.BOOLEAN)
        .build();
    EventMethodModel eventMethod = new EventMethodModel(
        new EventDeclarationModel(
            ClassName.OBJECT,
            TypeName.INT,
            ImmutableList.<EventDeclarationModel.FieldModel>of(),
            mRepresentedObject1),
        ImmutableList.of(Modifier.STATIC),
        "name",
        TypeName.INT,
        ImmutableList.<TypeVariableName>of(),
        ImmutableList.of(methodParam),
        mRepresentedObject2);

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "The first parameter for a method annotated with @OnEvent should be of type " +
            "com.facebook.litho.ComponentContext.");
  }

  @Test
  public void testEventMethodsWithFromEventParamThatDoesNotExist() {
    MethodParamModel methodParam0 = TestMethodParamModel.newBuilder()
        .type(ClassNames.COMPONENT_CONTEXT)
        .name("c")
        .build();
    MethodParamModel methodParam1 = TestMethodParamModel.newBuilder()
        .annotation(FromEvent.class)
        .type(TypeName.BOOLEAN)
        .name("booleanParam")
        .representedObject(mRepresentedObject3)
        .build();
    MethodParamModel methodParam2 = TestMethodParamModel.newBuilder()
        .annotation(FromEvent.class)
        .type(TypeName.BOOLEAN)
        .name("booleanParam2")
        .representedObject(mRepresentedObject4)
        .build();
    EventMethodModel eventMethod = new EventMethodModel(
        new EventDeclarationModel(
            ClassName.OBJECT,
            TypeName.INT,
            ImmutableList.of(
                new EventDeclarationModel.FieldModel(
                    FieldSpec.builder(TypeName.BOOLEAN, "booleanParam").build(),
                mRepresentedObject5)),
            mRepresentedObject1),
        ImmutableList.of(Modifier.STATIC),
        "name",
        TypeName.INT,
        ImmutableList.<TypeVariableName>of(),
        ImmutableList.of(methodParam0, methodParam1, methodParam2),
        mRepresentedObject2);

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject4);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Param with name booleanParam2 and type boolean is not a member of java.lang.Object.");
  }

  @Test
  public void testEventMethodNotStatic() {
    MethodParamModel methodParam = TestMethodParamModel.newBuilder()
        .type(ClassNames.COMPONENT_CONTEXT)
        .build();
    EventMethodModel eventMethod = new EventMethodModel(
        new EventDeclarationModel(
            ClassName.OBJECT,
            TypeName.BOOLEAN,
            ImmutableList.<EventDeclarationModel.FieldModel>of(),
            mRepresentedObject1),
        ImmutableList.<Modifier>of(),
        "name",
        TypeName.BOOLEAN,
        ImmutableList.<TypeVariableName>of(),
        ImmutableList.of(methodParam),
        mRepresentedObject2);

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Methods in a spec that doesn't have dependency injection must be static.");
  }
}
