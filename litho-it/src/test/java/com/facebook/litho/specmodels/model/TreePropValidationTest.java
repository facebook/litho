/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;
import java.util.List;

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.TestMethodParamModel;

import com.squareup.javapoet.TypeName;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TreePropValidation}
 */
public class TreePropValidationTest {
  private final SpecModel mSpecModel = mock(LayoutSpecModel.class);
  private final Object mModelRepresentedObject = new Object();
  private final Object mDelegateMethodObject = new Object();
  private final Object mMethodParamObject1 = new Object();
  private final Object mMethodParamObject2 = new Object();

  @Before
  public void setup() {
    when(mSpecModel.getRepresentedObject()).thenReturn(mModelRepresentedObject);
    when(mSpecModel.getComponentClass()).thenReturn(ClassNames.COMPONENT_CONTEXT);
  }

  @Test
  public void testOnCreateTreePropMethod() {
    when(mSpecModel.getDelegateMethods()).thenReturn(
        ImmutableList.<DelegateMethodModel>of(
            new DelegateMethodModel(
                ImmutableList.<Annotation>of(new OnCreateTreeProp() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return OnCreateTreeProp.class;
                  }
                }),
                ImmutableList.<Modifier>of(),
                "",
                TypeName.VOID,
                ImmutableList.<MethodParamModel>of(
                    TestMethodParamModel.newBuilder()
                        .type(TypeName.INT)
                        .representedObject(mMethodParamObject1)
                        .build(),
                    TestMethodParamModel.newBuilder()
                        .type(ClassNames.COMPONENT_CONTEXT)
                        .representedObject(mMethodParamObject2)
                        .build()),
                mDelegateMethodObject)));

    List<SpecModelValidationError> validationErrors = TreePropValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(2);

    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "@OnCreateTreeProp methods cannot return void.");

    assertThat(validationErrors.get(1).element).isEqualTo(mDelegateMethodObject);
    assertThat(validationErrors.get(1).message).isEqualTo(
        "The first argument of an @OnCreateTreeProp method should be " +
            "com.facebook.litho.ComponentContext.");
  }
}
