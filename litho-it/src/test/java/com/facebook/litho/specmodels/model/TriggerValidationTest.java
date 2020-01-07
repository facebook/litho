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

import static com.squareup.javapoet.ClassName.OBJECT;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static com.squareup.javapoet.TypeName.INT;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link TriggerValidation} */
@RunWith(JUnit4.class)
public class TriggerValidationTest {
  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final Object mRepresentedObject1 = new Object();
  private final Object mRepresentedObject2 = new Object();
  private final Object mRepresentedObject3 = new Object();
  private final Object mRepresentedObject4 = new Object();

  @Before
  public void setup() {
    when(mSpecModel.getTriggerMethods()).thenReturn(ImmutableList.of());
    when(mSpecModel.getSpecElementType()).thenReturn(SpecElementType.JAVA_CLASS);
    when(mSpecModel.getEventDeclarations()).thenReturn(ImmutableList.of());
    when(mSpecModel.getContextClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
  }

  @Test
  public void testTriggerMethodsWithSameName() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).build();
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethod1 =
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
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethod2 =
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

    when(mSpecModel.getTriggerMethods())
        .thenReturn(ImmutableList.of(triggerMethod1, triggerMethod2));

    List<SpecModelValidationError> validationErrors =
        TriggerValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Two methods annotated with @OnTrigger should not have the same name (sameName).");
  }

  @Test
  public void testEventMethodsWithWrongReturnType() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).build();
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethod =
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

    when(mSpecModel.getTriggerMethods()).thenReturn(ImmutableList.of(triggerMethod));

    List<SpecModelValidationError> validationErrors =
        TriggerValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("Method must return boolean since that is what java.lang.Object expects.");
  }

  @Test
  public void testTriggerMethodsWithPrimitiveReturnType() {
    MethodParamModel methodParam =
        MockMethodParamModel.newBuilder().type(ClassNames.COMPONENT_CONTEXT).build();
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethod =
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

    when(mSpecModel.getTriggerMethods()).thenReturn(ImmutableList.of(triggerMethod));

    List<SpecModelValidationError> validationErrors =
        TriggerValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Method must return java.lang.Integer since that is what java.lang.Object expects.");
  }

  @Test
  public void testTriggerMethodsWithWrongParams() {
    MethodParamModel methodParam1 =
        MockMethodParamModel.newBuilder().type(TypeName.BOOLEAN).build();
    MethodParamModel methodParam2 =
        MockMethodParamModel.newBuilder()
            .representedObject(mRepresentedObject3)
            .type(TypeName.BOOLEAN)
            .build();
    SpecMethodModel<EventMethod, EventDeclarationModel> triggerMethod =
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

    when(mSpecModel.getTriggerMethods()).thenReturn(ImmutableList.of(triggerMethod));

    List<SpecModelValidationError> validationErrors =
        TriggerValidation.validate(mSpecModel, RunMode.normal());
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "The first parameter for a method annotated with @OnTrigger should be of type "
                + "com.facebook.litho.ComponentContext.");
  }
}
