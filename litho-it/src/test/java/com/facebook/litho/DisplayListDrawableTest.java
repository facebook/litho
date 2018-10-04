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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.robolectric.RuntimeEnvironment;

/**
 * Test for {@link DisplayListDrawable}
 */
@RunWith(ComponentsTestRunner.class)
public class DisplayListDrawableTest  {

  private DisplayList mDisplayList;
  private DisplayListContainer mDisplayListContainer;
  private Drawable mDrawable;
  private Canvas mCanvas;
  private Canvas mDlCanvas;

  @Before
  public void setup() throws DisplayListException {
    mDisplayList = Mockito.mock(DisplayList.class);
    mDisplayListContainer = new DisplayListContainer();
    mDisplayListContainer.setDisplayList(mDisplayList);
    mDrawable = Mockito.mock(Drawable.class);
    mCanvas = Mockito.mock(Canvas.class);
    mDlCanvas = Mockito.mock(Canvas.class);
    when(mDisplayList.isValid()).thenReturn(true);
    when(mDrawable.getBounds()).thenReturn(new Rect());
    when(mDisplayList.start(anyInt(), anyInt())).thenReturn(mDlCanvas);
  }

  @Test
  public void testInvalidationSuppression() {
    DisplayListDrawable displayListDrawable =
        new DisplayListDrawable(mDrawable, mDisplayListContainer);
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
    DisplayListDrawable displayListDrawable =
        new DisplayListDrawable(mDrawable, mDisplayListContainer);
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
    layoutOutput.initDisplayListContainer("Test", false);
    layoutOutput.setDisplayList(mDisplayList);
    Component emptyTestComponent =
        Row.create(new ComponentContext(RuntimeEnvironment.application)).build();
    MountItem mountItem =
        ComponentsPools.acquireMountItem(emptyTestComponent, null, mDrawable, layoutOutput);
    DisplayListDrawable displayListDrawable = mountItem.getDisplayListDrawable();

    layoutOutput.setDisplayList(null);
    mountItem.init(emptyTestComponent, null, mDrawable, layoutOutput, displayListDrawable);

    displayListDrawable.draw(mCanvas);
    verify(mDrawable).draw(mCanvas);
  }
}
