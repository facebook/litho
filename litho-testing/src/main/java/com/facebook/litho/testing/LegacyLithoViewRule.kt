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

package com.facebook.litho.testing

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentTree
import com.facebook.litho.LayoutState
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoView
import com.facebook.litho.StateHandler
import com.facebook.litho.TreeProps
import com.facebook.litho.componentsfinder.findAllComponentsInLithoView
import com.facebook.litho.componentsfinder.findComponentInLithoView
import com.facebook.litho.componentsfinder.findDirectComponentInLithoView
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.viewtree.ViewPredicates
import com.facebook.litho.testing.viewtree.ViewTree
import com.facebook.rendercore.MountItemsPool
import com.google.common.base.Predicate
import kotlin.reflect.KClass
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * This test utility allows clients to test assertion on the view hierarchy rendered by a Litho
 * components. The utility has methods to override the default {@link LithoView}, {@link
 * ComponentTree}, width, and height specs.
 *
 * ```
 *  @RunWith(AndroidJUnit4::class)
 *  class LithoSampleTest {
 *
 *  @Rule @JvmField val lithoViewRule = LegacyLithoViewRule()
 *  @Test
 *  fun test() {
 *    lithoViewRule.render { TestComponent() }
 *
 *    // or you can use setRoot/measure/layout for more fine-grained control
 *    val lithoViewTest = lithoViewRule.attachToWindow().setRoot(TestComponent()).measure().layout()
 *    // Test your assertions on the litho view.
 *    }
 * }
 * ```
 */
