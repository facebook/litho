// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate

/**
 * Unique key for a [RenderUnit.Binder]. This must implement [Object.equals] and [Object.hashCode]
 * to ensure that it can be differentiated from other [BinderKey]s
 */
interface BinderKey {
  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int
}

/** Default [BinderKey] backed by the class of the [RenderUnit.Binder] */
@DataClassGenerate data class ClassBinderKey(private val clazz: Class<*>) : BinderKey

@DataClassGenerate
/** [BinderKey] backed by an index typically used by fixed [RenderUnit.Binder]s */
internal data class IndexedBinderKey(private val index: Int) : BinderKey
