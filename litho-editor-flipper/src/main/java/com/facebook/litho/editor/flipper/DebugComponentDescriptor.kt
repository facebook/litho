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
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Pair
import android.view.View
import com.facebook.flipper.core.FlipperDynamic
import com.facebook.flipper.core.FlipperObject
import com.facebook.flipper.plugins.inspector.HighlightedOverlay
import com.facebook.flipper.plugins.inspector.InspectorValue
import com.facebook.flipper.plugins.inspector.Named
import com.facebook.flipper.plugins.inspector.NodeDescriptor
import com.facebook.flipper.plugins.inspector.SetDataOperations.FlipperValueHint
import com.facebook.flipper.plugins.inspector.Touch
import com.facebook.flipper.plugins.inspector.descriptors.ObjectDescriptor
import com.facebook.flipper.plugins.inspector.descriptors.utils.ContextDescriptorUtils
import com.facebook.litho.Component
import com.facebook.litho.DebugComponent
import com.facebook.litho.DebugComponent.Overrider
import com.facebook.litho.DebugLayoutNodeEditor
import com.facebook.litho.StateContainer
import com.facebook.litho.editor.flipper.DataUtils.fromDrawable
import com.facebook.litho.editor.flipper.DataUtils.getPropData
import com.facebook.litho.editor.flipper.DataUtils.getStateData
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaValue
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Locale

@Suppress("KotlinGenericsCast")
class DebugComponentDescriptor : NodeDescriptor<DebugComponent>() {
  private val overrides:
      MutableMap<
          String, MutableList<Pair<Array<String>, Pair<FlipperValueHint?, FlipperDynamic>>>> =
      HashMap()

  private val overrider: Overrider =
      object : Overrider {
        override fun applyComponentOverrides(key: String, component: Component) {
          val overrides = overrides[key] ?: return

          for (override in overrides) {
            if (override.first[0] == "Props") {
              applyReflectiveOverride(
                  component, override.first, override.second.first, override.second.second)
            }
          }
        }

        override fun applyStateOverrides(key: String, stateContainer: StateContainer) {
          val overrides = overrides[key] ?: return

          for (override in overrides) {
            if (override.first[0] == "State") {
              applyReflectiveOverride(
                  stateContainer, override.first, override.second.first, override.second.second)
            }
          }
        }

        override fun applyLayoutOverrides(key: String, node: DebugLayoutNodeEditor) {
          val overrides = overrides[key] ?: return

          for (override in overrides) {
            if (override.first[0] == "Layout") {
              try {
                applyLayoutOverride(
                    node,
                    Arrays.copyOfRange(override.first, 1, override.first.size),
                    override.second.second)
              } catch (ignored: Exception) {}
            }
          }
        }
      }

  override fun init(node: DebugComponent) {
    // We rely on the LithoView being invalidated when a component hierarchy changes.
  }

  override fun getId(node: DebugComponent?): String? {
    return node?.globalKey
  }

  @Throws(Exception::class)
  override fun getName(node: DebugComponent?): String? {
    val componentDescriptor = descriptorForClass(node?.component?.javaClass) as NodeDescriptor<Any>?
    if (checkNotNull(componentDescriptor).javaClass != ObjectDescriptor::class.java) {
      return componentDescriptor.getName(node?.component)
    }
    return node?.component?.simpleName
  }

  override fun getChildCount(node: DebugComponent?): Int {
    return if (checkNotNull(node).mountedView != null || node.mountedDrawable != null) {
      1
    } else {
      node.childComponents.size
    }
  }

  override fun getChildAt(node: DebugComponent?, index: Int): Any {
    val mountedView = checkNotNull(node).mountedView
    val mountedDrawable = node.mountedDrawable

    return mountedView ?: (mountedDrawable ?: node.childComponents[index])
  }

