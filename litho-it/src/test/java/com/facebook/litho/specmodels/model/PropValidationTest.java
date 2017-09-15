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

/**
 * Tests {@link PropValidation}
 */
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
    when(mPropModel1.getType()).thenReturn(TypeName.BOOLEAN);
    when(mPropModel2.getType()).thenReturn(TypeName.INT);
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
    when(mPropModel1.getType()).thenReturn(TypeName.BOOLEAN);
    when(mPropModel2.getType()).thenReturn(TypeName.INT);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "The prop sameName is defined differently in different methods. Ensure that each " +
            "instance of this prop is declared in the same way (this means having the same type, " +
            "resType and value for isOptional).");
  }

  @Test
  public void testTwoPropsWithSameNameButDifferentIsOptional() {
    when(mPropModel1.getName()).thenReturn("sameName");
    when(mPropModel2.getName()).thenReturn("sameName");
    when(mPropModel1.getType()).thenReturn(TypeName.INT);
    when(mPropModel2.getType()).thenReturn(TypeName.INT);
    when(mPropModel1.isOptional()).thenReturn(true);

    List<SpecModelValidationError> validationErrors =
        PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "The prop sameName is defined differently in different methods. Ensure that each " +
            "instance of this prop is declared in the same way (this means having the same type, " +
            "resType and value for isOptional).");
  }

  @Test
  public void testTwoPropsWithSameNameButDifferentResType() {
    when(mPropModel1.getName()).thenReturn("sameName");
    when(mPropModel2.getName()).thenReturn("sameName");
    when(mPropModel1.getType()).thenReturn(TypeName.INT);
    when(mPropModel2.getType()).thenReturn(TypeName.INT);
    when(mPropModel1.getResType()).thenReturn(ResType.INT);

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "The prop sameName is defined differently in different methods. Ensure that each " +
            "instance of this prop is declared in the same way (this means having the same type, " +
            "resType and value for isOptional).");
  }

  @Test
  public void testPropWithReservedName() {
    when(mPropModel1.getName()).thenReturn("withLayout");

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "'withLayout' is a reserved prop name used by the component's layout builder. Please use " +
            "another name.");
  }

  @Test
  public void testPropWithReservedType() {
    when(mPropModel1.getType()).thenReturn(ClassNames.COMPONENT_LAYOUT);

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Props may not be declared with the following types: " +
            "[com.facebook.litho.ComponentLayout, " +
            "com.facebook.litho.ComponentLayout.Builder, " +
            "com.facebook.litho.ComponentLayout.ContainerBuilder, " +
            "com.facebook.litho.Component.Builder, " +
            "com.facebook.litho.Component.BuilderWithLayout, " +
            "com.facebook.litho.reference.Reference.Builder].");
  }

  @Test
  public void testOptionalPropWithDefault() {
    when(mPropModel1.isOptional()).thenReturn(false);
    when(mPropModel1.hasDefault(any(ImmutableList.class))).thenReturn(true);

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "name1 is not optional so it should not be declared with a default value.");
  }

  @Test
  public void testIncorrectTypeForResType() {
    when(mPropModel1.getResType()).thenReturn(ResType.BOOL);
    when(mPropModel1.getType()).thenReturn(TypeName.INT);

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
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

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
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
    when(mPropModel1.getType()).thenReturn(TypeName.get(String.class));

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
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
    when(mPropModel1.getType())
        .thenReturn(ParameterizedTypeName.get(ArrayList.class, String.class));

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "name1 is a variable argument, and thus should be a List<> type.");
  }

  @Test
  public void testIncorrectTypeForResTypeWithVarArg() {
    when(mPropModel1.getResType()).thenReturn(ResType.BOOL);
    when(mPropModel1.hasVarArgs()).thenReturn(true);
    when(mPropModel1.getType()).thenReturn(ParameterizedTypeName.get(ClassNames.LIST, TypeName.INT.box()));

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "A variable argument declared with resType BOOL must be one of the following types: " +
            "[java.util.List<java.lang.Boolean>].");
  }

  @Test
  public void testResTypeDimenMustNotHavePxOrDimensionAnnotations() {
    when(mPropModel1.getResType()).thenReturn(ResType.DIMEN_OFFSET);
    when(mPropModel1.getType()).thenReturn(TypeName.INT);
    when(mPropModel1.getExternalAnnotations())
        .thenReturn(ImmutableList.of(AnnotationSpec.builder(ClassNames.PX).build()));

    when(mPropModel2.getResType()).thenReturn(ResType.DIMEN_SIZE);
    when(mPropModel2.getType()).thenReturn(TypeName.INT);
    when(mPropModel2.getExternalAnnotations())
        .thenReturn(ImmutableList.of(AnnotationSpec.builder(ClassNames.DIMENSION).build()));

    List<SpecModelValidationError> validationErrors = PropValidation.validate(mSpecModel, PropValidation.RESERVED_PROP_NAMES);
    assertThat(validationErrors).hasSize(2);
    assertThat(validationErrors.get(0).element).isEqualTo(mRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Props with resType DIMEN_OFFSET should not be annotated with "
                + "android.support.annotation.Px or android.support.annotation.Dimension, since "
                + "these annotations will automatically be added to the relevant builder methods "
                + "in the generated code.");
    assertThat(validationErrors.get(1).element).isEqualTo(mRepresentedObject2);
    assertThat(validationErrors.get(1).message)
        .isEqualTo(
            "Props with resType DIMEN_SIZE should not be annotated with "
                + "android.support.annotation.Px or android.support.annotation.Dimension, since "
                + "these annotations will automatically be added to the relevant builder methods "
                + "in the generated code.");
  }
}
