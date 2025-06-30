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

package com.facebook.litho.editor.flipper

import android.graphics.Bitmap
import android.view.ViewGroup
import com.facebook.flipper.core.FlipperDynamic
import com.facebook.flipper.core.FlipperObject
import com.facebook.flipper.plugins.inspector.Named
import com.facebook.flipper.plugins.inspector.NodeDescriptor
import com.facebook.flipper.plugins.inspector.SetDataOperations.FlipperValueHint
import com.facebook.flipper.plugins.inspector.Touch
import com.facebook.litho.BaseMountingView
import com.facebook.litho.DebugComponent.Companion.getRootInstance

@Suppress("UNCHECKED_CAST", "KotlinGenericsCast")
class LithoViewDescriptor : NodeDescriptor<BaseMountingView>() {

  @Throws(Exception::class)
  override fun init(node: BaseMountingView) {
    node.setOnDirtyMountListener { view ->
      invalidate(view)
      invalidateAX(view)
    }
  }

  @Throws(Exception::class)
  override fun getId(node: BaseMountingView?): String? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getId(node)
  }

  @Throws(Exception::class)
  override fun getName(node: BaseMountingView?): String? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getName(node)
  }

  @Throws(Exception::class)
  override fun getAXName(node: BaseMountingView?): String? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getAXName(node)
  }

  override fun getChildCount(node: BaseMountingView?): Int {
    node ?: return 0
    return if (getRootInstance(node) == null) 0 else 1
  }

  @Throws(Exception::class)
  override fun getAXChildCount(node: BaseMountingView?): Int {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getAXChildCount(node) ?: 0
  }

  override fun getChildAt(node: BaseMountingView?, index: Int): Any? {
    node ?: return null
    return getRootInstance(node)
  }

  @Throws(Exception::class)
  override fun getAXChildAt(node: BaseMountingView?, index: Int): Any? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as? NodeDescriptor<BaseMountingView>?
    return descriptor?.getChildAt(node, index)
  }

  @Throws(Exception::class)
  override fun getData(node: BaseMountingView?): List<Named<FlipperObject>> {
    val props: MutableList<Named<FlipperObject>> = ArrayList()
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    val mountedBounds = node?.previousMountBounds

    props.add(
        0,
        Named(
            "LithoView",
            FlipperObject.Builder()
                .put(
                    "mountbounds",
                    FlipperObject.Builder()
                        .put("left", mountedBounds?.left)
                        .put("top", mountedBounds?.top)
                        .put("right", mountedBounds?.right)
                        .put("bottom", mountedBounds?.bottom))
                .build()))

    descriptor?.getData(node)?.let { props.addAll(it) }
    return props
  }

  @Throws(Exception::class)
  override fun getAXData(node: BaseMountingView?): List<Named<FlipperObject>> {
    val props: MutableList<Named<FlipperObject>> = ArrayList()
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    descriptor?.getAXData(node)?.let { props.addAll(it) }
    return props
  }

  @Throws(Exception::class)
  override fun setValue(
      node: BaseMountingView?,
      path: Array<String>,
      kind: FlipperValueHint?,
      value: FlipperDynamic
  ) {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    descriptor?.setValue(node, path, kind, value)
  }

  @Throws(Exception::class)
  override fun getAttributes(node: BaseMountingView?): List<Named<String>>? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getAttributes(node)
  }

  @Throws(Exception::class)
  override fun getAXAttributes(node: BaseMountingView?): List<Named<String>>? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getAXAttributes(node)
  }

  override fun getExtraInfo(node: BaseMountingView?): FlipperObject? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getExtraInfo(node)
  }

  @Throws(Exception::class)
  override fun setHighlighted(
      node: BaseMountingView?,
      selected: Boolean,
      isAlignmentMode: Boolean
  ) {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    descriptor?.setHighlighted(node, selected, isAlignmentMode)
  }

  @Throws(Exception::class)
  override fun getSnapshot(node: BaseMountingView?, includeChildren: Boolean): Bitmap? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getSnapshot(node, includeChildren)
  }

  override fun hitTest(node: BaseMountingView?, touch: Touch) {
    touch.continueWithOffset(0, 0, 0)
  }

  @Throws(Exception::class)
  override fun axHitTest(node: BaseMountingView?, touch: Touch) {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    descriptor?.axHitTest(node, touch)
  }

  @Throws(Exception::class)
  override fun getDecoration(node: BaseMountingView?): String? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getDecoration(node)
  }

  @Throws(Exception::class)
  override fun getAXDecoration(node: BaseMountingView?): String? {
    val descriptor = descriptorForClass(ViewGroup::class.java) as NodeDescriptor<BaseMountingView>?
    return descriptor?.getAXDecoration(node)
  }

  @Throws(Exception::class)
  override fun matches(query: String, node: BaseMountingView?): Boolean {
    val descriptor = descriptorForClass(Any::class.java) as? NodeDescriptor<BaseMountingView?>?
    return descriptor?.matches(query, node) ?: false
  }
}
