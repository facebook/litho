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

package com.facebook.rendercore;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.rendercore.renderunits.HostRenderUnit;
import com.facebook.rendercore.testing.DrawableWrapperUnit;
import com.facebook.rendercore.testing.LayoutResultWrappingNode;
import com.facebook.rendercore.testing.RenderCoreTestRule;
import com.facebook.rendercore.testing.SimpleLayoutResult;
import com.facebook.rendercore.testing.ViewWrapperUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HostViewTest {

  public final @Rule RenderCoreTestRule mRenderCoreTestRule = new RenderCoreTestRule();
  private Context mContext = ApplicationProvider.getApplicationContext();

  @Test
  public void testOnInterceptTouchEvent_withHandler() {
    final HostView hostView = new HostView(mContext);
    final MotionEvent ev = mock(MotionEvent.class);

    assertThat(hostView.onInterceptTouchEvent(ev)).isFalse();

    hostView.setInterceptTouchEventHandler(
        new InterceptTouchHandler() {
          @Override
          public boolean onInterceptTouchEvent(View view, MotionEvent ev) {
            return true;
          }
        });

    assertThat(hostView.onInterceptTouchEvent(ev)).isTrue();
  }

  @Test
  public void testMountViewItem() {
    final HostView hostView = new HostView(mContext);
    final View content = new View(mContext);
    final MountItem mountItem = createMountItem(hostView, content, 1);
    hostView.mount(0, mountItem);

    assertThat(hostView.getMountItemAt(0)).isSameAs(mountItem);
    assertThat(hostView.getChildAt(0)).isSameAs(content);
  }

  @Test
  public void testMoveItemToEnd() {
    final HostView hostView = new HostView(mContext);
    final MountItem mountItem = createMountItem(hostView, new View(mContext), 1);
    hostView.mount(0, mountItem);
    hostView.moveItem(mountItem, 0, 100);

    assertThat(hostView.getMountItemAt(100)).isSameAs(mountItem);
  }

  @Test
  public void testSwapItems() {
    final HostView hostView = new HostView(mContext);
    final MountItem mountItem = createMountItem(hostView, new View(mContext), 1);
    final MountItem mountItem2 = createMountItem(hostView, new View(mContext), 2);

    hostView.mount(0, mountItem);
    hostView.mount(1, mountItem2);

    hostView.moveItem(mountItem, 0, 1);
    hostView.moveItem(mountItem2, 1, 0);

    assertThat(hostView.getMountItemAt(0)).isSameAs(mountItem2);
    assertThat(hostView.getMountItemAt(1)).isSameAs(mountItem);
  }

  @Test
  public void onRenderMixedHierarchy_HostViewShouldHaveExpectedState() {
    final Context c = mRenderCoreTestRule.getContext();
    final List<Long> drawTracker = new ArrayList<>();
    final List<Long> setStateTracker = new ArrayList<>();
    final List<Long> jumpToCurrentStateTracker = new ArrayList<>();

    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(240)
            .height(240)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        new DrawableWrapperUnit(
                            new TrackingColorDrawable(
                                Color.BLACK,
                                2,
                                true,
                                drawTracker,
                                setStateTracker,
                                jumpToCurrentStateTracker),
                            2))
                    .width(120)
                    .height(120))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        new DrawableWrapperUnit(
                            new TrackingColorDrawable(
                                Color.BLUE,
                                3,
                                false,
                                drawTracker,
                                setStateTracker,
                                jumpToCurrentStateTracker),
                            3))
                    .x(5)
                    .y(5)
                    .width(110)
                    .height(110))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 4))
                    .x(10)
                    .y(10)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(
                        new DrawableWrapperUnit(
                            new TrackingColorDrawable(
                                Color.BLUE,
                                5,
                                true,
                                drawTracker,
                                setStateTracker,
                                jumpToCurrentStateTracker),
                            5))
                    .x(15)
                    .y(15)
                    .width(95)
                    .height(95))
            .build();

    mRenderCoreTestRule.useRootNode(new LayoutResultWrappingNode(root)).render();

    final HostView host = (HostView) mRenderCoreTestRule.getRootHost();

    assertThat(host.getMountItemCount()).describedAs("Mounted items").isEqualTo(4);

    host.dispatchDraw(new Canvas());
    assertThat(drawTracker).describedAs("Draw order").containsExactly(2L, 3L, 5L);
    assertThat(setStateTracker).describedAs("Set State order").containsExactly(2L, 5L);

    setStateTracker.clear();
    host.drawableStateChanged();
    assertThat(setStateTracker).describedAs("Set State order").containsExactly(2L, 5L);

    assertThat(jumpToCurrentStateTracker)
        .describedAs("Current state order")
        .containsExactly(2L, 3L, 5L);

    jumpToCurrentStateTracker.clear();

    host.jumpDrawablesToCurrentState();
    assertThat(jumpToCurrentStateTracker)
        .describedAs("Current state order")
        .containsExactly(2L, 3L, 5L);

    MountItem item = host.getMountItemAt(1);
    host.unmount(item);

    assertThat(host.getMountItemCount()).describedAs("Mounted items").isEqualTo(3);

    drawTracker.clear();
    host.dispatchDraw(new Canvas());
    assertThat(drawTracker).describedAs("Draw order").containsExactly(2L, 5L);

    host.setVisibility(View.INVISIBLE);

    assertThat(((Drawable) host.getMountItemAt(0).getContent()).isVisible())
        .describedAs("Drawable visibility is")
        .isFalse();
    assertThat(((Drawable) host.getMountItemAt(3).getContent()).isVisible())
        .describedAs("Drawable visibility is")
        .isFalse();
  }

  @Test
  public void onRenderUnitWithTouchableDrawable_shouldHandleTouchEvent() {
    final Point point = new Point(0, 0);
    final DrawableWrapperUnit unit =
        new DrawableWrapperUnit(
            new TouchableColorDrawable(
                new View.OnTouchListener() {

                  @Override
                  public boolean onTouch(View v, MotionEvent event) {
                    point.x = (int) event.getX();
                    point.y = (int) event.getY();
                    return true;
                  }
                }),
            1);

    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(100)
            .height(100)
            .child(SimpleLayoutResult.create().renderUnit(unit).width(100).height(100))
            .build();

    mRenderCoreTestRule.useRootNode(new LayoutResultWrappingNode(root)).render();

    final HostView host = (HostView) mRenderCoreTestRule.getRootHost();
    final MotionEvent event =
        MotionEvent.obtain(200, 300, MotionEvent.ACTION_DOWN, 10.0f, 10.0f, 0);
    host.onTouchEvent(event);

    assertThat(point.x).describedAs("touch x is").isEqualTo(10);
    assertThat(point.y).describedAs("touch y is").isEqualTo(10);
  }

  @Test
  public void onHostRenderUnitWithTouchListener_shouldHandleTouchEvent() {
    final HostRenderUnit unit = new HostRenderUnit(1);
    final Point point = new Point(0, 0);
    unit.setOnTouchListener(
        new View.OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
            point.x = (int) event.getX();
            point.y = (int) event.getY();
            return true;
          }
        });

    final LayoutResult root =
        SimpleLayoutResult.create().renderUnit(unit).width(100).height(100).build();

    mRenderCoreTestRule.useRootNode(new LayoutResultWrappingNode(root)).render();

    final HostView host = (HostView) ((HostView) mRenderCoreTestRule.getRootHost()).getChildAt(0);
    final MotionEvent event =
        MotionEvent.obtain(200, 300, MotionEvent.ACTION_BUTTON_PRESS, 10.0f, 10.0f, 0);
    host.dispatchTouchEvent(event);

    assertThat(point.x).describedAs("touch x is").isEqualTo(10);
    assertThat(point.y).describedAs("touch y is").isEqualTo(10);
  }

  public static MountItem createMountItem(Host host, Object content, final long id) {
    final RenderUnit.RenderType renderType =
        (content instanceof View) ? RenderUnit.RenderType.VIEW : RenderUnit.RenderType.DRAWABLE;
    final RenderUnit renderUnit = new TestRenderUnit(renderType);
    return new MountItem(
        new RenderTreeNode(null, renderUnit, null, new Rect(), new Rect(), 0), content);
  }

  private static class TestRenderUnit extends RenderUnit implements ContentAllocator<Object> {

    public TestRenderUnit(RenderUnit.RenderType renderType) {
      super(renderType);
    }

    @Override
    public Object createContent(Context context) {
      return null;
    }

    @Override
    public ContentAllocator<Object> getContentAllocator() {
      return this;
    }

    @Override
    public long getId() {
      return 0;
    }
  }

  public static class TrackingColorDrawable extends ColorDrawable {

    private final long mId;
    private final boolean mIsStateful;
    private final List<Long> mDrawTracker;
    private final List<Long> mSetStateTracker;
    private final List<Long> mJumpToCurrentStateTracker;

    public TrackingColorDrawable(
        final int color,
        final long id,
        final boolean isStateful,
        final List<Long> drawTracker,
        final List<Long> setStateTracker,
        final List<Long> jumpToCurrentStateTracker) {
      super(color);
      mId = id;
      mIsStateful = isStateful;
      mDrawTracker = drawTracker;
      mSetStateTracker = setStateTracker;
      mJumpToCurrentStateTracker = jumpToCurrentStateTracker;
    }

    @Override
    public void draw(Canvas canvas) {
      super.draw(canvas);
      mDrawTracker.add(mId);
    }

    @Override
    public boolean isStateful() {
      return mIsStateful || super.isStateful();
    }

    @Override
    public boolean setState(int[] stateSet) {
      mSetStateTracker.add(mId);
      return super.setState(stateSet);
    }

    @Override
    public void jumpToCurrentState() {
      mJumpToCurrentStateTracker.add(mId);
      super.jumpToCurrentState();
    }
  }

  public static class TouchableColorDrawable extends ColorDrawable implements Touchable {

    private final View.OnTouchListener mListener;

    public TouchableColorDrawable(final View.OnTouchListener listener) {
      mListener = listener;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event, final View host) {
      return mListener.onTouch(null, event);
    }

    @Override
    public boolean shouldHandleTouchEvent(final MotionEvent event) {
      return true;
    }
  }
}
