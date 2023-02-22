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

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.facebook.flipper.core.FlipperObject
import com.facebook.flipper.plugins.inspector.InspectorValue
import com.facebook.flipper.plugins.inspector.InspectorValue.Type.Color
import com.facebook.flipper.plugins.inspector.Named
import com.facebook.litho.Component
import com.facebook.litho.DebugComponent
import com.facebook.litho.KStateContainer
import com.facebook.litho.SpecGeneratedComponent
import com.facebook.litho.StateContainer
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.State
import com.facebook.litho.state

object DataUtils {

  @JvmStatic
  fun getPropData(node: Any): List<Named<FlipperObject>> {
    val props = FlipperObject.Builder()
    val data: MutableList<Named<FlipperObject>> = ArrayList()
    var hasProps = false
    val isSpecComponent = node is SpecGeneratedComponent
    for (f in node.javaClass.declaredFields) {
      f.isAccessible = true
      val prop =
          try {
            f[node]
          } catch (e: IllegalAccessException) {
            continue
          }
      val propName = f.name
      val annotation = f.getAnnotation(Prop::class.java)
      if (isSpecComponent && annotation == null) {
        // Only expose `@Prop` annotated fields for Spec components
        continue
      }
      hasProps = true
      if (prop != null && PropWithInspectorSection::class.java.isAssignableFrom(prop.javaClass)) {
        val datum = (prop as PropWithInspectorSection).flipperLayoutInspectorSection
        if (datum != null) {
          data.add(Named(datum.key, FlipperObject(datum.value)))
        }
      }
      if (annotation != null) {
        val resType = annotation.resType
        if (resType == ResType.COLOR) {
          props.put(propName, if (prop == null) "null" else fromColor(prop as Int))
          continue
        } else if (resType == ResType.DRAWABLE) {
          props.put(propName, if (prop == null) "null" else fromDrawable(prop as Drawable?))
          continue
        }
      }

      val description = getValueOverride(prop)
      if (description != null) {
        props.put(propName, description)
        continue
      }

      props.put(propName, FlipperEditor.makeFlipperField(node, f))
    }
    if (hasProps) {
      data.add(Named("Props", props.build()))
    }
    return data
  }

  @JvmStatic
  fun getMountingData(node: DebugComponent): FlipperObject? {

    val lithoView = node.lithoView
    val mountingData = FlipperObject.Builder()

    if (lithoView == null) {
      return mountingData.build()
    }

    val mountState = lithoView.mountDelegateTarget ?: return mountingData.build()
    val componentTree = lithoView.componentTree ?: return mountingData.build()
    var hasMountingInfo = false
    val component = node.component

    if (component.mountType != Component.MountType.NONE) {
      val renderUnit = DebugComponent.getRenderUnit(node, componentTree)
      if (renderUnit != null) {
        val renderUnitId = renderUnit.id
        val isMounted = mountState.getContentById(renderUnitId) != null
        mountingData.put("mounted", isMounted)
        hasMountingInfo = true
      }
    }

    val visibilityOutput = DebugComponent.getVisibilityOutput(node, componentTree)
    if (visibilityOutput != null) {
      val isVisible = DebugComponent.isVisible(node, lithoView)
      mountingData.put("visible", isVisible)
      hasMountingInfo = true
    }

    return if (hasMountingInfo) mountingData.build() else null
  }

  // Props can override the value shown in the Layout Inspector by implementing PropWithDescription
  // e.g. to include additional debug information.
  private fun getValueOverride(prop: Any?): FlipperObject? =
      prop
          ?.let { it as? PropWithDescription }
          ?.flipperLayoutInspectorPropDescription
          ?.let {
            when (it) {
              is Map<*, *> ->
                  FlipperObject.Builder()
                      .apply {
                        it.entries.forEach { (key, value) ->
                          put(key.toString(), InspectorValue.immutable(value))
                        }
                      }
                      .build()
              else -> InspectorValue.immutable(it).toFlipperObject()
            }
          }

  @JvmStatic
  fun getStateData(stateContainer: StateContainer?): FlipperObject? {
    stateContainer ?: return null

    val state = FlipperObject.Builder()
    var hasState = false
    if (stateContainer is KStateContainer) {
      val states = stateContainer.state
      hasState = states.isNotEmpty() == true
      states?.let {
        it.forEachIndexed { index, item ->
          // TODO(T131825073): Replace with Flipper Field to allow editing state
          state.put(index.toString(), item)
        }
      }
    } else {
      for (f in stateContainer.javaClass.declaredFields) {
        f.isAccessible = true
        val annotation = f.getAnnotation(State::class.java)
        if (annotation != null) {
          state.put(f.name, FlipperEditor.makeFlipperField(stateContainer, f))
          hasState = true
        }
      }
    }

    return if (hasState) state.build() else null
  }

  @JvmStatic
  fun fromDrawable(d: Drawable?): InspectorValue<*> =
      when (d) {
        is ColorDrawable -> InspectorValue.immutable(Color, d.color)
        else -> InspectorValue.immutable(d)
      }

  @JvmStatic fun fromColor(color: Int): InspectorValue<*> = InspectorValue.mutable(Color, color)
}
