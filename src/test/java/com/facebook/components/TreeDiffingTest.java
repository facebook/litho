/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.util.SparseArrayCompat;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestSizeDependentComponent;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
