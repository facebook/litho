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
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

public class PureRenderValidationTest {
  interface PureRenderSpecModel extends SpecModel, HasPureRender {}

  private final Object mDelegateMethodRepresentedObject1 = new Object();
  private PureRenderSpecModel mSpecModel = mock(PureRenderSpecModel.class);

  @Before
  public void setup() {
    SpecMethodModel<DelegateMethod, Void> delegateMethod =
        new SpecMethodModel<DelegateMethod, Void>(
            ImmutableList.<Annotation>of(
                new Annotation() {
                  @Override
                  public Class<? extends Annotation> annotationType() {
                    return ShouldUpdate.class;
                  }
                }),
            ImmutableList.<Modifier>of(),
            "method",
            TypeName.BOOLEAN,
            ImmutableList.of(),
            ImmutableList.<MethodParamModel>of(),
            mDelegateMethodRepresentedObject1,
            null);
    when(mSpecModel.getDelegateMethods()).thenReturn(ImmutableList.of(delegateMethod));
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
}