@Deprecated("Please use LithoViewRule and TestLithoView instead")
class LegacyLithoViewRule
@JvmOverloads
constructor(
    val componentsConfiguration: ComponentsConfiguration? = null,
    val themeResId: Int? = null
) : TestRule {

  init {
    ComponentsConfiguration.isDebugModeEnabled = true
  }

  val componentTree: ComponentTree
    get() {
      if (_componentTree == null) {
        _componentTree =
            ComponentTree.create(context).componentsConfiguration(componentsConfiguration).build()
      }
      return _componentTree ?: throw AssertionError("Set to null by another thread")
    }

  val lithoView: LithoView
    get() {
      if (_lithoView == null) {
        _lithoView = LithoView(context)
      }

      if (_lithoView?.componentTree == null) {
        _lithoView?.componentTree = componentTree
      }
      return _lithoView ?: throw AssertionError("Set to null by another thread")
    }

  val committedLayoutState: LayoutState?
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    get() = componentTree.committedLayoutState

  val currentRootNode: LithoLayoutResult?
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    get() = committedLayoutState?.rootLayoutResult

  var widthSpec = DEFAULT_WIDTH_SPEC
  var heightSpec = DEFAULT_HEIGHT_SPEC
  lateinit var context: ComponentContext
  lateinit var stateHandler: StateHandler
  private var _lithoView: LithoView? = null
  private var _componentTree: ComponentTree? = null
  private var threadLooperController: BaseThreadLooperController = ThreadLooperController()
  private val interactionsScope = InteractionsScope()

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        ensureThreadLooperType()

        try {
          if (themeResId != null) {
            val activity = Robolectric.buildActivity(Activity::class.java).create().get()
            activity.setTheme(themeResId)
            context = ComponentContext(activity)
          } else {
            context = ComponentContext(getApplicationContext<Context>())
          }
          stateHandler = StateHandler()
          threadLooperController.init()
          base.evaluate()
        } finally {
          threadLooperController.clean()
          MountItemsPool.clear()
          context.clearCalculationStateContext()
          _componentTree = null
          _lithoView = null
          widthSpec = DEFAULT_WIDTH_SPEC
          heightSpec = DEFAULT_HEIGHT_SPEC
        }
      }
    }
  }

  private fun ensureThreadLooperType() {
    if (ComponentsConfiguration.isSplitResolveAndLayoutWithSplitHandlers() &&
        threadLooperController is ThreadLooperController) {
      threadLooperController = ResolveAndLayoutThreadLooperController()
    } else if (!ComponentsConfiguration.isSplitResolveAndLayoutWithSplitHandlers() &&
        threadLooperController is ResolveAndLayoutThreadLooperController) {
      threadLooperController = ThreadLooperController()
    }
  }

  fun useContext(c: ComponentContext): LegacyLithoViewRule {
    context = c
    return this
  }

  /** Sets a new [LithoView] which should be used to render. */
  fun useLithoView(lithoView: LithoView): LegacyLithoViewRule {
    _lithoView = lithoView
    if (lithoView.componentContext !== context) {
      throw RuntimeException(
          "You must use the same ComponentContext for the LithoView as what is on the LegacyLithoViewRule @Rule!")
    }
    lithoView.componentTree.let {
      if (it == null && _componentTree != null) {
        lithoView.componentTree = _componentTree
      } else {
        _componentTree = it
      }
    }
    return this
  }

  /** Sets a new [ComponentTree] which should be used to render. */
  fun useComponentTree(componentTree: ComponentTree?): LegacyLithoViewRule {
    _componentTree = componentTree
    lithoView.componentTree = componentTree
    return this
  }

  /** Sets the new root [Component] to render. */
  fun setRoot(component: Component?): LegacyLithoViewRule {
    componentTree.setRoot(component)
    return this
  }

  /** Sets the new root [Component.Builder] to render. */
  fun setRoot(builder: Component.Builder<*>): LegacyLithoViewRule {
    componentTree.setRoot(builder.build())
    return this
  }

  /** Sets the new root [Component] to render asynchronously. */
  fun setRootAsync(component: Component?): LegacyLithoViewRule {
    componentTree.setRootAsync(component)
    return this
  }

  /** Sets the new root [Component.Builder] to render asynchronously. */
  fun setRootAsync(builder: Component.Builder<*>): LegacyLithoViewRule {
    componentTree.setRootAsync(builder.build())
    return this
  }

  /** Sets the new root [Component] with new size spec to render. */
  fun setRootAndSizeSpecSync(
      component: Component?,
      widthSpec: Int,
      heightSpec: Int
  ): LegacyLithoViewRule {
    this.widthSpec = widthSpec
    this.heightSpec = heightSpec
    componentTree.setRootAndSizeSpecSync(component, this.widthSpec, this.heightSpec)
    return this
  }

  /** Sets a new width and height which should be used to render. */
  fun setSizePx(widthPx: Int, heightPx: Int): LegacyLithoViewRule {
    widthSpec = MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY)
    heightSpec = MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY)
    return this
  }

  /** Sets a new width spec and height spec which should be used to render. */
  fun setSizeSpecs(widthSpec: Int, heightSpec: Int): LegacyLithoViewRule {
    this.widthSpec = widthSpec
    this.heightSpec = heightSpec
    return this
  }

  /** Sets a new [TreeProp] for the next layout pass. */
  fun setTreeProp(klass: Class<*>, instance: Any?): LegacyLithoViewRule {
    val props = context.treeProps ?: TreeProps()
    props.put(klass, instance)
    context.treeProps = props
    return this
  }

  /** Explicitly calls measure on the current root [LithoView] */
  fun measure(): LegacyLithoViewRule {
    lithoView.measure(widthSpec, heightSpec)
    return this
  }

  /**
   * Explicitly calls layout on the current root [LithoView]. If there are any async events
   * triggered by layout use together with [idle]
   */
  fun layout(): LegacyLithoViewRule {
    val lithoView: LithoView = lithoView
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return this
  }

  fun dispatchGlobalLayout(): LegacyLithoViewRule {
    lithoView.notifyVisibleBoundsChanged()

    return this
  }

  /** Explicitly attaches current root [LithoView] */
  fun attachToWindow(): LegacyLithoViewRule {
    lithoView.onAttachedToWindowForTest()
    return this
  }

  /** Explicitly detaches current root [LithoView] */
  fun detachFromWindow(): LegacyLithoViewRule {
    lithoView.onDetachedFromWindowForTest()
    return this
  }

  /** Explicitly releases current root [LithoView] */
  fun release(): LegacyLithoViewRule {
    lithoView.release()
    return this
  }

  /** Sets the new root to render. */
  fun render(componentFunction: ComponentScope.() -> Component?) {
    attachToWindow()
        .setRoot(with(ComponentScope(context)) { componentFunction() })
        .measure()
        .layout()
  }

  /**
   * Finds the first [View] with the specified tag in the rendered hierarchy, returning null if is
   * doesn't exist.
   */
  fun findViewWithTagOrNull(tag: Any): View? {
    return findViewWithTagTransversal(lithoView, tag)
  }

  /**
   * Finds the first [View] with the specified tag in the rendered hierarchy, throwing if it doesn't
   * exist.
   */
  fun findViewWithTag(tag: Any): View {
    return findViewWithTagOrNull(tag) ?: throw RuntimeException("Did not find view with tag '$tag'")
  }

  private fun findViewWithTagTransversal(view: View?, tag: Any): View? {
    if (view == null || view.tag == tag) {
      return view
    }
    if (view is ViewGroup) {
      for (i in 0..view.childCount) {
        val child = findViewWithTagTransversal(view.getChildAt(i), tag)
        if (child != null) {
          return child
        }
      }
    }
    return null
  }

  /**
   * Finds the first [View] with the specified text in the rendered hierarchy, returning null if is
   * doesn't exist.
   */
  fun findViewWithTextOrNull(text: String): View? {
    val viewTree = ViewTree.of(lithoView)
    return findViewWithPredicateOrNull(viewTree, ViewPredicates.hasVisibleText(text))
  }

  /**
   * Finds the first [View] with the specified text in the rendered hierarchy, throwing if it
   * doesn't exist.
   */
  fun findViewWithText(text: String): View {
    return findViewWithTextOrNull(text)
        ?: throw RuntimeException("Did not find view with text '$text'")
  }
  /**
   * Finds the first [View] with the specified content description in the rendered hierarchy,
   * returning null if is doesn't exist.
   */
  fun findViewWithContentDescriptionOrNull(contentDescription: String): View? {
    val viewTree = ViewTree.of(lithoView)
    return findViewWithPredicateOrNull(
        viewTree, ViewPredicates.hasContentDescription(contentDescription))
  }

  /**
   * Finds the first [View] with the specified content description in the rendered hierarchy,
   * throwing if it doesn't exist.
   */
  fun findViewWithContentDescription(contentDescription: String): View {
    return findViewWithContentDescriptionOrNull(contentDescription)
        ?: throw RuntimeException("Did not find view with contentDescription '$contentDescription'")
  }

  /**
   * Finds the first [View] with the specified text in the rendered hierarchy, returning null if is
   * doesn't exist.
   */
  fun findViewWithTextOrNull(@StringRes resourceId: Int): View? {
    val viewTree = ViewTree.of(lithoView)
    val text = getApplicationContext<Context>().resources.getString(resourceId)
    return findViewWithPredicateOrNull(viewTree, ViewPredicates.hasVisibleText(text))
  }

  /**
   * Finds the first [View] with the specified text in the rendered hierarchy, throwing if it
   * doesn't exist.
   */
  fun findViewWithText(@StringRes resourceId: Int): View =
      findViewWithText(getApplicationContext<Context>().resources.getString(resourceId))

  /**
   * Finds the first [View] with the specified content description in the rendered hierarchy,
   * returning null if is doesn't exist.
   */
  fun findViewWithContentDescriptionOrNull(@StringRes resourceId: Int): View? {
    val viewTree = ViewTree.of(lithoView)
    val contentDescription = getApplicationContext<Context>().resources.getString(resourceId)
    return findViewWithPredicateOrNull(
        viewTree, ViewPredicates.hasContentDescription(contentDescription))
  }

  /**
   * Finds the first [View] with the specified content description in the rendered hierarchy,
   * throwing if it doesn't exist.
   */
  fun findViewWithContentDescription(@StringRes resourceId: Int): View =
      findViewWithContentDescription(
          getApplicationContext<Context>().resources.getString(resourceId))

  /** Returns a component of the given class only if it is a direct child of the root component */
  fun findDirectComponent(clazz: KClass<out Component>): Component? {
    return findDirectComponentInLithoView(lithoView, clazz)
  }

  /** Returns a component of the given class only if it is a direct child of the root component */
  fun findDirectComponent(clazz: Class<out Component?>): Component? {
    return findDirectComponentInLithoView(lithoView, clazz)
  }

  /** Returns a component of the given class from the ComponentTree or null if not found */
  fun findComponent(clazz: KClass<out Component>): Component? {
    return findComponentInLithoView(lithoView, clazz)
  }

  /** Returns a component of the given class from the ComponentTree or null if not found */
  fun findComponent(clazz: Class<out Component?>): Component? {
    return findComponentInLithoView(lithoView, clazz)
  }

  /**
   * Returns a list of all components of the given classes from the ComponentTree or an empty list
   * if not found
   */
  fun findAllComponents(vararg clazz: KClass<out Component>): List<Component> {
    return findAllComponentsInLithoView(lithoView, *clazz)
  }

  /**
   * Returns a list of all components of the given classes from the ComponentTree or an empty list
   * if not found
   */
  fun findAllComponents(vararg clazz: Class<out Component?>): List<Component> {
    return findAllComponentsInLithoView(lithoView, *clazz)
  }

  private fun findViewWithPredicateOrNull(viewTree: ViewTree, predicate: Predicate<View>): View? {
    return viewTree.findChild(predicate)?.last()
  }

  /**
   * Perform any interactions defined in the [InteractionScope] or on the [LegacyLithoViewRule].
   *
   * During tests we need to make sure that everything is in sync in the Main Thread and in the
   * Background Thread, just like in real life use case. This functions takes off the responsibility
   * from you to use the Loopers and manage the thread synchronisation. You only need to pass here
   * one of the defined interactions from [LegacyLithoViewRule] or [InteractionScope], and we will
   * take care of all of the rest
   */
  fun act(action: InteractionsScope.() -> Unit): LegacyLithoViewRule {
    interactionsScope.action()
    idle()
    return this
  }

  /**
   * Runs through all tasks on the background thread and main lopper, blocking until it completes.
   * Use if there are any async events triggered by layout ( ie visibility events) to manually drain
   * the queue
   */
  fun idle() {
    runToEndOfBackgroundTasks()
    shadowOf(Looper.getMainLooper()).idle()
  }

  /**
   * Runs through all tasks on the background thread only, not touching the main lopper, blocking
   * until it completes.
   */
  fun runToEndOfBackgroundTasks() {
    threadLooperController.runToEndOfTasksSync()
  }

  /**
   * Class which exposes interactions that can take place on a view. Exposing interactions in this
   * class ensures that they are only accessible within [act], where the proper threading is taken
   * into account to properly update the components and views.
   */
  inner class InteractionsScope internal constructor() {
    /**
     * Clicks on a [View] with the specified text in the rendered hierarchy, throwing if the view
     * doesn't exists
     */
    fun clickOnText(text: String): Boolean = findViewWithText(text).performClick()

    /**
     * Clicks on a [View] with the specified tag in the rendered hierarchy, throwing if the view
     * doesn't exists
     */
    fun clickOnTag(tag: String): Boolean = findViewWithTag(tag).performClick()

    /**
     * Clicks on a [View] with the specified tag in the rendered hierarchy, throwing if the view
     * doesn't exists
     */
    fun clickOnContentDescription(contentDescription: String): Boolean =
        findViewWithContentDescription(contentDescription).performClick()

    /**
     * Clicks on the root view, if it has click handling, throwing if the view is not clickable.
     *
     * See other functions such as [clickOnText], [clickOnTag], and [clickOnContentDescription] if
     * you'd like more specifically click on portions of the view.
     */
    fun clickOnRootView() {
      checkNotNull(shadowOf(lithoView).onClickListener) {
        "No click handling found on root view.  The root view must be clickable in order to use this function."
      }
      lithoView.performClick()
    }

    /**
     * Clicks on a [View] with the specified text in the rendered hierarchy, throwing if the view
     * doesn't exists
     */
    fun clickOnText(@StringRes resourceId: Int): Boolean =
        findViewWithText(resourceId).performClick()

    /**
     * Clicks on a [View] with the specified tag in the rendered hierarchy, throwing if the view
     * doesn't exists
     */
    fun clickOnTag(@StringRes resourceId: Int): Boolean = findViewWithTag(resourceId).performClick()

    /**
     * Clicks on a [View] with the specified tag in the rendered hierarchy, throwing if the view
     * doesn't exists
     */
    fun clickOnContentDescription(@StringRes resourceId: Int): Boolean =
        findViewWithContentDescription(resourceId).performClick()
  }

  companion object {
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmStatic
    fun getRootLayout(
        rule: LegacyLithoViewRule,
        component: Component?,
        widthSpec: Int,
        heightSpec: Int
    ): LithoLayoutResult? {
      return rule
          .attachToWindow()
          .setRootAndSizeSpecSync(component, widthSpec, heightSpec)
          .measure()
          .layout()
          .currentRootNode
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmStatic
    fun getRootLayout(rule: LegacyLithoViewRule, component: Component?): LithoLayoutResult? {
      return rule.attachToWindow().setRoot(component).measure().layout().currentRootNode
    }
  }
}
