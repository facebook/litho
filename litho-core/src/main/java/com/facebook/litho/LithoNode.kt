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

import android.animation.StateListAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PathEffect
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Pair
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.facebook.infer.annotation.ThreadConfined
import com.facebook.litho.CommonProps.DefaultLayoutProps
import com.facebook.litho.Transition.TransitionKeyType
import com.facebook.litho.annotations.ImportantForAccessibility
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.rendercore.LayoutCache
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.Node
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.RenderUnit.DelegateBinder
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaMeasureFunction
import com.facebook.yoga.YogaNode
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap
import java.util.concurrent.atomic.AtomicInteger

/** [LithoNode] is the [Node] implementation of Litho. */
@ThreadConfined(ThreadConfined.ANY)
open class LithoNode : Node<LithoLayoutContext>, Cloneable {

  // region private properties
  private var _transitionId: TransitionId? = null
  private var _attachables: MutableList<Attachable>? = null
  private var debugComponents: MutableSet<DebugComponent>? = null
  private var _unresolvedComponents: MutableList<Component>? = null
  private var nestedTreeHolder: NestedTreeHolder? = null
  private var nestedPaddingEdges: Edges? = null
  private var nestedIsPaddingPercent: BooleanArray? = null
  private var debugLayoutProps: DefaultLayoutProps? = null
  private var _needsHostView: Boolean = false
  private var id: Int = idGenerator.getAndIncrement()
  private var frozen: Boolean = false
  private var nodeInfoWasWritten: Boolean = false
  // endregion

  // region Properties
  protected val borderEdgeWidths: IntArray = IntArray(Border.EDGE_COUNT)
  protected var paddingFromBackground: Rect? = null
  protected var layoutDirection: YogaDirection? = null
  protected var flexDirection: YogaFlexDirection? = null
  protected var justifyContent: YogaJustify? = null
  protected var alignContent: YogaAlign? = null
  protected var alignItems: YogaAlign? = null
  protected var yogaWrap: YogaWrap? = null
  protected var yogaMeasureFunction: YogaMeasureFunction? = null
  protected var privateFlags: Long = 0

  val borderColors: IntArray = IntArray(Border.EDGE_COUNT)
  val borderRadius: FloatArray = FloatArray(Border.RADIUS_COUNT)

  val childCount: Int
    get() = children.size

  val attachables: List<Attachable>?
    get() = _attachables

  val debugLayoutEditor: LayoutProps?
    get() {
      if (ComponentsConfiguration.isDebugModeEnabled && debugLayoutProps == null) {
        debugLayoutProps = DefaultLayoutProps()
      }
      return debugLayoutProps
    }

  val transitionGlobalKey: String
    get() = tailComponentKey

  val transitionId: TransitionId?
    get() {
      if (_transitionId == null) {
        LithoNodeUtils.createTransitionId(this)
      }
      return _transitionId
    }

  val unresolvedComponents: MutableList<Component>?
    get() = _unresolvedComponents

  val isImportantForAccessibilityIsSet: Boolean
    get() =
        (privateFlags and PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET == 0L ||
            importantForAccessibility == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO)

  val isLayoutDirectionInherit: Boolean
    get() = layoutDirection == null || layoutDirection == YogaDirection.INHERIT

  var primitive: Primitive? = null
  var children: MutableList<LithoNode> = ArrayList(4)

  var nodeInfo: NodeInfo? = null
    protected set

  var visibleHandler: EventHandler<VisibleEvent>? = null
    protected set

  var focusedHandler: EventHandler<FocusedVisibleEvent>? = null
    protected set

  var unfocusedHandler: EventHandler<UnfocusedVisibleEvent>? = null
    protected set

  var fullImpressionHandler: EventHandler<FullImpressionVisibleEvent>? = null
    protected set

  var invisibleHandler: EventHandler<InvisibleEvent>? = null
    protected set

  var visibilityChangedHandler: EventHandler<VisibilityChangedEvent>? = null
    protected set

  var background: Drawable? = null
    protected set

  var foreground: Drawable? = null
    protected set

  var borderPathEffect: PathEffect? = null
    protected set

  var stateListAnimator: StateListAnimator? = null
    protected set

  var transitionKey: String? = null
    protected set

  var transitionOwnerKey: String? = null
    protected set

  var transitionKeyType: TransitionKeyType? = null
    protected set

  var layerPaint: Paint? = null
    protected set

  var isPaddingSet: Boolean = false
    protected set

  var isDuplicateParentStateEnabled: Boolean = false
    protected set

  var isHostDuplicateParentStateEnabled: Boolean = false
    protected set

  var isDuplicateChildrenStatesEnabled: Boolean = false
    protected set

  var isForceViewWrapping: Boolean = false
    protected set

  var layerType: Int = LayerType.LAYER_TYPE_NOT_SET
    protected set

  var visibilityOutputTag: String? = null
    protected set

  var importantForAccessibility: Int = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
    protected set

  @DrawableRes
  var stateListAnimatorRes: Int = 0
    protected set

  var visibleHeightRatio: Float = 0f
    protected set

  var visibleWidthRatio: Float = 0f
    protected set

  /**
   * Returns a nullable map of [RenderUnit.Binder] that is aimed to be used to set the optional
   * mount binders right after creating a [MountSpecLithoRenderUnit]. These binders are meant to be
   * used only with [MountSpecLithoRenderUnit].
   */
  var customBindersForMountSpec: MutableMap<Class<*>, RenderUnit.Binder<Any, Any, Any>>? = null
    protected set

  /**
   * Returns a nullable map of [RenderUnit.DelegateBinder] that is aimed to be used to set the
   * optional mount binders right after creating a [MountSpecLithoRenderUnit].
   */
  var customDelegateBindersForMountSpec: MutableMap<Class<*>, DelegateBinder<Any, Any?, Any>>? =
      null
    protected set

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  var testKey: String? = null
    protected set

  var willMountView: Boolean = false
    private set

  var touchExpansion: Edges? = null
    private set

  var isClone: Boolean = false
    private set

  private var _transitions: ArrayList<Transition>? = null
  val transitions: ArrayList<Transition>?
    get() = _transitions

  private var _workingRangeRegistrations: MutableList<WorkingRangeContainer.Registration>? = null
  val workingRangeRegistrations: MutableList<WorkingRangeContainer.Registration>?
    get() = _workingRangeRegistrations

  @ThreadConfined(ThreadConfined.ANY)
  private val _scopedComponentInfos: MutableList<ScopedComponentInfo> = ArrayList(2)
  val simpleName: String
    get() = if (_scopedComponentInfos.isEmpty()) "<null>" else getComponentAt(0).simpleName

  val componentCount: Int
    get() = _scopedComponentInfos.size

  val scopedComponentInfos: List<ScopedComponentInfo>
    get() = _scopedComponentInfos

  val headComponent: Component
    get() = _scopedComponentInfos[_scopedComponentInfos.size - 1].component

