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

@file:Suppress("KtDataClass")

package com.facebook.litho.theming

import android.content.Context
import android.content.res.Configuration
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.TreePropProvider
import com.facebook.litho.TreePropProviderImpl
import libraries.components.litho.theming.Colors
import libraries.components.litho.theming.Typography
import libraries.components.litho.theming.darkColors
import libraries.components.litho.theming.lightColors

/**
 * Returns the instance of [LithoTheme] registered for this Component hierarchy. [LithoTheme] can be
 * registered using [WithTheme] function.
 */
fun ComponentScope.getTheme(): LithoTheme {
  return context.getTreeProp(LithoTheme::class.java) ?: LithoTheme(context = androidContext)
}

/**
 * Sets a Theme within Component hierarchy that can be accessed anytime using [getTheme] function.
 * Theme class should extend [LithoTheme] data class and any redeclaration of the Theme will
 * override the previous value, if no Theme is set, the default [LithoTheme] will be used.
 */
@Suppress("FunctionNaming")
inline fun <T> WithTheme(theme: T, component: () -> Component?): TreePropProviderImpl? {
  return TreePropProvider(LithoTheme::class.java to theme) { component() }
}

private fun isDarkMode(context: Context): Boolean {
  return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
    Configuration.UI_MODE_NIGHT_YES -> true
    else -> false
  }
}

data class LithoTheme(
    val context: Context,
    val lightColors: Colors = lightColors(),
    val darkColors: Colors = darkColors(),
    val typography: Typography = Typography(),
) {
  val colors: Colors = if (isDarkMode(context = context)) darkColors else lightColors
}
