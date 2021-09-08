/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.ComponentsPools
import com.facebook.litho.LayoutState
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.LithoView
import com.facebook.litho.TreeProps
import com.facebook.litho.config.ComponentsConfiguration
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@JvmField val DEFAULT_WIDTH_SPEC: Int = MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY)
@JvmField val DEFAULT_HEIGHT_SPEC: Int = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

/**
 * This test utility allows clients to test assertion on the view hierarchy rendered by a Litho
 * components. The utility has methods to override the default {@link LithoView}, {@link
 * ComponentTree}, width, and height specs.
 *
 * ```
 * @RunWith(LithoTestRunner.class)
 * public class LithoSampleTest {
 *
 *  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
 *
 *  @Test
 *  public void test() {
 *     final ComponentContext c = mLithoViewRule.getContext();
 *     final Component component = MyComponent.create(c).build();
 *
 *     mLithoViewRule.setRoot(component)
 *       .attachToWindow()
 *       .setRoot(component)
 *       .measure()
 *       .layout();
 *
 *     LithoView lithoView = mLithoViewRule.getLithoView();
 *
 *     // Test your assertions on the litho view.
 *  }
 * }
 *
 * ```
 */
class LithoViewRule(val componentsConfiguration: ComponentsConfiguration? = null) : TestRule {
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
    get() = componentTree?.committedLayoutState
  val currentRootNode: LithoLayoutResult?
    @VisibleForTesting(otherwise = VisibleForTesting.NONE) get() = committedLayoutState?.layoutRoot
  var widthSpec = DEFAULT_WIDTH_SPEC
  var heightSpec = DEFAULT_HEIGHT_SPEC
  lateinit var context: ComponentContext
  private var _lithoView: LithoView? = null
  private var _componentTree: ComponentTree? = null

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        try {
          context = ComponentContext(getApplicationContext<Context>())
          context.setLayoutStateContextForTesting()
          base.evaluate()
        } finally {
          ComponentsPools.clearMountContentPools()
          _componentTree = null
          _lithoView = null
          widthSpec = DEFAULT_WIDTH_SPEC
          heightSpec = DEFAULT_HEIGHT_SPEC
        }
      }
    }
  }

  fun useContext(c: ComponentContext): LithoViewRule {
    context = c
    return this
  }

  /** Sets a new [LithoView] which should be used to render. */
  fun useLithoView(lithoView: LithoView): LithoViewRule {
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
  fun useComponentTree(componentTree: ComponentTree?): LithoViewRule {
    _componentTree = componentTree
    lithoView.componentTree = componentTree
    return this
  }

  /** Sets the new root [Component] to render. */
  fun setRoot(component: Component?): LithoViewRule {
    componentTree.setRoot(component)
    return this
  }

  /** Sets the new root [Component.Builder] to render. */
  fun setRoot(builder: Component.Builder<*>): LithoViewRule {
    componentTree.setRoot(builder.build())
    return this
  }

  /** Sets the new root [Component] to render asynchronously. */
  fun setRootAsync(component: Component?): LithoViewRule {
    componentTree.setRootAsync(component)
    return this
  }

  /** Sets the new root [Component.Builder] to render asynchronously. */
  fun setRootAsync(builder: Component.Builder<*>): LithoViewRule {
    componentTree.setRootAsync(builder.build())
    return this
  }

  /** Sets the new root [Component] with new size spec to render. */
  fun setRootAndSizeSpec(component: Component?, widthSpec: Int, heightSpec: Int): LithoViewRule {
    this.widthSpec = widthSpec
    this.heightSpec = heightSpec
    componentTree.setRootAndSizeSpec(component, this.widthSpec, this.heightSpec)
    return this
  }

  /** Sets a new width and height which should be used to render. */
  fun setSizePx(widthPx: Int, heightPx: Int): LithoViewRule {
    widthSpec = MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY)
    heightSpec = MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY)
    return this
  }

  /** Sets a new width spec and height spec which should be used to render. */
  fun setSizeSpecs(widthSpec: Int, heightSpec: Int): LithoViewRule {
    this.widthSpec = widthSpec
    this.heightSpec = heightSpec
    return this
  }

  /** Sets a new [TreeProp] for the next layout pass. */
  fun setTreeProp(klass: Class<*>, instance: Any?): LithoViewRule {
    val props = context.treeProps ?: TreeProps()
    props.put(klass, instance)
    context.treeProps = props
    return this
  }

  /** Explicitly calls measure on the current root [LithoView] */
  fun measure(): LithoViewRule {
    lithoView.measure(widthSpec, heightSpec)
    return this
  }

  /** Explicitly calls layout on the current root [LithoView] */
  fun layout(): LithoViewRule {
    val lithoView: LithoView = lithoView
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return this
  }

  /** Explicitly attaches current root [LithoView] */
  fun attachToWindow(): LithoViewRule {
    lithoView.onAttachedToWindowForTest()
    return this
  }

  /** Explicitly detaches current root [LithoView] */
  fun detachFromWindow(): LithoViewRule {
    lithoView.onDetachedFromWindowForTest()
    return this
  }

  /** Explicitly releases current root [LithoView] */
  fun release(): LithoViewRule {
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

  companion object {
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmStatic
    fun getRootLayout(
        rule: LithoViewRule,
        component: Component?,
        widthSpec: Int,
        heightSpec: Int
    ): LithoLayoutResult? {
      return rule.attachToWindow()
          .setRootAndSizeSpec(component, widthSpec, heightSpec)
          .measure()
          .layout()
          .currentRootNode
    }
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @JvmStatic
    fun getRootLayout(rule: LithoViewRule, component: Component?): LithoLayoutResult? {
      return rule.attachToWindow().setRoot(component).measure().layout().currentRootNode
    }
  }
}
