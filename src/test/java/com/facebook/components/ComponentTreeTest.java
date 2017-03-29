/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.os.Looper;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestLayoutComponent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import static com.facebook.components.SizeSpec.AT_MOST;
import static com.facebook.components.SizeSpec.EXACTLY;
import static com.facebook.components.SizeSpec.makeSizeSpec;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
