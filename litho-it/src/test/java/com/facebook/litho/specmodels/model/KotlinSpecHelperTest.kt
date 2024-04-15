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

package com.facebook.litho.specmodels.model

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests [KotlinSpecHelper] */
@RunWith(JUnit4::class)
class KotlinSpecHelperTest {

  private lateinit var specModel: SpecModel

  @Before
  fun setUp() {
    specModel = mock()
    whenever(specModel.specElementType).thenReturn(SpecElementType.KOTLIN_SINGLETON)
  }

  @Test
  fun `isKotlinSpec - returns true for Kotlin singleton`() {
    whenever(specModel.specElementType).thenReturn(SpecElementType.KOTLIN_SINGLETON)

    assertThat(KotlinSpecHelper.isKotlinSpec(specModel)).isTrue
  }

  @Test
  fun `isKotlinSpec - returns true for Kotlin class`() {
    whenever(specModel.specElementType).thenReturn(SpecElementType.KOTLIN_CLASS)

    assertThat(KotlinSpecHelper.isKotlinSpec(specModel)).isTrue
  }

  @Test
  fun `isKotlinSpec - returns false for Java class`() {
    whenever(specModel.specElementType).thenReturn(SpecElementType.JAVA_CLASS)

    assertThat(KotlinSpecHelper.isKotlinSpec(specModel)).isFalse
  }

