/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.facebook.litho.displaylist.DisplayList;
import com.facebook.litho.displaylist.DisplayListException;
import com.facebook.litho.testing.TestDrawable;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

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
  public void testDrawing() throws DisplayListException {
    DisplayListDrawable displayListDrawable = new DisplayListDrawable(mDrawable);
    displayListDrawable.setDisplayList(mDisplayList);
    displayListDrawable.draw(mCanvas);
    verify(mDisplayList).start(anyInt(), anyInt());
    verify(mDrawable).draw(mDlCanvas);
    verify(mDisplayList).end(mDlCanvas);
    verify(mDisplayList).setBounds(anyInt(), anyInt(), anyInt(), anyInt());
  }

  @Test
  public void testInvalidationSuppression() {
    DisplayListDrawable displayListDrawable = new DisplayListDrawable(mDrawable);
    displayListDrawable.setDisplayList(mDisplayList);
    displayListDrawable.draw(mCanvas);
    verify(mDrawable, times(1)).draw((Canvas) anyObject());

    displayListDrawable.suppressInvalidations(true);
    displayListDrawable.invalidateDrawable(mDrawable);
    displayListDrawable.suppressInvalidations(false);
    displayListDrawable.draw(mCanvas);
    verify(mDrawable, times(1)).draw((Canvas) anyObject());
  }

  @Test
  public void testInvalidation() {
    DisplayListDrawable displayListDrawable = new DisplayListDrawable(mDrawable);
    displayListDrawable.setDisplayList(mDisplayList);
    displayListDrawable.draw(mCanvas);
    verify(mDrawable, times(1)).draw((Canvas) anyObject());

    displayListDrawable.invalidateDrawable(mDrawable);
    displayListDrawable.draw(mCanvas);
    verify(mDrawable, times(2)).draw((Canvas) anyObject());
  }

  @Test
  public void testNotAttemptingToUseDisplayListAfterFailure() throws DisplayListException {
    doThrow(new DisplayListException(null)).when(mDisplayList).draw(anyObject());

    DisplayListDrawable displayListDrawable = new DisplayListDrawable(mDrawable);
    displayListDrawable.setDisplayList(mDisplayList);
    displayListDrawable.draw(mCanvas);
    // Should draw mDrawable into mDisplayList, attempt to draw mDisplayList onto a canvas, fail,
    // and then draw mDrawable onto the canvas
    verify(mDisplayList, times(1)).draw(anyObject());
    verify(mDrawable, times(2)).draw(anyObject());

    displayListDrawable.draw(mCanvas);
    // Should not attempt DL re-creation/drawing, and draw mDrawable onto a canvas right away
    verify(mDisplayList, times(1)).draw(anyObject());
    verify(mDrawable, times(3)).draw(anyObject());
  }

  @Test
  public void testTransferringBounds() {
    TestDrawable drawable = new TestDrawable();
    DisplayListDrawable displayListDrawable = new DisplayListDrawable(drawable);

    Rect bounds = new Rect(100, 100, 200, 200);
    displayListDrawable.setBounds(bounds);
    assertThat(displayListDrawable.getBounds()).isEqualTo(bounds);
    assertThat(drawable.getBounds()).isEqualTo(bounds);

    bounds = new Rect(10, 20, 30, 40);
    displayListDrawable.setBounds(bounds);
    assertThat(displayListDrawable.getBounds()).isEqualTo(bounds);
    assertThat(drawable.getBounds()).isEqualTo(bounds);
  }

  @Test
  public void testInvalidatingOnBoundsChange() throws DisplayListException {
    DisplayListDrawable displayListDrawable = new DisplayListDrawable(new TestDrawable());
    Rect bounds = new Rect(100, 100, 200, 200);
    displayListDrawable.setBounds(bounds);
    displayListDrawable.setDisplayList(mDisplayList);
    displayListDrawable.draw(mCanvas);

    // Checking that DLDrawable drew into the DL
    verify(mDisplayList, once()).start(anyInt(), anyInt());
    verify(mDisplayList, once()).end(mDlCanvas);
    verify(mDisplayList, once()).setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);

    // Checking that the DL was drawn
    verify(mDisplayList).draw(anyObject());

    when(mDisplayList.getBounds()).thenReturn(new Rect(bounds));

    // Offsetting the drawable
    final int offsetX = 10;
    final int offsetY = 20;
    bounds.offset(offsetX, offsetY);
    displayListDrawable.setBounds(bounds);

    displayListDrawable.draw(mCanvas);

    // Checking that DLDrawable haven't drawn into DL again, but changed its bounds
    verify(mDisplayList, once()).start(anyInt(), anyInt());
    verify(mDisplayList, once()).end(mDlCanvas);
    verify(mDisplayList, once()).setTranslationX(offsetX);
    verify(mDisplayList, once()).setTranslationY(offsetY);

    // Checking that the DL was drawn
    verify(mDisplayList, twice()).draw(anyObject());

    when(mDisplayList.getBounds()).thenReturn(new Rect(bounds));

    // Changing the size
    bounds.right += 10;
    displayListDrawable.setBounds(bounds);

    displayListDrawable.draw(mCanvas);

    // Checking that DLDrawable did draw into DL again, since the sizes have changed
    verify(mDisplayList, twice()).start(anyInt(), anyInt());
    verify(mDisplayList, twice()).end(mDlCanvas);
    verify(mDisplayList, once()).setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);

    // Checking that the DL was drawn
    verify(mDisplayList, times(3)).draw(anyObject());
  }

  private static VerificationMode once() {
    return times(1);
  }

  private static VerificationMode twice() {
    return times(2);
  }
}
