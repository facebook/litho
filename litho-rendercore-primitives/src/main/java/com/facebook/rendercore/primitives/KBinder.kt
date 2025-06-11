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

package com.facebook.rendercore.primitives

import android.content.Context
import com.facebook.rendercore.BinderKey
import com.facebook.rendercore.ClassBinderKey
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.utils.CommonUtils.getSectionNameForTracing
import com.facebook.rendercore.utils.areObjectsEquivalent

fun <Model, Content> binder(
    description: (() -> String)? = null,
    dep: Model,
    func: BindFunc<Content>,
): RenderUnit.DelegateBinder<Model, Content, Any> =
    binder(description, dep, func as BindFuncWithLayoutData<Content>)

fun <Model, Content> binder(
    description: (() -> String)? = null,
    dep: Model,
    func: BindFuncWithLayoutData<Content>,
): RenderUnit.DelegateBinder<Model, Content, Any> {
  return RenderUnit.DelegateBinder.createDelegateBinder(
      dep,
      KBinder(
          describe = description ?: { getSectionNameForTracing(func.javaClass) },
          bindFunc = func,
      ),
  ) as RenderUnit.DelegateBinder<Model, Content, Any>
}

/** Defines the functions that will be called when the content is mounted and unmounted. */
fun interface BindFunc<Content> : BindFuncWithLayoutData<Content> {

  /**
   * This function is called when the content is mounted. [content] is the content that will be
   * mounted. It is important to note that content may not be attached or measured at this point.
   */
  fun BindScope.bind(content: Content): UnbindFunc

  override fun BindScope.bind(content: Content, layoutData: Any?): UnbindFunc {
    return bind(content)
  }

  /** This function tell the framework if the binders should be rerun. Return {@code true} */
  override fun shouldUpdate(
      currentModel: Any?,
      newModel: Any?,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean = !areObjectsEquivalent(currentModel, newModel)
}

fun interface BindFuncWithLayoutData<Content> {

  fun BindScope.bind(content: Content, layoutData: Any?): UnbindFunc

  fun shouldUpdate(
      currentModel: Any?,
      newModel: Any?,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    return !areObjectsEquivalent(currentLayoutData, nextLayoutData) ||
        !areObjectsEquivalent(currentModel, newModel)
  }
}

class UnbindFunc @PublishedApi internal constructor(val unbind: () -> Unit)

class BindScope {

  private var context: Context? = null

  internal inline fun <T> withContext(context: Context, block: () -> T): T {
    this.context = context
    return try {
      block()
    } finally {
      this.context = null
    }
  }

  val androidContext: Context
    get() = requireNotNull(context)

  /**
   * Defines the function that will be called when the content is unmounted. [func] should undo any
   * mutations to the content, cleanup any effects, and release resources.
   */
  inline fun onUnbind(noinline func: () -> Unit): UnbindFunc = UnbindFunc(unbind = func)
}

internal class KBinder<Model, Content>(
    private val describe: () -> String,
    private val bindFunc: BindFuncWithLayoutData<Content>,
) : RenderUnit.Binder<Model, Content, UnbindFunc> {

  val scope: BindScope = BindScope()

  override val description: String
    get() = "binder:${describe()}"

  override val key: BinderKey = ClassBinderKey(bindFunc.javaClass)

  override fun shouldUpdate(
      currentModel: Model,
      newModel: Model,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    return bindFunc.shouldUpdate(currentModel, newModel, currentLayoutData, nextLayoutData)
  }

  override fun bind(
      context: Context,
      content: Content,
      model: Model,
      layoutData: Any?
  ): UnbindFunc {
    return scope.withContext(context) { with(bindFunc) { scope.bind(content, layoutData) } }
  }

  override fun unbind(
      context: Context,
      content: Content,
      model: Model,
      layoutData: Any?,
      bindData: UnbindFunc?
  ) {
    scope.withContext(context) { bindData?.unbind?.invoke() }
  }
}
