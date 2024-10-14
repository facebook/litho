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

@file:Suppress("DEPRECATION")

package com.facebook.litho.specmodels.generator

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.Reason
import com.facebook.litho.specmodels.internal.RunMode
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory
import com.facebook.litho.specmodels.processor.MountSpecModelFactory
import com.google.testing.compile.CompilationRule
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class ComponentBodyGeneratorTest {

  @Rule @JvmField val compilationRule = CompilationRule()

  private val layoutSpecModelFactory = LayoutSpecModelFactory()
  private val mountSpecModelFactory = MountSpecModelFactory()
  private val elements
    get() = compilationRule.elements

  private val types
    get() = compilationRule.types

  @Test
  fun generate_forLayoutSpec_generatesGetPropsMethod() {
    val typeElement = elements.getTypeElement(TestLayoutSpec::class.java.canonicalName)
    val specModel =
        layoutSpecModelFactory.create(elements, types, typeElement, mock(), RunMode.normal(), null)
    val typeSpecDataHolder = ComponentBodyGenerator.generate(specModel, null, RunMode.normal())
    val getPropsSpec = typeSpecDataHolder.methodSpecs.find { it.name == "getProps" }
    Assertions.assertThat(getPropsSpec).isNotNull()
  }

  @Test
  fun generate_forMountSpec_doesNotGenerateGetPropsMethod() {
    val typeElement = elements.getTypeElement(TestMountSpec::class.java.canonicalName)
    val specModel =
        mountSpecModelFactory.create(elements, types, typeElement, mock(), RunMode.normal(), null)
    val typeSpecDataHolder = ComponentBodyGenerator.generate(specModel, null, RunMode.normal())
    val getPropsSpec = typeSpecDataHolder.methodSpecs.find { it.name == "getProps" }
    Assertions.assertThat(getPropsSpec).isNull()
  }

  @Test
  fun generateGetPropsMethod_hasProps_generatesArrayOfProps() {
    val typeElement = elements.getTypeElement(TestLayoutSpec::class.java.canonicalName)
    val specModel =
        layoutSpecModelFactory.create(elements, types, typeElement, mock(), RunMode.normal(), null)
    val getPropsSpec = ComponentBodyGenerator.generateGetPropsMethod(specModel)
    Assertions.assertThat(getPropsSpec).isNotNull()
    Assertions.assertThat(getPropsSpec.toString())
        .isEqualTo(
            """
            @java.lang.Override
            protected final java.lang.Object[] getProps() {
              return new java.lang.Object[] {age, id, name};
            }
            
            """
                .trimIndent())
  }

  @Test
  fun generateGetPropsMethod_emptyProps_returnsEmptyArray() {
    val typeElement = elements.getTypeElement(EmptyTestLayoutSpec::class.java.canonicalName)
    val specModel =
        layoutSpecModelFactory.create(elements, types, typeElement, mock(), RunMode.normal(), null)
    val getPropsSpec = ComponentBodyGenerator.generateGetPropsMethod(specModel)
    Assertions.assertThat(getPropsSpec).isNotNull()
    Assertions.assertThat(getPropsSpec.toString())
        .isEqualTo(
            """
            @java.lang.Override
            protected final java.lang.Object[] getProps() {
              return com.facebook.litho.Component.EMPTY_ARRAY;
            }
            
            """
                .trimIndent())
  }
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
}

@ExcuseMySpec(reason = Reason.LEGACY)
@LayoutSpec
private object EmptyTestLayoutSpec {
  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component {
    return Column.create(c).build()
  }
}

@ExcuseMySpec(reason = Reason.LEGACY)
@MountSpec
private object TestMountSpec {

  @OnCreateMountContent
  fun onCreateMountContent(): Content {
    return Content()
  }

  @OnMount
  fun onMount(c: ComponentContext?, content: Content, @Prop prop: String) {
    content.prop = prop
  }
}

private class Content {
  var prop: String? = null
}
