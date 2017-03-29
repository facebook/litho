/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.yoga.YogaDirection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static android.view.View.GONE;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.INVISIBLE;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.View.VISIBLE;
import static com.facebook.litho.MountItem.FLAG_DUPLICATE_PARENT_STATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    assertEquals(0, mHost.getParentHostMarker());

    mHost.setParentHostMarker(1);
    assertEquals(1, mHost.getParentHostMarker());
  }

  @Test
  public void testInvalidations() {
    assertEquals(0, mHost.getInvalidationCount());
    assertNull(mHost.getInvalidationRect());

    Drawable d1 = new ColorDrawable();
    d1.setBounds(0, 0, 1, 1);

    MountItem mountItem1 = mount(0, d1);
    assertEquals(1, mHost.getInvalidationCount());
    assertEquals(d1.getBounds(), mHost.getInvalidationRect());

    Drawable d2 = new ColorDrawable();
    d2.setBounds(0, 0, 2, 2);

    MountItem mountItem2 = mount(1, d2);
    assertEquals(2, mHost.getInvalidationCount());
    assertEquals(d2.getBounds(), mHost.getInvalidationRect());

    View v1 = new View(mContext);
    Rect v1Bounds = new Rect(0, 0, 10, 10);
    v1.measure(
        makeMeasureSpec(v1Bounds.width(), EXACTLY),
        makeMeasureSpec(v1Bounds.height(), EXACTLY));
    v1.layout(v1Bounds.left, v1Bounds.top, v1Bounds.right, v1Bounds.bottom);

    MountItem mountItem3 = mount(2, v1);
    assertEquals(3, mHost.getInvalidationCount());
    assertEquals(v1Bounds, mHost.getInvalidationRect());

    unmount(0, mountItem1);
    assertEquals(4, mHost.getInvalidationCount());
    assertEquals(d1.getBounds(), mHost.getInvalidationRect());

    unmount(1, mountItem2);
    assertEquals(5, mHost.getInvalidationCount());
    assertEquals(d2.getBounds(), mHost.getInvalidationRect());

    unmount(2, mountItem3);
    assertEquals(6, mHost.getInvalidationCount());
    assertEquals(v1Bounds, mHost.getInvalidationRect());
  }

  @Test
  public void testCallbacks() {
    Drawable d = new ColorDrawable();
    assertNull(d.getCallback());

    MountItem mountItem = mount(0, d);
    assertEquals(mHost, d.getCallback());

    unmount(0, mountItem);
    assertNull(d.getCallback());
  }

  @Test
  public void testGetMountItemCount() {
    assertEquals(0, mHost.getMountItemCount());

    MountItem mountItem1 = mount(0, new ColorDrawable());
    assertEquals(1, mHost.getMountItemCount());

    mount(1, new ColorDrawable());
    assertEquals(2, mHost.getMountItemCount());

    MountItem mountItem3 = mount(2, new View(mContext));
    assertEquals(3, mHost.getMountItemCount());

    unmount(0, mountItem1);
    assertEquals(2, mHost.getMountItemCount());

    MountItem mountItem4 = mount(1, new ColorDrawable());
    assertEquals(2, mHost.getMountItemCount());

    unmount(2, mountItem3);
    assertEquals(1, mHost.getMountItemCount());

    unmount(1, mountItem4);
    assertEquals(0, mHost.getMountItemCount());
  }

  @Test
  public void testGetMountItemAt() {
    assertNull(mHost.getMountItemAt(0));
    assertNull(mHost.getMountItemAt(1));
    assertNull(mHost.getMountItemAt(2));

    MountItem mountItem1 = mount(0, new ColorDrawable());
    MountItem mountItem2 = mount(1, new View(mContext));
    MountItem mountItem3 = mount(5, new ColorDrawable());

    assertEquals(mountItem1, mHost.getMountItemAt(0));
    assertEquals(mountItem2, mHost.getMountItemAt(1));
    assertEquals(mountItem3, mHost.getMountItemAt(2));

    unmount(1, mountItem2);

    assertEquals(mountItem1, mHost.getMountItemAt(0));
    assertEquals(mountItem3, mHost.getMountItemAt(1));

    unmount(0, mountItem1);

    assertEquals(mountItem3, mHost.getMountItemAt(0));
  }

  @Test
  public void testMoveItem() {
    MountItem mountItem1 = mount(1, new ColorDrawable());
    MountItem mountItem2 = mount(2, new View(mContext));

    assertEquals(2, mHost.getMountItemCount());

    assertEquals(mountItem1, mHost.getMountItemAt(0));
    assertEquals(mountItem2, mHost.getMountItemAt(1));

    mHost.moveItem(mountItem2, 2, 0);

    assertEquals(2, mHost.getMountItemCount());
    assertEquals(mountItem2, mHost.getMountItemAt(0));
    assertEquals(mountItem1, mHost.getMountItemAt(1));

    mHost.moveItem(mountItem2, 0, 1);

    assertEquals(1, mHost.getMountItemCount());
    assertEquals(mountItem2, mHost.getMountItemAt(0));

    mHost.moveItem(mountItem2, 1, 0);

    assertEquals(2, mHost.getMountItemCount());
    assertEquals(mountItem1, mHost.getMountItemAt(0));
    assertEquals(mountItem2, mHost.getMountItemAt(1));
  }

  @Test
  public void testMoveItemWithoutTouchables()
      throws Exception {
    Drawable d1 = new ColorDrawable(Color.BLACK);
    MountItem mountItem1 = mount(1, d1);

    Drawable d2 = new ColorDrawable(Color.BLACK);
    MountItem mountItem2 = mount(2, d2);

    assertEquals(2, getDrawableItemsSize());
    assertEquals(mountItem1, getDrawableMountItemAt(0));
    assertEquals(mountItem2, getDrawableMountItemAt(1));

    mHost.moveItem(mountItem2, 2, 0);

    // There are no Touchable Drawables so this call should return false and not crash.
    assertFalse(mHost.onTouchEvent(MotionEvent.obtain(0, 0, 0, 0, 0, 0)));
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
    assertEquals(2, drawables.size());
    assertEquals(d1, drawables.get(0));
    assertEquals(d2, drawables.get(1));

    unmount(0, mountItem1);

    drawables = mHost.getDrawables();
    assertEquals(1, drawables.size());
    assertEquals(d2, drawables.get(0));

    unmount(2, mountItem3);

    drawables = mHost.getDrawables();
    assertEquals(1, drawables.size());
    assertEquals(d2, drawables.get(0));
  }

  @Test
  public void testViewTag() {
    assertNull(mHost.getTag());

    Object tag = new Object();
    mHost.setViewTag(tag);
    assertEquals(tag, mHost.getTag());

    mHost.setViewTag(null);
    assertNull(mHost.getTag());
  }

  @Test
  public void testViewTags() {
    assertNull(mHost.getTag(1));
    assertNull(mHost.getTag(2));

    Object value1 = new Object();
    Object value2 = new Object();

    SparseArray<Object> viewTags = new SparseArray<>();
    viewTags.put(1, value1);
    viewTags.put(2, value2);

    mHost.setViewTags(viewTags);

    assertEquals(value1, mHost.getTag(1));
    assertEquals(value2, mHost.getTag(2));

    mHost.setViewTags(null);

    assertNull(mHost.getTag(1));
    assertNull(mHost.getTag(2));
  }

  @Test
  public void testComponentClickListener() {
    assertNull(mHost.getComponentClickListener());

    ComponentClickListener listener = new ComponentClickListener();
    mHost.setComponentClickListener(listener);

    assertEquals(listener, mHost.getComponentClickListener());

    mHost.setComponentClickListener(null);
    assertNull(mHost.getComponentClickListener());
  }

  @Test
  public void testComponentLongClickListener() {
    assertNull(mHost.getComponentLongClickListener());

    ComponentLongClickListener listener = new ComponentLongClickListener();
    mHost.setComponentLongClickListener(listener);

    assertEquals(listener, mHost.getComponentLongClickListener());

    mHost.setComponentLongClickListener(null);
    assertNull(mHost.getComponentLongClickListener());
  }

  @Test
  public void testComponentTouchListener() {
    assertNull(mHost.getComponentTouchListener());

    ComponentTouchListener listener = new ComponentTouchListener();
    mHost.setComponentTouchListener(listener);

    assertEquals(listener, mHost.getComponentTouchListener());

    mHost.setComponentTouchListener(null);
    assertNull(mHost.getComponentTouchListener());
  }

  @Test