  @Throws(Exception::class)
  override fun getData(node: DebugComponent?): List<Named<FlipperObject>> {
    val componentDescriptor =
        descriptorForClass(node?.component?.javaClass) as NodeDescriptor<Component>?
    if (checkNotNull(componentDescriptor).javaClass != ObjectDescriptor::class.java) {
      return componentDescriptor.getData(node?.component)
    }

    val data: MutableList<Named<FlipperObject>> = ArrayList()

    node ?: return data

    val layoutData = getLayoutData(node)
    if (layoutData != null) {
      data.add(Named("Layout", layoutData))
    }

    val propData = getPropData(node)
    if (propData != null) {
      data.addAll(propData)
    }

    val mountingData = getMountingData(node)
    if (mountingData != null) {
      data.add(Named("Mounting and Visibility", mountingData))
    }

    val stateData = getStateData(node)
    if (stateData != null) {
      data.add(Named("State", stateData))
    }

    data.add(Named("Theme", ContextDescriptorUtils.themeData(node.context.androidContext)))

    return data
  }

  override fun setValue(
      node: DebugComponent?,
      path: Array<String>,
      kind: FlipperValueHint?,
      value: FlipperDynamic
  ) {
    node ?: return
    var overrides = overrides[node.globalKey]
    if (overrides == null) {
      overrides = ArrayList()
      this.overrides[node.globalKey] = overrides
    }
    overrides.add(Pair(path, Pair(kind, value)))

    node.setOverrider(overrider)
    node.rerender()
  }

  override fun getAttributes(node: DebugComponent?): List<Named<String>> {
    val attributes: MutableList<Named<String>> = ArrayList()
    val key = node?.key
    val testKey = node?.testKey

    if (key != null && key.trim { it <= ' ' }.length > 0) {
      attributes.add(Named("key", key))
    }

    if (testKey != null && testKey.trim { it <= ' ' }.length > 0) {
      attributes.add(Named("testKey", testKey))
    }

    return attributes
  }

  override fun getExtraInfo(node: DebugComponent?): FlipperObject {
    val extraInfo = FlipperObject.Builder()

    node ?: return extraInfo.build()

    val descriptor = descriptorForClass(View::class.java) as NodeDescriptor<View>?
    val hostView: View? = node.componentHost
    val lithoView: View? = node.lithoView

    if (hostView != null) {
      try {
        extraInfo.put("linkedNode", descriptor?.getId(hostView))
      } catch (ignored: Exception) {
        // doesn't have linked node descriptor
      }
    } else if (lithoView != null) {
      try {
        // NULLSAFE_FIXME[Nullable Dereference]
        extraInfo.put("linkedNode", descriptor?.getId(lithoView)).put("expandWithParent", true)
      } catch (ignored: Exception) {
        // doesn't add linked node descriptor
      }
    }
    val metaData = FlipperObject.Builder()
    metaData.put("className", node.component.javaClass.name)
    metaData.put("framework", "LITHO")

    extraInfo.put("metaData", metaData)

    return extraInfo.build()
  }

  override fun setHighlighted(node: DebugComponent?, selected: Boolean, isAlignmentMode: Boolean) {
    val lithoView = node?.lithoView ?: return

    if (!selected) {
      HighlightedOverlay.removeHighlight(lithoView)
      return
    }

    val layout = node.layoutNode
    val margin =
        if (!node.isRoot) {
          Rect(
              layout?.getLayoutMargin(YogaEdge.START)?.toInt() ?: 0,
              layout?.getLayoutMargin(YogaEdge.TOP)?.toInt() ?: 0,
              layout?.getLayoutMargin(YogaEdge.END)?.toInt() ?: 0,
              layout?.getLayoutMargin(YogaEdge.BOTTOM)?.toInt() ?: 0)
        } else {
          // Margin not applied if you're at the root
          Rect()
        }

    val padding =
        Rect(
            layout?.getLayoutPadding(YogaEdge.START)?.toInt() ?: 0,
            layout?.getLayoutPadding(YogaEdge.TOP)?.toInt() ?: 0,
            layout?.getLayoutPadding(YogaEdge.END)?.toInt() ?: 0,
            layout?.getLayoutPadding(YogaEdge.BOTTOM)?.toInt() ?: 0)

    val contentBounds = node.boundsInLithoView
    HighlightedOverlay.setHighlighted(lithoView, margin, padding, contentBounds, isAlignmentMode)
  }

  @Throws(Exception::class)
  override fun getSnapshot(node: DebugComponent?, includeChildren: Boolean): Bitmap? {
    val lithoView = node?.lithoView ?: return null

    val contentBounds = node.boundsInLithoView
    val bitmap =
        Bitmap.createBitmap(contentBounds.width(), contentBounds.height(), Bitmap.Config.ARGB_8888)

    val c = Canvas(bitmap)
    lithoView.draw(c)
    return bitmap
  }

