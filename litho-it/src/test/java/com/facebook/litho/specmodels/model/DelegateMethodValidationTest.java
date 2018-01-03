/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import static com.facebook.litho.specmodels.model.DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link DelegateMethodValidation}
 */
public class DelegateMethodValidationTest {
  private final LayoutSpecModel mLayoutSpecModel = mock(LayoutSpecModel.class);
  private final MountSpecModel mMountSpecModel = mock(MountSpecModel.class);
  private final Object mModelRepresentedObject = new Object();
  private final Object mMountSpecObject = new Object();
  private final Object mDelegateMethodObject1 = new Object();
  private final Object mDelegateMethodObject2 = new Object();
  private final Object mMethodParamObject1 = new Object();
  private final Object mMethodParamObject2 = new Object();
  private final Object mMethodParamObject3 = new Object();
  private SpecMethodModel<DelegateMethod, Void> mOnCreateMountContent;
  private final Object mOnCreateMountContentObject = new Object();

  @Before
  public void setup() {
    when(mLayoutSpecModel.getRepresentedObject()).thenReturn(mModelRepresentedObject);
    when(mMountSpecModel.getRepresentedObject()).thenReturn(mMountSpecObject);

    mOnCreateMountContent =
        new SpecMethodModel<DelegateMethod, Void>(
            ImmutableList.of((Annotation) () -> OnCreateMountContent.class),
            ImmutableList.of(Modifier.STATIC),
            "onCreateMountContent",
            ClassName.bestGuess("java.lang.MadeUpClass"),
            ImmutableList.of(),
            ImmutableList.of(
                MockMethodParamModel.newBuilder()
                    .name("c")
                    .type(ClassNames.COMPONENT_CONTEXT)
                    .representedObject(new Object())
                    .build()),
            mOnCreateMountContentObject,
            null);
  }

