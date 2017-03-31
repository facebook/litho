/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.facebook.litho.displaylist.DisplayList;
import com.facebook.litho.displaylist.DisplayListException;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DisplayListDrawable}
 */
@RunWith(ComponentsTestRunner.class)
public class DisplayListDrawableTest  {

  private DisplayList mDisplayList;
  private Drawable mDrawable;
  private Canvas mCanvas;
  private Canvas mDlCanvas;

  @Before
  public void setup() throws DisplayListException {
    mDisplayList = Mockito.mock(DisplayList.class);
    mDrawable = Mockito.mock(Drawable.class);
    mCanvas = Mockito.mock(Canvas.class);
    mDlCanvas = Mockito.mock(Canvas.class);
    when(mDisplayList.isValid()).thenReturn(true);
    when(mDrawable.getBounds()).thenReturn(new Rect());
    when(mDisplayList.start(anyInt(), anyInt())).thenReturn(mDlCanvas);
  }

  @Test
  public void testInvalidationSuppression() {
    DisplayListDrawable displayListDrawable = new DisplayListDrawable(mDrawable, mDisplayList);
    displayListDrawable.draw(mCanvas);
    verify(mDrawable, never()).draw((Canvas) anyObject());

    displayListDrawable.suppressInvalidations(true);
    displayListDrawable.invalidateDrawable(mDrawable);
    displayListDrawable.suppressInvalidations(false);
    displayListDrawable.draw(mCanvas);
    verify(mDrawable, never()).draw((Canvas) anyObject());
  }

  @Test
  public void testInvalidation() throws DisplayListException {
    DisplayListDrawable displayListDrawable = new DisplayListDrawable(mDrawable, mDisplayList);
    displayListDrawable.draw(mCanvas);
    verify(mDrawable, never()).draw((Canvas) anyObject());

    displayListDrawable.invalidateDrawable(mDrawable);
    displayListDrawable.draw(mCanvas);
    verify(mDisplayList).start(anyInt(), anyInt());
    verify(mDrawable).draw(mDlCanvas);
    verify(mDisplayList).end(mDlCanvas);
    verify(mDisplayList).setBounds(anyInt(), anyInt(), anyInt(), anyInt());
  }

  @Test
  public void testMountItemUpdate() {
    LayoutOutput layoutOutput = ComponentsPools.acquireLayoutOutput();
    layoutOutput.setDisplayList(mDisplayList);
    MountItem mountItem = ComponentsPools.acquireMountItem(null, null, mDrawable, layoutOutput);
    DisplayListDrawable displayListDrawable = mountItem.getDisplayListDrawable();

    layoutOutput.setDisplayList(null);
    mountItem.init(null, null, mDrawable, layoutOutput, displayListDrawable);

    displayListDrawable.draw(mCanvas);
    verify(mDrawable).draw(mCanvas);
  }
}
