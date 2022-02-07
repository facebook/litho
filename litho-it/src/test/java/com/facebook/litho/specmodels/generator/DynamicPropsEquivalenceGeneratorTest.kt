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

import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.Prop
import com.facebook.litho.specmodels.internal.RunMode
import com.facebook.litho.specmodels.model.SpecModel
import com.facebook.litho.specmodels.processor.MountSpecModelFactory
import com.google.testing.compile.CompilationRule
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.MockitoAnnotations

@RunWith(JUnit4::class)
class DynamicPropsEquivalenceGeneratorTest {

  @Rule @JvmField val compilationRule = CompilationRule()

  private val elements: Elements
    get() = compilationRule.elements
  private val types: Types
    get() = compilationRule.types

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
  }

  @Test
  fun `test dynamic prop equivalency checks only compare refs`() {

    @MountSpec
    class MountTestSpec {
      @OnBind
      fun testDelegateMethod(
          @Prop(dynamic = true) intArg: Int,
          @Prop(dynamic = true) longArg: Long,
          @Prop(dynamic = true) floatArg: Float,
          @Prop(dynamic = true) doubleArg: Double,
          @Prop(dynamic = true) objectArg: Any?
      ) = Unit
    }

    val mountSpecModel: SpecModel =
        MountSpecModelFactory()
            .create(
                elements,
                types,
                elements.getTypeElement(MountTestSpec::class.java.canonicalName),
                null,
                RunMode.normal(),
                null,
                null)

    val generatedIsEquivalentMethod =
        ComponentBodyGenerator.generateIsEquivalentMethod(mountSpecModel, RunMode.normal())
            .toString()

    assertThat(generatedIsEquivalentMethod)
        .isEqualTo(
            """
            @java.lang.Override
            public boolean isEquivalentTo(com.facebook.litho.Component other) {
              if (this == other) {
                return true;
              }
              if (other == null || getClass() != other.getClass()) {
                return false;
              }
              MountTest mountTestRef = (MountTest) other;
              if (doubleArg != null ? !doubleArg.equals(mountTestRef.doubleArg) : mountTestRef.doubleArg != null) {
                return false;
              }
              if (floatArg != null ? !floatArg.equals(mountTestRef.floatArg) : mountTestRef.floatArg != null) {
                return false;
              }
              if (intArg != null ? !intArg.equals(mountTestRef.intArg) : mountTestRef.intArg != null) {
                return false;
              }
              if (longArg != null ? !longArg.equals(mountTestRef.longArg) : mountTestRef.longArg != null) {
                return false;
              }
              if (objectArg != null ? !objectArg.equals(mountTestRef.objectArg) : mountTestRef.objectArg != null) {
                return false;
              }
              if (!useTreePropsFromContext()) {
              }
              return true;
            }
            """.trimIndent())
  }
}
