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
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.Event
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.Reason
import com.facebook.litho.specmodels.internal.RunMode
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory
import com.google.testing.compile.CompilationRule
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class EventHandlerLambdaFactoryTest {

  @Rule @JvmField val compilationRule = CompilationRule()

  private val layoutSpecModelFactory = LayoutSpecModelFactory()
  private val elements
    get() = compilationRule.elements

  private val types
    get() = compilationRule.types

  @Test
  fun `when @OnEvent is present then generate lambda factory methods`() {
    val typeElement = elements.getTypeElement(TestLayoutSpec::class.java.canonicalName)
    val specModel =
        layoutSpecModelFactory.create(
            elements,
            types,
            typeElement,
            mock(),
            RunMode.normal(),
            null,
        )
    val holder = EventGenerator.generateEventHandlerFactories(specModel)

    Assertions.assertThat(holder.methodSpecs[1].toString())
        .isEqualTo(
            """
            static kotlin.jvm.functions.Function1<com.facebook.litho.ClickEvent, kotlin.Unit> onClickAsLambda(
                final com.facebook.litho.ComponentContext c,
                @org.jetbrains.annotations.NotNull final java.lang.String something) {
              final TestLayout _ref = (TestLayout) c.getComponentScope();
              return new kotlin.jvm.functions.Function1<com.facebook.litho.ClickEvent, kotlin.Unit>() {
                @Override
                public kotlin.Unit invoke(com.facebook.litho.ClickEvent e) {
                  TestLayoutSpec.INSTANCE.onClick(
                    c,
                    (android.view.View) _ref.view,
                    (java.lang.String) _ref.name,
                    (int) _ref.age,
                    something);
                  return kotlin.Unit.INSTANCE;
                }
              };
            }
            
            """
                .trimIndent())
  }

  @Test
  fun `when @OnEvent with return is present then generate lambda factory methods`() {
    val typeElement = elements.getTypeElement(TestLayoutSpec::class.java.canonicalName)
    val specModel =
        layoutSpecModelFactory.create(
            elements,
            types,
            typeElement,
            mock(),
            RunMode.normal(),
            null,
        )
    val holder = EventGenerator.generateEventHandlerFactories(specModel)

    Assertions.assertThat(holder.methodSpecs[3].toString())
        .isEqualTo(
            """
            static kotlin.jvm.functions.Function1<com.facebook.litho.specmodels.generator.EventHandlerLambdaFactoryTest.StringEvent, java.lang.String> getStringAsLambda(
                final com.facebook.litho.ComponentContext c,
                @org.jetbrains.annotations.NotNull final java.lang.String something) {
              final TestLayout _ref = (TestLayout) c.getComponentScope();
              return new kotlin.jvm.functions.Function1<com.facebook.litho.specmodels.generator.EventHandlerLambdaFactoryTest.StringEvent, java.lang.String>() {
                @Override
                public java.lang.String invoke(com.facebook.litho.specmodels.generator.EventHandlerLambdaFactoryTest.StringEvent e) {
                  java.lang.String _result = (java.lang.String) TestLayoutSpec.INSTANCE.getString(
                    c,
                    (java.lang.String) _ref.name,
                    (int) _ref.age,
                    something);
                  return _result;
                }
              };
            }
            
            """
                .trimIndent())
  }

  @ExcuseMySpec(reason = Reason.LEGACY)
  @LayoutSpec
  private object TestLayoutSpec {
    @OnCreateLayout
    fun onCreateLayout(
        c: ComponentContext,
        @Prop name: String,
        @Prop age: Int,
        @Prop id: Long
    ): Component {
      return Column.create(c).build()
    }

    @OnEvent(ClickEvent::class)
    fun onClick(
        c: ComponentContext,
        @FromEvent view: View,
        @Prop name: String,
        @Prop age: Int,
        @Param something: String,
    ) {
      // no-op
    }

    @OnEvent(StringEvent::class)
    fun getString(
        c: ComponentContext,
        @Prop name: String,
        @Prop age: Int,
        @Param something: String,
    ): String {
      return ""
    }
  }

  @Event(returnType = String::class) private class StringEvent
}
