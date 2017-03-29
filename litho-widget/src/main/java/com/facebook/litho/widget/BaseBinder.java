/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Looper;
import android.support.v4.util.Pools;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;
import com.facebook.components.ComponentView;
import com.facebook.components.config.ComponentsConfiguration;
import com.facebook.components.Size;
import com.facebook.components.SizeSpec;
import com.facebook.infer.annotation.ThreadSafe;

import static com.facebook.components.SizeSpec.EXACTLY;
import static com.facebook.components.SizeSpec.UNSPECIFIED;
import static com.facebook.components.ThreadUtils.assertDoesntHoldLock;
import static com.facebook.components.ThreadUtils.assertMainThread;

