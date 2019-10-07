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

package com.facebook.litho.intellij.inspections;

import static org.junit.Assert.assertEquals;

import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.intellij.psi.PsiElement;
import org.junit.Test;
import org.mockito.Mockito;

public class AnnotatorUtilsTest {

  @Test
  public void addError() {
    String message = "test message";
    PsiElement element = Mockito.mock(PsiElement.class);
    TestHolder holder = new TestHolder();
    AnnotatorUtils.addError(holder, new SpecModelValidationError(element, message));

    assertEquals(1, holder.errorMessages.size());
    assertEquals(message, holder.errorMessages.get(0));
    assertEquals(1, holder.errorElements.size());
    assertEquals(element, holder.errorElements.get(0));
  }
}
