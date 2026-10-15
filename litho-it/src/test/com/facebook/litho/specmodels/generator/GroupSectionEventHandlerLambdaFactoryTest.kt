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

package com.facebook.litho.specmodels.generator

import android.view.View
import com.facebook.litho.ClickEvent
import com.facebook.litho.annotations.Event
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.specmodels.processor.GroupSectionSpecModelFactory
import com.facebook.litho.specmodels.internal.RunMode
import com.google.testing.compile.CompilationRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class GroupSectionEventHandlerLambdaFactoryTest {

  @Rule @JvmField val compilationRule = CompilationRule()

  private val groupSectionSpecModelFactory = GroupSectionSpecModelFactory()
  private val elements
    get() = compilationRule.elements

  private val types
    get() = compilationRule.types

  @Test
  fun `when @OnEvent is present in GroupSectionSpec then generate lambda factory methods`() {
    val typeElement = elements.getTypeElement(TestGroupSectionSpec::class.java.canonicalName)
    val specModel =
        groupSectionSpecModelFactory.create(
            elements,
            types,
            typeElement,
            mock(),
            RunMode.normal(),
            null,
        )
    val holder = EventGenerator.generateEventHandlerFactories(specModel)

    assertThat(holder.methodSpecs[1].toString())
        .isEqualTo(
            """
            @java.lang.SuppressWarnings({"unchecked", "cast"})
            static kotlin.jvm.functions.Function1<com.facebook.litho.ClickEvent, kotlin.Unit> onClickEventAsLambda(
                final com.facebook.litho.sections.SectionContext c,
                @org.jetbrains.annotations.NotNull final java.lang.String something) {
              return new com.facebook.litho.EquivalentEventLambda<com.facebook.litho.ClickEvent, kotlin.Unit>(TestGroupSection.onClickEvent(c, something), true);
            }

            """
                .trimIndent(),
        )
  }

  @Test
  fun `when @OnEvent with return in GroupSectionSpec then generate lambda factory methods`() {
    val typeElement = elements.getTypeElement(TestGroupSectionSpec::class.java.canonicalName)
    val specModel =
        groupSectionSpecModelFactory.create(
            elements,
            types,
            typeElement,
            mock(),
            RunMode.normal(),
            null,
        )
    val holder = EventGenerator.generateEventHandlerFactories(specModel)

    assertThat(holder.methodSpecs[3].toString())
        .isEqualTo(
            """
            @java.lang.SuppressWarnings({"unchecked", "cast"})
            static kotlin.jvm.functions.Function1<com.facebook.litho.specmodels.generator.GroupSectionEventHandlerLambdaFactoryTest.StringEvent, java.lang.String> getStringAsLambda(
                final com.facebook.litho.sections.SectionContext c,
                @org.jetbrains.annotations.NotNull final java.lang.String something) {
              return new com.facebook.litho.EquivalentEventLambda<com.facebook.litho.specmodels.generator.GroupSectionEventHandlerLambdaFactoryTest.StringEvent, java.lang.String>(TestGroupSection.getString(c, something), false);
            }

            """
                .trimIndent(),
        )
  }

  @Suppress("DeprecatedSectionSpecApiInKotlin")
  @GroupSectionSpec
  private object TestGroupSectionSpec {
    @OnCreateChildren
    fun onCreateChildren(
        c: SectionContext,
        @Prop name: String,
        @Prop age: Int,
    ): Children {
      return Children.create().build()
    }

    @OnEvent(ClickEvent::class)
    fun onClickEvent(
        c: SectionContext,
        @Prop name: String,
        @Prop age: Int,
        @FromEvent view: View,
        @Param something: String,
    ) {
      // no-op
    }

    @OnEvent(StringEvent::class)
    fun getString(
        c: SectionContext,
        @Prop name: String,
        @Prop age: Int,
        @Param something: String,
    ): String {
      return ""
    }
  }

  @Event(returnType = String::class) private class StringEvent
}
