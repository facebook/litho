/*
 * Copyright 2018-present Facebook, Inc.
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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import java.util.Collection;
import javax.lang.model.element.Modifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests {@link FieldsValidation}. */
@RunWith(JUnit4.class)
public class FieldsValidationTest {

  private static final String FIELD_TEST_NAME = "fieldTestName";
  private final SpecModel layoutSpecModel = mock(LayoutSpecModel.class);
  private final Object representedObject = new Object();
  private final FieldModel fieldPrivateFinal =
      new FieldModel(
          FieldSpec.builder(TypeName.BOOLEAN, FIELD_TEST_NAME, Modifier.PRIVATE, Modifier.FINAL)
              .build(),
          representedObject);
  private final FieldModel fieldPrivateStatic =
      new FieldModel(
          FieldSpec.builder(TypeName.CHAR, FIELD_TEST_NAME, Modifier.PRIVATE, Modifier.STATIC)
              .build(),
          representedObject);
  private final FieldModel fieldPrivateStaticFinal =
      new FieldModel(
          FieldSpec.builder(
                  TypeName.LONG, FIELD_TEST_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
              .build(),
          representedObject);
  private final FieldModel fieldPrivate =
      new FieldModel(
          FieldSpec.builder(TypeName.INT, FIELD_TEST_NAME, Modifier.PRIVATE).build(),
          representedObject);

  @Test
  public void testNoFields() {
    verifyErrors(0);
  }

  @Test
  public void testNoStaticPresentFinal() {
    verifyErrors(1, fieldPrivateFinal, fieldPrivateStaticFinal);
  }

  @Test
  public void testPresentStaticNoFinal() {
    verifyErrors(1, fieldPrivateStatic, fieldPrivateStaticFinal);
  }

  @Test
  public void testPresentStaticFinal() {
    verifyErrors(0, fieldPrivateStaticFinal, fieldPrivateStaticFinal, fieldPrivateStaticFinal);
  }

  @Test
  public void testNoStaticNoFinal() {
    verifyErrors(3, fieldPrivateStatic, fieldPrivateFinal, fieldPrivate);
    verifyErrors(1, fieldPrivateFinal);
    verifyErrors(1, fieldPrivateStatic);
    verifyErrors(1, fieldPrivate);
  }

  private void verifyErrors(int errorNumber, FieldModel... fields) {
    when(layoutSpecModel.getFields()).thenReturn(ImmutableList.of(fields));

    Collection<SpecModelValidationError> validationErrors =
        FieldsValidation.validate(layoutSpecModel);

    assertThat(validationErrors).hasSize(errorNumber);

    for (SpecModelValidationError validationError : validationErrors) {
      assertThat(validationError.element).isSameAs(representedObject);
      assertThat(validationError.message)
          .isEqualTo(FIELD_TEST_NAME + " should be declared static and final.");
    }
  }
}
