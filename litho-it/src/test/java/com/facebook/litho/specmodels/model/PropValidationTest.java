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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.ResType;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link PropValidation} */
@RunWith(JUnit4.class)
public class PropValidationTest {
  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final PropModel mPropModel1 = mock(PropModel.class);
  private final PropModel mPropModel2 = mock(PropModel.class);
  private final Object mRepresentedObject1 = new Object();
  private final Object mRepresentedObject2 = new Object();

  @Before
  public void setup() {
    when(mPropModel1.getName()).thenReturn("name1");
    when(mPropModel2.getName()).thenReturn("name2");
    when(mPropModel1.getTypeName()).thenReturn(TypeName.BOOLEAN);
    when(mPropModel2.getTypeName()).thenReturn(TypeName.INT);
    when(mPropModel1.isOptional()).thenReturn(false);
    when(mPropModel2.isOptional()).thenReturn(false);
    when(mPropModel1.getResType()).thenReturn(ResType.NONE);
    when(mPropModel2.getResType()).thenReturn(ResType.NONE);
    when(mPropModel1.getRepresentedObject()).thenReturn(mRepresentedObject1);
    when(mPropModel2.getRepresentedObject()).thenReturn(mRepresentedObject2);
    when(mSpecModel.getProps()).thenReturn(ImmutableList.of(mPropModel1, mPropModel2));
    when(mSpecModel.getPropDefaults()).thenReturn(ImmutableList.<PropDefaultModel>of());
  }

