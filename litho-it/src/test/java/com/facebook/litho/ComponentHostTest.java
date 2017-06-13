/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseArray;
import android.view.View;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaDirection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static android.graphics.Color.BLACK;
import static android.view.MotionEvent.obtain;
import static android.view.View.GONE;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.INVISIBLE;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.View.VISIBLE;
import static com.facebook.litho.MountItem.FLAG_DUPLICATE_PARENT_STATE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ComponentHost}
 */
@RunWith(ComponentsTestRunner.class)
public class ComponentHostTest {

  private Component<?> mViewGroupHost;

  private TestableComponentHost mHost;
  private Component<?> mDrawableComponent;
  private Component<?> mViewComponent;
  private ComponentContext mContext;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mViewComponent = TestViewComponent.create(mContext).build();
    mDrawableComponent = TestDrawableComponent.create(mContext).build();

    mHost = new TestableComponentHost(mContext);

    mViewGroupHost = HostComponent.create();
  }

  @Test
  public void testParentHostMarker() {
    assertThat(mHost.getParentHostMarker()).isEqualTo(0);

    mHost.setParentHostMarker(1);
    assertThat(mHost.getParentHostMarker()).isEqualTo(1);
  }

  @Test
  public void testInvalidations() {
    assertThat(mHost.getInvalidationCount()).isEqualTo(0);
    assertThat(mHost.getInvalidationRect()).isNull();

    Drawable d1 = new ColorDrawable();
    d1.setBounds(0, 0, 1, 1);

    MountItem mountItem1 = mount(0, d1);
    assertThat(mHost.getInvalidationCount()).isEqualTo(1);
    assertThat(mHost.getInvalidationRect()).isEqualTo(d1.getBounds());

    Drawable d2 = new ColorDrawable();
    d2.setBounds(0, 0, 2, 2);

    MountItem mountItem2 = mount(1, d2);
    assertThat(mHost.getInvalidationCount()).isEqualTo(2);
    assertThat(mHost.getInvalidationRect()).isEqualTo(d2.getBounds());

    View v1 = new View(mContext);
    Rect v1Bounds = new Rect(0, 0, 10, 10);
    v1.measure(
        makeMeasureSpec(v1Bounds.width(), EXACTLY),
        makeMeasureSpec(v1Bounds.height(), EXACTLY));
    v1.layout(v1Bounds.left, v1Bounds.top, v1Bounds.right, v1Bounds.bottom);

    MountItem mountItem3 = mount(2, v1);
    assertThat(mHost.getInvalidationCount()).isEqualTo(3);
    assertThat(mHost.getInvalidationRect()).isEqualTo(v1Bounds);

    unmount(0, mountItem1);
    assertThat(mHost.getInvalidationCount()).isEqualTo(4);
    assertThat(mHost.getInvalidationRect()).isEqualTo(d1.getBounds());

    unmount(1, mountItem2);
    assertThat(mHost.getInvalidationCount()).isEqualTo(5);
    assertThat(mHost.getInvalidationRect()).isEqualTo(d2.getBounds());

    unmount(2, mountItem3);
    assertThat(mHost.getInvalidationCount()).isEqualTo(6);
    assertThat(mHost.getInvalidationRect()).isEqualTo(v1Bounds);
  }

  @Test
  public void testCallbacks() {
    Drawable d = new ColorDrawable();
    assertThat(d.getCallback()).isNull();

    MountItem mountItem = mount(0, d);
    assertThat(d.getCallback()).isEqualTo(mHost);

    unmount(0, mountItem);
    assertThat(d.getCallback()).isNull();
  }

  @Test
  public void testGetMountItemCount() {
    assertThat(mHost.getMountItemCount()).isEqualTo(0);

    MountItem mountItem1 = mount(0, new ColorDrawable());
    assertThat(mHost.getMountItemCount()).isEqualTo(1);

    mount(1, new ColorDrawable());
    assertThat(mHost.getMountItemCount()).isEqualTo(2);

    MountItem mountItem3 = mount(2, new View(mContext));
    assertThat(mHost.getMountItemCount()).isEqualTo(3);

    unmount(0, mountItem1);
    assertThat(mHost.getMountItemCount()).isEqualTo(2);

    MountItem mountItem4 = mount(1, new ColorDrawable());
    assertThat(mHost.getMountItemCount()).isEqualTo(2);

    unmount(2, mountItem3);
    assertThat(mHost.getMountItemCount()).isEqualTo(1);

    unmount(1, mountItem4);
    assertThat(mHost.getMountItemCount()).isEqualTo(0);
  }

  @Test
  public void testGetMountItemAt() {
    assertThat(mHost.getMountItemAt(0)).isNull();
    assertThat(mHost.getMountItemAt(1)).isNull();
    assertThat(mHost.getMountItemAt(2)).isNull();

    MountItem mountItem1 = mount(0, new ColorDrawable());
    MountItem mountItem2 = mount(1, new View(mContext));
    MountItem mountItem3 = mount(5, new ColorDrawable());

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem2);
    assertThat(mHost.getMountItemAt(2)).isEqualTo(mountItem3);

    unmount(1, mountItem2);

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem3);

    unmount(0, mountItem1);

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem3);
  }

  @Test
  public void testMoveItem() {
    MountItem mountItem1 = mount(1, new ColorDrawable());
    MountItem mountItem2 = mount(2, new View(mContext));

    assertThat(mHost.getMountItemCount()).isEqualTo(2);

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem2);

    mHost.moveItem(mountItem2, 2, 0);

    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem2);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem1);

    mHost.moveItem(mountItem2, 0, 1);

    assertThat(mHost.getMountItemCount()).isEqualTo(1);
    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem2);

    mHost.moveItem(mountItem2, 1, 0);

    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem2);
  }

  @Test
  public void testMoveItemWithoutTouchables()
      throws Exception {
    Drawable d1 = new ColorDrawable(BLACK);
    MountItem mountItem1 = mount(1, d1);

    Drawable d2 = new ColorDrawable(BLACK);
    MountItem mountItem2 = mount(2, d2);

    assertThat(getDrawableItemsSize()).isEqualTo(2);
    assertThat(getDrawableMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(getDrawableMountItemAt(1)).isEqualTo(mountItem2);

    mHost.moveItem(mountItem2, 2, 0);

    // There are no Touchable Drawables so this call should return false and not crash.
    assertThat(mHost.onTouchEvent(obtain(0, 0, 0, 0, 0, 0))).isFalse();
  }

  @Test
  public void testDrawableStateChangedOnDrawables() {
    Drawable d1 = mock(ColorDrawable.class);
    when(d1.getBounds()).thenReturn(new Rect());
    when(d1.isStateful()).thenReturn(false);

    MountItem mountItem1 = mount(0, d1);
    verify(d1, never()).setState(any(int[].class));

    unmount(0, mountItem1);

    Drawable d2 = mock(ColorDrawable.class);
    when(d2.getBounds()).thenReturn(new Rect());
    when(d2.isStateful()).thenReturn(true);

    mount(0, d2, FLAG_DUPLICATE_PARENT_STATE);
    verify(d2, times(1)).setState(eq(mHost.getDrawableState()));

    mHost.setSelected(true);

    verify(d2, times(1)).setState(eq(mHost.getDrawableState()));
  }

  @Test
  public void testMoveTouchExpansionItem() {
    View view = mock(View.class);
    when(view.getContext()).thenReturn(RuntimeEnvironment.application);

    MountItem mountItem = mountTouchExpansionItem(0, view);
    mHost.moveItem(mountItem, 0, 1);

    unmount(1, mountItem);
  }

  @Test
  public void testDuplicateParentStateOnViews() {
    View v1 = mock(View.class);
    mount(0, v1);

    View v2 = mock(View.class);
    mount(1, v2, FLAG_DUPLICATE_PARENT_STATE);

    verify(v1, times(1)).setDuplicateParentStateEnabled(eq(false));
    verify(v2, times(1)).setDuplicateParentStateEnabled(eq(true));
  }

  @Test
  public void testJumpDrawablesToCurrentState() {
    mHost.jumpDrawablesToCurrentState();

    Drawable d1 = mock(ColorDrawable.class);
    when(d1.getBounds()).thenReturn(new Rect());
    mount(0, d1);

    Drawable d2 = mock(ColorDrawable.class);
    when(d2.getBounds()).thenReturn(new Rect());
    mount(1, d2);

    View v1 = mock(View.class);
    mount(2, v1);

    mHost.jumpDrawablesToCurrentState();

    verify(d1, times(1)).jumpToCurrentState();
    verify(d2, times(1)).jumpToCurrentState();
  }

  @Test
  public void testSetVisibility() {
    Drawable d1 = mock(ColorDrawable.class);
    when(d1.getBounds()).thenReturn(new Rect());
    mount(0, d1);

    Drawable d2 = mock(ColorDrawable.class);
    when(d2.getBounds()).thenReturn(new Rect());
    mount(1, d2);

    View v1 = mock(View.class);
    mount(2, v1);

    mHost.setVisibility(GONE);
    mHost.setVisibility(INVISIBLE);
    mHost.setVisibility(VISIBLE);

    verify(d1, times(2)).setVisible(eq(true), eq(false));
    verify(d1, times(2)).setVisible(eq(false), eq(false));

    verify(d2, times(2)).setVisible(eq(true), eq(false));
    verify(d2, times(2)).setVisible(eq(false), eq(false));

    verify(v1, never()).setVisibility(anyInt());
  }

  @Test
  public void testGetDrawables() {
    Drawable d1 = new ColorDrawable();
    MountItem mountItem1 = mount(0, d1);

    Drawable d2 = new ColorDrawable();
    mount(1, d2);

    MountItem mountItem3 = mount(2, new View(mContext));

    List<Drawable> drawables = mHost.getDrawables();
    assertThat(drawables).hasSize(2);
    assertThat(drawables.get(0)).isEqualTo(d1);
    assertThat(drawables.get(1)).isEqualTo(d2);

    unmount(0, mountItem1);

    drawables = mHost.getDrawables();
    assertThat(drawables).hasSize(1);
    assertThat(drawables.get(0)).isEqualTo(d2);

    unmount(2, mountItem3);

    drawables = mHost.getDrawables();
    assertThat(drawables).hasSize(1);
    assertThat(drawables.get(0)).isEqualTo(d2);
  }

  @Test
  public void testViewTag() {
    assertThat(mHost.getTag()).isNull();

    Object tag = new Object();
    mHost.setViewTag(tag);
    assertThat(mHost.getTag()).isEqualTo(tag);

    mHost.setViewTag(null);
    assertThat(mHost.getTag()).isNull();
  }

  @Test
  public void testViewTags() {
    assertThat(mHost.getTag(1)).isNull();
    assertThat(mHost.getTag(2)).isNull();

    Object value1 = new Object();
    Object value2 = new Object();

    SparseArray<Object> viewTags = new SparseArray<>();
    viewTags.put(1, value1);
    viewTags.put(2, value2);

    mHost.setViewTags(viewTags);

    assertThat(mHost.getTag(1)).isEqualTo(value1);
    assertThat(mHost.getTag(2)).isEqualTo(value2);

    mHost.setViewTags(null);

    assertThat(mHost.getTag(1)).isNull();
    assertThat(mHost.getTag(2)).isNull();
  }

  @Test
  public void testComponentClickListener() {
    assertThat(mHost.getComponentClickListener()).isNull();

    ComponentClickListener listener = new ComponentClickListener();
    mHost.setComponentClickListener(listener);

    assertThat(mHost.getComponentClickListener()).isEqualTo(listener);

    mHost.setComponentClickListener(null);
    assertThat(mHost.getComponentClickListener()).isNull();
  }

  @Test
  public void testComponentLongClickListener() {
    assertThat(mHost.getComponentLongClickListener()).isNull();

    ComponentLongClickListener listener = new ComponentLongClickListener();
    mHost.setComponentLongClickListener(listener);

    assertThat(mHost.getComponentLongClickListener()).isEqualTo(listener);

    mHost.setComponentLongClickListener(null);
    assertThat(mHost.getComponentLongClickListener()).isNull();
  }

  @Test
  public void testComponentTouchListener() {
    assertThat(mHost.getComponentTouchListener()).isNull();

    ComponentTouchListener listener = new ComponentTouchListener();
    mHost.setComponentTouchListener(listener);

    assertThat(mHost.getComponentTouchListener()).isEqualTo(listener);

    mHost.setComponentTouchListener(null);
    assertThat(mHost.getComponentTouchListener()).isNull();
  }

  @Test
  public void testSuppressInvalidations() {
    mHost.layout(0, 0, 100, 100);

    mHost.invalidate();
    assertThat(mHost.getInvalidationRect()).isEqualTo(new Rect(0, 0, 100, 100));

    mHost.suppressInvalidations(true);

    mHost.invalidate();
    mHost.invalidate(0, 0, 5, 5);

    mHost.suppressInvalidations(false);

    assertThat(mHost.getInvalidationRect()).isEqualTo(new Rect(0, 0, 100, 100));
  }

  @Test
  public void testSuppressInvalidationsWithCoordinates() {
    mHost.layout(0, 0, 100, 100);

    mHost.invalidate(0, 0, 20, 20);
    assertThat(mHost.getInvalidationRect()).isEqualTo(new Rect(0, 0, 20, 20));

    mHost.suppressInvalidations(true);

    mHost.invalidate(0, 0, 10, 10);
    mHost.invalidate(0, 0, 5, 5);

    mHost.suppressInvalidations(false);

    assertThat(mHost.getInvalidationRect()).isEqualTo(new Rect(0, 0, 100, 100));
  }

  @Test
  public void testSuppressInvalidationsWithRect() {
    mHost.layout(0, 0, 100, 100);

    mHost.invalidate(new Rect(0, 0, 20, 20));
    assertThat(mHost.getInvalidationRect()).isEqualTo(new Rect(0, 0, 20, 20));

    mHost.suppressInvalidations(true);

    mHost.invalidate(new Rect(0, 0, 10, 10));
    mHost.invalidate(new Rect(0, 0, 5, 5));

    mHost.suppressInvalidations(false);

    assertThat(mHost.getInvalidationRect()).isEqualTo(new Rect(0, 0, 100, 100));
  }

  @Test
  public void testNoScrapHosts() {
    assertThat(mHost.recycleHost()).isNull();
  }

  @Test
  public void testViewGroupScrapHosts() {
    testScrapHostsForComponent(mViewGroupHost, ComponentHost.class);
  }

  @Test
  public void testGetContentDescriptions() {
    CharSequence hostContentDescription = "hostContentDescription";
    mHost.setContentDescription(hostContentDescription);

    CharSequence drawableContentDescription = "drawableContentDescription";
    MountItem mountItem0 = mount(0, new ColorDrawable(), 0, drawableContentDescription);
    CharSequence viewContentDescription = "viewContentDescription";
    mount(1, mock(View.class), 0, viewContentDescription);

    assertThat(mHost.getContentDescriptions()).contains(hostContentDescription);
    assertThat(mHost.getContentDescriptions()).contains(drawableContentDescription);
    assertThat(mHost.getContentDescriptions()).doesNotContain(viewContentDescription);

    unmount(0, mountItem0);

    assertThat(mHost.getContentDescriptions()).contains(hostContentDescription);
    assertThat(mHost.getContentDescriptions()).doesNotContain(drawableContentDescription);
    assertThat(mHost.getContentDescriptions()).doesNotContain(viewContentDescription);
  }

  @Test
  public void testGetChildDrawingOrder() {
    View v1 = new View(mContext);
    mount(2, v1);

    View v2 = new View(mContext);
    MountItem mountItem2 = mount(0, v2);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);

    View v3 = new ComponentHost(mContext);
    MountItem mountItem3 = mount(1, v3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(2);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(0);

    mHost.unmount(1, mountItem3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);

    mount(1, v3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(2);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(0);

    mHost.unmount(0, mountItem2);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);

    mHost.moveItem(mountItem3, 1, 3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(0);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(1);
  }

  @Test
  public void testDisappearingItemDrawingOrder() {
    View v1 = new View(mContext);
    mount(5, v1);

    View v2 = new View(mContext);
    mount(2, v2);

    View v3 = new View(mContext);
    MountItem mountItem3 = mount(4, v3);

    View v4 = new View(mContext);
    MountItem mountItem4 = mount(0, v4);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(3);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(2);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 3)).isEqualTo(0);

    assertThat(mHost.getMountItemCount()).isEqualTo(4);
    assertThat(mHost.getChildCount()).isEqualTo(4);

    // mountItem3 started disappearing
    mHost.startUnmountDisappearingItem(4, mountItem3);

    assertThat(mHost.getMountItemCount()).isEqualTo(3);
    assertThat(mHost.getChildCount()).isEqualTo(4);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(3);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(0);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 3)).isEqualTo(2);

    // mountItem4 started disappearing
    mHost.startUnmountDisappearingItem(0, mountItem4);

    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getChildCount()).isEqualTo(4);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(3);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 3)).isEqualTo(2);

    // mountItem4 finished disappearing
    mHost.unmountDisappearingItem(mountItem4);
    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getChildCount()).isEqualTo(3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(2);

    // mountItem3 finished disappearing
    mHost.unmountDisappearingItem(mountItem3);
    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getChildCount()).isEqualTo(2);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);
  }

  @Test
  public void testDrawableItemsSize()
      throws Exception {

    assertThat(getDrawableItemsSize()).isEqualTo(0);

    assertThat(getDrawableItemsSize()).isEqualTo(0);

    Drawable d1 = new ColorDrawable(BLACK);
    MountItem m1 = mount(0, d1);
    assertThat(getDrawableItemsSize()).isEqualTo(1);

    Drawable d2 = new ColorDrawable(BLACK);
    mount(1, d2);
    assertThat(getDrawableItemsSize()).isEqualTo(2);

    unmount(0, m1);
    assertThat(getDrawableItemsSize()).isEqualTo(1);

    Drawable d3 = new ColorDrawable(BLACK);
    MountItem m3 = mount(1, d3);
    assertThat(getDrawableItemsSize()).isEqualTo(1);

    unmount(1, m3);
    assertThat(getDrawableItemsSize()).isEqualTo(0);
  }

  @Test
  public void testGetDrawableMountItem()
      throws Exception {
    Drawable d1 = new ColorDrawable(BLACK);
    MountItem mountItem1 = mount(0, d1);

    Drawable d2 = new ColorDrawable(BLACK);
    MountItem mountItem2 = mount(1, d2);

    Drawable d3 = new ColorDrawable(BLACK);
    MountItem mountItem3 = mount(5, d3);

    assertThat(getDrawableMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(getDrawableMountItemAt(1)).isEqualTo(mountItem2);
    assertThat(getDrawableMountItemAt(2)).isEqualTo(mountItem3);
  }

  private int getDrawableItemsSize()
      throws Exception {
    SparseArrayCompat drawableItems = Whitebox.getInternalState(mHost, "mDrawableMountItems");
    return Whitebox.invokeMethod(drawableItems, "size");
  }

  private MountItem getDrawableMountItemAt(int index)
      throws Exception {
    SparseArrayCompat drawableItems = Whitebox.getInternalState(mHost, "mDrawableMountItems");
    return Whitebox.invokeMethod(drawableItems, "valueAt", index);
  }

  private void testScrapHostsForComponent(
      Component<?> component,
      Class<? extends View> viewClass) {
    View view = mock(viewClass);
    MountItem mountItem = new MountItem();
    mountItem.init(
        component,
        null,
        view,
        null,
        null,
        null,
        0,
        IMPORTANT_FOR_ACCESSIBILITY_AUTO);

    mHost.mount(0, mountItem, new Rect());
    assertThat(mHost.recycleHost()).isNull();
    assertThat(mHost.getChildCount()).isEqualTo(1);

    mHost.unmount(0, mountItem);
    assertThat(mHost.recycleHost()).isNotNull();
    assertThat(mHost.getChildCount()).isEqualTo(1);

    assertThat(mHost.recycleHost()).isNull();

    when(view.getParent()).thenReturn(mHost);

    mHost.mount(0, mountItem, new Rect());
    assertThat(mHost.recycleHost()).isNull();
    assertThat(mHost.getChildCount()).isEqualTo(1);

    assertThat(mHost.recycleHost()).isNull();

    verify(view).setVisibility(GONE);
    verify(view).setVisibility(VISIBLE);
  }

  private MountItem mount(int index, Object content) {
    return mount(index, content, 0);
  }

  private MountItem mount(int index, Object content, int flags) {
    return mount(index, content, flags, null);
  }

  private MountItem mount(int index, Object content, int flags, CharSequence contentDescription) {
    MountItem mountItem = new MountItem();
    NodeInfo nodeInfo = NodeInfo.acquire();
    nodeInfo.setContentDescription(contentDescription);

    mountItem.init(
        content instanceof Drawable ? mDrawableComponent : mViewComponent,
        null,
        content,
        nodeInfo,
        null,
        null,
        flags,
        IMPORTANT_FOR_ACCESSIBILITY_AUTO);

    mHost.mount(
        index,
        mountItem,
        content instanceof Drawable ? ((Drawable) content).getBounds() : new Rect());
    return mountItem;
  }

  private MountItem mountTouchExpansionItem(int index, Object content) {
    final MountItem viewMountItem = new MountItem();
    final ViewNodeInfo viewNodeInfo = ViewNodeInfo.acquire();
    viewNodeInfo.setLayoutDirection(YogaDirection.LTR);

    viewMountItem.init(
        mViewComponent,
        null,
        content,
        null,
        viewNodeInfo,
        null,
        0,
        IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    mHost.mount(index, viewMountItem, new Rect());

    return viewMountItem;
  }

  private void unmount(int index, MountItem mountItem) {
    mHost.unmount(index, mountItem);
  }

  private static class TestableComponentHost extends ComponentHost {

    private int mInvalidationCount = 0;
    private Rect mInvalidationRect = null;

    public TestableComponentHost(ComponentContext context) {
      super(context);
    }

    public TestableComponentHost(Context context) {
      super(context);
    }

    @Override
    public void invalidate(Rect dirty) {
      super.invalidate(dirty);

      trackInvalidation(
          dirty.left,
          dirty.top,
          dirty.right,
          dirty.bottom);
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
      super.invalidate(l, t, r, b);

      trackInvalidation(l, t, r, b);
    }

    @Override
    public void invalidate() {
      super.invalidate();

      trackInvalidation(0, 0, getWidth(), getHeight());
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
      super.addView(child, index, params);

      trackInvalidation(
          child.getLeft(),
          child.getTop(),
          child.getRight(),
          child.getBottom());
    }

    @Override
    public void removeView(View child) {
      super.removeView(child);

      trackInvalidation(
          child.getLeft(),
          child.getTop(),
          child.getRight(),
          child.getBottom());
    }

    int getInvalidationCount() {
      return mInvalidationCount;
    }

    Rect getInvalidationRect() {
      return mInvalidationRect;
    }

    private void trackInvalidation(int l, int t, int r, int b) {
      mInvalidationCount++;

      mInvalidationRect = new Rect();
      mInvalidationRect.set(l, t, r, b);
    }
  }
}
