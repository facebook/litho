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

import android.view.View
import com.facebook.infer.annotation.ThreadConfined
import com.facebook.litho.DebugComponentDescriptionHelper.ExtraDescription
import com.facebook.proguard.annotations.DoNotStrip
import java.util.Deque
import kotlin.jvm.JvmStatic

/**
 * Helper class to access metadata from [LithoView] that is relevant during end to end tests. In
 * order for the data to be collected, [ComponentsConfiguration#isEndToEndTestRun] must be enabled.
 */
@DoNotStrip
object LithoViewTestHelper {

  /**
   * @param lithoView The component view the component is mounted to.
   * @param testKey The unique identifier the component was constructed with.
   * @return Test item if found, null otherwise.
   * @throws UnsupportedOperationException If the e2e flag is not enabled in the configuration.
   * @see findTestItems(LithoView, String) **Note**: If there is more than one element mounted under
   *   the given key, the last one to render will be returned.
   */
  @JvmStatic
  @DoNotStrip
  fun findTestItem(lithoView: LithoView, testKey: String?): TestItem? {
    val items = lithoView.findTestItems(testKey)
    return if (items.isEmpty()) null else items.last
  }

  /**
   * Finds a [TestItem] given a [LithoView] based on the test key it was assigned during
   * construction.
   *
   * **Example use:**
   *
   * ```
   * final LithoView lithoView = ComponentTestHelper.mountComponent(
   *     mContext,
   *     new InlineLayoutSpec() {
   *       protected ComponentLayout onCreateLayout(ComponentContext c) {
   *         return Column.create(c)
   *           .child(
   *             Column.create(c)
   *               .child(SimpleMountSpecTester.create(c))
   *               .child(SimpleMountSpecTester.create(c))
   *               .testKey("mytestkey"))
   *           .build();
   *      }
   * });
   * final TestItem testItem = LithoViewTestHelper.findTestItem(lithoView, "mytestkey");
   * ```
   *
   * @param lithoView The component view the component is mounted to.
   * @param testKey The unique identifier the component was constructed with.
   * @return Queue of mounted items in order by mount time.
   * @throws UnsupportedOperationException If the e2e flag is not enabled in the configuration.
   */
  @JvmStatic
  @DoNotStrip
  fun findTestItems(lithoView: LithoView, testKey: String): Deque<TestItem> =
      lithoView.findTestItems(testKey)

  @JvmStatic
  @DoNotStrip
  fun viewToString(view: BaseMountingView): String = viewToString(view, false).trim()

  /**
   * Provide a nested string representation of a LithoView and its nested components for E2E testing
   * purposes. Note: this method is called via reflection to prevent direct or shared dependencies.
   * DO NOT CHANGE the method signature.
   *
   * @param depth the offset to set on the litho nodes
   * @param withProps if to dump extra properties
   */
  @JvmStatic
  @DoNotStrip
  @JvmOverloads
  fun viewToStringForE2E(
      view: View,
      depth: Int,
      withProps: Boolean,
      extraDescription: ExtraDescription? = null
  ): String {
    if (view !is BaseMountingView) {
      return ""
    }
    val root = DebugComponent.getRootInstance(view) ?: return ""
    val sb = StringBuilder()
    viewToString(root, sb, true, withProps, depth, 0, 0, extraDescription)
    return sb.toString()
  }

  /**
   * Provide a nested string representation of a LithoView and its nested components for debugging
   * purposes.
   *
   * @param view A Litho view with mounted components.
   * @param embedded if the call is embedded in "adb dumpsys activity"
   */
  @JvmStatic
  @DoNotStrip
  fun viewToString(view: BaseMountingView, embedded: Boolean): String {
    val root = DebugComponent.getRootInstance(view)
    return rootInstanceToString(root, embedded, 0)
  }

  /**
   * Provides a nested string representation of a DebugComponent and its nested components for
   * debugging.
   *
   * @param root A root DebugComponent
   * @param embedded if the call is embedded in "adb dumpsys activity"
   * @param startingDepth the starting depth of the true for printing components (normally defaults
   *   to 0)
   */
  @JvmStatic
  @DoNotStrip
  fun rootInstanceToString(root: DebugComponent?, embedded: Boolean, startingDepth: Int): String {
    if (root == null) {
      return ""
    }
    val view = root.lithoView
    val sb = StringBuilder()
    val depth = if (embedded && view != null) getLithoViewDepthInAndroid(view) else startingDepth
    sb.append("\n")
    viewToString(component = root, sb = sb, embedded = embedded, withProps = false, depth = depth)
    return sb.toString()
  }