  val headComponentKey: String
    get() =
        checkNotNull(_scopedComponentInfos[_scopedComponentInfos.size - 1].context.globalKey) {
          "Cannot have a null global key"
        }

  val headComponentContext: ComponentContext
    get() = _scopedComponentInfos[_scopedComponentInfos.size - 1].context

  val tailComponent: Component
    get() = _scopedComponentInfos[0].component

  val tailComponentKey: String
    get() =
        checkNotNull(_scopedComponentInfos[0].context.globalKey) { "Cannot have a null global key" }

  val tailComponentContext: ComponentContext
    get() = _scopedComponentInfos[0].context

  val tailScopedComponentInfo: ScopedComponentInfo
    get() = _scopedComponentInfos[0]

  private var _scopedComponentInfosNeedingPreviousRenderData:
      MutableMap<String, ScopedComponentInfo>? =
      null
  val scopedComponentInfosNeedingPreviousRenderData: Map<String, ScopedComponentInfo>?
    get() = _scopedComponentInfosNeedingPreviousRenderData
  // endregion

  override fun calculateLayout(
      context: LayoutContext<LithoLayoutContext>,
      sizeConstraints: SizeConstraints,
  ): LithoLayoutResult {

    val renderContext: LithoLayoutContext? = context.renderContext
    checkNotNull(renderContext) { "Cannot calculate a layout without RenderContext." }
    check(!renderContext.isReleased) {
      "Cannot calculate a layout with a released LayoutStateContext."
    }

    val isTracing: Boolean = ComponentsSystrace.isTracing

    applyOverridesRecursive(this)

    if (isTracing) {
      ComponentsSystrace.beginSection("buildYogaTree:${headComponent.simpleName}")
    }

    val layoutResult: LithoLayoutResult = buildYogaTree(context = context, currentNode = this)
    val yogaRoot: YogaNode = layoutResult.yogaNode

    if (isTracing) {
      ComponentsSystrace.endSection()
    }

    val widthSpec = sizeConstraints.toWidthSpec()
    val heightSpec = sizeConstraints.toHeightSpec()

    if (isLayoutDirectionInherit && Layout.isLayoutDirectionRTL(context.androidContext)) {
      yogaRoot.setDirection(YogaDirection.RTL)
    }
    if (YogaConstants.isUndefined(yogaRoot.width.value)) {
      Layout.setStyleWidthFromSpec(yogaRoot, widthSpec)
    }
    if (YogaConstants.isUndefined(yogaRoot.height.value)) {
      Layout.setStyleHeightFromSpec(yogaRoot, heightSpec)
    }

    val width: Float =
        if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
          YogaConstants.UNDEFINED
        } else {
          SizeSpec.getSize(widthSpec).toFloat()
        }
    val height: Float =
        if (SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
          YogaConstants.UNDEFINED
        } else {
          SizeSpec.getSize(heightSpec).toFloat()
        }

    if (isTracing) {
      ComponentsSystrace.beginSection("yogaCalculateLayout:${headComponent.simpleName}")
    }

    yogaRoot.calculateLayout(width, height)

    layoutResult.setSizeSpec(widthSpec, heightSpec)

    context.renderContext?.rootOffset =
        Point(
            yogaRoot.layoutX.toInt(),
            yogaRoot.layoutY.toInt(),
        )

    if (isTracing) {
      ComponentsSystrace.endSection()
    }

