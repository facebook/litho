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

package com.facebook.litho

import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

/**
 * Every public `(Style, …) -> Style` builder must return equal styles when called twice with the
 * same arguments. Style equality is item equality, so a style item that isn't a data class makes
 * every component using that builder compare as changed on every render.
 */
@RunWith(LithoTestRunner::class)
class StyleBuilderEqualityTest {

  private val styleFacades = listOf(
      "com.facebook.litho.core.CoreStylesKt",
      "com.facebook.litho.flexbox.FlexboxStyleKt",
      "com.facebook.litho.view.ViewStylesKt",
      "com.facebook.litho.visibility.VisibilityStylesKt",
      "com.facebook.litho.accessibility.AccessibilityStylesKt",
      "com.facebook.litho.animation.LayoutStylesKt",
      "com.facebook.litho.animation.StyleTransitionsKt",
      "com.facebook.litho.transition.TransitionsStylesKt",
      "com.facebook.litho.animated.DynamicValueStylesKt",
  )

  // These wrap a binder or listener holder created on every call, so they never compare equal.
  private val equalByIdentityOnly =
      setOf("additionalOnClick", "onHover", "onKeyEvent", "pivotPercent")

  // One instance per type, so both calls of a builder get the same arguments.
  private val arguments = HashMap<Class<*>, Any?>()

  @Test
  fun `every style builder equals itself rebuilt from the same arguments`() {
    val notEqual = mutableListOf<String>()
    var checked = 0
    for (facade in styleFacades) {
      for (builder in Class.forName(facade).declaredMethods) {
        val params = builder.parameterTypes
        val isStyleBuilder =
            Modifier.isStatic(builder.modifiers) &&
                Modifier.isPublic(builder.modifiers) &&
                !builder.isSynthetic &&
                params.isNotEmpty() &&
                params[0] == Style::class.java &&
                builder.returnType == Style::class.java
        val name = builder.name.substringBefore('-')
        if (!isStyleBuilder || name in equalByIdentityOnly) {
          continue
        }
        val args = arrayOfNulls<Any?>(params.size)
        args[0] = Style
        for (i in 1 until params.size) {
          args[i] = argumentFor(params[i])
        }
        val first: Any?
        val second: Any?
        try {
          first = builder.invoke(null, *args)
          second = builder.invoke(null, *args)
        } catch (e: InvocationTargetException) {
          // The builder rejects the generic argument (e.g. a negative or out-of-range value).
          continue
        }
        checked++
        if (first != second || first.hashCode() != second.hashCode()) {
          notEqual.add("${facade.substringAfterLast('.')}.$name")
        }
      }
    }

    assertThat(checked).describedAs("style builders exercised").isGreaterThan(100)
    assertThat(notEqual)
        .describedAs("style builders not equal to themselves rebuilt from the same arguments")
        .isEmpty()
  }

  private fun argumentFor(type: Class<*>): Any? =
      arguments.getOrPut(type) {
        when {
          type == java.lang.Float.TYPE || type == java.lang.Float::class.java -> 0.5f
          type == java.lang.Integer.TYPE || type == java.lang.Integer::class.java -> 1
          // Dimen is a value class over Long, so builders taking a Dimen take a long.
          type == java.lang.Long.TYPE -> 10.dp.encodedValue
          type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java -> true
          type == String::class.java || type == CharSequence::class.java -> "a"
          type == Dimen::class.java -> 10.dp
          type == Style::class.java -> Style
          type.isEnum -> type.enumConstants[0]
          type.isArray -> java.lang.reflect.Array.newInstance(type.componentType, 1)
          type.isInterface ->
              Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
                when (method.name) {
                  "equals" -> proxy === args?.get(0)
                  "hashCode" -> System.identityHashCode(proxy)
                  else -> if (method.returnType == java.lang.Boolean.TYPE) false else null
                }
              }
          else ->
              try {
                type.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
              } catch (e: ReflectiveOperationException) {
                mock(type)
              }
        }
      }
}