  /** For E2E tests */
  @JvmStatic
  private fun viewToString(
      component: DebugComponent,
      sb: StringBuilder,
      embedded: Boolean,
      withProps: Boolean,
      depth: Int = 0,
      leftOffset: Int = 0,
      topOffset: Int = 0,
      extraDescription: ExtraDescription? = null
  ) {
    writeIndentByDepth(sb, depth)
    DebugComponentDescriptionHelper.addViewDescription(
        component, sb, leftOffset, topOffset, embedded, withProps, extraDescription)
    sb.append("\n")

    val spannedTextContent = DebugComponentDescriptionHelper.getSyntheticViewDescriptions(component)
    // for each line of text, we need to add an extra indent
    spannedTextContent.forEach { line ->
      writeIndentByDepth(sb, depth + 1)
      sb.append(line)
      sb.append("\n")
    }

    val bounds = component.boundsInLithoView
    for (child in component.childComponents) {
      viewToString(
          child, sb, embedded, withProps, depth + 1, bounds.left, bounds.top, extraDescription)
    }
  }

  /** calculate the depth on the litho components in general android view hierarchy */
  @JvmStatic
  private fun getLithoViewDepthInAndroid(view: BaseMountingView): Int {
    var depth = 3
    var parent = view.parent
    while (parent != null) {
      depth++
      parent = parent.parent
    }
    return depth
  }

  /** Add new line and two-space indent for each level to match android view hierarchy dump */
  @JvmStatic
  private fun writeIndentByDepth(sb: StringBuilder, depth: Int) {
    for (i in 0 until depth) {
      sb.append("  ")
    }
  }

  @JvmStatic
  fun toDebugString(baseMountingView: BaseMountingView?): String {
    if (baseMountingView == null) {
      return ""
    }
    val debugString = viewToString(baseMountingView, true)
    return debugString.ifEmpty { viewBoundsToString(baseMountingView) }
  }

  @JvmStatic
  private fun viewBoundsToString(baseMountingView: BaseMountingView): String {
    val sb = StringBuilder()
    sb.append("(")
    sb.append(baseMountingView.left)
    sb.append(",")
    sb.append(baseMountingView.top)
    sb.append("-")
    sb.append(baseMountingView.right)
    sb.append(",")
    sb.append(baseMountingView.bottom)
    sb.append(")")
    return sb.toString()
  }

  /**
   * Obtain a reference to a LithoView's internal layout root, if present. This is used to restore a
   * view's root after it has been freed for testing purposes.
   *
   * @see setRootLayoutRef(LithoView, InternalNodeRef)
   */
  @JvmStatic
  @ThreadConfined(ThreadConfined.UI)
  fun getRootLayoutRef(view: LithoView): InternalNodeRef? {
    val componentTree = view.componentTree
    val mainThreadLayoutState = componentTree?.mainThreadLayoutState
    if (mainThreadLayoutState != null) {
      val layoutResult = mainThreadLayoutState.rootLayoutResult
      check(layoutResult is LithoLayoutResult) {
        "Expected LithoLayoutResult but got $layoutResult"
      }
      return InternalNodeRef(layoutResult)
    }
    return null
  }

  /**
   * Restore a previously saved root layout reference.
   *
   * @see getRootLayoutRef(LithoView)
   */
  @JvmStatic
  @ThreadConfined(ThreadConfined.UI)
  fun setRootLayoutRef(view: LithoView, rootLayoutNode: InternalNodeRef) {
    val componentTree = view.componentTree
    val mainThreadLayoutState = componentTree?.mainThreadLayoutState
    if (mainThreadLayoutState != null) {
      mainThreadLayoutState.rootLayoutResult = rootLayoutNode.internalNodeRef
    }
  }

  /** Holds an opaque reference to an [LithoNode] without giving the holder any access to it. */
  class InternalNodeRef(val internalNodeRef: LithoLayoutResult)
}
