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
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.utils.areObjectsEquivalent
import kotlin.reflect.KFunction2
import kotlin.reflect.KMutableProperty1

/** The implicit receiver for [MountBehavior.mountConfigurationCall]. */
class MountConfigurationScope<ContentType : Any> internal constructor() {

  /**
   * If true, the nested tree hierarchy (if present) will be notified about parent's bounds changes.
   * It will ensure that visibility events and incremental mount works correctly for the nested tree
   * hierarchy.
   *
   * Default is false.
   */
  var doesMountRenderTreeHosts: Boolean = false

  internal val fixedBinders: List<RenderUnit.DelegateBinder<*, ContentType, in Any>>
    get() = _fixedBinders

  private val _fixedBinders: MutableList<RenderUnit.DelegateBinder<*, ContentType, in Any>> =
      mutableListOf()

  /**
   * Stores the current binder description which is used by a binder defined within
   * withDescription{} block.
   */
  @PublishedApi internal var binderDescription: String? = null

  /**
   * Creates a binding between the value, and the content’s property. Allows for specifying custom
   * logic and handling complex cases.
   *
   * Additionally, any time the [deps] changes between updates, the existing [UnbindFunc.onUnbind]
   * cleanup callback will be invoked, and the new [bindCall] callback will be invoked.
   *
   * @param deps Should contain any props or state your [bindCall]/[UnbindFunc.onUnbind] callbacks
   *   use. For example, if you're using [bind] to set a background based on a color you get as a
   *   prop, [deps] should include that color.
   * @param bindCall A function that allows for applying properties to the content.
   */
  fun bind(vararg deps: Any?, bindCall: BindScope.(content: ContentType) -> UnbindFunc) {
    val bindScope = BindScope()
    _fixedBinders.add(
        RenderUnit.DelegateBinder.createDelegateBinder(
            deps,
            object : RenderUnit.Binder<Array<out Any?>, ContentType, UnbindFunc> {

              private val fixedBinderIndex = _fixedBinders.size
              private val customDescription: String? = binderDescription

              override val description: String
                get() = "binder:${customDescription ?: fixedBinderIndex}"

              override fun shouldUpdate(
                  currentModel: Array<out Any?>,
                  newModel: Array<out Any?>,
                  currentLayoutData: Any?,
                  nextLayoutData: Any?
              ): Boolean {
                return !areObjectsEquivalent(currentModel, newModel)
              }

              override fun bind(
                  context: Context,
                  content: ContentType,
                  model: Array<out Any?>,
                  layoutData: Any?
              ): UnbindFunc {
                return bindScope.withContext(context) { bindScope.bindCall(content) }
              }

              override fun unbind(
                  context: Context,
                  content: ContentType,
                  model: Array<out Any?>,
                  layoutData: Any?,
                  unbindFunc: UnbindFunc?
              ) {
                bindScope.withContext(context) { unbindFunc?.onUnbind() }
              }
            }
                as RenderUnit.Binder<Array<out Any?>, ContentType, Any>))
  }

  /**
   * Creates a binding between the value, and the content’s property. Allows for specifying custom
   * logic and handling complex cases and for accessing in [bindCall] the layout data that was
   * generated during the layout pass.
   *
   * Additionally, any time the [deps] or layout data changes between updates, the existing
   * [UnbindFunc.onUnbind] cleanup callback will be invoked, and the new [bindCall] callback will be
   * invoked.
   *
   * @param deps Should contain any props or state your [bindCall]/[UnbindFunc.onUnbind] callbacks
   *   use. For example, if you're using [bind] to set a background based on a color you get as a
   *   prop, [deps] should include that color. If no deps are provided, then only layout data is
   *   compared between updates.
   * @param bindCall A function that allows for applying properties to the content and accessing the
   *   layout data that was generated during the layout pass.
   */
  fun <LayoutDataT> bindWithLayoutData(
      vararg deps: Any?,
      bindCall: BindScope.(content: ContentType, layoutData: LayoutDataT) -> UnbindFunc
  ) {
    val bindScope = BindScope()
    _fixedBinders.add(
        RenderUnit.DelegateBinder.createDelegateBinder(
            deps,
            object : RenderUnit.Binder<Array<out Any?>, ContentType, UnbindFunc> {

              private val fixedBinderIndex = _fixedBinders.size
              private val customDescription: String? = binderDescription

              override val description: String
                get() = "binder:${customDescription ?: fixedBinderIndex}"

              override fun shouldUpdate(
                  currentModel: Array<out Any?>,
                  newModel: Array<out Any?>,
                  currentLayoutData: Any?,
                  nextLayoutData: Any?
              ): Boolean {

                if (!areObjectsEquivalent(currentLayoutData, nextLayoutData)) {
                  // layout data has changed
                  return true
                }

                return !areObjectsEquivalent(currentModel, newModel)
              }

              @Suppress("UNCHECKED_CAST")
              override fun bind(
                  context: Context,
                  content: ContentType,
                  model: Array<out Any?>,
                  layoutData: Any?
              ): UnbindFunc {
                return bindScope.withContext(context) {
                  bindScope.bindCall(content, layoutData as LayoutDataT)
                }
              }

              override fun unbind(
                  context: Context,
                  content: ContentType,
                  model: Array<out Any?>,
                  layoutData: Any?,
                  unbindFunc: UnbindFunc?
              ) {
                bindScope.withContext(context) { unbindFunc?.onUnbind() }
              }
            }
                as RenderUnit.Binder<Array<out Any?>, ContentType, Any>))
  }

