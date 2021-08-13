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

package com.facebook.litho;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MOVED_COUNT;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.TestDrawableComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.graphics.Color;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.widget.LayoutSpecConditionalReParenting;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import com.facebook.litho.widget.TextInput;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class MountStateRemountInPlaceTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  private final boolean useStatelessComponent;
  private final boolean useStatelessComponentDefault;

  private ComponentContext mContext;
  private TestComponentsLogger mComponentsLogger;

  public MountStateRemountInPlaceTest(boolean useStatelessComponent) {
    this.useStatelessComponent = useStatelessComponent;
    useStatelessComponentDefault = ComponentsConfiguration.useStatelessComponent;
  }

  @ParameterizedRobolectricTestRunner.Parameters(name = "useStatelessComponent={0}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false}, {true},
        });
  }

  @Before
  public void setup() {
    ComponentsConfiguration.useStatelessComponent = useStatelessComponent;
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(getApplicationContext(), "tag", mComponentsLogger);
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.useStatelessComponent = useStatelessComponentDefault;
  }

  @Test
  public void testMountUnmountWithShouldUpdate() {
    final TestComponent firstComponent = create(mContext).color(Color.RED).build();

    final LithoView lithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent = create(mContext).color(Color.BLUE).build();

    lithoView.getComponentTree().setRoot(Column.create(mContext).child(secondComponent).build());

    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testRebindWithNoShouldUpdate() {
    final TestComponent firstComponent = create(mContext).build();

    final LithoView lithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent = create(mContext).build();

    lithoView.getComponentTree().setRoot(Column.create(mContext).child(secondComponent).build());

    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithNewOrientation() {
    final LifecycleTracker tracker = new LifecycleTracker();
    final Component root =
        MountSpecLifecycleTester.create(mContext).lifecycleTracker(tracker).build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(20, SizeSpec.EXACTLY))
        .measure()
        .layout();

    assertThat(tracker.getSteps())
        .describedAs("Should call lifecycle methods")
        .contains(LifecycleStep.ON_BIND, LifecycleStep.ON_MOUNT)
        .doesNotContain(LifecycleStep.ON_UNMOUNT);

    tracker.reset();

    mLithoViewRule
        .setRootAndSizeSpec(
            root, makeSizeSpec(20, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY))
        .measure()
        .layout();

    assertThat(tracker.getSteps())
        .describedAs("Should call lifecycle methods")
        .contains(
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void mountState_onNoForceShouldUpdateAndNewOrientation_shouldNotRemount() {

    mContext.getResources().getConfiguration().orientation = ORIENTATION_PORTRAIT;
    final TestComponent firstComponent = create(mContext).build();

    final LithoView lithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    mContext.getResources().getConfiguration().orientation = ORIENTATION_LANDSCAPE;
    final TestComponent secondComponent = create(mContext).build();

    lithoView.getComponentTree().setRoot(Column.create(mContext).child(secondComponent).build());

    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnbindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void mountState_onNoForceShouldUpdateAndNewOrientationAndSameSize_shouldNotRemount() {
    mContext.getResources().getConfiguration().orientation = ORIENTATION_PORTRAIT;
    final TestComponent firstComponent =
        create(mContext, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build();

    final LithoView lithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    mContext.getResources().getConfiguration().orientation = ORIENTATION_LANDSCAPE;
    final TestComponent secondComponent =
        create(mContext, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build();

    lithoView.getComponentTree().setRoot(Column.create(mContext).child(secondComponent).build());

    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnbindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdateAndDifferentSize() {
    final TestComponent firstComponent =
        create(mContext, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build();

    final LithoView lithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(11)
            .build();

    lithoView.getComponentTree().setRoot(Column.create(mContext).child(secondComponent).build());

    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdateAndSameSize() {
    final TestComponent firstComponent =
        create(mContext, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build();

    final LithoView lithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent =
        create(mContext, 0, 0, true, true, false, true /*isMountSizeDependent*/)
            .measuredHeight(10)
            .build();

    lithoView.getComponentTree().setRoot(Column.create(mContext).child(secondComponent).build());

    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdateAndDifferentMeasures() {
    final TestComponent firstComponent = create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            new LithoView(mContext),
            ComponentTree.create(mContext, Column.create(mContext).child(firstComponent).build())
                .build(),
            makeMeasureSpec(100, AT_MOST),
            makeMeasureSpec(100, AT_MOST));

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent = create(mContext).build();

    lithoView
        .getComponentTree()
        .setRoot(Column.create(mContext).child(secondComponent).widthPx(10).heightPx(10).build());

    assertThat(lithoView.isLayoutRequested()).isTrue();
    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isFalse();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithNoShouldUpdateAndSameMeasures() {
    final TestComponent firstComponent =
        create(mContext, 0, 0, true, true, false, true).color(Color.GRAY).build();

    final LithoView lithoView =
        mountComponent(
            new LithoView(mContext),
            ComponentTree.create(mContext, Column.create(mContext).child(firstComponent).build())
                .build(),
            makeMeasureSpec(100, EXACTLY),
            makeMeasureSpec(100, EXACTLY));

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent = create(mContext).color(Color.RED).build();

    lithoView
        .getComponentTree()
        .setRoot(Column.create(mContext).child(secondComponent).widthPx(10).heightPx(10).build());

    assertThat(lithoView.isLayoutRequested()).isFalse();
    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testRebindWithNoShouldUpdateAndSameMeasures() {
    final TestComponent firstComponent = create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            new LithoView(mContext),
            ComponentTree.create(mContext, Column.create(mContext).child(firstComponent).build())
                .build(),
            makeMeasureSpec(100, EXACTLY),
            makeMeasureSpec(100, EXACTLY));

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent = create(mContext).build();

    lithoView
        .getComponentTree()
        .setRoot(Column.create(mContext).child(secondComponent).widthPx(10).heightPx(10).build());

    assertThat(lithoView.isLayoutRequested()).isFalse();
    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithSkipShouldUpdate() {
    final TestComponent firstComponent = create(mContext).color(BLACK).build();

    final LithoView lithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent = create(mContext).color(BLACK).build();

    lithoView.getComponentTree().setRoot(Column.create(mContext).child(secondComponent).build());

    assertThat(secondComponent.wasOnMountCalled()).isFalse();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();
  }

  @Test
  public void testMountUnmountWithSkipShouldUpdateAndRemount() {
    final TestComponent firstComponent = create(mContext).color(BLACK).build();

    final LithoView lithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent = create(mContext).color(WHITE).build();

    lithoView.getComponentTree().setRoot(Column.create(mContext).child(secondComponent).build());

    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testMountUnmountDoesNotSkipShouldUpdateAndRemount() {
    final TestComponent firstComponent = create(mContext).color(Color.RED).build();

    final LithoView firstLithoView =
        mountComponent(mContext, Column.create(mContext).child(firstComponent).build());

    assertThat(firstComponent.wasOnMountCalled()).isTrue();
    assertThat(firstComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isFalse();

    final TestComponent secondComponent = create(mContext).color(Color.BLUE).build();

    final ComponentTree secondTree =
        ComponentTree.create(mContext, Column.create(mContext).child(secondComponent).build())
            .build();
    secondTree.setSizeSpec(100, 100);

    final TestComponent thirdComponent = spy(create(mContext).build());

    doReturn(thirdComponent).when(thirdComponent).makeShallowCopy();

    secondTree.setRoot(Column.create(mContext).child(thirdComponent).build());

    mountComponent(firstLithoView, secondTree);

    if (!ComponentsConfiguration.shouldSkipShallowCopy) {
      verify(thirdComponent).makeShallowCopy();
    }

    assertThat(thirdComponent.wasOnMountCalled()).isTrue();
    assertThat(thirdComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testRemountSameSubTreeWithDifferentParentHost() {
    ComponentContext c = new ComponentContext(getApplicationContext(), "tag", mComponentsLogger);
    mLithoViewRule.useContext(c);

    Component component = TextInput.create(c).heightDip(100).widthDip(100).build();

    mLithoViewRule
        .attachToWindow()
        .setRootAndSizeSpec(
            LayoutSpecConditionalReParenting.create(c)
                .firstComponent(component)
                .reParent(false)
                .build(),
            MeasureSpecUtils.exactly(1000),
            MeasureSpecUtils.exactly(1000))
        .measure()
        .layout();

    mLithoViewRule
        .attachToWindow()
        .setRootAndSizeSpec(
            LayoutSpecConditionalReParenting.create(c)
                .firstComponent(component)
                .reParent(true)
                .build(),
            MeasureSpecUtils.exactly(1000),
            MeasureSpecUtils.exactly(1000))
        .measure()
        .layout();

    final List<TestPerfEvent> events =
        mComponentsLogger.getLoggedPerfEvents().stream()
            .filter(
                new Predicate<PerfEvent>() {
                  @Override
                  public boolean test(PerfEvent e) {
                    return e.getMarkerId() == FrameworkLogEvents.EVENT_MOUNT;
                  }
                })
            .map(
                new Function<PerfEvent, TestPerfEvent>() {
                  @Override
                  public TestPerfEvent apply(PerfEvent e) {
                    return (TestPerfEvent) e;
                  }
                })
            .collect(Collectors.toList());
    assertThat(events).hasSize(2);
    assertThat(events.get(1).getAnnotations()).containsEntry(PARAM_MOVED_COUNT, 2);
    assertThat(events.get(1).getPoints()).contains("PREPARE_MOUNT_START", "PREPARE_MOUNT_END");
  }
}
