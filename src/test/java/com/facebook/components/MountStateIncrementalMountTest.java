/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.content.Context;
import android.graphics.Rect;
import android.view.ViewGroup;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestComponentContextWithView;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestViewComponent;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.components.ComponentsLogger.EVENT_MOUNT;
import static com.facebook.components.ComponentsLogger.PARAM_MOUNTED_COUNT;
import static com.facebook.components.ComponentsLogger.PARAM_UNMOUNTED_COUNT;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaFlexDirection.COLUMN;
import static com.facebook.yoga.YogaFlexDirection.ROW;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(ComponentsTestRunner.class)
