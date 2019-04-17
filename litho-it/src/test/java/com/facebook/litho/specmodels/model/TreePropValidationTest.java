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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests {@link TreePropValidation}
 */
@RunWith(JUnit4.class)
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
    when(mSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.<SpecMethodModel<DelegateMethod, Void>>of(
                SpecMethodModel.<DelegateMethod, Void>builder()
                    .annotations(
                        ImmutableList.<Annotation>of(
                            new OnCreateTreeProp() {
                              @Override
                              public Class<? extends Annotation> annotationType() {
                                return OnCreateTreeProp.class;
                              }
                            }))
                    .modifiers(ImmutableList.<Modifier>of())
                    .name("")
                    .returnTypeSpec(new TypeSpec(TypeName.VOID))
                    .methodParams(
                        ImmutableList.<MethodParamModel>of(
                            MockMethodParamModel.newBuilder()
                                .type(TypeName.INT)
                                .representedObject(mMethodParamObject1)
                                .build(),
                            MockMethodParamModel.newBuilder()
                                .type(ClassNames.COMPONENT_CONTEXT)
                                .representedObject(mMethodParamObject2)
                                .build()))
                    .representedObject(mDelegateMethodObject)
                    .build()));

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
