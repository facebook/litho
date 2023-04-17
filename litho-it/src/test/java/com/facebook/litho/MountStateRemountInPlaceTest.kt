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

import android.content.res.Configuration
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.atMost
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.logging.TestComponentsLogger
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecLifecycleTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy

@RunWith(LithoTestRunner::class)
class MountStateRemountInPlaceTest {

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()
  private lateinit var context: ComponentContext
  private lateinit var componentsLogger: TestComponentsLogger

  @Before
  fun setup() {
    componentsLogger = TestComponentsLogger()
    context = ComponentContext(ApplicationProvider.getApplicationContext(), "tag", componentsLogger)
  }

  @Test
  fun testMountUnmountWithShouldUpdate() {
    val firstComponent = TestDrawableComponent.create(context).color(Color.RED).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent = TestDrawableComponent.create(context).color(Color.BLUE).build()
    lithoView.componentTree?.root = Column.create(context).child(secondComponent).build()
    assertThat(secondComponent.wasOnMountCalled()).isTrue
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue
  }

  @Test
  fun testRebindWithNoShouldUpdate() {
    val firstComponent = TestDrawableComponent.create(context).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent = TestDrawableComponent.create(context).build()
    lithoView.componentTree?.root = Column.create(context).child(secondComponent).build()
    assertThat(secondComponent.wasOnMountCalled()).isFalse
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
  }

  @Test
  fun testMountUnmountWithNewOrientation() {
    val tracker = LifecycleTracker()
    val root = MountSpecLifecycleTester.create(context).lifecycleTracker(tracker).build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(20))
        .measure()
        .layout()
    assertThat(tracker.steps)
        .describedAs("Should call lifecycle methods")
        .contains(LifecycleStep.ON_BIND, LifecycleStep.ON_MOUNT)
        .doesNotContain(LifecycleStep.ON_UNMOUNT)
    tracker.reset()
    legacyLithoViewRule.setRootAndSizeSpecSync(root, exactly(20), exactly(10)).measure().layout()
    assertThat(tracker.steps)
        .describedAs("Should call lifecycle methods")
        .contains(
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
  }

  @Test
  fun mountState_onNoForceShouldUpdateAndNewOrientation_shouldNotRemount() {
    context.resources.configuration.orientation = Configuration.ORIENTATION_PORTRAIT
    val firstComponent = TestDrawableComponent.create(context).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    context.resources.configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
    val secondComponent = TestDrawableComponent.create(context).build()
    lithoView.componentTree?.root = Column.create(context).child(secondComponent).build()
    assertThat(secondComponent.wasOnMountCalled()).isFalse
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnbindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
  }

