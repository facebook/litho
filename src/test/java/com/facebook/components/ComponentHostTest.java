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