  @Test
  public void testTwoPropsWithSameNameButDifferentType() {
    when(mPropModel1.getName()).thenReturn("sameName");
    when(mPropModel2.getName()).thenReturn("sameName");
    when(mPropModel1.getTypeName()).thenReturn(TypeName.BOOLEAN);
    when(mPropModel2.getTypeName()).thenReturn(TypeName.INT);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "The prop sameName is defined differently in different methods. Ensure that each "
                + "instance of this prop is declared in the same way (this means having the same type, "
                + "resType and values for isOptional, isCommonProp and overrideCommonPropBehavior).");
  }

  @Test
  public void testTwoPropsWithSameNameButDifferentIsOptional() {
    when(mPropModel1.getName()).thenReturn("sameName");
    when(mPropModel2.getName()).thenReturn("sameName");
    when(mPropModel1.getTypeName()).thenReturn(TypeName.INT);
    when(mPropModel2.getTypeName()).thenReturn(TypeName.INT);
    when(mPropModel1.isOptional()).thenReturn(true);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "The prop sameName is defined differently in different methods. Ensure that each "
                + "instance of this prop is declared in the same way (this means having the same type, "
                + "resType and values for isOptional, isCommonProp and overrideCommonPropBehavior).");
  }

  @Test
  public void testTwoPropsWithSameNameButDifferentResType() {
    when(mPropModel1.getName()).thenReturn("sameName");
    when(mPropModel2.getName()).thenReturn("sameName");
    when(mPropModel1.getTypeName()).thenReturn(TypeName.INT);
    when(mPropModel2.getTypeName()).thenReturn(TypeName.INT);
    when(mPropModel1.getResType()).thenReturn(ResType.INT);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "The prop sameName is defined differently in different methods. Ensure that each "
                + "instance of this prop is declared in the same way (this means having the same type, "
                + "resType and values for isOptional, isCommonProp and overrideCommonPropBehavior).");
  }

  @Test
  public void testPropWithReservedName() {
    when(mPropModel1.getName()).thenReturn("layoutDirection");

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "'layoutDirection' is a reserved prop name used by the component's builder. Please"
                + " use another name or add \"isCommonProp\" to the Prop's definition.");
  }

  @Test
  public void testPropMarkedCommonWithoutCommonName() {
    when(mPropModel1.getName()).thenReturn("badName");
    when(mPropModel1.isCommonProp()).thenReturn(true);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Prop with isCommonProp and name badName is incorrectly defined - see PropValidation.java for a list of common props that may be used.");
  }

  @Test
  public void testPropMarkedCommonWithWrongType() {
    when(mPropModel1.getName()).thenReturn("focusable");
    when(mPropModel1.isCommonProp()).thenReturn(true);
    when(mPropModel1.getTypeName()).thenReturn(TypeName.OBJECT);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("A common prop with name focusable must have type of: boolean");
  }

  @Test
  public void testPropMarkedOverrideCommonButNotCommon() {
    when(mPropModel1.overrideCommonPropBehavior()).thenReturn(true);
    when(mPropModel1.isCommonProp()).thenReturn(false);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo("overrideCommonPropBehavior may only be true is isCommonProp is true.");
  }

  @Test
  public void testPropWithReservedType() {
    when(mPropModel1.getTypeName()).thenReturn(ClassNames.COMPONENT_LAYOUT);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Props may not be declared with the following argument types: " +
            "[com.facebook.litho.ComponentLayout, " +
            "com.facebook.litho.Component.Builder].");
  }

  @Test
  public void testOptionalPropWithDefault() {
    when(mPropModel1.isOptional()).thenReturn(false);
    when(mPropModel1.hasDefault(any(ImmutableList.class))).thenReturn(true);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "name1 is not optional so it should not be declared with a default value.");
  }

  @Test
  public void testIncorrectTypeForResType() {
    when(mPropModel1.getResType()).thenReturn(ResType.BOOL);
    when(mPropModel1.getTypeName()).thenReturn(TypeName.INT);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "A prop declared with resType BOOL must be one of the following types: " +
            "[boolean, java.lang.Boolean].");
  }

  @Test
  public void testDefaultDefinedWithNoCorrespondingProp() {
    Object propDefaultObject1 = new Object();
    Object propDefaultObject2 = new Object();
    PropDefaultModel propDefault1 =
        new PropDefaultModel(
            TypeName.CHAR, "name1", ImmutableList.<Modifier>of(), propDefaultObject1);
    PropDefaultModel propDefault2 =
        new PropDefaultModel(
            TypeName.CHAR, "notAPropName", ImmutableList.<Modifier>of(), propDefaultObject2);

    when(mSpecModel.getPropDefaults()).thenReturn(ImmutableList.of(propDefault1, propDefault2));

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(2);
    assertThat(validationErrors.get(0).element).isEqualTo(propDefaultObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "PropDefault name1 of type char should be of type boolean");
    assertThat(validationErrors.get(1).element).isEqualTo(propDefaultObject2);
    assertThat(validationErrors.get(1).message).isEqualTo(
        "PropDefault notAPropName of type char does not correspond to any defined prop");
  }

  @Test
  public void testVarArgPropMustHaveListType() {
    when(mPropModel1.getResType()).thenReturn(ResType.NONE);
    when(mPropModel1.getVarArgsSingleName()).thenReturn("test");
    when(mPropModel1.hasVarArgs()).thenReturn(true);
    when(mPropModel1.getTypeName()).thenReturn(TypeName.get(String.class));

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "name1 is a variable argument, and thus requires a parameterized List type.");
  }

  @Test
  public void testVarArgPropMustHaveParameterizedListType() {
    when(mPropModel1.getResType()).thenReturn(ResType.NONE);
    when(mPropModel1.getVarArgsSingleName()).thenReturn("test");
    when(mPropModel1.hasVarArgs()).thenReturn(true);
    when(mPropModel1.getTypeName())
        .thenReturn(ParameterizedTypeName.get(ArrayList.class, String.class));

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "name1 is a variable argument, and thus should be a List<> type.");
  }

  @Test
  public void testIncorrectTypeForResTypeWithVarArg() {
    when(mPropModel1.getResType()).thenReturn(ResType.BOOL);
    when(mPropModel1.hasVarArgs()).thenReturn(true);
    when(mPropModel1.getTypeName())
        .thenReturn(ParameterizedTypeName.get(ClassNames.LIST, TypeName.INT.box()));

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "A variable argument declared with resType BOOL must be one of the following types: "
                + "[java.util.List<java.lang.Boolean>].");
  }

  @Test
  public void testResTypeDimenMustNotHavePxOrDimensionAnnotations() {
    when(mPropModel1.getResType()).thenReturn(ResType.DIMEN_OFFSET);
    when(mPropModel1.getTypeName()).thenReturn(TypeName.INT);
    when(mPropModel1.getExternalAnnotations())
        .thenReturn(ImmutableList.of(AnnotationSpec.builder(ClassNames.PX).build()));

    when(mPropModel2.getResType()).thenReturn(ResType.DIMEN_SIZE);
    when(mPropModel2.getTypeName()).thenReturn(TypeName.INT);
    when(mPropModel2.getExternalAnnotations())
        .thenReturn(ImmutableList.of(AnnotationSpec.builder(ClassNames.DIMENSION).build()));

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(
            mSpecModel, PropValidation.COMMON_PROP_NAMES, PropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(2);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Props with resType DIMEN_OFFSET should not be annotated with "
                + "androidx.annotation.Px or androidx.annotation.Dimension, since "
                + "these annotations will automatically be added to the relevant builder methods "
                + "in the generated code.");
    assertThat(validationErrors.get(1).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(1).message)
        .isEqualTo(
            "Props with resType DIMEN_SIZE should not be annotated with "
                + "androidx.annotation.Px or androidx.annotation.Dimension, since "
                + "these annotations will automatically be added to the relevant builder methods "
                + "in the generated code.");
  }
}
