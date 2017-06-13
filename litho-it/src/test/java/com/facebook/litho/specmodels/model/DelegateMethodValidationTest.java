/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

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
import com.facebook.litho.testing.specmodels.TestMethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.lang.model.element.Modifier;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
  private DelegateMethodModel mOnCreateMountContent;
  private final Object mOnCreateMountContentObject = new Object();

  @Before
  public void setup() {
    when(mLayoutSpecModel.getRepresentedObject()).thenReturn(mModelRepresentedObject);
    when(mMountSpecModel.getRepresentedObject()).thenReturn(mMountSpecObject);

    mOnCreateMountContent = new DelegateMethodModel(
        ImmutableList.<Annotation>of(new Annotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return OnCreateMountContent.class;
          }
        }),
        ImmutableList.of(Modifier.STATIC),
        "onCreateMountContent",
        ClassName.bestGuess("java.lang.MadeUpClass"),
        ImmutableList.<MethodParamModel>of(
            TestMethodParamModel.newBuilder()
                .name("c")
                .type(ClassNames.COMPONENT_CONTEXT)
                .representedObject(new Object())
                .build()),
        mOnCreateMountContentObject);
  }

  @Test
  public void testNoDelegateMethods() {
    when(mLayoutSpecModel.getDelegateMethods()).thenReturn(ImmutableList.<DelegateMethodModel>of());

    List<SpecModelValidationError> validationErrors =
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
    when(mLayoutSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.of(
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnCreateLayout.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "name",
                ClassNames.COMPONENT_LAYOUT,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .name("c")
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(new Object())
                        .build()),
                new Object()),
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnCreateLayoutWithSizeSpec.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "name",
                ClassNames.COMPONENT_LAYOUT,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .name("c")
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(new Object())
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .name("widthSpec")
                        .type(TypeName.INT)
                        .representedObject(new Object())
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .name("heightSpec")
                        .type(TypeName.INT)
                        .representedObject(new Object())
                        .build()),
                new Object())));

    List<SpecModelValidationError> validationErrors =
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
    when(mLayoutSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.<DelegateMethodModel>of(
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnCreateLayoutWithSizeSpec.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "name",
                ClassNames.COMPONENT_LAYOUT,
                ImmutableList.<MethodParamModel>of(),
                mDelegateMethodObject1)));

    List<SpecModelValidationError> validationErrors =
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
    when(mLayoutSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.<DelegateMethodModel>of(
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnCreateLayout.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "name",
                ClassNames.COMPONENT_LAYOUT,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .type(TypeName.BOOLEAN)
                        .representedObject(mMethodParamObject1)
                        .build()),
                mDelegateMethodObject1)));

    List<SpecModelValidationError> validationErrors =
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
    when(mLayoutSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.<DelegateMethodModel>of(
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnCreateLayout.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "name",
                ClassNames.COMPONENT_LAYOUT,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject1)
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .type(TypeName.INT)
                        .representedObject(mMethodParamObject2)
                        .build()),
                mDelegateMethodObject1)));

    List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject2);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Not a valid parameter, should be one of the following: @Prop T somePropName. " +
            "@TreeProp T someTreePropName. @State T someStateName. ");
  }

  @Test
  public void testDelegateMethodIsNotStatic() {
    when(mLayoutSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.of(
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnCreateLayout.class;
                  }
                }),
                ImmutableList.<Modifier>of(),
                "name",
                ClassNames.COMPONENT_LAYOUT,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject1)
                        .build()),
                mDelegateMethodObject1)));

    List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateLayoutSpecModel(mLayoutSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodObject1);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "Methods in a spec that doesn't have dependency injection must be static.");
  }

  @Test
  public void testMountSpecHasOnCreateMountContent() {
    when(mMountSpecModel.getDelegateMethods()).thenReturn(ImmutableList.<DelegateMethodModel>of());

    List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateMountSpecModel(mMountSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMountSpecObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "All MountSpecs need to have a method annotated with @OnCreateMountContent.");
  }

  @Test
  public void testSecondParameter() {
    when(mMountSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.of(
            mOnCreateMountContent,
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnMount.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "onMount",
                TypeName.VOID,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject1)
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.OBJECT)
                        .representedObject(mMethodParamObject2)
                        .build()),
                mDelegateMethodObject1),
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnBind.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "onBind",
                TypeName.VOID,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject1)
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.OBJECT)
                        .representedObject(mMethodParamObject2)
                        .build()),
                mDelegateMethodObject2)));

    List<SpecModelValidationError> validationErrors =
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
    InterStageInputParamModel interStageInputParamModel = mock(InterStageInputParamModel.class);
    when(interStageInputParamModel.getAnnotations()).thenReturn(
        ImmutableList.<Annotation>of(new Annotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return FromBind.class;
          }
        }));
    when(interStageInputParamModel.getRepresentedObject()).thenReturn(mMethodParamObject3);
    when(mMountSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.of(
            mOnCreateMountContent,
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnUnmount.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "onUnmount",
                TypeName.VOID,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject1)
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .type(ClassName.bestGuess("java.lang.MadeUpClass"))
                        .representedObject(mMethodParamObject2)
                        .build(),
                    interStageInputParamModel),
                mDelegateMethodObject1)));

    List<SpecModelValidationError> validationErrors =
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
    InterStageInputParamModel interStageInputParamModel = mock(InterStageInputParamModel.class);
    when(interStageInputParamModel.getAnnotations()).thenReturn(
        ImmutableList.<Annotation>of(new Annotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return FromPrepare.class;
          }
        }));
    when(interStageInputParamModel.getName()).thenReturn("interStageInput");
    when(interStageInputParamModel.getType()).thenReturn(TypeName.INT);
    when(interStageInputParamModel.getRepresentedObject()).thenReturn(mMethodParamObject3);
    when(mMountSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.of(
            mOnCreateMountContent,
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnMount.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "onMount",
                TypeName.VOID,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject1)
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .type(ClassName.bestGuess("java.lang.MadeUpClass"))
                        .representedObject(mMethodParamObject2)
                        .build(),
                    interStageInputParamModel),
                mDelegateMethodObject1)));

    List<SpecModelValidationError> validationErrors =
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
    InterStageInputParamModel interStageInputParamModel = mock(InterStageInputParamModel.class);
    when(interStageInputParamModel.getAnnotations()).thenReturn(
        ImmutableList.<Annotation>of(new Annotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return FromPrepare.class;
          }
        }));
    when(interStageInputParamModel.getName()).thenReturn("interStageInput");
    when(interStageInputParamModel.getType()).thenReturn(TypeName.INT);
    when(interStageInputParamModel.getRepresentedObject()).thenReturn(mMethodParamObject3);
    when(mMountSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.of(
            mOnCreateMountContent,
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnMount.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "onMount",
                TypeName.VOID,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .name("c")
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject1)
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .name("param")
                        .type(ClassName.bestGuess("java.lang.MadeUpClass"))
                        .representedObject(mMethodParamObject2)
                        .build(),
                    interStageInputParamModel),
                mDelegateMethodObject1),
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnPrepare.class;
                  }
                }),
                ImmutableList.of(Modifier.STATIC),
                "onPrepare",
                TypeName.VOID,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .name("c")
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject1)
                        .build()),
                mDelegateMethodObject2)));

    List<SpecModelValidationError> validationErrors =
        DelegateMethodValidation.validateMountSpecModel(mMountSpecModel);
    for (SpecModelValidationError validationError : validationErrors) {
      System.out.println(validationError.message);
    }
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject3);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "To use interface com.facebook.litho.annotations.FromPrepare on param interStageInput " +
            "your method annotated with interface com.facebook.litho.annotations.OnPrepare must " +
            "have a param Output<java.lang.Integer> interStageInput");
  }
}
