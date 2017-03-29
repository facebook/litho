/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.LinearLayoutManager;

import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@link LinearLayoutInfo}
 */
