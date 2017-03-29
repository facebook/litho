/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.specmodels.model;

import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;
import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.components.testing.specmodels.TestMethodParamModel;
import com.facebook.testing.robolectric.v3.WithTestDefaultsRunner;

import com.squareup.javapoet.TypeName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TreePropValidation}
 */
@RunWith(WithTestDefaultsRunner.class)
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
                  public String name() {
                    return "";
                  }

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
    assertThat(validationErrors).hasSize(3);
    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodObject);
    assertThat(validationErrors.get(0).message).isEqualTo(
        "@OnCreateTreeProp must define a valid name.");

    assertThat(validationErrors.get(1).element).isEqualTo(mDelegateMethodObject);
    assertThat(validationErrors.get(1).message).isEqualTo(
        "@OnCreateTreeProp methods cannot return void.");

    assertThat(validationErrors.get(2).element).isEqualTo(mDelegateMethodObject);
    assertThat(validationErrors.get(2).message).isEqualTo(
        "The first argument of an @OnCreateTreeProp method should be " +
            "com.facebook.components.ComponentContext.");
  }
}
