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

import android.content.Context
import android.os.HandlerThread
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.ComponentTree.SIZE_UNINITIALIZED
import com.facebook.litho.ComponentTreeTest.DoubleMeasureViewGroup
import com.facebook.litho.SizeSpec.AT_MOST
import com.facebook.litho.SizeSpec.EXACTLY
import com.facebook.litho.SizeSpec.UNSPECIFIED
import com.facebook.litho.SizeSpec.getMode
import com.facebook.litho.SizeSpec.getSize
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoStatsRule
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.TestDrawableComponent.BlockInPrepareComponentListener
import com.facebook.litho.testing.TestLayoutComponent
import com.facebook.litho.testing.ThreadTestingUtils
import com.facebook.litho.testing.TimeOutSemaphore
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.atMost
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ComponentTreeTester
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.SimpleStateUpdateEmulator
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec
import com.facebook.rendercore.RunnableHandler
import java.lang.Exception
import java.lang.RuntimeException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import junit.framework.AssertionFailedError
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ComponentTreeTest {

  @JvmField @Rule var backgroundLayoutLooperRule = BackgroundLayoutLooperRule()
  @JvmField @Rule var lithoStatsRule = LithoStatsRule()

  private lateinit var context: ComponentContext
  private lateinit var component: Component
  private lateinit var resolveThreadShadowLooper: ShadowLooper
  private lateinit var layoutThreadShadowLooper: ShadowLooper
  private var oldWrapperConfig: RootWrapperComponentFactory? = null
  private var widthSpec: Int = 0
  private var widthSpec2: Int = 0
  private var heightSpec: Int = 0
  private var heightSpec2: Int = 0

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    component = SimpleMountSpecTester.create(context).build()
    layoutThreadShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultLayoutThreadLooper")
                as Looper)
    resolveThreadShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultResolveThreadLooper")
                as Looper)
    widthSpec = makeSizeSpec(39, EXACTLY)
    widthSpec2 = makeSizeSpec(40, EXACTLY)
    heightSpec = makeSizeSpec(41, EXACTLY)
    heightSpec2 = makeSizeSpec(42, EXACTLY)
  }

  private fun runToEndOfTasks() {
    resolveThreadShadowLooper.runToEndOfTasks()
    layoutThreadShadowLooper.runToEndOfTasks()
  }

  private fun runOneTask() {
    resolveThreadShadowLooper.runOneTask()
    layoutThreadShadowLooper.runOneTask()
  }

  @Before
  fun saveConfig() {
    oldWrapperConfig = ErrorBoundariesConfiguration.rootWrapperComponentFactory
  }

  @After
  fun restoreConfig() {
    ErrorBoundariesConfiguration.rootWrapperComponentFactory = oldWrapperConfig
  }

  @After
  fun tearDown() {
    // Clear pending tasks in case test failed
    runToEndOfTasks()
  }

  private fun creationCommonChecks(componentTree: ComponentTree) {
    // Not view or attached yet
    assertNull(componentTree.lithoView)

    // The component input should be the one we passed in
    assertSame(component, componentTree.root)
  }

  private fun postSizeSpecChecks(
      componentTree: ComponentTree,
      widthSpec: Int = this.widthSpec,
      heightSpec: Int = this.heightSpec
  ) {
    // Spec specified in create
    assertThat(componentTreeHasSizeSpec(componentTree)).isTrue
    val mainThreadLayoutState = componentTree.mainThreadLayoutState
    val committedLayoutState = componentTree.committedLayoutState
    assertThat(mainThreadLayoutState).isEqualTo(committedLayoutState)
    assertThat(
            mainThreadLayoutState?.isCompatibleComponentAndSpec(
                component.id, widthSpec, heightSpec))
        .isTrue
  }

  @Test
  fun testCreate() {
    val componentTree = ComponentTree.create(context, component).build()
    creationCommonChecks(componentTree)

    // Both the main thread and the background layout state shouldn't be calculated yet.
    assertNull(componentTree.mainThreadLayoutState)
    assertNull(componentTree.committedLayoutState)
    assertFalse(componentTreeHasSizeSpec(componentTree))
  }

  @Test
  fun testCreate_ContextIsNotScoped() {
    val scopedContext =
        ComponentContext.withComponentScope(context, Row.create(context).build(), "global_key")
    val componentTree = ComponentTree.create(scopedContext, component).build()
    val c = componentTree.context
    assertNull(c.componentScope)
  }

  @Test
  fun testSetSizeSpec() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setSizeSpec(widthSpec, heightSpec)
    postSizeSpecChecks(componentTree)
  }

  @Test
  fun testSetSizeSpecAsync() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)

    // Only fields changed but no layout is done yet.
    assertThat(componentTreeHasSizeSpec(componentTree)).isTrue
    assertNull(componentTree.mainThreadLayoutState)
    assertNull(componentTree.committedLayoutState)

    // Now the background thread run the queued task.
    runOneTask()
    if (!ComponentsConfiguration.useSeparateThreadHandlersForResolveAndLayout) {
      runOneTask()
    }
    postSizeSpecChecks(componentTree)
  }

  @Test
  fun testLayoutState_ContextIsNotScoped() {
    val scopedContext =
        ComponentContext.withComponentScope(context, Row.create(context).build(), "global_key")
    val root = Column.create(scopedContext).key("key").build()
    val componentTree = ComponentTree.create(scopedContext, root).build()
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)
    runOneTask()
    if (!ComponentsConfiguration.useSeparateThreadHandlersForResolveAndLayout) {
      runOneTask()
    }
    val layoutState = componentTree.mainThreadLayoutState
    val c = componentTree.context
    assertThat(c).isNotEqualTo(scopedContext)
    assertNull(c.componentScope)
    assertThat(layoutState?.rootLayoutResult?.context).isNotEqualTo(scopedContext)
  }

  private class MeasureListener : ComponentTree.MeasureListener {
    var width: Int = 0
    var height: Int = 0

    override fun onSetRootAndSizeSpec(
        layoutVersion: Int,
        width: Int,
        height: Int,
        stateUpdate: Boolean
    ) {
      this.width = width
      this.height = height
    }
  }

  @Test
  fun testRacyLayouts() {
    val asyncLatch = CountDownLatch(1)
    val syncLatch = CountDownLatch(1)
    val endOfTest = CountDownLatch(1)
    val widthSpec = makeSizeSpec(100, EXACTLY)
    val heightSpec = makeSizeSpec(100, EXACTLY)
    val asyncWidthSpec = makeSizeSpec(200, EXACTLY)
    val asyncHeightSpec = makeSizeSpec(200, EXACTLY)
    val componentTree = ComponentTree.create(context).build()
    val innerComponentTree = ComponentTree.create(context).build()
    val measureListener = MeasureListener()
    innerComponentTree.addMeasureListener(measureListener)
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? {
            innerComponentTree.setVersionedRootAndSizeSpec(
                object : InlineLayoutSpec() {}, widthSpec, heightSpec, null, null, c.layoutVersion)
            return super.onCreateLayout(c)
          }
        }
    val asyncComponent: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? {
            ThreadTestingUtils.failSilentlyIfInterrupted {
              syncLatch.countDown()
              assertThat(asyncLatch.await(5, TimeUnit.SECONDS)).describedAs("Timeout!").isTrue()
            }
            innerComponentTree.setVersionedRootAndSizeSpec(
                object : InlineLayoutSpec() {},
                asyncWidthSpec,
                asyncHeightSpec,
                null,
                null,
                c.layoutVersion)
            return super.onCreateLayout(c)
          }
        }
    object : Thread() {
          override fun run() {
            componentTree.setRootAndSizeSpecSync(asyncComponent, 0, 0)
            endOfTest.countDown()
          }
        }
        .start()
    assertThat(syncLatch.await(5, TimeUnit.SECONDS)).describedAs("Timeout!").isTrue
    componentTree.setRootAndSizeSpecSync(component, 0, 0)
    junit.framework.Assert.assertEquals(measureListener.width, 100)
    junit.framework.Assert.assertEquals(measureListener.height, 100)
    asyncLatch.countDown()
    componentTree.setRootAsync(component)
    assertThat(endOfTest.await(5, TimeUnit.SECONDS)).describedAs("Timeout!").isTrue

    // Verify your stuff at this point
    junit.framework.Assert.assertEquals(measureListener.width, 100)
    junit.framework.Assert.assertEquals(measureListener.height, 100)
  }

  @Test
  fun testSetSizeSpecAsyncThenSyncBeforeRunningTask() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)
    componentTree.setSizeSpec(widthSpec2, heightSpec2)
    runToEndOfTasks()
    postSizeSpecChecks(componentTree, widthSpec2, heightSpec2)
  }

  @Test
  fun testSetRootSynchThenAsyncThenSync() {
    val componentTree = ComponentTree.create(context).build()
    componentTree.setRootAndSizeSpecSync(
        SimpleMountSpecTester.create(context).widthPx(200).heightPx(200).build(),
        makeSizeSpec(0, UNSPECIFIED),
        makeSizeSpec(0, UNSPECIFIED))
    val newComponent = SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).build()
    componentTree.setRootAndSizeSpecAsync(
        newComponent, makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED))
    val size = Size()
    componentTree.setRootAndSizeSpecSync(
        newComponent, makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED), size)
    assertThat(size.width).isEqualTo(100)
    assertThat(size.height).isEqualTo(100)
  }

  @Test
  fun testSetSizeSpecAsyncThenSyncAfterRunningTask() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setSizeSpecAsync(widthSpec, heightSpec)
    runToEndOfTasks()
    componentTree.setSizeSpec(widthSpec2, heightSpec2)
    postSizeSpecChecks(componentTree, widthSpec2, heightSpec2)
  }

  @Test
  fun testSetSizeSpecWithOutput() {
    val componentTree = ComponentTree.create(context, component).build()
    val size = Size()
    componentTree.setSizeSpec(widthSpec, heightSpec, size)
    junit.framework.Assert.assertEquals(getSize(widthSpec).toDouble(), size.width.toDouble(), 0.0)
    junit.framework.Assert.assertEquals(getSize(heightSpec).toDouble(), size.height.toDouble(), 0.0)
    postSizeSpecChecks(componentTree)
  }

  @Test
  fun testSetSizeSpecWithOutputWhenAttachedToViewWithSameSpec() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    val size = Size()
    componentTree.measure(widthSpec, heightSpec, IntArray(2), false)
    componentTree.attach()
    componentTree.setSizeSpec(widthSpec, heightSpec, size)
    junit.framework.Assert.assertEquals(getSize(widthSpec).toDouble(), size.width.toDouble(), 0.0)
    junit.framework.Assert.assertEquals(getSize(heightSpec).toDouble(), size.height.toDouble(), 0.0)
    assertThat(componentTree.hasCompatibleLayout(widthSpec, heightSpec)).isTrue
    assertThat(componentTree.root).isEqualTo(component)
  }

  @Test
  fun testSetSizeSpecWithOutputWhenAttachedToViewWithNewSpec() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    val size = Size()
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    componentTree.attach()
    componentTree.setSizeSpec(widthSpec, heightSpec, size)
    junit.framework.Assert.assertEquals(getSize(widthSpec).toDouble(), size.width.toDouble(), 0.0)
    junit.framework.Assert.assertEquals(getSize(heightSpec).toDouble(), size.height.toDouble(), 0.0)
    assertThat(componentTree.hasCompatibleLayout(widthSpec, heightSpec)).isTrue
    assertThat(componentTree.root).isEqualTo(component)
  }

  @Test
  fun testSetCompatibleSizeSpec() {
    val componentTree = ComponentTree.create(context, component).build()
    val size = Size()
    componentTree.setSizeSpec(makeSizeSpec(100, AT_MOST), makeSizeSpec(100, AT_MOST), size)
    junit.framework.Assert.assertEquals(100.0, size.width.toDouble(), 0.0)
    junit.framework.Assert.assertEquals(100.0, size.height.toDouble(), 0.0)
    val firstLayoutState = componentTree.mainThreadLayoutState
    assertThat(firstLayoutState).isNotNull
    componentTree.setSizeSpec(makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY), size)
    junit.framework.Assert.assertEquals(100.0, size.width.toDouble(), 0.0)
    junit.framework.Assert.assertEquals(100.0, size.height.toDouble(), 0.0)
    assertThat(componentTree.mainThreadLayoutState).isEqualTo(firstLayoutState)
  }

  @Test
  fun testSetCompatibleSizeSpecWithDifferentRoot() {
    val componentTree = ComponentTree.create(context, component).build()
    val size = Size()
    componentTree.setSizeSpec(makeSizeSpec(100, AT_MOST), makeSizeSpec(100, AT_MOST), size)
    junit.framework.Assert.assertEquals(100.0, size.width.toDouble(), 0.0)
    junit.framework.Assert.assertEquals(100.0, size.height.toDouble(), 0.0)
    val firstLayoutState = componentTree.mainThreadLayoutState
    assertThat(firstLayoutState).isNotNull
    componentTree.setRootAndSizeSpecSync(
        SimpleMountSpecTester.create(context).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        size)
    Assert.assertNotEquals(firstLayoutState, componentTree.mainThreadLayoutState)
  }

  @Test
  fun testSetRootAndSizeSpecWithTreeProps() {
    val componentTree = ComponentTree.create(context, component).build()
    val size = Size()
    val treeProps = TreeProps()
    treeProps.put(Any::class.java, "hello world")
    componentTree.setRootAndSizeSpecSync(
        SimpleMountSpecTester.create(context).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        size,
        treeProps)
    val c =
        Whitebox.getInternalState<ComponentContext>(componentTree.mainThreadLayoutState, "mContext")
    assertThat(c.treeProps).isSameAs(treeProps)
  }

  @Test
  fun testDefaultInitialisationAndSetRoot() {
    val componentTree = ComponentTree.create(context).build()
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    assertThat(componentTree.root).isNotNull
    componentTree.setRootAndSizeSpecSync(component, widthSpec, heightSpec)
    assertThat(componentTree.root).isEqualTo(component)
  }

  @Test
  fun testSetRootWithTreePropsThenMeasure() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    val treeProps = TreeProps()
    treeProps.put(Any::class.java, "hello world")
    componentTree.setRootAndSizeSpecAsync(
        SimpleMountSpecTester.create(context).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        treeProps)
    assertThat(componentTree.committedLayoutState).isNull()
    componentTree.measure(
        makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY), IntArray(2), false)
    val c =
        Whitebox.getInternalState<ComponentContext>(componentTree.mainThreadLayoutState, "mContext")
    assertThat(c.treeProps).isNotNull
    assertThat(c.treeProps?.get(Any::class.java)).isEqualTo(treeProps.get(Any::class.java))
  }

  @Test
  fun testSetRootWithTreePropsThenSetSizeSpec() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    val treeProps = TreeProps()
    treeProps.put(Any::class.java, "hello world")
    componentTree.setRootAndSizeSpecAsync(
        SimpleMountSpecTester.create(context).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        treeProps)
    assertThat(componentTree.committedLayoutState).isNull()
    componentTree.setSizeSpec(makeSizeSpec(200, EXACTLY), makeSizeSpec(200, EXACTLY))
    val c =
        Whitebox.getInternalState<ComponentContext>(componentTree.mainThreadLayoutState, "mContext")
    assertThat(c.treeProps).isNotNull
    assertThat(c.treeProps?.get(Any::class.java)).isEqualTo(treeProps.get(Any::class.java))
  }

  @Test
  fun testSetRootWithTreePropsThenSetNewRoot() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    val treeProps = TreeProps()
    treeProps.put(Any::class.java, "hello world")
    componentTree.setRootAndSizeSpecAsync(
        SimpleMountSpecTester.create(context).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        treeProps)
    assertThat(componentTree.committedLayoutState).isNull()
    componentTree.setRootAndSizeSpecSync(
        SimpleMountSpecTester.create(context).build(),
        makeSizeSpec(200, EXACTLY),
        makeSizeSpec(200, EXACTLY))
    val c =
        Whitebox.getInternalState<ComponentContext>(componentTree.mainThreadLayoutState, "mContext")
    assertThat(c.treeProps).isNotNull
    assertThat(c.treeProps?.get(Any::class.java)).isEqualTo(treeProps.get(Any::class.java))
  }

  @Test
  fun testSetRootWithTreePropsThenUpdateState() {
    val caller = SimpleStateUpdateEmulatorSpec.Caller()
    val componentTree =
        ComponentTree.create(
                context, SimpleStateUpdateEmulator.create(context).caller(caller).build())
            .build()
    componentTree.setLithoView(LithoView(context))
    componentTree.attach()
    componentTree.setRootAndSizeSpecSync(
        SimpleStateUpdateEmulator.create(context).caller(caller).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        Size())
    val treeProps = TreeProps()
    treeProps.put(Any::class.java, "hello world")
    componentTree.setRootAndSizeSpecAsync(
        SimpleStateUpdateEmulator.create(context).caller(caller).build(),
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY),
        treeProps)
    caller.increment()
    ShadowLooper.runUiThreadTasks()
    val c =
        Whitebox.getInternalState<ComponentContext>(componentTree.mainThreadLayoutState, "mContext")
    assertThat(c.treeProps).isNotNull
    assertThat(c.treeProps?.get(Any::class.java)).isEqualTo(treeProps.get(Any::class.java))
  }

  @Test
  fun testSetInput() {
    val component = TestLayoutComponent.create(context).build()
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.root = this.component
    creationCommonChecks(componentTree)
    assertNull(componentTree.mainThreadLayoutState)
    assertNull(componentTree.committedLayoutState)
    componentTree.setSizeSpec(widthSpec, heightSpec)
    postSizeSpecChecks(componentTree)
  }

  @Test
  fun testSetComponentFromView() {
    val component1 = SimpleMountSpecTester.create(context).build()
    val componentTree1 = ComponentTree.create(context, component1).build()
    val component2 = SimpleMountSpecTester.create(context).build()
    val componentTree2 = ComponentTree.create(context, component2).build()
    assertNull(componentTree1.lithoView)
    assertNull(componentTree2.lithoView)
    val lithoView = LithoView(context)
    lithoView.componentTree = componentTree1
    assertNotNull(componentTree1.lithoView)
    assertNull(componentTree2.lithoView)
    lithoView.componentTree = componentTree2
    assertNull(componentTree1.lithoView)
    assertNotNull(componentTree2.lithoView)
  }

  @Test
  fun testComponentTreeReleaseClearsView() {
    val component = SimpleMountSpecTester.create(context).build()
    val componentTree = ComponentTree.create(context, component).build()
    val lithoView = LithoView(context)
    lithoView.componentTree = componentTree
    assertThat(componentTree).isEqualTo(lithoView.componentTree)
    componentTree.release()
    assertThat(lithoView.componentTree).isNull()
  }

  @Test
  fun testSetTreeToTwoViewsBothAttached() {
    val component = SimpleMountSpecTester.create(context).build()
    val componentTree = ComponentTree.create(context, component).build()

    // Attach first view.
    val lithoView1 = LithoView(context)
    lithoView1.componentTree = componentTree
    lithoView1.onAttachedToWindow()

    // Attach second view.
    val lithoView2 = LithoView(context)
    lithoView2.onAttachedToWindow()

    // Set the component that is already mounted on the first view, on the second attached view.
    // This should be ok.
    lithoView2.componentTree = componentTree
  }

  @Test
  fun testSettingNewViewToTree() {
    val component = SimpleMountSpecTester.create(context).build()
    val componentTree = ComponentTree.create(context, component).build()

    // Attach first view.
    val lithoView1 = LithoView(context)
    lithoView1.componentTree = componentTree
    assertThat(componentTree.lithoView).isEqualTo(lithoView1)
    assertThat(lithoView1.componentTree).isEqualTo(componentTree)

    // Attach second view.
    val lithoView2 = LithoView(context)
    assertNull(lithoView2.componentTree)
    lithoView2.componentTree = componentTree
    assertThat(componentTree.lithoView).isEqualTo(lithoView2)
    assertThat(lithoView2.componentTree).isEqualTo(componentTree)
    assertNull(lithoView1.componentTree)
  }

  @Test
  fun testSetRootAsyncFollowedByMeasureDoesntComputeSyncLayout() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    componentTree.measure(widthSpec, heightSpec, IntArray(2), false)
    componentTree.attach()
    val newComponent: Component = SimpleMountSpecTester.create(context).color(1_234).build()
    componentTree.setRootAsync(newComponent)
    assertThat(componentTree.root).isEqualTo(newComponent)
    componentTree.measure(widthSpec, heightSpec, IntArray(2), false)
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(component.id)).isTrue
    runToEndOfTasks()
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id)).isTrue
  }

  @Test
  fun testSetRootAsyncFollowedByNonCompatibleMeasureComputesSyncLayout() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    componentTree.measure(widthSpec, heightSpec, IntArray(2), false)
    componentTree.attach()
    val newComponent: Component = SimpleMountSpecTester.create(context).color(1_234).build()
    componentTree.setRootAsync(newComponent)
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id)).isTrue

    // Clear tasks
    runToEndOfTasks()
  }

  /*
   * This test is meant to simulate a LithoView in a LinearLayout or RelativeLayout where it gets
   * measured twice in a single layout pass with the second measurement depending on the result
   * of the first (e.g. if the LithoView measures to be 500px in the first measure pass, the parent
   * remeasures with AT_MOST 500). We need to make sure we end up showing the correct root and
   * respecting the size it would have within its parent.
   *
   * In this test, the first component has fixed size 100x100 and will produce a layout compatible
   * with any specs that have AT_MOST >=100. In the test, we expect the initial measure with
   * AT_MOST 2000, an async setRoot call, and then a double measure from the parent, first with
   * AT_MOST 1000 (compatible with the old root) and then with AT_MOST <result of first measure>.
   * We need to make sure the second component (which unlike the first component will take up the
   * full size of the parent) ends up being allocated AT_MOST 1000 and not constrained to the 100px
   * the first component measured to.
   */
  @Test
  fun testSetRootAsyncFollowedByMeasurementInParentWithDoubleMeasure() {
    // TODO (T134949954) reexamine need for this
    if (true) {
      return
    }
    val componentTree =
        ComponentTree.create(context, Row.create(context).minWidthPx(100).minHeightPx(100)).build()
    val lithoView = LithoView(context)
    lithoView.componentTree = componentTree
    val parent = DoubleMeasureViewGroup(context.androidContext)
    parent.addView(lithoView)
    lithoView.measure(atMost(2_000), atMost(2_000))
    val blockInPrepare = BlockInPrepareComponentListener()
    val newComponent = TestDrawableComponent.create(context).build()
    blockInPrepare.setDoNotBlockOnThisThread()
    newComponent.setTestComponentListener(blockInPrepare)
    componentTree.setRootAsync(newComponent)
    val asyncLayout = backgroundLayoutLooperRule.runToEndOfTasksAsync()
    blockInPrepare.awaitPrepareStart()

    // This is a bit of a hack: Robolectric's ShadowLegacyLooper implementation is synchronized
    // on runToEndOfTasks and post(). Since we are simulating being in the middle of calculating a
    // layout, this means that we can't post() to the same Handler (as we will try to do in measure)
    // The "fix" here is to update the layout thread to a new handler/looper that can be controlled
    // separately.
    val newHandlerThread = createAndStartNewHandlerThread()
    componentTree.updateLayoutThreadHandler(RunnableHandler.DefaultHandler(newHandlerThread.looper))
    parent.requestLayout()
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(1_000, View.MeasureSpec.EXACTLY))
    parent.layout(0, 0, 1_000, 1_000)
    blockInPrepare.allowPrepareToComplete()
    asyncLayout.acquire()
    assertThat(lithoView.width).isEqualTo(1_000)
    assertThat(lithoView.height).isEqualTo(1_000)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(
            componentTree.mainThreadLayoutState?.isForComponentId((newComponent as Component).id))
        .isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(1_000)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a layout from measure.")
        .isEqualTo(3)
  }

  /*
   * Context for the test:
   * - The original component mComponent will measure to height 1000 with heightSpec1 and to 0 with
   *   heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 0 with heightSpec2
   *
   * In this test, we setRootAsync but before any of its code runs, we measure with new width/height
   * specs that both the original component and the new component are incompatible with.
   */
  @Test
  fun testSetRootAsyncWithIncompatibleMeasureBeforeStart() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, EXACTLY)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(0, UNSPECIFIED)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val newComponent: Component = SimpleMountSpecTester.create(context).color(1_234).build()
    componentTree.setRootAsync(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should not be compatible.")
        .isFalse
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)

    // Since the layout thread hasn't started the async layout, we know it will capture the updated
    // size specs when it does run
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id))
        .describedAs(
            "The old component spec is not compatible so we should do a sync layout with the new root.")
        .isTrue
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.runUiThreadTasks()

    // Once the async layout finishes, the main thread should have the updated layout.
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(0)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout and one layout after measure. The async layout shouldn't happen.")
        .isEqualTo(2)
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   *
   * In this test, we setRootAsync but before any of its code runs, we measure with new width/height
   * specs that both the original component and the new component are compatible with.
   */
  @Test
  fun testSetRootAsyncWithCompatibleMeasureBeforeStart() {
    val oldComponent =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    val componentTree = ComponentTree.create(context, oldComponent).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, AT_MOST)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(500, AT_MOST)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val newComponent: Component =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    componentTree.setRootAsync(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id)).isTrue
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.runUiThreadTasks()
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(100)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs("We expect one initial layout and the async layout.")
        .isEqualTo(2)
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 500 with heightSpec2
   *
   * In this test, we setRootAsync but before any of its code runs, we measure with new width/height
   * specs the original component is compatible with but the new component is incompatible with.
   */
  @Test
  fun testSetRootAsyncWithIncompatibleMeasureButCompatibleMeasureForExistingLayoutBeforeStart() {
    val oldComponent =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    val componentTree = ComponentTree.create(context, oldComponent).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, AT_MOST)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(500, AT_MOST)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val newComponent: Component =
        SimpleMountSpecTester.create(context).flexGrow(1f).color(1_234).build()
    componentTree.setRootAsync(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(500)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.runUiThreadTasks()
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(500)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout and one layout after measure. The async layout shouldn't happen.")
        .isEqualTo(2)
  }

  /*
   * Context for the test:
   * - The original component mComponent will measure to height 1000 with heightSpec1 and to 0 with
   *   heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 0 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run, but before it is promoted on the main
   * thread, we measure with new width/height specs both the original component and the new
   * component are incompatible with.
   */
  @Test
  fun testSetRootAsyncWithIncompatibleMeasureAfterFinish() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, EXACTLY)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(0, UNSPECIFIED)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val newComponent: Component = SimpleMountSpecTester.create(context).color(1_234).build()
    componentTree.setRootAsync(newComponent)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should not be compatible.")
        .isFalse
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id))
        .isTrue
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not be used once it completes")
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(0)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout after measure.")
        .isEqualTo(3)
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run, but before it is promoted on the main
   * thread, we measure with new width/height specs both the original component and the new
   * component are compatible with.
   */
  @Test
  fun testSetRootAsyncWithCompatibleMeasureAfterFinish() {
    val oldComponent =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    val componentTree = ComponentTree.create(context, oldComponent).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, AT_MOST)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(500, AT_MOST)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val newComponent: Component =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    componentTree.setRootAsync(newComponent)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id))
        .isTrue
        .withFailMessage(
            "The main thread should promote the committed layout to the UI thread in measure.")
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(100)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs("We expect one initial layout and the async layout.")
        .isEqualTo(2)
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 500 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run, but before it is promoted on the main
   * thread, we measure with new width/height specs both the original component and the new
   * component are compatible with.
   */
  @Test
  fun testSetRootAsyncWithIncompatibleMeasureButCompatibleMeasureForExistingLayoutAfterFinish() {
    val oldComponent =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    val componentTree = ComponentTree.create(context, oldComponent).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, AT_MOST)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(500, AT_MOST)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val newComponent: Component =
        SimpleMountSpecTester.create(context).flexGrow(1f).color(1_234).build()
    componentTree.setRootAsync(newComponent)
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newComponent.id))
        .isTrue
        .withFailMessage(
            "The main thread will calculate a new layout synchronously because the background layout isn't compatible.")
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(500)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout from measure.")
        .isEqualTo(3)
  }

  /*
   * Context for the test:
   * - The original component mComponent will measure to height 1000 with heightSpec1 and to 0 with
   *   heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 0 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run but before it can commit on the background
   * thread, we measure with new width/height specs both the original component and the new
   * component are incompatible with.
   */
  @Test
  fun testSetRootAsyncWithIncompatibleMeasureDuringLayout() {
    // TODO (T134949954) reexamine need for this
    if (true) {
      return
    }
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, EXACTLY)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(0, UNSPECIFIED)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val blockInPrepare = BlockInPrepareComponentListener()
    blockInPrepare.setDoNotBlockOnThisThread()
    val newComponent = TestDrawableComponent.create(context).color(1_234).build()
    newComponent.setTestComponentListener(blockInPrepare)
    componentTree.setRootAsync(newComponent)
    val asyncLayoutFinish = runOnBackgroundThread { runToEndOfTasks() }
    blockInPrepare.awaitPrepareStart()

    // At this point, the Layout thread is blocked in prepare (waiting for unblockAsyncPrepare) and
    // will have already captured the "bad" specs, but not completed its layout. We expect the main
    // thread to determine that this async layout will not be correct and that it needs to compute
    // one in measure
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should not be compatible.")
        .isFalse
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(
            componentTree.mainThreadLayoutState?.isForComponentId((newComponent as Component).id))
        .isTrue
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not have compatible size specs")
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(0)

    // Finally, let the async layout finish and make sure it doesn't replace the layout from measure
    blockInPrepare.allowPrepareToComplete()
    asyncLayoutFinish.acquire()
    ShadowLooper.runUiThreadTasks()
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(
            componentTree.mainThreadLayoutState?.isForComponentId((newComponent as Component).id))
        .isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(0)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout after measure.")
        .isEqualTo(3)
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run but before it can commit on the background
   * thread, we measure with new width/height specs both the original component and the new
   * component are compatible with.
   */
  @Test
  fun testSetRootAsyncWithCompatibleMeasureDuringLayout() {
    // TODO (T134949954) reexamine need for this
    if (true) {
      return
    }
    val oldComponent =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    val componentTree = ComponentTree.create(context, oldComponent).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, AT_MOST)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(500, AT_MOST)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val blockInPrepare = BlockInPrepareComponentListener()
    blockInPrepare.setDoNotBlockOnThisThread()
    val newComponent =
        TestDrawableComponent.create(context).widthPx(100).heightPx(100).color(1_234).build()
    newComponent.setTestComponentListener(blockInPrepare)
    componentTree.setRootAsync(newComponent)
    val asyncLayoutFinish = runOnBackgroundThread { runToEndOfTasks() }
    blockInPrepare.awaitPrepareStart()

    // At this point, the Layout thread is blocked in prepare (waiting for unblockAsyncPrepare) and
    // will have already captured the "bad" specs, but not completed its layout.

    // This is a bit of a hack: Robolectric's ShadowLegacyLooper implementation is synchronized
    // on runToEndOfTasks and post(). Since we are simulating being in the middle of calculating a
    // layout, this means that we can't post() to the same Handler (as we will try to do in measure)
    // The "fix" here is to update the layout thread to a new handler/looper that can be controlled
    // separately.
    val newHandlerThread = createAndStartNewHandlerThread()
    componentTree.updateLayoutThreadHandler(RunnableHandler.DefaultHandler(newHandlerThread.looper))
    val newThreadLooper = Shadows.shadowOf(newHandlerThread.looper)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(
            componentTree.mainThreadLayoutState?.isForComponentId((newComponent as Component).id))
        .isTrue
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not have compatible size specs")
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(100)

    // Finally, let the async layout finish and make sure it doesn't replace the layout from measure
    blockInPrepare.allowPrepareToComplete()
    asyncLayoutFinish.acquire()
    newComponent.setTestComponentListener(null)
    newThreadLooper.runToEndOfTasks()
    runToEndOfTasks()
    ShadowLooper.runUiThreadTasks()
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(
            componentTree.mainThreadLayoutState?.isForComponentId((newComponent as Component).id))
        .isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(100)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout after measure.")
        .isEqualTo(3)
    newHandlerThread.quit()
  }

  /*
   * Context for the test:
   * - oldComponent will measure to height 100 with heightSpec1 and to 100 with heightSpec2
   * - newComponent will measure to height 1000 with heightSpec1 and to 500 with heightSpec2
   *
   * In this test, we setRootAsync and let its code run but before it can commit on the background
   * thread, we measure with new width/height specs that the original component is compatible with
   * but and the new component isn't.
   */
  @Test
  fun testSetRootAsyncWithIncompatibleMeasureButCompatibleMeasureForExistingLayoutDuringLayout() {
    // TODO (T134949954) reexamine need for this
    if (true) {
      return
    }
    val oldComponent =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    val componentTree = ComponentTree.create(context, oldComponent).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, AT_MOST)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(500, AT_MOST)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    val blockInPrepare = BlockInPrepareComponentListener()
    blockInPrepare.setDoNotBlockOnThisThread()
    val newComponent = TestDrawableComponent.create(context).flexGrow(1f).color(1_234).build()
    newComponent.setTestComponentListener(blockInPrepare)
    componentTree.setRootAsync(newComponent)
    val asyncLayoutFinish = runOnBackgroundThread { runToEndOfTasks() }
    blockInPrepare.awaitPrepareStart()

    // At this point, the Layout thread is blocked in prepare (waiting for unblockAsyncPrepare) and
    // will have already captured the "bad" specs, but not completed its layout.

    // This is a bit of a hack: Robolectric's ShadowLegacyLooper implementation is synchronized
    // on runToEndOfTasks and post(). Since we are simulating being in the middle of calculating a
    // layout, this means that we can't post() to the same Handler (as we will try to do in measure)
    // The "fix" here is to update the layout thread to a new handler/looper that can be controlled
    // separately.
    val newHandlerThread = createAndStartNewHandlerThread()
    componentTree.updateLayoutThreadHandler(RunnableHandler.DefaultHandler(newHandlerThread.looper))
    val newThreadLooper = Shadows.shadowOf(newHandlerThread.looper)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible already.")
        .isTrue
    componentTree.measure(widthSpec2, heightSpec2, IntArray(2), false)
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(
            componentTree.mainThreadLayoutState?.isForComponentId((newComponent as Component).id))
        .isTrue
        .withFailMessage(
            "The main thread should calculate a new layout synchronously because the async layout will not have compatible size specs")
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(500)

    // Finally, let the async layout finish and make sure it doesn't replace the layout from measure
    blockInPrepare.allowPrepareToComplete()
    asyncLayoutFinish.acquire()
    newComponent.setTestComponentListener(null)
    newThreadLooper.runToEndOfTasks()
    runToEndOfTasks()
    ShadowLooper.runUiThreadTasks()
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(
            componentTree.mainThreadLayoutState?.isForComponentId((newComponent as Component).id))
        .isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(500)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a final layout from measure.")
        .isEqualTo(3)
    newHandlerThread.quit()
  }

  @Test
  fun testSetRootAndSetSizeSpecInParallelProduceCorrectResult() {
    // TODO (T134949954) reexamine need for this
    if (true) {
      return
    }
    val oldComponent =
        SimpleMountSpecTester.create(context).widthPx(100).heightPx(100).color(1_234).build()
    val componentTree = ComponentTree.create(context, oldComponent).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, AT_MOST)
    val widthSpec2 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec2 = makeSizeSpec(500, AT_MOST)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)

    // This new component will produce a layout compatible with the first specs but not the second
    // specs.
    val newComponent = TestDrawableComponent.create(context).flexGrow(1f).color(1_234).build()
    val blockInPrepare = BlockInPrepareComponentListener()
    blockInPrepare.setDoNotBlockOnThisThread()
    newComponent.setTestComponentListener(blockInPrepare)
    componentTree.setRootAsync(newComponent)
    val asyncLayout = backgroundLayoutLooperRule.runToEndOfTasksAsync()
    blockInPrepare.awaitPrepareStart()
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2))
        .describedAs("Asserting test setup, second set of specs should be compatible.")
        .isTrue
    componentTree.setSizeSpec(widthSpec2, heightSpec2)
    blockInPrepare.allowPrepareToComplete()
    asyncLayout.acquire()
    ShadowLooper.runUiThreadTasks()
    assertThat(componentTree.root).isEqualTo(newComponent)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(
            componentTree.mainThreadLayoutState?.isForComponentId((newComponent as Component).id))
        .isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(500)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "We expect one initial layout, the async layout (thrown away), and a layout from setSizeSpec.")
        .isEqualTo(3)
  }

  @Test
  fun testComponentTreeHasLatestMainThreadLayoutStateAfterMeasure() {
    val blockInPrepare = BlockInPrepareComponentListener()
    val blockingComponent = TestDrawableComponent.create(context).flexGrow(1f).color(1_234).build()
    blockingComponent.setTestComponentListener(blockInPrepare)
    val lithoView = LithoView(context)
    val componentTree = ComponentTree.create(context, blockingComponent).build()
    lithoView.componentTree = componentTree
    val widthSpec = makeSizeSpec(1_000, AT_MOST)
    val heightSpec = makeSizeSpec(500, EXACTLY)
    val newRoot = Row.create(context).flexGrow(1f).build()
    val backgroundLayout = runOnBackgroundThread {
      blockInPrepare.setDoNotBlockOnThisThread()
      blockInPrepare.awaitPrepareStart()
      componentTree.root = newRoot
      blockInPrepare.allowPrepareToComplete()
    }
    componentTree.attach()
    lithoView.measure(widthSpec, heightSpec)
    backgroundLayout.acquire()

    // In this case, because the background layout completed before the measure layout, the
    // measure layout won't be committed. We want to ensure that we still have a non-null and
    // compatible main thread LayoutState at the end of measure.
    assertThat(componentTree.hasCompatibleLayout(widthSpec, heightSpec)).isTrue
    assertThat(componentTree.mainThreadLayoutState).isNotNull
    assertThat(componentTree.mainThreadLayoutState?.isForComponentId(newRoot.id)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(500)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs(
            "One from measure that will be thrown away and one from the background setRoot")
        .isEqualTo(2)
  }

  @Test
  fun testSetSizeSpecAsyncFollowedBySetSizeSpecSyncBeforeStartReturnsCorrectSize() {
    val component = SimpleMountSpecTester.create(context).flexGrow(1f).color(1_234).build()
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.setLithoView(LithoView(context))
    val widthSpec1 = makeSizeSpec(1_000, EXACTLY)
    val heightSpec1 = makeSizeSpec(1_000, AT_MOST)
    val widthSpec2 = makeSizeSpec(500, EXACTLY)
    val heightSpec2 = makeSizeSpec(500, AT_MOST)
    componentTree.attach()
    componentTree.measure(widthSpec1, heightSpec1, IntArray(2), false)
    componentTree.setSizeSpecAsync(widthSpec2, heightSpec2)
    val size = Size()
    componentTree.setSizeSpec(widthSpec2, heightSpec2, size)
    assertThat(size).isEqualToComparingFieldByField(Size(500, 500))
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    ShadowLooper.runUiThreadTasks()
    assertThat(componentTree.root).isEqualTo(component)
    assertThat(componentTree.hasCompatibleLayout(widthSpec2, heightSpec2)).isTrue
    assertThat(componentTree.mainThreadLayoutState?.height).isEqualTo(500)
    assertThat(componentTree.mainThreadLayoutState?.width).isEqualTo(500)
    assertThat(lithoStatsRule.componentCalculateLayoutCount)
        .describedAs("We expect one initial layout and the async layout.")
        .isEqualTo(2)
  }

  @Test
  fun testSetRootAfterRelease() {
    val componentTree = ComponentTree.create(context, component).build()
    componentTree.release()

    // Verify we don't crash
    componentTree.root = SimpleMountSpecTester.create(context).build()
  }

  @Test
  fun testCachedValues() {
    val componentTree = ComponentTree.create(context, component).build()
    assertThat(componentTree.getCachedValue("key1", false)).isNull()
    componentTree.putCachedValue("key1", "value1", false)
    assertThat(componentTree.getCachedValue("key1", false)).isEqualTo("value1")
    assertThat(componentTree.getCachedValue("key2", false)).isNull()
  }

  @Test
  fun testVersioningCalculate() {
    val root1: Component = ComponentTreeTester.create(context).build()
    val componentTree = ComponentTree.create(context).build()
    componentTree.setVersionedRootAndSizeSpec(root1, widthSpec, heightSpec, Size(), null, 0)
    var layoutState = componentTree.mainThreadLayoutState
    junit.framework.Assert.assertEquals(root1.id, layoutState?.rootComponent?.id)
    val root2: Component = ComponentTreeTester.create(context).build()
    val root3: Component = ComponentTreeTester.create(context).build()
    componentTree.setVersionedRootAndSizeSpec(root3, widthSpec, heightSpec, Size(), null, 2)
    layoutState = componentTree.mainThreadLayoutState
    junit.framework.Assert.assertEquals(root3.id, layoutState?.rootComponent?.id)
    componentTree.setVersionedRootAndSizeSpec(root2, widthSpec, heightSpec, Size(), null, 1)
    layoutState = componentTree.mainThreadLayoutState
    junit.framework.Assert.assertEquals(root3.id, layoutState?.rootComponent?.id)
  }

  @Test
  fun testAttachFromListenerDoesntCrash() {
    val component = TestLayoutComponent.create(context).build()
    val lithoView = LithoView(context)
    val componentTree = ComponentTree.create(context, component).build()
    lithoView.componentTree = componentTree
    componentTree.newLayoutStateReadyListener =
        ComponentTree.NewLayoutStateReadyListener { lithoView.onAttachedToWindow() }
    componentTree.setRootAndSizeSpecSync(this.component, widthSpec, heightSpec)
  }

  @Test
  fun testDetachFromListenerDoesntCrash() {
    val component = TestLayoutComponent.create(context).build()
    val lithoView = LithoView(context)
    val componentTree = ComponentTree.create(context, component).build()
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    componentTree.newLayoutStateReadyListener =
        ComponentTree.NewLayoutStateReadyListener { componentTree ->
          lithoView.onDetachedFromWindow()
          componentTree.clearLithoView()
        }
    componentTree.setRootAndSizeSpecSync(this.component, widthSpec, heightSpec)
  }

  @Test
  fun testComponentIdIsUniqueAfterShallowCopy() {
    val firstComponent = Column.create(context).build()
    val copyFirstComponent = firstComponent.makeShallowCopyWithNewId()
    val secondComponent = Column.create(context).build()
    assertThat(secondComponent.id).isNotEqualTo(copyFirstComponent.id)
    assertThat(secondComponent.id).isNotEqualTo(firstComponent.id)
    assertThat(firstComponent.id).isNotEqualTo(secondComponent.id)
  }

  private class DoubleMeasureViewGroup(context: Context?) : ViewGroup(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      if (childCount == 0) {
        throw RuntimeException("Missing child!")
      }
      if (getMode(widthMeasureSpec) == UNSPECIFIED || getMode(heightMeasureSpec) == UNSPECIFIED) {
        throw RuntimeException("Must give AT_MOST or EXACT measurements")
      }
      val child = getChildAt(0)
      child.measure(
          makeSizeSpec(getSize(widthMeasureSpec), AT_MOST),
          makeSizeSpec(getSize(heightMeasureSpec), AT_MOST))
      child.measure(
          makeSizeSpec(child.measuredWidth, AT_MOST), makeSizeSpec(child.measuredHeight, AT_MOST))
      setMeasuredDimension(child.measuredWidth, child.measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      if (childCount > 0) {
        val child = getChildAt(0)
        child.layout(0, 0, child.measuredWidth, child.measuredHeight)
      }
    }
  }

  companion object {
    private fun componentTreeHasSizeSpec(componentTree: ComponentTree): Boolean {
      synchronized(componentTree) {
        return componentTree.widthSpec != SIZE_UNINITIALIZED &&
            componentTree.heightSpec != SIZE_UNINITIALIZED
      }
    }

    private fun runOnBackgroundThread(runnable: Runnable): TimeOutSemaphore {
      val latch = TimeOutSemaphore(0)
      Thread {
            try {
              runnable.run()
            } catch (e: Exception) {
              latch.setException(e)
            } catch (e: AssertionFailedError) {
              latch.setException(e)
            }
            latch.release()
          }
          .start()
      return latch
    }

    private fun createAndStartNewHandlerThread(): HandlerThread {
      val newHandlerThread =
          HandlerThread(
              "test_handler_thread", ComponentsConfiguration.DEFAULT_BACKGROUND_THREAD_PRIORITY)
      newHandlerThread.start()
      return newHandlerThread
    }
  }
}
