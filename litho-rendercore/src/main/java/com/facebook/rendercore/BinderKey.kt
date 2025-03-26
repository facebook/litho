// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore

/**
 * Unique key for a [RenderUnit.Binder]. This must implement [Object.equals] and [Object.hashCode]
 * to ensure that it can be differentiated from other [BinderKey]s
 */
interface BinderKey {
  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int
}

/** Default [BinderKey] backed by the class of the [RenderUnit.Binder] */
class ClassBinderKey(private val clazz: Class<*>) : BinderKey {
  override fun equals(other: Any?): Boolean = other is ClassBinderKey && other.clazz == clazz

  override fun hashCode(): Int = clazz.hashCode()
}
