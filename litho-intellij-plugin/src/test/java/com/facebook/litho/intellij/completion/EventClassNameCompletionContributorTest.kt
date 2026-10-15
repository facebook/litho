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

package com.facebook.litho.intellij.completion

import com.facebook.litho.annotations.Event
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EventClassNameCompletionContributorTest {

  @Test
  fun `apply with Litho event returns class lookup with event type text`() {
    val insertedLookupStrings = mutableListOf<String>()
    val filter =
        EventClassNameCompletionContributor.EventClassFilter(
            InsertHandler { _, item -> insertedLookupStrings.add(item.lookupString) },
        )

    val lookupElement =
        filter.apply(LookupElementBuilder.create("ClickEvent"), eventClass("ClickEvent"))

    assertThat(lookupElement).isNotNull
    assertThat(lookupElement?.lookupString).isEqualTo("ClickEvent.class")

    val presentation = LookupElementPresentation()
    lookupElement?.renderElement(presentation)
    assertThat(presentation.typeText).isEqualTo("Litho Event")

    lookupElement?.handleInsert(mock<InsertionContext>())
    assertThat(insertedLookupStrings).containsExactly("ClickEvent.class")
  }

  @Test
  fun `apply with non event class filters lookup out`() {
    val filter =
        EventClassNameCompletionContributor.EventClassFilter(
            InsertHandler { _, _ -> error("Filtered elements must not install insert handlers") },
        )

    val lookupElement =
        filter.apply(
            LookupElementBuilder.create("PlainClass"),
            psiClass("PlainClass", "example.Annotation"),
        )

    assertThat(lookupElement).isNull()
  }

  @Test
  fun `apply with unnamed event class filters lookup out`() {
    val filter =
        EventClassNameCompletionContributor.EventClassFilter(
            InsertHandler { _, _ -> error("Filtered elements must not install insert handlers") },
        )

    val lookupElement =
        filter.apply(LookupElementBuilder.create("AnonymousEvent"), eventClass(null))

    assertThat(lookupElement).isNull()
  }

  companion object {
    private fun eventClass(name: String?): PsiClass {
      return psiClass(name, Event::class.java.name)
    }

    private fun psiClass(
        name: String?,
        annotationName: String,
    ): PsiClass {
      val psiClass = mock<PsiClass>()
      val annotation = psiAnnotation(annotationName)
      whenever(psiClass.name).thenReturn(name)
      whenever(psiClass.annotations).thenReturn(arrayOf(annotation))
      return psiClass
    }

    private fun psiAnnotation(annotationName: String): PsiAnnotation {
      val annotation = mock<PsiAnnotation>()
      whenever(annotation.qualifiedName).thenReturn(annotationName)
      return annotation
    }
  }
}
