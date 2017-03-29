/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.util.SparseArray;
import android.view.View;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link MountItem}
 */

@RunWith(ComponentsTestRunner.class)
public class MountItemTest {
  private MountItem mMountItem;
  private Component<?> mComponent;
  private ComponentHost mComponentHost;
  private Object mContent;
  private CharSequence mContentDescription;
  private Object mViewTag;
  private SparseArray<Object> mViewTags;
  private EventHandler mClickHandler;
  private EventHandler mLongClickHandler;
  private EventHandler mTouchHandler;
  private EventHandler mDispatchPopulateAccessibilityEventHandler;
  private int mFlags;
