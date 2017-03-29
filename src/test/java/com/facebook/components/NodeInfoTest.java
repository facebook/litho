/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.support.v4.util.Pools;
import android.util.SparseArray;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import static com.facebook.components.NodeInfo.FOCUS_SET_FALSE;
import static com.facebook.components.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.components.NodeInfo.FOCUS_UNSET;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;

