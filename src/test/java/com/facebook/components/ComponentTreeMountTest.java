/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import static org.junit.Assert.assertEquals;

@PrepareForTest(ThreadUtils.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
