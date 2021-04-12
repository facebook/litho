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
import static com.facebook.litho.testing.TestDrawableComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.graphics.Color;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateRemountInPlaceTest {
  private ComponentContext mContext;
  private TestComponentsLogger mComponentsLogger;

  @Before
  public void setup() {
    mComponentsLogger = new TestComponentsLogger();
    mContext = new ComponentContext(getApplicationContext(), "tag", mComponentsLogger);
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

    assertThat(secondComponent.wasOnMountCalled()).isTrue();
    assertThat(secondComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnbindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void mountState_onNoForceShouldUpdateAndNewOrientation_shouldNotRemount() {
    ComponentsConfiguration.shouldForceComponentUpdateOnOrientationChange = false;

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

    ComponentsConfiguration.shouldForceComponentUpdateOnOrientationChange = true;
  }

  @Test
  public void mountState_onNoForceShouldUpdateAndNewOrientationAndSameSize_shouldNotRemount() {
    ComponentsConfiguration.shouldForceComponentUpdateOnOrientationChange = false;

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

    ComponentsConfiguration.shouldForceComponentUpdateOnOrientationChange = true;
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

    verify(thirdComponent).makeShallowCopy();

    assertThat(thirdComponent.wasOnMountCalled()).isTrue();
    assertThat(thirdComponent.wasOnBindCalled()).isTrue();
    assertThat(firstComponent.wasOnUnmountCalled()).isTrue();
  }

  @Test
  public void testRemountSameSubTreeWithDifferentParentHost() {
    ComponentContext scopedContext =
        ComponentContext.withComponentScope(mContext, Row.create(mContext).build(), null);
    final TestComponent firstComponent =
        TestDrawableComponent.create(scopedContext, true, true, false)
            .widthPx(100)
            .heightPx(100)
            .build();

    final Component firstLayout =
        Column.create(scopedContext)
            .child(
                Column.create(scopedContext)
                    .clickHandler(scopedContext.newEventHandler(3))
                    .child(Text.create(scopedContext).widthPx(100).heightPx(100).text("test")))
            .child(
                Column.create(scopedContext)
                    .clickHandler(scopedContext.newEventHandler(2))
                    .child(Text.create(scopedContext).widthPx(100).heightPx(100).text("test2"))
                    .child(
                        Column.create(scopedContext)
                            .clickHandler(scopedContext.newEventHandler(1))
                            .child(firstComponent)
                            .child(
                                SolidColor.create(scopedContext)
                                    .widthPx(100)
                                    .heightPx(100)
                                    .color(Color.GREEN))))
            .build();

    final Component secondLayout =
        Column.create(scopedContext)
            .child(
                Column.create(scopedContext)
                    .clickHandler(scopedContext.newEventHandler(3))
                    .child(Text.create(scopedContext).widthPx(100).heightPx(100).text("test"))
                    .child(
                        Column.create(scopedContext)
                            .clickHandler(scopedContext.newEventHandler(1))
                            .child(firstComponent)
                            .child(
                                SolidColor.create(scopedContext)
                                    .widthPx(100)
                                    .heightPx(100)
                                    .color(Color.GREEN))))
            .child(
                Column.create(scopedContext)
                    .clickHandler(scopedContext.newEventHandler(2))
                    .child(Text.create(scopedContext).widthPx(100).heightPx(100).text("test2")))
            .build();

    ComponentTree tree = ComponentTree.create(scopedContext, firstLayout).build();
    LithoView cv = new LithoView(scopedContext);

    ComponentTestHelper.mountComponent(cv, tree);
    tree.setRoot(secondLayout);

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
