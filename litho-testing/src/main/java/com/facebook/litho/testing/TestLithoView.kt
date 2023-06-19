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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LayoutState
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoLifecycleProvider
import com.facebook.litho.LithoView
import com.facebook.litho.componentsfinder.findAllComponentsInLithoView
import com.facebook.litho.componentsfinder.findComponentInLithoView
import com.facebook.litho.componentsfinder.findDirectComponentInLithoView
import com.facebook.litho.componentsfinder.getRootComponentInLithoView
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.viewtree.ViewPredicates
import com.facebook.litho.testing.viewtree.ViewTree
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.collection.LazyCollection
import com.google.common.base.Predicate
import kotlin.reflect.KClass
import org.robolectric.Shadows

/**
 * Holder class for the result of [LithoViewRule.render] call, exposing methods to for finding the
 * views/components and assertions
 *
 * ```
 *  @RunWith(AndroidJUnit4::class)
 *  class LithoSampleTest {
 *
 *  @Rule @JvmField val lithoViewRule = LithoViewRule()
 *  @Test
 *  fun test() {
 *   val testLithoView =  lithoViewRule.render { TestComponent() }
 *
 *   LithoAssertions.assertThat(testLithoView)
 *   .willRenderContent()
 *   .containsComponents(AnotherTestComponent::class)
 *   .containsContentDescription(R.string.content_descr)
 *
 *   lithoViewRule.act(testLithoView) { clickOnTag("test_tag") }
 *
 *   LithoAssertions.assertThat(testLithoView)
 *   .containsComponents(NewTestComponent::class)
 *
 *    }
 * }
 * ```
 */
class TestLithoView
internal constructor(
    val context: ComponentContext,
    val componentsConfiguration: ComponentsConfiguration? = null,
    private val lithoLifecycleProvider: LithoLifecycleProvider? = null
) {
  val componentTree: ComponentTree
    get() {
      if (_componentTree == null) {
        _componentTree =
            ComponentTree.create(context)
                .withLithoLifecycleProvider(lithoLifecycleProvider)
                .componentsConfiguration(componentsConfiguration)
                .build()
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

  val rootComponent: Component?
    get() = getRootComponentInLithoView(lithoView)

  val committedLayoutState: LayoutState?
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    get() = componentTree.committedLayoutState

  val currentRootNode: LithoLayoutResult?
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    get() = committedLayoutState?.rootLayoutResult

  private var widthSpec = DEFAULT_WIDTH_SPEC
  private var heightSpec = DEFAULT_HEIGHT_SPEC
  private var _lithoView: LithoView? = null
  private var _componentTree: ComponentTree? = null

  /** Sets a new [LithoView] which should be used to render. */
  fun useLithoView(lithoView: LithoView): TestLithoView {
    _lithoView = lithoView
    if (lithoView.componentContext !== context) {
      throw RuntimeException(
          "You must use the same ComponentContext for the LithoView as what is on the LithoViewRule @Rule!")
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
  fun useComponentTree(componentTree: ComponentTree?): TestLithoView {
    _componentTree = componentTree
    lithoView.componentTree = componentTree
    return this
  }
  /** Sets the new root [Component] to render. */
  fun setRoot(component: Component?): TestLithoView {
    componentTree.setRoot(component)
    return this
  }

  /** Sets the new root [Component.Builder] to render. */
  fun setRoot(builder: Component.Builder<*>): TestLithoView {
    componentTree.setRoot(builder.build())
    return this
  }

  /** Sets the new root [Component] to render asynchronously. */
  fun setRootAsync(component: Component?): TestLithoView {
    componentTree.setRootAsync(component)
    return this
  }

  /** Sets the new root [Component.Builder] to render asynchronously. */
  fun setRootAsync(builder: Component.Builder<*>): TestLithoView {
    componentTree.setRootAsync(builder.build())
    return this
  }

  /** Sets the new root [Component] with new size spec to render. */
  fun setRootAndSizeSpecSync(
      component: Component?,
      widthSpec: Int,
      heightSpec: Int
  ): TestLithoView {
    this.widthSpec = widthSpec
    this.heightSpec = heightSpec
    componentTree.setRootAndSizeSpecSync(component, this.widthSpec, this.heightSpec)
    return this
  }

  /** Sets a new width and height which should be used to render. */
  fun setSizePx(widthPx: Int, heightPx: Int): TestLithoView {
    widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
    heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
    return this
  }

  /** Sets a new width spec and height spec which should be used to render. */
  fun setSizeSpecs(widthSpec: Int, heightSpec: Int): TestLithoView {
    this.widthSpec = widthSpec
    this.heightSpec = heightSpec
    return this
  }
  /** Explicitly calls measure on the current root [LithoView] */
  fun measure(): TestLithoView {
    lithoView.measure(widthSpec, heightSpec)
    return this
  }

  /**
   * Explicitly calls layout on the current root [LithoView]. If there are any async events
   * triggered by layout use together with [idle]
   */
  fun layout(): TestLithoView {
    val lithoView: LithoView = lithoView
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return this
  }

  /** Explicitly attaches current root [LithoView] */
  fun attachToWindow(): TestLithoView {
    lithoView.onAttachedToWindowForTest()
    return this
  }

  /** Explicitly detaches current root [LithoView] */
  fun detachFromWindow(): TestLithoView {
    lithoView.onDetachedFromWindowForTest()
    return this
  }

  fun markAsNeedsRemount() {
    lithoView.setMountStateAsNeedingRemount()
  }

  fun setComponentTree(componentTree: ComponentTree) {
    lithoView.componentTree = componentTree
  }

  /** Explicitly releases current root [LithoView] */
  fun release(): TestLithoView {
    lithoView.release()
    return this
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

  /** Returns the first [LazyCollection] from the ComponentTree, or null if not found. */
  fun findCollectionComponent(): TestCollection? {
    val recycler = findComponent(Recycler::class.java) as Recycler? ?: return null
    return TestCollection(recycler)
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
      checkNotNull(Shadows.shadowOf(lithoView).onClickListener) {
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
}

@JvmField
val DEFAULT_WIDTH_SPEC: Int = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
@JvmField
val DEFAULT_HEIGHT_SPEC: Int = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