  /**
   * Creates a binding between the value, and the content’s property.
   *
   * @param defaultValue value that will be set to the Content after unbind
   * @param setter function reference that will set the value on the content
   */
  fun <T> T.bindTo(setter: KFunction2<ContentType, T, *>, defaultValue: T) {
    _fixedBinders.add(
        RenderUnit.DelegateBinder.createDelegateBinder(
            this,
            object : RenderUnit.Binder<T, ContentType, Any> {

              private val fixedBinderIndex = _fixedBinders.size
              private val customDescription: String? = binderDescription

              override val description: String
                get() = "binder:${customDescription ?: fixedBinderIndex}"

              override fun shouldUpdate(
                  currentModel: T,
                  newModel: T,
                  currentLayoutData: Any?,
                  nextLayoutData: Any?
              ): Boolean {
                return !areObjectsEquivalent(currentModel, newModel)
              }

              override fun bind(
                  context: Context,
                  content: ContentType,
                  model: T,
                  layoutData: Any?
              ): Any? {
                setter(content, model)
                return null
              }

              override fun unbind(
                  context: Context,
                  content: ContentType,
                  model: T,
                  layoutData: Any?,
                  bindData: Any?
              ) {
                setter(content, defaultValue)
              }
            }))
  }

  /**
   * Creates a binding between the value, and the content’s property.
   *
   * @param defaultValue value that will be set to the Content after unbind
   * @param setter property reference that will set the value on the content
   */
  fun <T> T.bindTo(setter: KMutableProperty1<ContentType, T>, defaultValue: T) {
    _fixedBinders.add(
        RenderUnit.DelegateBinder.createDelegateBinder(
            this,
            object : RenderUnit.Binder<T, ContentType, Any> {

              private val fixedBinderIndex = _fixedBinders.size
              private val customDescription: String? = binderDescription

              override val description: String
                get() = "binder:${customDescription ?: fixedBinderIndex}"

              override fun shouldUpdate(
                  currentModel: T,
                  newModel: T,
                  currentLayoutData: Any?,
                  nextLayoutData: Any?
              ): Boolean {
                return !areObjectsEquivalent(currentModel, newModel)
              }

              override fun bind(
                  context: Context,
                  content: ContentType,
                  model: T,
                  layoutData: Any?
              ): Any? {
                setter.set(content, model)
                return null
              }

              override fun unbind(
                  context: Context,
                  content: ContentType,
                  model: T,
                  layoutData: Any?,
                  bindData: Any?
              ) {
                setter.set(content, defaultValue)
              }
            }))
  }

  /**
   * Creates a binding between the value, and the content’s property. The default value of the
   * property is assumed to be null, so after unbind, null value will be set to the Content.
   *
   * @param setter function reference that will set the value on the content
   */
  inline fun <T> T.bindTo(setter: KFunction2<ContentType, T?, *>) = bindTo(setter, null)

  /**
   * Creates a binding between the value, and the content’s property. The default value of the
   * property is assumed to be null, so after unbind, null value will be set to the Content.
   *
   * @param setter property reference that will set the value on the content
   */
  inline fun <T> T.bindTo(setter: KMutableProperty1<ContentType, T?>) = bindTo(setter, null)

  /**
   * Sets the description on the [RenderUnit.Binder] defined within [binderCall]. Descriptions are
   * used mainly for debugging purposes such as tracing and logs. Maximum description length is 127
   * characters. Everything above that will be truncated.
   */
  inline fun withDescription(
      description: String,
      crossinline binderCall: MountConfigurationScope<ContentType>.() -> Unit
  ) {
    try {
      binderDescription = description.take(RenderUnit.MAX_DESCRIPTION_LENGTH)
      this.binderCall()
    } finally {
      binderDescription = null
    }
  }

  /**
   * Creates a binding between the value, and the content’s property. Allows for specifying custom
   * logic and handling complex cases.
   *
   * It is an error to call [bind] without deps parameter.
   */
  // This deprecated-error function shadows the varargs overload so that the varargs version is not
  // used without key parameters.
  @Deprecated(BIND_NO_DEPS_ERROR, level = DeprecationLevel.ERROR)
  fun bind(bindCall: BindScope.(content: ContentType) -> UnbindFunc): Unit =
      throw IllegalStateException(BIND_NO_DEPS_ERROR)

  companion object {
    private const val BIND_NO_DEPS_ERROR =
        "bind must provide 'deps' parameter that determines whether the existing 'onUnbind' cleanup callback will be invoked, and the new 'bind' callback will be invoked"
  }
}

/**
 * Interface for the [onUnbind] function: use [onUnbind] to define the cleanup function for your
 * content.
 */
fun interface UnbindFunc {
  fun onUnbind()
}

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
   * Defines an unbind function to be invoked when the content needs to be updated or a [Primitive]
   * is detached.
   */
  inline fun onUnbind(crossinline unbindFunc: () -> Unit): UnbindFunc = UnbindFunc { unbindFunc() }
}