  @Test
  fun `maybeRemoveWildcardFromVarArgsIfKotlinSpec - does nothing for Java Spec`() {
    whenever(specModel.specElementType).thenReturn(SpecElementType.JAVA_CLASS)
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), WildcardTypeName.subtypeOf(Any::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isSameAs(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromVarArgsIfKotlinSpec - does nothing for Kotlin non-parameterized type`() {
    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(specModel, TypeName.DOUBLE))
        .isSameAs(TypeName.DOUBLE)

    val nonParameterizedType = ClassName.get(List::class.java)
    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(
                specModel, nonParameterizedType))
        .isSameAs(nonParameterizedType)
  }

  @Test
  fun `maybeRemoveWildcardFromVarArgsIfKotlinSpec - does nothing for Kotlin parameterized type with no wild cards`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), ClassName.get(String::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isEqualTo(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromVarArgsIfKotlinSpec - cleans up wildcards for Kotlin parameterized type with one argument as wildcard`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), WildcardTypeName.subtypeOf(Any::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isEqualTo(
            ParameterizedTypeName.get(
                ClassName.get(List::class.java), ClassName.get(Any::class.java)))
  }

  @Test
  fun `maybeRemoveWildcardFromVarArgsIfKotlinSpec - does not cleanup wildcards for Kotlin parameterized type with one argument as wildcard and second not wildcard`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            WildcardTypeName.subtypeOf(Any::class.java),
            TypeName.OBJECT)

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isSameAs(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromVarArgsIfKotlinSpec - does not cleanup wildcards for Kotlin parameterized type with one argument as wildcard and second wildcard`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            WildcardTypeName.subtypeOf(Any::class.java),
            WildcardTypeName.subtypeOf(ClassName.get(Class::class.java)))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isSameAs(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromVarArgsIfKotlinSpec - does not cleanup wildcards for Kotlin parameterized type when only second argument is wildcard`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            TypeName.OBJECT,
            WildcardTypeName.subtypeOf(Any::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isSameAs(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - throws exception when type arguments is 0 or less`() {
    whenever(specModel.specElementType).thenReturn(SpecElementType.JAVA_CLASS)
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), WildcardTypeName.subtypeOf(Any::class.java))

    assertThatThrownBy {
          KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
              specModel, parameterizedTypeName, 0 /* maxTypeArgumentsToCleanup */)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy {
          KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
              specModel, parameterizedTypeName, -1 /* maxTypeArgumentsToCleanup */)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy {
          KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
              specModel, parameterizedTypeName, -500 /* maxTypeArgumentsToCleanup */)
        }
        .isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - does nothing for Java Spec`() {
    whenever(specModel.specElementType).thenReturn(SpecElementType.JAVA_CLASS)
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), WildcardTypeName.subtypeOf(Any::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName, 2 /* maxTypeArgumentsToCleanup */))
        .isSameAs(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - does nothing for Kotlin non-parameterized type`() {
    assertThat(KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(specModel, TypeName.INT))
        .isSameAs(TypeName.INT)

    val nonParameterizedType = ClassName.get(List::class.java)
    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, nonParameterizedType, 2 /* maxTypeArgumentsToCleanup */))
        .isSameAs(nonParameterizedType)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - does nothing for Kotlin parameterized type with no wild cards`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), ClassName.get(String::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName, 2 /* maxTypeArgumentsToCleanup */))
        .isEqualTo(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - cleans up wildcards for Kotlin parameterized type with number of type arguments equal to number to cleanup`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            WildcardTypeName.subtypeOf(Any::class.java),
            WildcardTypeName.subtypeOf(TypeName.OBJECT))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName, 2 /* maxTypeArgumentsToCleanup */))
        .isEqualTo(
            ParameterizedTypeName.get(
                ClassName.get(List::class.java), ClassName.get(Any::class.java), TypeName.OBJECT))
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - cleans up wildcards for Kotlin parameterized type with number of type arguments less than to number to cleanup`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), WildcardTypeName.subtypeOf(Number::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName, 2 /* maxTypeArgumentsToCleanup */))
        .isEqualTo(
            ParameterizedTypeName.get(
                ClassName.get(List::class.java), ClassName.get(Number::class.java)))
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - does not clean up wildcards for Kotlin parameterized type if more type arguments than to cleanup`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            WildcardTypeName.subtypeOf(Number::class.java),
            WildcardTypeName.subtypeOf(Number::class.java),
            WildcardTypeName.subtypeOf(Number::class.java),
            WildcardTypeName.subtypeOf(Number::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName, 3 /* maxTypeArgumentsToCleanup */))
        .isSameAs(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - does not clean up wildcards for Kotlin parameterized type if (wild cards after) max number to cleanup`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            TypeName.OBJECT,
            TypeName.OBJECT,
            WildcardTypeName.subtypeOf(Number::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName, 2 /* maxTypeArgumentsToCleanup */))
        .isSameAs(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - handles less type arguments without wildcards than max number to cleanup without issue`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), ClassName.get(Any::class.java), TypeName.OBJECT)

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName, 50 /* maxTypeArgumentsToCleanup */))
        .isEqualTo(
            ParameterizedTypeName.get(
                ClassName.get(List::class.java), ClassName.get(Any::class.java), TypeName.OBJECT))
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec - handles less type arguments and with wildcards than max number to cleanup without issue`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            WildcardTypeName.subtypeOf(Any::class.java),
            TypeName.OBJECT)

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName, 15 /* maxTypeArgumentsToCleanup */))
        .isEqualTo(
            ParameterizedTypeName.get(
                ClassName.get(List::class.java), ClassName.get(Any::class.java), TypeName.OBJECT))
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec (no max) - does nothing for Java Spec`() {
    whenever(specModel.specElementType).thenReturn(SpecElementType.JAVA_CLASS)
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), WildcardTypeName.subtypeOf(Any::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isSameAs(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec (no max) - does nothing for Kotlin non-parameterized type`() {
    assertThat(KotlinSpecHelper.maybeRemoveWildcardFromVarArgsIfKotlinSpec(specModel, TypeName.INT))
        .isSameAs(TypeName.INT)

    val nonParameterizedType = ClassName.get(List::class.java)
    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, nonParameterizedType))
        .isSameAs(nonParameterizedType)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec (no max) - does nothing for Kotlin parameterized type with no wild cards`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java), ClassName.get(Number::class.java))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isEqualTo(parameterizedTypeName)
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec (no max) - cleans up wildcards for Kotlin parameterized type (~2)`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            WildcardTypeName.subtypeOf(Any::class.java),
            WildcardTypeName.subtypeOf(TypeName.OBJECT))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isEqualTo(
            ParameterizedTypeName.get(
                ClassName.get(List::class.java), ClassName.get(Any::class.java), TypeName.OBJECT))
  }

  @Test
  fun `maybeRemoveWildcardFromContainerIfKotlinSpec (no max) - cleans up wildcards (many) for Kotlin parameterized type`() {
    val parameterizedTypeName =
        ParameterizedTypeName.get(
            ClassName.get(List::class.java),
            WildcardTypeName.subtypeOf(Any::class.java),
            WildcardTypeName.subtypeOf(TypeName.OBJECT),
            WildcardTypeName.subtypeOf(Any::class.java),
            WildcardTypeName.subtypeOf(TypeName.OBJECT),
            WildcardTypeName.subtypeOf(Any::class.java),
            WildcardTypeName.subtypeOf(TypeName.OBJECT),
            WildcardTypeName.subtypeOf(Any::class.java),
            WildcardTypeName.subtypeOf(TypeName.OBJECT))

    assertThat(
            KotlinSpecHelper.maybeRemoveWildcardFromContainerIfKotlinSpec(
                specModel, parameterizedTypeName))
        .isEqualTo(
            ParameterizedTypeName.get(
                ClassName.get(List::class.java),
                ClassName.get(Any::class.java),
                TypeName.OBJECT,
                ClassName.get(Any::class.java),
                TypeName.OBJECT,
                ClassName.get(Any::class.java),
                TypeName.OBJECT,
                ClassName.get(Any::class.java),
                TypeName.OBJECT))
  }

  @Test
  fun `getBaseTypeIfWildcard - returns same type if not wildcard`() {
    val nonWildcardType = ClassName.get(String::class.java)

    assertThat(KotlinSpecHelper.getBaseTypeIfWildcard(nonWildcardType)).isSameAs(nonWildcardType)
  }

  @Test
  fun `getBaseTypeIfWildcard - returns upperbound from wildcard`() {
    assertThat(KotlinSpecHelper.getBaseTypeIfWildcard(WildcardTypeName.subtypeOf(TypeName.OBJECT)))
        .isEqualTo(TypeName.OBJECT)
  }

  @Test
  fun `getBaseTypeIfWildcard - returns proper upperbound typename if lower bound`() {
    assertThat(
            KotlinSpecHelper.getBaseTypeIfWildcard(
                WildcardTypeName.supertypeOf(Number::class.java)))
        .isEqualTo(TypeName.OBJECT)
  }
}
