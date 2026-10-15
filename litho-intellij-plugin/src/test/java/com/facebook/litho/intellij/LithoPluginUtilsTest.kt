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

package com.facebook.litho.intellij

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class LithoPluginUtilsTest {

  @Test
  fun `isComponentClass`() {
    val component = createSubclassOf("com.facebook.litho.Component")
    assertThat(LithoPluginUtils.isComponentClass(component)).isTrue()
    assertThat(LithoPluginUtils.isGeneratedClass(component)).isTrue()

    val notComponent = createSubclassOf("com.facebook.litho.Column")
    assertThat(LithoPluginUtils.isComponentClass(notComponent)).isFalse()
    assertThat(LithoPluginUtils.isGeneratedClass(notComponent)).isFalse()
  }

  @Test
  fun `isSectionClass`() {
    val section = createSubclassOf("com.facebook.litho.sections.Section")
    assertThat(LithoPluginUtils.isSectionClass(section)).isTrue()
    assertThat(LithoPluginUtils.isGeneratedClass(section)).isTrue()

    val notSection = createSubclassOf("com.facebook.litho.sections.SectionTest")
    assertThat(LithoPluginUtils.isSectionClass(notSection)).isFalse()
    assertThat(LithoPluginUtils.isGeneratedClass(notSection)).isFalse()
  }

  @Test
  fun `hasAnnotationStartingWith`() {
    val prefix = "abc"
    val withPrefix = createWithAnnotation<PsiClass>(prefix + "AnyEnding")
    assertThat(LithoPluginUtils.hasAnnotation(withPrefix, LithoPluginUtils.startsWith(prefix)))
        .isTrue()

    val withoutPrefix = createWithAnnotation<PsiClass>("AnyEnding")
    assertThat(LithoPluginUtils.hasAnnotation(withoutPrefix, LithoPluginUtils.startsWith(prefix)))
        .isFalse()
  }

  @Test
  fun `hasLithoComponentSpecAnnotation`() {
    val withLitho = createSpecWithAnnotation("com.facebook.litho.annotations.MountSpec")
    assertThat(LithoPluginUtils.hasLithoComponentSpecAnnotation(withLitho)).isTrue()

    val withoutLitho = createSpecWithAnnotation("com.facebook.litho.sections.annotations.Any")
    assertThat(LithoPluginUtils.hasLithoComponentSpecAnnotation(withoutLitho)).isFalse()

    val notSpec = createWithAnnotation<PsiClass>("com.facebook.litho.annotations.Any")
    assertThat(LithoPluginUtils.hasLithoComponentSpecAnnotation(notSpec)).isFalse()
    assertThat(LithoPluginUtils.hasLithoComponentSpecAnnotation(null)).isFalse()
  }

  @Test
  fun `hasLithoSectionSpecAnnotation`() {
    val withLithoSection = createSpecWithAnnotation("com.facebook.litho.sections.annotations.Any")
    assertThat(LithoPluginUtils.hasLithoSectionSpecAnnotation(withLithoSection)).isTrue()

    val withoutLithoSection = createSpecWithAnnotation("com.facebook.litho.annotations.Any")
    assertThat(LithoPluginUtils.hasLithoSectionSpecAnnotation(withoutLithoSection)).isFalse()

    val notSpec = createWithAnnotation<PsiClass>("com.facebook.litho.sections.annotations.Any")
    assertThat(LithoPluginUtils.hasLithoSectionSpecAnnotation(notSpec)).isFalse()
    assertThat(LithoPluginUtils.hasLithoSectionSpecAnnotation(null)).isFalse()
  }

  @Test
  fun `isSpecName`() {
    assertThat(LithoPluginUtils.isSpecName("AnySpec")).isTrue()
    assertThat(LithoPluginUtils.isSpecName("my.domain.TestSpec")).isTrue()
    assertThat(LithoPluginUtils.isSpecName("Any")).isFalse()
    assertThat(LithoPluginUtils.isSpecName("")).isFalse()
  }

  @Test
  fun `isProp`() {
    val prop = createWithAnnotation<PsiParameter>("com.facebook.litho.annotations.Prop")
    assertThat(LithoPluginUtils.isProp(prop)).isTrue()

    val notProp = createWithAnnotation<PsiParameter>("com.facebook.litho.annotations.PropDefault")
    assertThat(LithoPluginUtils.isProp(notProp)).isFalse()

    val notProp2 = createWithAnnotation<PsiParameter>("any.Prop")
    assertThat(LithoPluginUtils.isProp(notProp2)).isFalse()
  }

  @Test
  fun `isState`() {
    val state = createWithAnnotation<PsiParameter>("com.facebook.litho.annotations.State")
    assertThat(LithoPluginUtils.isState(state)).isTrue()

    val notState = createWithAnnotation<PsiParameter>("com.facebook.litho.annotations.StateAny")
    assertThat(LithoPluginUtils.isState(notState)).isFalse()

    val notState2 = createWithAnnotation<PsiParameter>("any.State")
    assertThat(LithoPluginUtils.isState(notState2)).isFalse()
  }

  @Test
  fun `isEvent`() {
    val event = createWithAnnotation<PsiClass>("com.facebook.litho.annotations.Event")
    assertThat(LithoPluginUtils.isEvent(event)).isTrue()

    val notEvent = createWithAnnotation<PsiClass>("com.facebook.litho.annotations.EventAny")
    assertThat(LithoPluginUtils.isEvent(notEvent)).isFalse()

    val notEvent2 = createWithAnnotation<PsiClass>("any.Event")
    assertThat(LithoPluginUtils.isEvent(notEvent2)).isFalse()
  }

  @Test
  fun `getLithoComponentNameFromSpec`() {
    assertThat(LithoPluginUtils.getLithoComponentNameFromSpec("TestSpec")).isEqualTo("Test")
  }

  @Test
  fun `getLithoComponentSpecNameFromComponent`() {
    assertThat(LithoPluginUtils.getLithoComponentSpecNameFromComponent("Name"))
        .isEqualTo("NameSpec")
  }

  companion object {
    private fun createSubclassOf(superClass: String): PsiClass {
      val componentSubclass = mock<PsiClass>()
      val componentClass = mock<PsiClass>()
      whenever(componentClass.qualifiedName).thenReturn(superClass)
      whenever(componentSubclass.superClass).thenReturn(componentClass)
      return componentSubclass
    }

    private inline fun <reified T : PsiModifierListOwner> createWithAnnotation(
        annotationName: String,
    ): T {
      val withAnnotation = mock<T>()
      val annotation = createPsiAnnotation(annotationName)
      whenever(withAnnotation.annotations).thenReturn(arrayOf(annotation))
      return withAnnotation
    }

    private fun createPsiAnnotation(annotationName: String): PsiAnnotation {
      val annotation = mock<PsiAnnotation>()
      whenever(annotation.qualifiedName).thenReturn(annotationName)
      return annotation
    }

    private fun createSpecWithAnnotation(annotationName: String): PsiClass {
      val withAnnotation = createWithAnnotation<PsiClass>(annotationName)
      whenever(withAnnotation.name).thenReturn("AnySpec")
      return withAnnotation
    }
  }
}