  override fun hitTest(node: DebugComponent?, touch: Touch) {
    var finish = true
    for (i in getChildCount(node) - 1 downTo 0) {
      val child = getChildAt(node, i)
      if (child is DebugComponent) {
        val bounds = child.bounds

        if (touch.containedIn(bounds.left, bounds.top, bounds.right, bounds.bottom)) {
          touch.continueWithOffset(i, bounds.left, bounds.top)
          finish = false
        }
      } else if (child is View || child is Drawable) {
        // Components can only mount one view or drawable and its bounds are the same as the
        // hosting component.
        touch.continueWithOffset(i, 0, 0)
        finish = false
      }
    }

    if (finish) touch.finish()
  }

  @Throws(Exception::class)
  override fun getDecoration(node: DebugComponent?): String {
    if (node?.component != null) {
      val componentDescriptor = descriptorForClass(node.component.javaClass) as NodeDescriptor<Any>?
      if (checkNotNull(componentDescriptor).javaClass != ObjectDescriptor::class.java) {
        return componentDescriptor.getDecoration(node.component)
      }
    }
    return "litho"
  }

  @Throws(Exception::class)
  override fun matches(query: String, node: DebugComponent?): Boolean {
    val descriptor = descriptorForClass(Any::class.java) as NodeDescriptor<Any>?
    return descriptor?.matches(query, node) == true || getId(node) == query
  }

