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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PureRenderValidationTest {
  interface PureRenderSpecModel extends SpecModel, HasPureRender {}

  private final Object mDelegateMethodRepresentedObject1 = new Object();
  private PureRenderSpecModel mSpecModel = mock(PureRenderSpecModel.class);

  @Before
  public void setup() {
    SpecMethodModel<DelegateMethod, Void> delegateMethod =
        SpecMethodModel.<DelegateMethod, Void>builder()
            .annotations(
                ImmutableList.<Annotation>of(
                    new Annotation() {
                      @Override
                      public Class<? extends Annotation> annotationType() {
                        return ShouldUpdate.class;
                      }
                    }))
            .modifiers(ImmutableList.<Modifier>of())
            .name("method")
            .returnTypeSpec(new TypeSpec(TypeName.BOOLEAN))
            .typeVariables(ImmutableList.of())
            .methodParams(ImmutableList.<MethodParamModel>of())
            .representedObject(mDelegateMethodRepresentedObject1)
            .build();
    when(mSpecModel.getDelegateMethods()).thenReturn(ImmutableList.of(delegateMethod));
  }

  @Test
  public void testShouldUpdateDefinedButNotPureRender() {
    when(mSpecModel.isPureRender()).thenReturn(false);

    List<SpecModelValidationError> validationErrors = PureRenderValidation.validate(mSpecModel);
    assertThat(validationErrors).hasSize(1);
    assertThat(validationErrors.get(0).element).isEqualTo(mDelegateMethodRepresentedObject1);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Specs defining a method annotated with @ShouldUpdate should also set "
                + "isPureRender = true in the top-level spec annotation.");
  }
}
