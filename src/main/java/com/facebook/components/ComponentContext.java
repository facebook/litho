/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;

import com.facebook.R;
import com.facebook.infer.annotation.ThreadConfined;

/**
 * A Context subclass for use within the Components framework. Contains extra bookkeeping
 * information used internally in the library.
 */
public class ComponentContext extends ContextWrapper {

  static final InternalNode NULL_LAYOUT = new NoOpInternalNode();

  private final String mLogTag;
  private final ComponentsLogger mLogger;
  private final StateHandler mStateHandler;

  // Hold a reference to the component which scope we are currently within.
  private @ThreadConfined(ThreadConfined.ANY) Component<?> mComponentScope;
  private @ThreadConfined(ThreadConfined.ANY) ResourceCache mResourceCache;
  private @ThreadConfined(ThreadConfined.ANY) int mWidthSpec;
  private @ThreadConfined(ThreadConfined.ANY) int mHeightSpec;
  private @ThreadConfined(ThreadConfined.ANY) TreeProps mTreeProps;

  private @ThreadConfined(ThreadConfined.ANY) ComponentTree mComponentTree;

  // Used to hold styling information applied to components
  private @ThreadConfined(ThreadConfined.ANY) @StyleRes int mDefStyleRes = 0;
  private @ThreadConfined(ThreadConfined.ANY) @AttrRes int mDefStyleAttr = 0;

  public ComponentContext(Context context) {
    this(context, null, null, null);
  }

  public ComponentContext(Context context, StateHandler stateHandler) {
    this(context, null, null, stateHandler);
  }

  /**
   *  Constructor that can be used to receive log data from components.
   *  Check {@link ComponentsLogger} for the type of events you can listen for.
   *
   * @param context Android context.
   * @param logTag Specify a log tag, to be used with the logger.
   * @param logger Specify the lifecycle logger to be used.
   */
  public ComponentContext(Context context, String logTag, ComponentsLogger logger) {