  @Test
  public void testNoDelegateMethods() {
    when(mLayoutSpecModel.getDelegateMethods()).thenReturn(ImmutableList.of());

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mModelRepresentedObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "You need to have a method annotated with either @OnCreateLayout " +
            "or @OnCreateLayoutWithSizeSpec in your spec. In most cases, @OnCreateLayout " +
            "is what you want.");
  }

  @Test
  public void testOnCreateLayoutAndOnCreateLayoutWithSizeSpec() {
    when(mLayoutSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayout.class),
                    ImmutableList.of(Modifier.STATIC),
                    "name",
                    ClassNames.COMPONENT,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .name("c")
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(new Object())
                            .build()),
                    new Object(),
                    null),
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayoutWithSizeSpec.class),
                    ImmutableList.of(Modifier.STATIC),
                    "name",
                    ClassNames.COMPONENT,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .name("c")
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(new Object())
                            .build(),
                        MockMethodParamModel.newBuilder()
                            .name("widthSpec")
                            .type(TypeName.INT)
                            .representedObject(new Object())
                            .build(),
                        MockMethodParamModel.newBuilder()
                            .name("heightSpec")
                            .type(TypeName.INT)
                            .representedObject(new Object())
                            .build()),
                    new Object(),
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mModelRepresentedObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Your LayoutSpec should have a method annotated with either @OnCreateLayout " +
            "or @OnCreateLayoutWithSizeSpec, but not both. In most cases, @OnCreateLayout " +
            "is what you want.");
  }

  @Test
  public void testDelegateMethodDoesNotDefineEnoughParams() {
    when(mLayoutSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayoutWithSizeSpec.class),
                    ImmutableList.of(Modifier.STATIC),
                    "name",
                    ClassNames.COMPONENT,
                    ImmutableList.of(),
                    ImmutableList.of(),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Methods annotated with interface " +
            "com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec " +
            "must have at least 3 parameters, and they should be of type " +
            "com.facebook.litho.ComponentContext, int, int.");
  }

  @Test
  public void testDelegateMethodDoesNotDefinedParamsOfCorrectType() {
    when(mLayoutSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayout.class),
                    ImmutableList.of(Modifier.STATIC),
                    "name",
                    ClassNames.COMPONENT,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(TypeName.BOOLEAN)
                            .representedObject(mMethodParamObject1)
                            .build()),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Parameter in position 0 of a method annotated with interface " +
            "com.facebook.litho.annotations.OnCreateLayout should be of type " +
            "com.facebook.litho.ComponentContext.");
  }

  @Test
  public void testDelegateMethodHasIncorrectOptionalParams() {
    when(mLayoutSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayout.class),
                    ImmutableList.of(Modifier.STATIC),
                    "name",
                    ClassNames.COMPONENT,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build(),
                        MockMethodParamModel.newBuilder()
                            .type(TypeName.INT)
                            .representedObject(mMethodParamObject2)
                            .build()),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject2);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Not a valid parameter, should be one of the following: @Prop T somePropName. @TreeProp T someTreePropName. @State T someStateName. @InjectProp T someInjectPropName. ");
  }

  @Test
  public void testDelegateMethodIsNotStatic() {
    when(mLayoutSpecModel.getSpecElementType()).thenReturn(SpecElementType.JAVA_CLASS);
    when(mLayoutSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayout.class),
                    ImmutableList.of(),
                    "name",
                    ClassNames.COMPONENT,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build()),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Methods in a spec that doesn't have dependency injection must be static.");
  }

  @Test
  public void testDelegateMethodIsNotStaticWithKotlinSingleton() {
    when(mLayoutSpecModel.getSpecElementType()).thenReturn(SpecElementType.KOTLIN_SINGLETON);
    when(mLayoutSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayout.class),
                    ImmutableList.of(),
                    "name",
                    ClassNames.COMPONENT,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build()),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).isEmpty();
  }

  @Test
  public void testDelegateMethodHasIncorrectReturnType() {
    when(mLayoutSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayout.class),
                    ImmutableList.of(Modifier.STATIC),
                    "name",
                    ClassNames.COMPONENT_LAYOUT,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build()),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "A method annotated with @OnCreateLayout needs to return "
                + "com.facebook.litho.Component. Note that even if your return value is a "
                + "subclass of com.facebook.litho.Component, you should still use "
                + "com.facebook.litho.Component as the return type.");
  }

  @Test
  public void testMountSpecHasOnCreateMountContent() {
    when(mMountSpecModel.getDelegateMethods()).thenReturn(ImmutableList.of());

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateMountSpecModel(mMountSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMountSpecObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "All MountSpecs need to have a method annotated with @OnCreateMountContent.");
  }

  @Test
  public void testSecondParameter() {
    when(mMountSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                mOnCreateMountContent,
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnMount.class),
                    ImmutableList.of(Modifier.STATIC),
                    "onMount",
                    TypeName.VOID,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build(),
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.OBJECT)
                            .representedObject(mMethodParamObject2)
                            .build()),
                    mDelegateMethodObject1,
                    null),
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnBind.class),
                    ImmutableList.of(Modifier.STATIC),
                    "onBind",
                    TypeName.VOID,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build(),
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.OBJECT)
                            .representedObject(mMethodParamObject2)
                            .build()),
                    mDelegateMethodObject2,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateMountSpecModel(mMountSpecModel);
    assertThat(validationErrors).hasSize(2);
    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "The second parameter of a method annotated with interface " +
            "com.facebook.litho.annotations.OnMount must have the same type as the " +
            "return type of the method annotated with @OnCreateMountContent (i.e. " +
            "java.lang.MadeUpClass).");
    assertThat(validationErrors.get(1).element).isEqualTo(mDelegateMethodObject2);
    assertThat(validationErrors.get(1).message).isEqualTo(
        "The second parameter of a method annotated with interface " +
            "com.facebook.litho.annotations.OnBind must have the same type as the " +
            "return type of the method annotated with @OnCreateMountContent (i.e. " +
            "java.lang.MadeUpClass).");
  }

  @Test
  public void testInterStageInputParamAnnotationNotValid() {
    final InterStageInputParamModel interStageInputParamModel =
        mock(InterStageInputParamModel.class);
    when(interStageInputParamModel.getAnnotations())
        .thenReturn(ImmutableList.of((Annotation) () -> FromBind.class));
    when(interStageInputParamModel.getRepresentedObject()).thenReturn(mMethodParamObject3);
    when(mMountSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                mOnCreateMountContent,
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnUnmount.class),
                    ImmutableList.of(Modifier.STATIC),
                    "onUnmount",
                    TypeName.VOID,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build(),
                        MockMethodParamModel.newBuilder()
                            .type(ClassName.bestGuess("java.lang.MadeUpClass"))
                            .representedObject(mMethodParamObject2)
                            .build(),
                        interStageInputParamModel),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateMountSpecModel(mMountSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject3);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Inter-stage input annotation is not valid for this method, please use one of the " +
            "following: [interface com.facebook.litho.annotations.FromPrepare, " +
            "interface com.facebook.litho.annotations.FromMeasure, " +
            "interface com.facebook.litho.annotations.FromMeasureBaseline, " +
            "interface com.facebook.litho.annotations.FromBoundsDefined]");
  }

  @Test
  public void testInterStageInputMethodDoesNotExist() {
    final InterStageInputParamModel interStageInputParamModel =
        mock(InterStageInputParamModel.class);
    when(interStageInputParamModel.getAnnotations())
        .thenReturn(ImmutableList.of((Annotation) () -> FromPrepare.class));
    when(interStageInputParamModel.getName()).thenReturn("interStageInput");
    when(interStageInputParamModel.getType()).thenReturn(TypeName.INT);
    when(interStageInputParamModel.getRepresentedObject()).thenReturn(mMethodParamObject3);
    when(mMountSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                mOnCreateMountContent,
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnMount.class),
                    ImmutableList.of(Modifier.STATIC),
                    "onMount",
                    TypeName.VOID,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build(),
                        MockMethodParamModel.newBuilder()
                            .type(ClassName.bestGuess("java.lang.MadeUpClass"))
                            .representedObject(mMethodParamObject2)
                            .build(),
                        interStageInputParamModel),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateMountSpecModel(mMountSpecModel);
    for (SpecModelValidationError validationError : validationErrors) {
      System.out.println(validationError.message);
    }
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject3);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "To use interface com.facebook.litho.annotations.FromPrepare on param interStageInput " +
            "you must have a method annotated with interface " +
            "com.facebook.litho.annotations.OnPrepare that has a param Output<java.lang.Integer> " +
            "interStageInput");
  }

  @Test
  public void testInterStageInputOutputDoesNotExist() {
    final InterStageInputParamModel interStageInputParamModel =
        mock(InterStageInputParamModel.class);
    when(interStageInputParamModel.getAnnotations())
        .thenReturn(ImmutableList.of((Annotation) () -> FromPrepare.class));
    when(interStageInputParamModel.getName()).thenReturn("interStageInput");
    when(interStageInputParamModel.getType()).thenReturn(TypeName.INT);
    when(interStageInputParamModel.getRepresentedObject()).thenReturn(mMethodParamObject3);
    when(mMountSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                mOnCreateMountContent,
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnMount.class),
                    ImmutableList.of(Modifier.STATIC),
                    "onMount",
                    TypeName.VOID,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .name("c")
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build(),
                        MockMethodParamModel.newBuilder()
                            .name("param")
                            .type(ClassName.bestGuess("java.lang.MadeUpClass"))
                            .representedObject(mMethodParamObject2)
                            .build(),
                        interStageInputParamModel),
                    mDelegateMethodObject1,
                    null),
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnPrepare.class),
                    ImmutableList.of(Modifier.STATIC),
                    "onPrepare",
                    TypeName.VOID,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .name("c")
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build()),
                    mDelegateMethodObject2,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateMountSpecModel(mMountSpecModel);
    for (final SpecModelValidationError validationError : validationErrors) {
      System.out.println(validationError.message);
    }
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject3);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "To use interface com.facebook.litho.annotations.FromPrepare on param interStageInput " +
            "your method annotated with interface com.facebook.litho.annotations.OnPrepare must " +
            "have a param Output<java.lang.Integer> interStageInput");
  }

  @Test
  public void testDelegateMethodWithOptionalParameters() throws Exception {
    final Map<Class<? extends Annotation>, DelegateMethodDescription> map = new HashMap<>();
    map.put(
        OnCreateLayout.class,
        DelegateMethodDescription.fromDelegateMethodDescription(
                LAYOUT_SPEC_DELEGATE_METHODS_MAP.get(OnCreateLayout.class))
            .optionalParameters(
                ImmutableList.of(
                    MethodParamModelFactory.createSimpleMethodParamModel(
                        TypeName.INT.box(), "matched", new Object()),
                    MethodParamModelFactory.createSimpleMethodParamModel(
                        TypeName.CHAR, "unmatched", new Object())))
            .build());

    when(mLayoutSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                new SpecMethodModel<DelegateMethod, Void>(
                    ImmutableList.of((Annotation) () -> OnCreateLayout.class),
                    ImmutableList.of(Modifier.STATIC),
                    "name",
                    ClassNames.COMPONENT,
                    ImmutableList.of(),
                    ImmutableList.of(
                        MockMethodParamModel.newBuilder()
                            .type(ClassNames.COMPONENT_CONTEXT)
                            .representedObject(mMethodParamObject1)
                            .build(),
                        MethodParamModelFactory.createSimpleMethodParamModel(
                            TypeName.INT.box(), "matched", mMethodParamObject2),
                        MethodParamModelFactory.createSimpleMethodParamModel(
                            TypeName.OBJECT, "unmatched", mMethodParamObject3)),
                    mDelegateMethodObject1,
                    null)));

    final List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateMethods(mLayoutSpecModel, map);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject3);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Not a valid parameter, should be one of the following: @Prop T somePropName. @TreeProp T someTreePropName. @State T someStateName. @InjectProp T someInjectPropName. Or one of the following, where no annotations should be added to the parameter: java.lang.Integer matched. char unmatched. ");
  }
}
