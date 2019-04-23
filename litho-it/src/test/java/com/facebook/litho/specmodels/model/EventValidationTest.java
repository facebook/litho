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

import static com.squareup.javapoet.ClassName.OBJECT;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static com.squareup.javapoet.TypeName.INT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link EventValidation} */
@RunWith(JUnit4.class)
public class EventValidationTest {
  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final Object mRepresentedObject1 = new Object();
  private final Object mRepresentedObject2 = new Object();
  private final Object mRepresentedObject3 = new Object();
  private final Object mRepresentedObject4 = new Object();
  private final Object mRepresentedObject5 = new Object();

  @Before
  public void setup() {
    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of());
    when(mSpecModel.getSpecElementType()).thenReturn(SpecElementType.JAVA_CLASS);
    when(mSpecModel.getEventDeclarations()).thenReturn(ImmutableList.of());
    when(mSpecModel.getContextClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
  }

  @Test
  public void testNonPublicFields() {
    FieldModel fieldModel1 =
        new FieldModel(
            FieldSpec.builder(TypeName.INT, "field1", Modifier.PRIVATE).build(),
            mRepresentedObject1);
    FieldModel fieldModel2 =
        new FieldModel(
            FieldSpec.builder(TypeName.INT, "field2", Modifier.PROTECTED).build(),
            mRepresentedObject2);
    FieldModel fieldModel3 =
        new FieldModel(FieldSpec.builder(TypeName.INT, "field3").build(), mRepresentedObject3);
    FieldModel fieldModel4 =
        new FieldModel(
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
        EventValidation.validate(mSpecModel, RunMode.normal());
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
    FieldModel fieldModel1 =
        new FieldModel(
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
        EventValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Event fields must be declared as public non-final.");
  }

  @Test
  public void testNullReturnType() {
    when(mSpecModel.getEventDeclarations())
        .thenReturn(
            ImmutableList.of(
                new EventDeclarationModel(
                    ClassName.OBJECT, null, ImmutableList.of(), mRepresentedObject5)));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject5);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Event declarations must be annotated with @Event.");
  }

  @Test
  public void testEventMethodsWithSameName() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).build();
    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod1 =
        SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.of(Modifier.STATIC))
            .name("sameName")
            .returnTypeSpec(new TypeSpec(INT))
            .typeVariables(ImmutableList.of())
            .methodParams(ImmutableList.of(methodParam))
            .representedObject(mRepresentedObject2)
            .typeModel(
                new EventDeclarationModel(OBJECT, INT, ImmutableList.of(), mRepresentedObject1))
            .build();
    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod2 =
        SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.of(Modifier.STATIC))
            .name("sameName")
            .returnTypeSpec(new TypeSpec(INT))
            .typeVariables(ImmutableList.of())
            .methodParams(ImmutableList.of(methodParam))
            .representedObject(mRepresentedObject4)
            .typeModel(
                new EventDeclarationModel(OBJECT, INT, ImmutableList.of(), mRepresentedObject3))
            .build();

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod1, eventMethod2));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Two methods annotated with @OnEvent should not have the same name (sameName).");
  }

  @Test
  public void testEventMethodsWithWrongReturnType() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).build();
    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod =
        SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.of(Modifier.STATIC))
            .name("name")
            .returnTypeSpec(new TypeSpec(INT))
            .typeVariables(ImmutableList.of())
            .methodParams(ImmutableList.of(methodParam))
            .representedObject(mRepresentedObject2)
            .typeModel(
                new EventDeclarationModel(OBJECT, BOOLEAN, ImmutableList.of(), mRepresentedObject1))
            .build();

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Method must return boolean since that is what java.lang.Object expects.");
  }

  @Test
  public void testEventMethodsWithPrimitiveReturnType() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).build();
    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod =
        SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.of(Modifier.STATIC))
            .name("name")
            .returnTypeSpec(new TypeSpec(INT))
            .typeVariables(ImmutableList.of())
            .methodParams(ImmutableList.of(methodParam))
            .representedObject(mRepresentedObject2)
            .typeModel(
                new EventDeclarationModel(
                    OBJECT, INT.box(), ImmutableList.of(), mRepresentedObject1))
            .build();

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(0);
  }

  @Test
  public void testEventMethodsWithWrongParams() {
    MethodParamModel methodParam1 =
        MockMethodParamModel.newBuilder().type(TypeName.BOOLEAN).build();
    MethodParamModel methodParam2 =
        MockMethodParamModel.newBuilder()
            .representedObject(mRepresentedObject3)
            .type(TypeName.BOOLEAN)
            .build();
    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod =
        SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.of(Modifier.STATIC))
            .name("name")
            .returnTypeSpec(new TypeSpec(INT))
            .typeVariables(ImmutableList.of())
            .methodParams(ImmutableList.of(methodParam1, methodParam2))
            .representedObject(mRepresentedObject2)
            .typeModel(
                new EventDeclarationModel(OBJECT, INT, ImmutableList.of(), mRepresentedObject1))
            .build();

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(2);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "The first parameter for a method annotated with @OnEvent should be of type " +
            "com.facebook.litho.ComponentContext.");

    assertThat(validationErrors.get(1).element).isEqualTo(mRepresentedObject3);
    assertThat(validationErrors.get(1).message)
        .isEqualTo(
            "Param must be annotated with one of @FromEvent, @Prop, @InjectProp, @TreeProp, @CachedValue, @State or @Param.");
  }

  @Test
  public void testEventMethodsWithFromEventParamThatDoesNotExist() {
    MethodParamModel methodParam0 =
        MockMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).name("c").build();
    MethodParamModel methodParam1 =
        MockMethodParamModel.newBuilder()
            .annotations(FromEvent.class)
            .type(TypeName.BOOLEAN)
            .name("booleanParam")
            .representedObject(mRepresentedObject3)
            .build();
    MethodParamModel methodParam2 =
        MockMethodParamModel.newBuilder()
            .annotations(FromEvent.class)
            .type(TypeName.BOOLEAN)
            .name("booleanParam2")
            .representedObject(mRepresentedObject4)
            .build();
    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod =
        SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
            .annotations(ImmutableList.of())
            .modifiers(ImmutableList.of(Modifier.STATIC))
            .name("name")
            .returnTypeSpec(new TypeSpec(INT))
            .typeVariables(ImmutableList.of())
            .methodParams(ImmutableList.of(methodParam0, methodParam1, methodParam2))
            .representedObject(mRepresentedObject2)
            .typeModel(
                new EventDeclarationModel(
                    OBJECT,
                    INT,
                    ImmutableList.of(
                        new FieldModel(
                            FieldSpec.builder(BOOLEAN, "booleanParam").build(),
                            mRepresentedObject5)),
                    mRepresentedObject1))
            .build();

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject4);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Param with name booleanParam2 and type boolean is not a member of java.lang.Object.");
  }

  @Test
  public void testEventMethodNotStatic() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).build();
    SpecMethodModel<EventMethod, EventDeclarationModel> eventMethod =
        SpecMethodModel.<EventMethod, EventDeclarationModel>builder()
            .annotations(ImmutableList.<Annotation>of())
            .modifiers(ImmutableList.<Modifier>of())
            .name("name")
            .returnTypeSpec(new TypeSpec(BOOLEAN))
            .typeVariables(ImmutableList.<TypeVariableName>of())
            .methodParams(ImmutableList.of(methodParam))
            .representedObject(mRepresentedObject2)
            .typeModel(
                new EventDeclarationModel(OBJECT, BOOLEAN, ImmutableList.of(), mRepresentedObject1))
            .build();

    when(mSpecModel.getEventMethods()).thenReturn(ImmutableList.of(eventMethod));

    List<SpecModelValidationError> validationErrors =
        EventValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message).isEqualTo("Methods in a spec must be static.");
  }
}