  @Test
  fun mountState_onNoForceShouldUpdateAndNewOrientationAndSameSize_shouldNotRemount() {
    context.resources.configuration.orientation = Configuration.ORIENTATION_PORTRAIT
    val firstComponent =
        TestDrawableComponent.create(
                context, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    context.resources.configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
    val secondComponent =
        TestDrawableComponent.create(
                context, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build()
    lithoView.componentTree?.root = Column.create(context).child(secondComponent).build()
    assertThat(secondComponent.wasOnMountCalled()).isFalse
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnbindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
  }

  @Test
  fun testMountUnmountWithNoShouldUpdateAndDifferentSize() {
    val firstComponent =
        TestDrawableComponent.create(
                context, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent =
        TestDrawableComponent.create(
                context, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(11)
            .build()
    lithoView.componentTree?.root = Column.create(context).child(secondComponent).build()
    assertThat(secondComponent.wasOnMountCalled()).isTrue
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue
  }

  @Test
  fun testMountUnmountWithNoShouldUpdateAndSameSize() {
    val firstComponent =
        TestDrawableComponent.create(
                context, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent =
        TestDrawableComponent.create(
                context, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build()
    lithoView.componentTree?.root = Column.create(context).child(secondComponent).build()
    assertThat(secondComponent.wasOnMountCalled()).isFalse
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
  }

  @Test
  fun testMountUnmountWithNoShouldUpdateAndDifferentMeasures() {
    val firstComponent = TestDrawableComponent.create(context).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            LithoView(context),
            ComponentTree.create(context, Column.create(context).child(firstComponent).build())
                .build(),
            atMost(100),
            atMost(100))
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent = TestDrawableComponent.create(context).build()
    lithoView.componentTree?.root =
        Column.create(context).child(secondComponent).widthPx(10).heightPx(10).build()
    assertThat(lithoView.isLayoutRequested).isTrue
    assertThat(secondComponent.wasOnMountCalled()).isFalse
    assertThat(secondComponent.wasOnBindCalled()).isFalse
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
  }

  @Test
  fun testMountUnmountWithNoShouldUpdateAndSameMeasures() {
    val firstComponent =
        TestDrawableComponent.create(context, 0, 0, true, true, false, true)
            .color(Color.GRAY)
            .build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            LithoView(context),
            ComponentTree.create(context, Column.create(context).child(firstComponent).build())
                .build(),
            exactly(100),
            exactly(100))
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent = TestDrawableComponent.create(context).color(Color.RED).build()
    lithoView.componentTree?.root =
        Column.create(context).child(secondComponent).widthPx(10).heightPx(10).build()
    assertThat(lithoView.isLayoutRequested).isFalse
    assertThat(secondComponent.wasOnMountCalled()).isTrue
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue
  }

  @Test
  fun testRebindWithNoShouldUpdateAndSameMeasures() {
    val firstComponent = TestDrawableComponent.create(context).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            LithoView(context),
            ComponentTree.create(context, Column.create(context).child(firstComponent).build())
                .build(),
            exactly(100),
            exactly(100))
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent = TestDrawableComponent.create(context).build()
    lithoView.componentTree?.root =
        Column.create(context).child(secondComponent).widthPx(10).heightPx(10).build()
    assertThat(lithoView.isLayoutRequested).isFalse
    assertThat(secondComponent.wasOnMountCalled()).isFalse
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
  }

  @Test
  fun testMountUnmountWithSkipShouldUpdate() {
    val firstComponent = TestDrawableComponent.create(context).color(Color.BLACK).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent = TestDrawableComponent.create(context).color(Color.BLACK).build()
    lithoView.componentTree?.root = Column.create(context).child(secondComponent).build()
    assertThat(secondComponent.wasOnMountCalled()).isFalse
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
  }

  @Test
  fun testMountUnmountWithSkipShouldUpdateAndRemount() {
    val firstComponent = TestDrawableComponent.create(context).color(Color.BLACK).build()
    val lithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent = TestDrawableComponent.create(context).color(Color.WHITE).build()
    lithoView.componentTree?.root = Column.create(context).child(secondComponent).build()
    assertThat(secondComponent.wasOnMountCalled()).isTrue
    assertThat(secondComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue
  }

  @Test
  fun testMountUnmountDoesNotSkipShouldUpdateAndRemount() {
    val firstComponent = TestDrawableComponent.create(context).color(Color.RED).build()
    val firstLithoView =
        ComponentTestHelper.mountComponent(
            context, Column.create(context).child(firstComponent).build())
    assertThat(firstComponent.wasOnMountCalled()).isTrue
    assertThat(firstComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse
    val secondComponent = TestDrawableComponent.create(context).color(Color.BLUE).build()
    val secondTree =
        ComponentTree.create(context, Column.create(context).child(secondComponent).build()).build()
    secondTree.setSizeSpec(100, 100)
    val thirdComponent = spy(TestDrawableComponent.create(context).build())
    doReturn(thirdComponent).`when`(thirdComponent).makeShallowCopy()
    secondTree.root = Column.create(context).child(thirdComponent).build()
    ComponentTestHelper.mountComponent(firstLithoView, secondTree)
    assertThat(thirdComponent.wasOnMountCalled()).isTrue
    assertThat(thirdComponent.wasOnBindCalled()).isTrue
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue
  }
}
