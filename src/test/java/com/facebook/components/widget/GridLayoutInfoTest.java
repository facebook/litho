/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v7.widget.GridLayoutManager;

import com.facebook.components.SizeSpec;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.support.v7.widget.OrientationHelper.HORIZONTAL;
import static android.support.v7.widget.OrientationHelper.VERTICAL;
import static com.facebook.components.SizeSpec.EXACTLY;
import static com.facebook.components.SizeSpec.UNSPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(ComponentsTestRunner.class)
