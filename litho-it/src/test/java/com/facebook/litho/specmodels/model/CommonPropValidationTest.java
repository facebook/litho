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

import com.facebook.litho.annotations.CommonProp;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.testing.specmodels.MockMethodParamModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.junit.Before;
import org.junit.Test;

/** Tests {@link DelegateMethodValidation} */
public class CommonPropValidationTest {
  private final SpecModel mSpecModel = mock(SpecModel.class);
  private final Object mModelRepresentedObject = new Object();
  private final Object mMethodParamObject1 = new Object();
  private final Object mMethodParamObject2 = new Object();
  private final Object mMethodParamObject3 = new Object();
  private final Object mMethodParamObject4 = new Object();

  @Before
  public void setup() {
    when(mSpecModel.getRepresentedObject()).thenReturn(mModelRepresentedObject);
  }

  @Test
  public void testCommonPropNotValid() {
    when(mSpecModel.getDelegateMethods())
        .thenReturn(
            ImmutableList.of(
                SpecMethodModel.<DelegateMethod, Void>builder()
                    .annotations(ImmutableList.of((Annotation) () -> OnCreateLayout.class))
                    .modifiers(ImmutableList.of(Modifier.STATIC))
                    .name("name")
                    .returnTypeSpec(new TypeSpec(ClassNames.COMPONENT))
                    .typeVariables(ImmutableList.of())
                    .methodParams(
                        ImmutableList.of(
                            MockMethodParamModel.newBuilder()
                                .name("c")
                                .type(ClassNames.COMPONENT_CONTEXT)
                                .representedObject(mMethodParamObject1)
                                .build(),
                            MockMethodParamModel.newBuilder()
                                .name("contentDescription")
                                .annotations(CommonProp.class)
                                .type(ClassName.bestGuess("java.lang.CharSequence"))
                                .representedObject(mMethodParamObject2)
                                .build(),
                            MockMethodParamModel.newBuilder()
                                .name("rubbish")
                                .annotations(CommonProp.class)
                                .type(TypeName.INT)
                                .representedObject(mMethodParamObject3)
                                .build(),
                            MockMethodParamModel.newBuilder()
                                .name("focusable")
                                .annotations(CommonProp.class)
                                .type(TypeName.OBJECT)
                                .representedObject(mMethodParamObject4)
                                .build()))
                    .representedObject(new Object())
                    .typeModel(null)
                    .build()));

    final List<SpecModelValidationError> validationErrors =
        CommonPropValidation.validate(mSpecModel, CommonPropValidation.VALID_COMMON_PROPS);
    assertThat(validationErrors).hasSize(2);
    assertThat(validationErrors.get(0).element).isEqualTo(mMethodParamObject3);
    assertThat(validationErrors.get(0).message)
        .isEqualTo(
            "Common prop with name rubbish is incorrectly defined - see CommonPropValidation.java for a list of common props that may be used.");
    assertThat(validationErrors.get(1).element).isEqualTo(mMethodParamObject4);
    assertThat(validationErrors.get(1).message)
        .isEqualTo("A common prop with name focusable must have type of: boolean");
  }
}