    return layoutResult
  }

  open fun applyDiffNode(
      current: LithoLayoutContext,
      result: LithoLayoutResult,
      parent: LithoLayoutResult?
  ) {
    if (current.isReleased) {
      return // Cannot apply diff nodes with a released LayoutStateContext
    }

    val diff: DiffNode =
        when {
          (parent == null) -> { // If root, then get diff node root from the current layout state
            if (Component.isLayoutSpecWithSizeSpec(headComponent) &&
                current.hasNestedTreeDiffNodeSet()) {
              current.consumeNestedTreeDiffNode()
            } else {
              current.currentDiffTree
            }
          }
          (parent.diffNode != null) -> { // Otherwise get it from the parent
            val parentDiffNode: DiffNode = parent.diffNode ?: return
            val index: Int = parent.node.getChildIndex(this)
            if (index != -1 && index < parentDiffNode.childCount) {
              parentDiffNode.getChildAt(index)
            } else {
              null
            }
          }
          else -> {
            null
          }
        } ?: return // Return if no diff node to apply.

    val component: Component = tailComponent
    if (!ComponentUtils.isSameComponentType(component, diff.component) &&
        !(parent != null && Component.isLayoutSpecWithSizeSpec(component))) {
      return
    }

    result.diffNode = diff

    val isTracing: Boolean = ComponentsSystrace.isTracing

    if (isTracing) {
      ComponentsSystrace.beginSection("shouldRemeasure:${headComponent.simpleName}")
    }

    val isPrimitiveBehaviorEquivalent: Boolean =
        allNotNull(primitive?.layoutBehavior, diff.primitive?.layoutBehavior) {
            layoutBehavior1,
            layoutBehavior2 ->
          layoutBehavior1.isEquivalentTo(layoutBehavior2)
        } == true

    if (isPrimitiveBehaviorEquivalent) {
      result.layoutData = diff.layoutData
      result.cachedMeasuresValid = true
    } else if (!Layout.shouldComponentUpdate(this, diff)) {
      val scopedComponentInfo = tailScopedComponentInfo
      val diffNodeScopedComponentInfo = checkNotNull(diff.scopedComponentInfo)
      if (component is SpecGeneratedComponent) {
        component.copyInterStageImpl(
            result.layoutData as InterStagePropsContainer?,
            diff.layoutData as InterStagePropsContainer?)
        component.copyPrepareInterStageImpl(
            scopedComponentInfo.prepareInterStagePropsContainer,
            diffNodeScopedComponentInfo.prepareInterStagePropsContainer)
      }
      result.cachedMeasuresValid = true
    }

    if (isTracing) {
      ComponentsSystrace.endSection()
    }
  }

  public override fun clone(): LithoNode {
    val node: LithoNode
    try {
      node = super.clone() as LithoNode
      node.isClone = true
      node.id = id
    } catch (e: CloneNotSupportedException) {
      throw RuntimeException(e)
    }
    return node
  }

  protected open fun createYogaNodeWriter(): YogaLayoutProps =
      YogaLayoutProps(NodeConfig.createYogaNode())

  open fun writeToYogaNode(writer: YogaLayoutProps) {
    val node: YogaNode = writer.node

    // Apply the extra layout props
    layoutDirection?.let { node.setDirection(it) }
    flexDirection?.let { node.flexDirection = it }
    justifyContent?.let { node.justifyContent = it }
    alignContent?.let { node.alignContent = it }
    alignItems?.let { node.alignItems = it }
    yogaWrap?.let { node.wrap = it }
    yogaMeasureFunction?.let { node.setMeasureFunction(it) }

    // Apply the layout props from the components to the YogaNode
    for (info in _scopedComponentInfos) {
      val component: Component = info.component

      // If a NestedTreeHolder is set then transfer its resolved props into this LithoNode.
      if (nestedTreeHolder != null && Component.isLayoutSpecWithSizeSpec(component)) {
        nestedTreeHolder?.transferInto(this)
        // TODO (T151239896): Revaluate copy into and freeze after common props are refactored
        _needsHostView = needsHostView(this)
        paddingFromBackground?.let { setPaddingFromDrawable(writer, it) }
      } else {
        info.commonProps?.let { props ->
          val styleAttr: Int = props.defStyleAttr
          val styleRes: Int = props.defStyleRes
          if (styleAttr != 0 || styleRes != 0) {
            val context: Context = tailComponentContext.androidContext
            val a =
                context.obtainStyledAttributes(
                    null, R.styleable.ComponentLayout, styleAttr, styleRes)
            applyLayoutStyleAttributes(writer, a)
            a.recycle()
          }

          // Set the padding from the background
          props.paddingFromBackground?.let { padding -> setPaddingFromDrawable(writer, padding) }

          // Copy the layout props into this LithoNode.
          props.copyLayoutProps(writer)
        }
      }
    }

    // Apply the border widths
    if (privateFlags and PFLAG_BORDER_IS_SET != 0L) {
      for (i in borderEdgeWidths.indices) {
        writer.setBorderWidth(Border.edgeFromIndex(i), borderEdgeWidths[i].toFloat())
      }
    }

    // Maybe apply the padding if parent is a Nested Tree Holder
    nestedPaddingEdges?.let { edges ->
      for (i in 0 until Edges.EDGES_LENGTH) {
        val value: Float = edges.getRaw(i)
        if (!YogaConstants.isUndefined(value)) {
          val edge: YogaEdge = YogaEdge.fromInt(i)
          if (nestedIsPaddingPercent?.get(edge.intValue()) != null) {
            writer.paddingPercent(edge, value)
          } else {
            writer.paddingPx(edge, value.toInt())
          }
        }
      }
    }

    debugLayoutProps?.copyInto(writer)
    isPaddingSet = writer.isPaddingSet
  }

  open fun createLayoutResult(
      node: YogaNode,
      widthFromStyle: Float,
      heightFromStyle: Float,
  ): LithoLayoutResult =
      LithoLayoutResult(
          context = tailComponentContext,
          node = this,
          yogaNode = node,
          widthFromStyle = widthFromStyle,
          heightFromStyle = heightFromStyle)

  open fun border(widths: IntArray, colors: IntArray, radii: FloatArray, effect: PathEffect?) {
    privateFlags = privateFlags or PFLAG_BORDER_IS_SET
    System.arraycopy(widths, 0, borderEdgeWidths, 0, borderEdgeWidths.size)
    System.arraycopy(colors, 0, borderColors, 0, borderColors.size)
    System.arraycopy(radii, 0, borderRadius, 0, borderRadius.size)
    borderPathEffect = effect
  }

  open fun background(background: Drawable? = null) {
    privateFlags = privateFlags or PFLAG_BACKGROUND_IS_SET
    this.background = background
  }

  open fun backgroundColor(@ColorInt backgroundColor: Int) {
    background(ComparableColorDrawable.create(backgroundColor))
  }

  open fun backgroundRes(context: Context, @DrawableRes resId: Int) {
    if (resId == 0) {
      background()
    } else {
      background(ContextCompat.getDrawable(context, resId))
    }
  }

  open fun border(border: Border) {
    border(border.edgeWidths, border.edgeColors, border.radius, border.pathEffect)
  }

  open fun foreground(foreground: Drawable? = null) {
    privateFlags = privateFlags or PFLAG_FOREGROUND_IS_SET
    this.foreground = foreground
  }

  open fun foregroundColor(@ColorInt foregroundColor: Int) {
    foreground(ComparableColorDrawable.create(foregroundColor))
  }

  open fun foregroundRes(context: Context, @DrawableRes resId: Int) {
    if (resId == 0) {
      foreground()
    } else {
      foreground(ContextCompat.getDrawable(context, resId))
    }
  }

  open fun importantForAccessibility(importantForAccessibility: Int): LithoNode {
    privateFlags = privateFlags or PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET
    this.importantForAccessibility = importantForAccessibility
    return this
  }

  open fun layoutDirection(direction: YogaDirection) {
    privateFlags = privateFlags or PFLAG_LAYOUT_DIRECTION_IS_SET
    layoutDirection = direction
  }

  open fun wrapInView() {
    isForceViewWrapping = true
  }

  @JvmOverloads
  fun applyParentDependentCommonProps(
      context: CalculationContext,
      parentImportantForAccessibility: Int =
          ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
      parentEnabledState: Int = NodeInfo.ENABLED_UNSET,
      parentDuplicatesParentState: Boolean = true
  ) {
    if (frozen) {
      return
    }

    val isRoot: Boolean = (context.rootComponentId == headComponent.id)

    if (!isRoot) { // if not root component

      // If parents important for A11Y is YES_HIDE_DESCENDANTS then
      // child's important for A11Y needs to be NO_HIDE_DESCENDANTS
      @Suppress("DEPRECATION")
      if (parentImportantForAccessibility ==
          ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES_HIDE_DESCENDANTS) {
        importantForAccessibility(
            ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
      }

      // If the parent of this node is disabled, this node has to be disabled too.
      if (parentEnabledState == NodeInfo.ENABLED_SET_FALSE) {
        mutableNodeInfo().setEnabled(false)
      }
    }

    isHostDuplicateParentStateEnabled = isDuplicateParentStateEnabled
    _needsHostView = needsHostView(this)

    // We need to take into account flattening when setting duplicate parent state. The parent after
    // flattening may no longer exist. Therefore the value of duplicate parent state should only be
    // true if the path between us (inclusive) and our inner/root host (exclusive) all are
    // duplicate parent state.

    val shouldDuplicateParentState: Boolean =
        _needsHostView || isRoot || (parentDuplicatesParentState && isDuplicateParentStateEnabled)

    duplicateParentState(shouldDuplicateParentState)

    _transitionId = LithoNodeUtils.createTransitionId(this)

    for (i in 0 until childCount) {
      getChildAt(i)
          .applyParentDependentCommonProps(
              context = context,
              parentImportantForAccessibility = importantForAccessibility,
              parentEnabledState = (nodeInfo?.enabledState ?: NodeInfo.ENABLED_UNSET),
              parentDuplicatesParentState = isDuplicateParentStateEnabled)
    }

    // Sets mFrozen as true to avoid anymore mutation.
    frozen = true
  }

  fun addComponentNeedingPreviousRenderData(
      globalKey: String,
      scopedComponentInfo: ScopedComponentInfo
  ) {
    _scopedComponentInfosNeedingPreviousRenderData
        .getOrCreate {
          HashMap<String, ScopedComponentInfo>(1).also {
            _scopedComponentInfosNeedingPreviousRenderData = it
          }
        }[globalKey] = scopedComponentInfo
  }

  fun addTransition(transition: Transition) {
    _transitions.getOrCreate { ArrayList<Transition>(1).also { _transitions = it } }.add(transition)
  }

  fun addWorkingRanges(registrations: List<WorkingRangeContainer.Registration>) {
    _workingRangeRegistrations
        .getOrCreate {
          ArrayList<WorkingRangeContainer.Registration>(registrations.size).also {
            _workingRangeRegistrations = it
          }
        }
        .addAll(registrations)
  }

  fun addAttachable(attachable: Attachable) {
    _attachables.getOrCreate { ArrayList<Attachable>(4).also { _attachables = it } }.add(attachable)
  }

  fun alignContent(alignContent: YogaAlign) {
    this.alignContent = alignContent
  }

  fun alignItems(alignItems: YogaAlign) {
    this.alignItems = alignItems
  }

  fun appendComponent(scopedComponentInfo: ScopedComponentInfo) {
    _scopedComponentInfos.add(scopedComponentInfo)
    if (_scopedComponentInfos.size == 1) {
      willMountView = willMountView(this)
    }
  }

  fun appendUnresolvedComponent(component: Component) {
    _unresolvedComponents
        .getOrCreate { ArrayList<Component>().also { _unresolvedComponents = it } }
        .add(component)
  }

  /**
   * The goal of this method is to add the optional mount binders to the associated to this
   * [LithoNode]. If we are dealing with a Primitive, we will get the corresponding [LithoNode] and
   * associate the binders map as optional mount binders. For this reason, this method should be
   * called as soon as their [RenderUnit] is created. In Litho, this happens in the Resolve phase,
   * specifically when the mount content preparation is invoked.
   *
   * For [MountSpecLithoRenderUnit] (e.g., the node is associated with a MountSpec, or the Primitive
   * mounts a Drawable and, therefore will need to be wrapped in a [ComponentHost] to work with the
   * view binders), the addition of the optional mount binders is delayed until the moment of its
   * creation. For that, we store these binders in the [LithoNode] and use them later.
   */
  fun addCustomBinders(
      bindersMap: Map<Class<*>, RenderUnit.Binder<Any, Any, Any>>? = null,
      delegateBindersMap: Map<Class<*>, DelegateBinder<Any, Any?, Any>>? = null
  ) {
    if (bindersMap.isNullOrEmpty() && delegateBindersMap.isNullOrEmpty()) {
      return
    }

    privateFlags = privateFlags or PFLAG_BINDER_IS_SET

    if (!willMountDrawable(this)) {
      allNotNull(primitive, bindersMap) { primitive, map ->
        for (binder in map.values) {
          primitive.renderUnit.addOptionalMountBinder(
              DelegateBinder.createDelegateBinder(primitive.renderUnit, binder))
        }
      }
      allNotNull(primitive, delegateBindersMap) { primitive, map ->
        for (binder in map.values) {
          primitive.renderUnit.addOptionalMountBinder(binder)
        }
      }
    }

    if (bindersMap != null) {
      customBindersForMountSpec
          .getOrCreate {
            LinkedHashMap<Class<*>, RenderUnit.Binder<Any, Any, Any>>().also {
              customBindersForMountSpec = it
            }
          }
          .putAll(bindersMap)
    }
    if (delegateBindersMap != null) {
      customDelegateBindersForMountSpec
          .getOrCreate {
            LinkedHashMap<Class<*>, DelegateBinder<Any, Any?, Any>>().also {
              customDelegateBindersForMountSpec = it
            }
          }
          .putAll(delegateBindersMap)
    }
  }

  fun child(resolveContext: ResolveContext, c: ComponentContext, child: Component?) {
    child?.let { child(Resolver.resolve(resolveContext, c, it)) }
  }

  fun child(child: LithoNode?) {
    child?.let { addChildAt(it, children.size) }
  }

  fun addChildAt(child: LithoNode, index: Int) {
    children.add(index, child)
  }

  fun getChildIndex(child: LithoNode): Int {
    for (i in 0 until children.size) {
      if (children[i] === child) {
        return i
      }
    }
    return -1
  }

  fun duplicateParentState(duplicateParentState: Boolean) {
    isDuplicateParentStateEnabled = duplicateParentState
  }

  fun duplicateChildrenStates(duplicateChildrenStates: Boolean) {
    privateFlags = privateFlags or PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET
    isDuplicateChildrenStatesEnabled = duplicateChildrenStates
  }

  fun flexDirection(direction: YogaFlexDirection) {
    flexDirection = direction
  }

  fun focusedHandler(focusedHandler: EventHandler<FocusedVisibleEvent>?) {
    privateFlags = privateFlags or PFLAG_FOCUSED_HANDLER_IS_SET
    this.focusedHandler = addVisibilityHandler(this.focusedHandler, focusedHandler)
  }

  fun layerType(@LayerType type: Int, paint: Paint?) {
    if (type != LayerType.LAYER_TYPE_NOT_SET) {
      layerType = type
      layerPaint = paint
    }
  }

  fun visibilityOutputTag(visibilityOutputTag: String?) {
    this.visibilityOutputTag = visibilityOutputTag
  }

  fun fullImpressionHandler(fullImpressionHandler: EventHandler<FullImpressionVisibleEvent>?) {
    privateFlags = privateFlags or PFLAG_FULL_IMPRESSION_HANDLER_IS_SET
    this.fullImpressionHandler =
        addVisibilityHandler(this.fullImpressionHandler, fullImpressionHandler)
  }

  fun getChildAt(index: Int): LithoNode = children[index]

  fun mutableNodeInfo(): NodeInfo {
    val nodeInfo: NodeInfo
    if (!nodeInfoWasWritten) {
      nodeInfoWasWritten = true
      nodeInfo = NodeInfo()
      this.nodeInfo?.copyInto(nodeInfo)
    } else {
      nodeInfo =
          this.nodeInfo
              // In theory this will not happen, just to avoid any lint warnings from static
              // analysis
              ?: NodeInfo()
    }
    this.nodeInfo = nodeInfo
    return nodeInfo
  }

  fun applyNodeInfo(nodeInfo: NodeInfo?) {
    if (nodeInfo != null) {
      if (nodeInfoWasWritten || this.nodeInfo != null) {
        nodeInfo.copyInto(mutableNodeInfo())
      } else {
        this.nodeInfo = nodeInfo
      }
    }
  }

  fun getComponentInfoAt(index: Int): ScopedComponentInfo = _scopedComponentInfos[index]

  fun getCommonPropsAt(index: Int): CommonProps? = getComponentInfoAt(index).commonProps

  fun getComponentContextAt(index: Int): ComponentContext = getComponentInfoAt(index).context

  fun getComponentAt(index: Int): Component = getComponentInfoAt(index).component

  fun getGlobalKeyAt(index: Int): String =
      checkNotNull(getComponentContextAt(index).globalKey) { "Cannot have a null global key" }

  fun setNestedPadding(padding: Edges?, isPercentage: BooleanArray?) {
    nestedPaddingEdges = padding
    nestedIsPaddingPercent = isPercentage
  }

  fun hasBorderColor(): Boolean {
    return borderColors.any { color -> color != Color.TRANSPARENT }
  }

  fun hasStateListAnimatorResSet(): Boolean =
      privateFlags and PFLAG_STATE_LIST_ANIMATOR_RES_SET != 0L

  fun hasTouchExpansion(): Boolean = privateFlags and PFLAG_TOUCH_EXPANSION_IS_SET != 0L

  fun hasTransitionKey(): Boolean = !transitionKey.isNullOrEmpty()

  fun hasVisibilityHandlers(): Boolean =
      visibleHandler != null ||
          focusedHandler != null ||
          unfocusedHandler != null ||
          fullImpressionHandler != null ||
          invisibleHandler != null ||
          visibilityChangedHandler != null

  fun invisibleHandler(invisibleHandler: EventHandler<InvisibleEvent>?): LithoNode {
    privateFlags = privateFlags or PFLAG_INVISIBLE_HANDLER_IS_SET
    this.invisibleHandler = addVisibilityHandler(this.invisibleHandler, invisibleHandler)
    return this
  }

  fun justifyContent(justifyContent: YogaJustify) {
    this.justifyContent = justifyContent
  }

  fun registerDebugComponent(debugComponent: DebugComponent) {
    if (debugComponents == null) {
      debugComponents = HashSet()
    }
    debugComponents?.add(debugComponent)
  }

  fun setMeasureFunction(measureFunction: YogaMeasureFunction) {
    yogaMeasureFunction = measureFunction
  }

  fun stateListAnimator(stateListAnimator: StateListAnimator?) {
    privateFlags = privateFlags or PFLAG_STATE_LIST_ANIMATOR_SET
    this.stateListAnimator = stateListAnimator
    wrapInView()
  }

  fun stateListAnimatorRes(@DrawableRes resId: Int) {
    privateFlags = privateFlags or PFLAG_STATE_LIST_ANIMATOR_RES_SET
    stateListAnimatorRes = resId
    wrapInView()
  }

  fun testKey(testKey: String?) {
    this.testKey = testKey
  }

  fun touchExpansionPx(edge: YogaEdge?, @Px touchExpansion: Int) {
    if (this.touchExpansion == null) {
      this.touchExpansion = Edges()
    }
    privateFlags = privateFlags or PFLAG_TOUCH_EXPANSION_IS_SET
    this.touchExpansion?.set(edge, touchExpansion.toFloat())
  }

  fun transitionKey(key: String?, ownerKey: String?) {
    if (!key.isNullOrEmpty()) {
      privateFlags = privateFlags or PFLAG_TRANSITION_KEY_IS_SET
      transitionKey = key
      transitionOwnerKey = ownerKey
    }
  }

  fun transitionKeyType(type: TransitionKeyType?) {
    privateFlags = privateFlags or PFLAG_TRANSITION_KEY_TYPE_IS_SET
    transitionKeyType = type
  }

  fun unfocusedHandler(unfocusedHandler: EventHandler<UnfocusedVisibleEvent>?) {
    privateFlags = privateFlags or PFLAG_UNFOCUSED_HANDLER_IS_SET
    this.unfocusedHandler = addVisibilityHandler(this.unfocusedHandler, unfocusedHandler)
  }

  fun visibilityChangedHandler(visibilityChangedHandler: EventHandler<VisibilityChangedEvent>?) {
    privateFlags = privateFlags or PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET
    this.visibilityChangedHandler =
        addVisibilityHandler(this.visibilityChangedHandler, visibilityChangedHandler)
  }

  fun visibleHandler(visibleHandler: EventHandler<VisibleEvent>?) {
    privateFlags = privateFlags or PFLAG_VISIBLE_HANDLER_IS_SET
    this.visibleHandler = addVisibilityHandler(this.visibleHandler, visibleHandler)
  }

  fun visibleHeightRatio(visibleHeightRatio: Float) {
    this.visibleHeightRatio = visibleHeightRatio
  }

  fun visibleWidthRatio(visibleWidthRatio: Float) {
    this.visibleWidthRatio = visibleWidthRatio
  }

  fun wrap(wrap: YogaWrap) {
    yogaWrap = wrap
  }

  fun applyAttributes(c: Context, @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int) {
    val a = c.obtainStyledAttributes(null, R.styleable.ComponentLayout, defStyleAttr, defStyleRes)
    for (i in 0 until a.indexCount) {
      when (val attr = a.getIndex(i)) {
        R.styleable.ComponentLayout_android_importantForAccessibility ->
            importantForAccessibility(a.getInt(attr, 0))
        R.styleable.ComponentLayout_android_duplicateParentState ->
            duplicateParentState(a.getBoolean(attr, false))
        R.styleable.ComponentLayout_android_background -> {
          if (TypedArrayUtils.isColorAttribute(a, R.styleable.ComponentLayout_android_background)) {
            backgroundColor(a.getColor(attr, 0))
          } else {
            backgroundRes(c, a.getResourceId(attr, -1))
          }
        }
        R.styleable.ComponentLayout_android_foreground -> {
          if (TypedArrayUtils.isColorAttribute(a, R.styleable.ComponentLayout_android_foreground)) {
            foregroundColor(a.getColor(attr, 0))
          } else {
            foregroundRes(c, a.getResourceId(attr, -1))
          }
        }
        R.styleable.ComponentLayout_android_contentDescription ->
            mutableNodeInfo().contentDescription = a.getString(attr)
      }
    }
    a.recycle()
  }

  /**
   * Note: Is only resolved after layout.
   *
   * @return `true` iff the node's out requires a host to wrap it
   */
  fun needsHostView(): Boolean {
    check(frozen) { "LithoNode:(${simpleName}) has not been resolved." }
    return _needsHostView
  }

  fun setNestedTreeHolder(holder: NestedTreeHolder?) {
    nestedTreeHolder = holder
  }

  fun resetDebugInfo() {
    debugComponents = null
  }

  private fun hasCustomBindersForMountSpec(): Boolean =
      (customBindersForMountSpec?.isNotEmpty() == true) ||
          (customDelegateBindersForMountSpec?.isNotEmpty() == true)

  companion object {
    private val idGenerator = AtomicInteger(1)

    // Flags used to indicate that a certain attribute was explicitly set on the node.
    private const val PFLAG_LAYOUT_DIRECTION_IS_SET: Long = 1L
    private const val PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET: Long = 1L shl 7
    protected const val PFLAG_BACKGROUND_IS_SET: Long = 1L shl 18
    protected const val PFLAG_FOREGROUND_IS_SET: Long = 1L shl 19
    protected const val PFLAG_VISIBLE_HANDLER_IS_SET: Long = 1L shl 20
    protected const val PFLAG_FOCUSED_HANDLER_IS_SET: Long = 1L shl 21
    protected const val PFLAG_FULL_IMPRESSION_HANDLER_IS_SET: Long = 1L shl 22
    protected const val PFLAG_INVISIBLE_HANDLER_IS_SET: Long = 1L shl 23
    protected const val PFLAG_UNFOCUSED_HANDLER_IS_SET: Long = 1L shl 24
    private const val PFLAG_TOUCH_EXPANSION_IS_SET: Long = 1L shl 25
    protected const val PFLAG_TRANSITION_KEY_IS_SET: Long = 1L shl 27
    protected const val PFLAG_BORDER_IS_SET: Long = 1L shl 28
    protected const val PFLAG_STATE_LIST_ANIMATOR_SET: Long = 1L shl 29
    protected const val PFLAG_STATE_LIST_ANIMATOR_RES_SET: Long = 1L shl 30
    protected const val PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET: Long = 1L shl 31
    protected const val PFLAG_TRANSITION_KEY_TYPE_IS_SET: Long = 1L shl 32
    protected const val PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET: Long = 1L shl 33
    protected const val PFLAG_BINDER_IS_SET: Long = 1L shl 34

    /**
     * Builds the YogaNode tree from this tree of LithoNodes. At the same time, builds the
     * LayoutResult tree and sets it in the data of the corresponding YogaNodes.
     */
    private fun buildYogaTree(
        context: LayoutContext<LithoLayoutContext>,
        currentNode: LithoNode,
        parentNode: YogaNode? = null,
    ): LithoLayoutResult {

      val isTracing: Boolean = ComponentsSystrace.isTracing
      var layoutResult: LithoLayoutResult? = null
      var yogaNode: YogaNode? = null

      if (currentNode.tailComponentContext.shouldCacheLayouts()) {
        val layoutCache: LayoutCache = context.layoutCache
        var cacheItem: LayoutCache.CacheItem? = layoutCache[currentNode]
        if (cacheItem != null) {
          val cachedLayoutResult: LayoutResult = cacheItem.layoutResult
          if (isTracing) {
            ComponentsSystrace.beginSection(
                "buildYogaTreeFromCache:${currentNode.headComponent.simpleName}")
          }

          // The situation that we can fully reuse the yoga tree
          val lithoLayoutResult =
              buildYogaTreeFromCache(context, cachedLayoutResult as LithoLayoutResult, isTracing)
          resetSizeIfNecessary(parentNode, lithoLayoutResult)
          if (isTracing) {
            ComponentsSystrace.endSection()
          }
          return lithoLayoutResult
        }

        cacheItem = layoutCache.get(currentNode.id.toLong())
        if (cacheItem != null) {
          val cachedLayoutResult: LayoutResult = cacheItem.layoutResult

          // The situation that we can partially reuse the yoga tree
          val clonedNode: YogaNode =
              (cachedLayoutResult as LithoLayoutResult).yogaNode.cloneWithoutChildren()
          yogaNode = clonedNode
          layoutResult = cachedLayoutResult.copyLayoutResult(currentNode, clonedNode)
          resetSizeIfNecessary(parentNode, layoutResult)
        }
      }

      if (layoutResult == null) {
        val writer: YogaLayoutProps = currentNode.createYogaNodeWriter()

        // return am empty layout result if the writer is null
        if (isTracing) {
          ComponentsSystrace.beginSection("createYogaNode:${currentNode.headComponent.simpleName}")
        }

        // Transfer the layout props to YogaNode
        currentNode.writeToYogaNode(writer)
        yogaNode = writer.node
        layoutResult =
            currentNode.createLayoutResult(
                node = yogaNode,
                widthFromStyle = writer.widthFromStyle,
                heightFromStyle = writer.heightFromStyle)

        if (isTracing) {
          ComponentsSystrace.endSection()
        }
      }

      checkNotNull(yogaNode) { "YogaNode cannot be null when building YogaTree." }
      val renderContext: LithoLayoutContext? = context.renderContext
      checkNotNull(renderContext) { "RenderContext cannot be null when building YogaTree." }

      yogaNode.data = Pair(context, layoutResult)
      applyDiffNode(renderContext, currentNode, yogaNode, parentNode)
      saveLithoLayoutResultIntoCache(context, currentNode, layoutResult)

      for (i in 0 until currentNode.childCount) {
        val childLayoutResult: LithoLayoutResult =
            buildYogaTree(
                context = context, currentNode = currentNode.getChildAt(i), parentNode = yogaNode)

        yogaNode.addChildAt(childLayoutResult.yogaNode, yogaNode.childCount)
        layoutResult.addChild(childLayoutResult)
      }

      return layoutResult
    }

    private fun buildYogaTreeFromCache(
        context: LayoutContext<LithoLayoutContext>,
        cachedLayoutResult: LithoLayoutResult,
        isTracing: Boolean
    ): LithoLayoutResult {
      if (isTracing) {
        ComponentsSystrace.beginSection(
            "cloneYogaNodeTree:${cachedLayoutResult.node.headComponent.simpleName}")
      }
      val clonedNode: YogaNode = cachedLayoutResult.yogaNode.cloneWithChildren()
      if (isTracing) {
        ComponentsSystrace.endSection()
      }
      return cloneLayoutResultsRecursively(context, cachedLayoutResult, clonedNode, isTracing)
    }

    private fun cloneLayoutResultsRecursively(
        context: LayoutContext<LithoLayoutContext>,
        cachedLayoutResult: LithoLayoutResult,
        clonedYogaNode: YogaNode,
        isTracing: Boolean
    ): LithoLayoutResult {
      if (isTracing) {
        ComponentsSystrace.beginSection("copyLayoutResult")
      }
      val node: LithoNode = cachedLayoutResult.node
      val result: LithoLayoutResult = cachedLayoutResult.copyLayoutResult(node, clonedYogaNode)
      clonedYogaNode.data = Pair<Any, Any>(context, result)
      saveLithoLayoutResultIntoCache(context, node, result)
      if (isTracing) {
        ComponentsSystrace.endSection()
      }
      for (i in 0 until cachedLayoutResult.childCount) {
        val child: LithoLayoutResult =
            cloneLayoutResultsRecursively(
                context, cachedLayoutResult.getChildAt(i), clonedYogaNode.getChildAt(i), isTracing)
        result.addChild(child)
      }
      return result
    }

    /**
     * Only add the YogaNode and LayoutResult if the node renders something. If it does not render
     * anything then it should not participate in grow/shrink behaviours.
     */
    private fun applyDiffNode(
        context: LithoLayoutContext,
        currentNode: LithoNode,
        currentYogaNode: YogaNode,
        parentYogaNode: YogaNode? = null,
    ) {
      val parentLayoutResult: LithoLayoutResult? =
          parentYogaNode?.let { LithoLayoutResult.getLayoutResultFromYogaNode(it) }
      val currentLayoutResult: LithoLayoutResult =
          LithoLayoutResult.getLayoutResultFromYogaNode(currentYogaNode)
      currentNode.applyDiffNode(context, currentLayoutResult, parentLayoutResult)
    }

    @JvmStatic
    protected fun applyLayoutStyleAttributes(props: YogaLayoutProps, a: TypedArray) {
      for (i in 0 until a.indexCount) {
        when (val attr = a.getIndex(i)) {
          R.styleable.ComponentLayout_android_layout_width -> {
            val width = a.getLayoutDimension(attr, -1)
            // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
            if (width >= 0) {
              props.widthPx(width)
            }
          }
          R.styleable.ComponentLayout_android_layout_height -> {
            val height = a.getLayoutDimension(attr, -1)
            // We don't support WRAP_CONTENT or MATCH_PARENT so no-op for them
            if (height >= 0) {
              props.heightPx(height)
            }
          }
          R.styleable.ComponentLayout_android_minHeight ->
              props.minHeightPx(a.getDimensionPixelSize(attr, 0))
          R.styleable.ComponentLayout_android_minWidth ->
              props.minWidthPx(a.getDimensionPixelSize(attr, 0))
          R.styleable.ComponentLayout_android_paddingLeft ->
              props.paddingPx(YogaEdge.LEFT, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_paddingTop ->
              props.paddingPx(YogaEdge.TOP, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_paddingRight ->
              props.paddingPx(YogaEdge.RIGHT, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_paddingBottom ->
              props.paddingPx(YogaEdge.BOTTOM, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_paddingStart ->
              props.paddingPx(YogaEdge.START, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_paddingEnd ->
              props.paddingPx(YogaEdge.END, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_padding ->
              props.paddingPx(YogaEdge.ALL, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_layout_marginLeft ->
              props.marginPx(YogaEdge.LEFT, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_layout_marginTop ->
              props.marginPx(YogaEdge.TOP, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_layout_marginRight ->
              props.marginPx(YogaEdge.RIGHT, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_layout_marginBottom ->
              props.marginPx(YogaEdge.BOTTOM, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_layout_marginStart ->
              props.marginPx(YogaEdge.START, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_layout_marginEnd ->
              props.marginPx(YogaEdge.END, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_android_layout_margin ->
              props.marginPx(YogaEdge.ALL, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_flex_direction ->
              props.flexDirection(YogaFlexDirection.fromInt(a.getInteger(attr, 0)))
          R.styleable.ComponentLayout_flex_wrap ->
              props.wrap(YogaWrap.fromInt(a.getInteger(attr, 0)))
          R.styleable.ComponentLayout_flex_justifyContent ->
              props.justifyContent(YogaJustify.fromInt(a.getInteger(attr, 0)))
          R.styleable.ComponentLayout_flex_alignItems ->
              props.alignItems(YogaAlign.fromInt(a.getInteger(attr, 0)))
          R.styleable.ComponentLayout_flex_alignSelf ->
              props.alignSelf(YogaAlign.fromInt(a.getInteger(attr, 0)))
          R.styleable.ComponentLayout_flex_positionType ->
              props.positionType(YogaPositionType.fromInt(a.getInteger(attr, 0)))
          R.styleable.ComponentLayout_flex_layoutDirection -> {
            val layoutDirection = a.getInteger(attr, -1)
            props.layoutDirection(YogaDirection.fromInt(layoutDirection))
          }
          R.styleable.ComponentLayout_flex -> {
            val flex = a.getFloat(attr, -1f)
            if (flex >= 0f) {
              props.flex(flex)
            }
          }
          R.styleable.ComponentLayout_flex_left ->
              props.positionPx(YogaEdge.LEFT, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_flex_top ->
              props.positionPx(YogaEdge.TOP, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_flex_right ->
              props.positionPx(YogaEdge.RIGHT, a.getDimensionPixelOffset(attr, 0))
          R.styleable.ComponentLayout_flex_bottom ->
              props.positionPx(YogaEdge.BOTTOM, a.getDimensionPixelOffset(attr, 0))
        }
      }
    }

    @JvmStatic
    protected fun setPaddingFromDrawable(target: YogaLayoutProps, padding: Rect) {
      target.paddingPx(YogaEdge.LEFT, padding.left)
      target.paddingPx(YogaEdge.TOP, padding.top)
      target.paddingPx(YogaEdge.RIGHT, padding.right)
      target.paddingPx(YogaEdge.BOTTOM, padding.bottom)
    }

    /**
     * This utility method checks if the {@param result} will mount a [View]. It returns true if and
     * only if the {@param result} will mount a [View]. If it returns `false` then the result will
     * either mount a [Drawable] or it is [NestedTreeHolderResult], which will not mount anything.
     *
     * @return `true` iff the result will mount a view.
     */
    @JvmStatic
    fun willMountView(node: LithoNode): Boolean =
        if (node.primitive?.renderUnit?.renderType == RenderUnit.RenderType.VIEW) {
          true
        } else {
          node.tailComponent.mountType == Component.MountType.VIEW
        }

    @JvmStatic
    fun willMountDrawable(node: LithoNode): Boolean =
        if (node.primitive?.renderUnit?.renderType == RenderUnit.RenderType.DRAWABLE) {
          true
        } else {
          node.tailComponent.mountType == Component.MountType.DRAWABLE
        }

    /**
     * Returns true if this is the root node (which always generates a matching layout output), if
     * the node has view attributes e.g. tags, content description, etc, or if the node has
     * explicitly been forced to be wrapped in a view.
     *
     * @param node The LithoNode to check
     */
    @JvmStatic
    fun needsHostView(node: LithoNode): Boolean {
      if (node.willMountView) {
        // Component already represents a View.
        return false
      }
      if (node.isForceViewWrapping) {
        // Wrapping into a View requested.
        return true
      }
      if (hasViewContent(node)) {
        // Has View content (e.g. Accessibility content, Focus change listener, shadow, view tag
        // etc) thus needs a host View.
        return true
      }
      if (needsHostViewForCommonDynamicProps(node)) {
        return true
      }
      if (needsHostViewForTransition(node)) {
        return true
      }
      if (hasSelectedStateWhenDisablingDrawableOutputs(node)) {
        return true
      }
      if (Component.isLayoutSpec(node.tailComponent) && node.hasCustomBindersForMountSpec()) {
        return true
      }
      return willMountDrawable(node) && node.hasCustomBindersForMountSpec()
    }

    @JvmStatic
    fun hasViewAttributes(nodeInfo: NodeInfo?): Boolean {
      if (nodeInfo == null) {
        return false
      }
      val hasFocusChangeHandler = nodeInfo.hasFocusChangeHandler()
      val hasEnabledTouchEventHandlers =
          nodeInfo.hasTouchEventHandlers() && nodeInfo.enabledState != NodeInfo.ENABLED_SET_FALSE
      val hasViewId = nodeInfo.hasViewId()
      val hasViewTag = nodeInfo.viewTag != null
      val hasViewTags = nodeInfo.viewTags != null
      val hasShadowElevation = nodeInfo.shadowElevation != 0f
      val hasAmbientShadowColor = nodeInfo.ambientShadowColor != Color.BLACK
      val hasSpotShadowColor = nodeInfo.spotShadowColor != Color.BLACK
      val hasOutlineProvider = nodeInfo.outlineProvider != null
      val hasClipToOutline = nodeInfo.clipToOutline
      val isFocusableSetTrue = nodeInfo.focusState == NodeInfo.FOCUS_SET_TRUE
      val isClickableSetTrue = nodeInfo.clickableState == NodeInfo.CLICKABLE_SET_TRUE
      val isKeyboardNavigationClusterSetTrue =
          nodeInfo.keyboardNavigationClusterState == NodeInfo.KEYBOARD_NAVIGATION_CLUSTER_SET_TRUE
      val hasClipChildrenSet = nodeInfo.isClipChildrenSet
      val hasTransitionName = nodeInfo.transitionName != null
      return hasFocusChangeHandler ||
          hasEnabledTouchEventHandlers ||
          hasViewId ||
          hasViewTag ||
          hasViewTags ||
          hasShadowElevation ||
          hasAmbientShadowColor ||
          hasSpotShadowColor ||
          hasOutlineProvider ||
          hasClipToOutline ||
          hasClipChildrenSet ||
          isFocusableSetTrue ||
          isClickableSetTrue ||
          isKeyboardNavigationClusterSetTrue ||
          hasTransitionName
    }

    private fun hasSelectedStateWhenDisablingDrawableOutputs(node: LithoNode): Boolean =
        ComponentContext.getComponentsConfig(node.headComponentContext)
            .shouldAddHostViewForRootComponent &&
            !node.willMountView &&
            node.nodeInfo != null &&
            node.nodeInfo?.selectedState != NodeInfo.SELECTED_UNSET

    /**
     * Similar to [LithoNode.needsHostView] but without dependency to [LayoutState] instance. This
     * will be used for debugging tools to indicate whether the mountable output is a wrapped View
     * or View MountSpec. Unlike [LithoNode.needsHostView] this does not consider accessibility also
     * does not consider root component, but this approximation is good enough for debugging
     * purposes.
     */
    @JvmStatic
    fun hasViewOutput(node: LithoNode): Boolean =
        node.isForceViewWrapping ||
            node.willMountView ||
            hasViewAttributes(node.nodeInfo) ||
            needsHostViewForCommonDynamicProps(node) ||
            needsHostViewForTransition(node)

    @JvmStatic
    fun needsHostViewForCommonDynamicProps(node: LithoNode): Boolean {
      val infos: List<ScopedComponentInfo> = node.scopedComponentInfos
      for (info in infos) {
        if (info.commonProps?.hasCommonDynamicProps() == true) {
          // Need a host View to apply the dynamic props to
          return true
        }
      }
      return false
    }

    @JvmStatic
    fun needsHostViewForTransition(node: LithoNode): Boolean =
        !node.transitionKey.isNullOrEmpty() && !node.willMountView

    /**
     * Determine if a given [LithoNode] within the context of a given [LayoutState] requires to be
     * wrapped inside a view.
     *
     * @see LithoNode.needsHostView
     */
    private fun hasViewContent(node: LithoNode): Boolean {
      val component: Component = node.tailComponent
      val nodeInfo: NodeInfo? = node.nodeInfo
      val implementsAccessibility =
          nodeInfo?.needsAccessibilityDelegate() == true ||
              (component is SpecGeneratedComponent && component.implementsAccessibility())
      val importantForAccessibility: Int = node.importantForAccessibility
      val c: ComponentContext = node.headComponentContext
      val context: CalculationContext? = c.calculationStateContext

      // A component has accessibility content if:
      //   1. Accessibility is currently enabled.
      //   2. Accessibility hasn't been explicitly disabled on it
      //      i.e. IMPORTANT_FOR_ACCESSIBILITY_NO.
      //   3. Any of these conditions are true:
      //      - It implements accessibility support.
      //      - It has a content description.
      //      - It has importantForAccessibility set as either IMPORTANT_FOR_ACCESSIBILITY_YES
      //        or IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS.
      // IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS should trigger an inner host
      // so that such flag is applied in the resulting view hierarchy after the component
      // tree is mounted. Click handling is also considered accessibility content but
      // this is already covered separately i.e. click handler is not null.
      val hasBackgroundOrForeground =
          ComponentContext.getComponentsConfig(c).shouldAddRootHostViewOrDisableBgFgOutputs &&
              (node.background != null || node.foreground != null)
      val hasAccessibilityContent =
          (context?.isAccessibilityEnabled == true) &&
              (importantForAccessibility != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO) &&
              (implementsAccessibility ||
                  !(nodeInfo?.contentDescription.isNullOrEmpty()) ||
                  (importantForAccessibility != ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO))
      return hasBackgroundOrForeground ||
          hasAccessibilityContent ||
          node.isDuplicateChildrenStatesEnabled ||
          hasViewAttributes(nodeInfo) ||
          node.layerType != LayerType.LAYER_TYPE_NOT_SET
    }

    private fun applyOverridesRecursive(node: LithoNode) {
      if (ComponentsConfiguration.isDebugModeEnabled) {
        DebugComponent.applyOverrides(node.tailComponentContext, node)
        for (i in 0 until node.childCount) {
          applyOverridesRecursive(node.getChildAt(i))
        }
      }
    }

    private fun <T> addVisibilityHandler(
        currentHandler: EventHandler<T>? = null,
        newHandler: EventHandler<T>? = null,
    ): EventHandler<T>? {
      if (currentHandler == null) {
        return newHandler
      }
      if (newHandler == null) {
        return currentHandler
      }
      return if (currentHandler is DelegatingEventHandler<*>) {
        val delegatingEventHandler = currentHandler as DelegatingEventHandler<T>
        delegatingEventHandler.addEventHandler(newHandler)
      } else {
        DelegatingEventHandler(currentHandler, newHandler)
      }
    }

    /** Save LithoLayoutResult into LayoutCache, using node itself and id as keys. */
    private fun saveLithoLayoutResultIntoCache(
        context: LayoutContext<LithoLayoutContext>,
        node: LithoNode,
        result: LithoLayoutResult
    ) {
      if (!node.tailComponentContext.shouldCacheLayouts()) {
        return
      }
      val layoutCache: LayoutCache = context.layoutCache
      // TODO(T163437982): Refactor this to build the cache after layout calculation
      val cacheItem = LayoutCache.CacheItem(result, -1, -1)
      layoutCache.put(node, cacheItem)
      layoutCache.put(node.id.toLong(), cacheItem)
    }

    // Since we could potentially change with/maxWidth and height/maxHeight, we should reset them to
    // default value before we re-measure with the latest size specs.
    // We don't need to reset the size if last measured size equals to the original specified size.
    private fun resetSizeIfNecessary(parent: YogaNode?, layoutResult: LithoLayoutResult) {
      if (parent != null) {
        return
      }
      val yogaNode: YogaNode = layoutResult.yogaNode
      if (layoutResult.widthFromStyle.compareTo(yogaNode.width.value) != 0) {
        yogaNode.setWidthAuto()
      }
      if (layoutResult.heightFromStyle.compareTo(yogaNode.height.value) != 0) {
        yogaNode.setHeightAuto()
      }
    }
  }
}
