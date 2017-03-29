/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.view.ContextThemeWrapper;

import com.facebook.litho.R;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.components.testing.ComponentsRule;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
@org.junit.Ignore("t16280359")
public class ComponentStyleTest {
  private int mDimen;
  private int mLargeDimen;
  private ComponentContext mContext;

