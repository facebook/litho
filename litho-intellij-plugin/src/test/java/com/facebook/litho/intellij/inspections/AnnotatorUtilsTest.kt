/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.intellij.inspections

import com.facebook.litho.specmodels.model.SpecModelValidationError
import com.intellij.psi.PsiElement
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class AnnotatorUtilsTest {

  @Test
  fun `addError`() {
    val message = "test message"
    val element = mock<PsiElement>()
    val holder = TestHolder()
    AnnotatorUtils.addError(holder, SpecModelValidationError(element, message))

    assertThat(holder.errorMessages.size).isEqualTo(1)
    assertThat(holder.errorMessages[0]).isEqualTo(message)
    assertThat(holder.errorElements.size).isEqualTo(1)
    assertThat(holder.errorElements[0]).isEqualTo(element)
  }
}