  companion object {
    private fun getLayoutData(node: DebugComponent): FlipperObject? {
      val layout = node.layoutNode ?: return null

      val data = FlipperObject.Builder()
      data.put("<PLAYGROUND>", InspectorValue.immutable("https://yogalayout.dev/playground/"))

      data.put("background", fromDrawable(layout.background))
      data.put("foreground", fromDrawable(layout.foreground))

      data.put(
          "direction",
          InspectorValue.mutable(
              InspectorValue.Type.Picker,
              InspectorValue.Picker(
                  enumToSet(YogaDirection.entries.toTypedArray()), layout.layoutDirection.name)))
      data.put(
          "flex-direction",
          InspectorValue.mutable(
              InspectorValue.Type.Picker,
              InspectorValue.Picker(
                  enumToSet(YogaFlexDirection.entries.toTypedArray()), layout.flexDirection.name)))
      data.put(
          "justify-content",
          InspectorValue.mutable(
              InspectorValue.Type.Picker,
              InspectorValue.Picker(
                  enumToSet(YogaJustify.entries.toTypedArray()), layout.justifyContent.name)))
      data.put(
          "align-items",
          InspectorValue.mutable(
              InspectorValue.Type.Picker,
              InspectorValue.Picker(
                  enumToSet(YogaAlign.entries.toTypedArray()), layout.alignItems.name)))
      data.put(
          "align-self",
          InspectorValue.mutable(
              InspectorValue.Type.Picker,
              InspectorValue.Picker(
                  enumToSet(YogaAlign.entries.toTypedArray()), layout.alignSelf.name)))
      data.put(
          "align-content",
          InspectorValue.mutable(
              InspectorValue.Type.Picker,
              InspectorValue.Picker(
                  enumToSet(YogaAlign.entries.toTypedArray()), layout.alignContent.name)))
      data.put(
          "position-type",
          InspectorValue.mutable(
              InspectorValue.Type.Picker,
              InspectorValue.Picker(
                  enumToSet(YogaPositionType.entries.toTypedArray()), layout.positionType.name)))

      data.put("flex-grow", fromFloat(layout.flexGrow))
      data.put("flex-shrink", fromFloat(layout.flexShrink))
      data.put("flex-basis", fromYogaValue(layout.flexBasis))

      data.put("width", fromYogaValue(layout.width))
      data.put("min-width", fromYogaValue(layout.minWidth))
      data.put("max-width", fromYogaValue(layout.maxWidth))

      data.put("height", fromYogaValue(layout.height))
      data.put("min-height", fromYogaValue(layout.minHeight))
      data.put("max-height", fromYogaValue(layout.maxHeight))

      data.put("aspect-ratio", fromFloat(layout.aspectRatio))

      data.put(
          "margin",
          FlipperObject.Builder()
              .put("left", fromYogaValue(layout.getMargin(YogaEdge.LEFT)))
              .put("top", fromYogaValue(layout.getMargin(YogaEdge.TOP)))
              .put("right", fromYogaValue(layout.getMargin(YogaEdge.RIGHT)))
              .put("bottom", fromYogaValue(layout.getMargin(YogaEdge.BOTTOM)))
              .put("start", fromYogaValue(layout.getMargin(YogaEdge.START)))
              .put("end", fromYogaValue(layout.getMargin(YogaEdge.END)))
              .put("horizontal", fromYogaValue(layout.getMargin(YogaEdge.HORIZONTAL)))
              .put("vertical", fromYogaValue(layout.getMargin(YogaEdge.VERTICAL)))
              .put("all", fromYogaValue(layout.getMargin(YogaEdge.ALL))))

      data.put(
          "padding",
          FlipperObject.Builder()
              .put("left", fromYogaValue(layout.getPadding(YogaEdge.LEFT)))
              .put("top", fromYogaValue(layout.getPadding(YogaEdge.TOP)))
              .put("right", fromYogaValue(layout.getPadding(YogaEdge.RIGHT)))
              .put("bottom", fromYogaValue(layout.getPadding(YogaEdge.BOTTOM)))
              .put("start", fromYogaValue(layout.getPadding(YogaEdge.START)))
              .put("end", fromYogaValue(layout.getPadding(YogaEdge.END)))
              .put("horizontal", fromYogaValue(layout.getPadding(YogaEdge.HORIZONTAL)))
              .put("vertical", fromYogaValue(layout.getPadding(YogaEdge.VERTICAL)))
              .put("all", fromYogaValue(layout.getPadding(YogaEdge.ALL))))

      data.put(
          "border",
          FlipperObject.Builder()
              .put("left", fromFloat(layout.getBorderWidth(YogaEdge.LEFT)))
              .put("top", fromFloat(layout.getBorderWidth(YogaEdge.TOP)))
              .put("right", fromFloat(layout.getBorderWidth(YogaEdge.RIGHT)))
              .put("bottom", fromFloat(layout.getBorderWidth(YogaEdge.BOTTOM)))
              .put("start", fromFloat(layout.getBorderWidth(YogaEdge.START)))
              .put("end", fromFloat(layout.getBorderWidth(YogaEdge.END)))
              .put("horizontal", fromFloat(layout.getBorderWidth(YogaEdge.HORIZONTAL)))
              .put("vertical", fromFloat(layout.getBorderWidth(YogaEdge.VERTICAL)))
              .put("all", fromFloat(layout.getBorderWidth(YogaEdge.ALL))))

      data.put(
          "position",
          FlipperObject.Builder()
              .put("left", fromYogaValue(layout.getPosition(YogaEdge.LEFT)))
              .put("top", fromYogaValue(layout.getPosition(YogaEdge.TOP)))
              .put("right", fromYogaValue(layout.getPosition(YogaEdge.RIGHT)))
              .put("bottom", fromYogaValue(layout.getPosition(YogaEdge.BOTTOM)))
              .put("start", fromYogaValue(layout.getPosition(YogaEdge.START)))
              .put("end", fromYogaValue(layout.getPosition(YogaEdge.END)))
              .put("horizontal", fromYogaValue(layout.getPosition(YogaEdge.HORIZONTAL)))
              .put("vertical", fromYogaValue(layout.getPosition(YogaEdge.VERTICAL)))
              .put("all", fromYogaValue(layout.getPosition(YogaEdge.ALL))))

      data.put(
          "hasViewOutput",
          InspectorValue.immutable(InspectorValue.Type.Boolean, layout.hasViewOutput()))
      if (layout.hasViewOutput()) {
        data.put("alpha", fromFloat(layout.alpha))
        data.put("scale", fromFloat(layout.scale))
        data.put("rotation", fromFloat(layout.rotation))
      }

      return data.build()
    }

    private fun <E : Enum<E>> enumToSet(enums: Array<E>): HashSet<String> {
      val names = HashSet<String>()
      for (aEnum in enums) {
        names.add(aEnum.name)
      }
      return names
    }

    @Throws(Exception::class)
    private fun getPropData(node: DebugComponent): List<Named<FlipperObject>>? {
      if (node.canResolve()) {
        return null
      }

      val component = node.component
      return getPropData(component)
    }

    @Throws(Exception::class)
    private fun getMountingData(node: DebugComponent): FlipperObject? {
      return DataUtils.getMountingData(node)
    }

    private fun getStateData(node: DebugComponent): FlipperObject? {
      return getStateData(node.stateContainer)
    }

    private fun applyLayoutOverride(
        node: DebugLayoutNodeEditor,
        path: Array<String>,
        value: FlipperDynamic
    ) {
      when (path[0]) {
        "background" -> node.setBackgroundColor(value.asInt())
        "foreground" -> node.setForegroundColor(value.asInt())
        "direction" ->
            node.setLayoutDirection(
                YogaDirection.valueOf(value.asString()!!.uppercase(Locale.getDefault())))
        "flex-direction" ->
            node.setFlexDirection(
                YogaFlexDirection.valueOf(value.asString()!!.uppercase(Locale.getDefault())))
        "justify-content" ->
            node.setJustifyContent(
                YogaJustify.valueOf(value.asString()!!.uppercase(Locale.getDefault())))
        "align-items" ->
            node.setAlignItems(YogaAlign.valueOf(value.asString()!!.uppercase(Locale.getDefault())))
        "align-self" ->
            node.setAlignSelf(YogaAlign.valueOf(value.asString()!!.uppercase(Locale.getDefault())))
        "align-content" ->
            node.setAlignContent(
                YogaAlign.valueOf(value.asString()!!.uppercase(Locale.getDefault())))
        "position-type" ->
            node.setPositionType(
                YogaPositionType.valueOf(value.asString()!!.uppercase(Locale.getDefault())))
        "flex-grow" -> node.setFlexGrow(value.asFloat())
        "flex-shrink" -> node.setFlexShrink(value.asFloat())
        "flex-basis" -> node.setFlexBasis(YogaValue.parse(value.asString()))
        "width" -> node.setWidth(YogaValue.parse(value.asString()))
        "min-width" -> node.setMinWidth(YogaValue.parse(value.asString()))
        "max-width" -> node.setMaxWidth(YogaValue.parse(value.asString()))
        "height" -> node.setHeight(YogaValue.parse(value.asString()))
        "min-height" -> node.setMinHeight(YogaValue.parse(value.asString()))
        "max-height" -> node.setMaxHeight(YogaValue.parse(value.asString()))
        "aspect-ratio" -> node.setAspectRatio(value.asFloat())
        "margin" -> node.setMargin(edgeFromString(path[1]), YogaValue.parse(value.asString()))
        "padding" -> node.setPadding(edgeFromString(path[1]), YogaValue.parse(value.asString()))
        "border" -> node.setBorderWidth(edgeFromString(path[1]), value.asFloat())
        "position" -> node.setPosition(edgeFromString(path[1]), YogaValue.parse(value.asString()))
        "alpha" -> node.setAlpha(value.asFloat())
        "scale" -> node.setScale(value.asFloat())
        "rotation" -> node.setRotation(value.asFloat())
      }
    }

    private fun edgeFromString(s: String): YogaEdge {
      return YogaEdge.valueOf(s.uppercase(Locale.getDefault()))
    }

    // The path follows the pattern (Props|State)/field/(field|index)*
    private fun applyReflectiveOverride(
        o: Any,
        path: Array<String>,
        hint: FlipperValueHint?,
        dynamic: FlipperDynamic
    ) {
      try {
        val field = o.javaClass.getDeclaredField(path[1])
        FlipperEditor.updateComponent(path, field, o, hint, dynamic)
      } catch (ignored: Exception) {}
    }

    private fun fromFloat(f: Float): InspectorValue<*> {
      if (java.lang.Float.isNaN(f)) {
        return InspectorValue.mutable(InspectorValue.Type.Enum, "undefined")
      }
      return InspectorValue.mutable(InspectorValue.Type.Number, f)
    }

    fun fromYogaValue(v: YogaValue): InspectorValue<*> {
      // TODO add support for Type.Dimension or similar
      return InspectorValue.mutable(InspectorValue.Type.Enum, v.toString())
    }

    private val REVISION_DATE_FORMAT: DateFormat =
        SimpleDateFormat("hh:mm:ss.SSS", Locale.getDefault())
  }
}
