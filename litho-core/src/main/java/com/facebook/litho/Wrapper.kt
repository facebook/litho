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

import com.facebook.litho.Resolver.resolve
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.RequiredProp
import java.util.BitSet

/**
 * Utility class for wrapping an existing [Component]. This is useful for adding further
 * [CommonProps] to an already created component.
 */
class Wrapper private constructor() : SpecGeneratedComponent("Wrapper") {

  @Prop var delegate: Component? = null

  override fun onCreateLayout(c: ComponentContext): Component {
    return this
  }

  private fun resolve(resolveContext: ResolveContext, c: ComponentContext): LithoNode? {
    val delegate = delegate
    return if (delegate == null) {
      null
    } else {
      resolve(resolveContext, c, delegate)
    }
  }

  override fun resolve(
      resolveContext: ResolveContext,
      scopedComponentInfo: ScopedComponentInfo,
      parentWidthSpec: Int,
      parentHeightSpec: Int,
      componentsLogger: ComponentsLogger?
  ): ComponentResolveResult {
    val lithoNode: LithoNode? = resolve(resolveContext, scopedComponentInfo.context)
    return ComponentResolveResult(lithoNode, commonProps)
  }

  override fun isEquivalentProps(other: Component?, shouldCompareCommonProps: Boolean): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val wrapper = other as Wrapper
    if (this.id == wrapper.id) {
      return true
    }
    val delegate = delegate
    return !if (delegate != null) {
      !delegate.isEquivalentTo(wrapper.delegate, shouldCompareCommonProps)
    } else {
      wrapper.delegate != null
    }
  }

  override fun getSimpleNameDelegate(): Component? {
    return delegate
  }

  class Builder
  internal constructor(
      context: ComponentContext,
      defStyleAttr: Int,
      defStyleRes: Int,
      private var wrapper: Wrapper
  ) : Component.Builder<Builder>(context, defStyleAttr, defStyleRes, wrapper) {
    private val required = BitSet(REQUIRED_PROPS_COUNT)

    @RequiredProp("delegate")
    fun delegate(delegate: Component?): Builder {
      required.set(0)
      wrapper.delegate = delegate
      return this
    }

    override fun setComponent(component: Component) {
      wrapper = component as Wrapper
    }

    override fun getThis(): Builder {
      return this
    }

    override fun build(): Wrapper {
      checkArgs(REQUIRED_PROPS_COUNT, required, REQUIRED_PROPS_NAMES)
      return wrapper
    }

    companion object {
      private val REQUIRED_PROPS_NAMES = arrayOf("delegate")
      private const val REQUIRED_PROPS_COUNT = 1
    }
  }

  companion object {
    @JvmOverloads
    @JvmStatic
    fun create(context: ComponentContext?, defStyleAttr: Int = 0, defStyleRes: Int = 0): Builder {
      return Builder(requireNotNull(context), defStyleAttr, defStyleRes, Wrapper())
    }
  }
}

/**
 * Builder function for creating [Wrapper] components. It's useful for adding additional [Style]
 * props to a given component.
 */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ResourcesScope.Wrapper(style: Style, content: ResourcesScope.() -> Component): Wrapper {
  return Wrapper.create(context).delegate(content()).kotlinStyle(style).build()
}
