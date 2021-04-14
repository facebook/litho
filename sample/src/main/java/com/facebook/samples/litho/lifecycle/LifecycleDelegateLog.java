/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.litho.lifecycle;

import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_ATTACHED;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_BIND;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_BOUNDS_DEFINED;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_CREATE_INITIAL_STATE;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_CREATE_LAYOUT;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_CREATE_TRANSITION;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_CREATE_TREE_PROP;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_DETACHED;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_INVISIBLE;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_MEASURE;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_MOUNT;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_PREPARE;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_UNBIND;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_UNMOUNT;
import static com.facebook.samples.litho.lifecycle.DelegateListener.ON_VISIBLE;

import android.os.SystemClock;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;

public class LifecycleDelegateLog {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd hh:mm:ss.SSS");

  public static String log(int type) {
    String log;
    switch (type) {
      case ON_CREATE_INITIAL_STATE:
        log = "onCreateInitialState";
        break;
      case ON_CREATE_TREE_PROP:
        log = "onCreateTreeProp";
        break;
      case ON_CREATE_LAYOUT:
        log = "onCreateLayout";
        break;
      case ON_CREATE_TRANSITION:
        log = "onCreateTransition";
        break;
      case ON_ATTACHED:
        log = "onAttached";
        break;
      case ON_DETACHED:
        log = "onDetached";
        break;
      case ON_VISIBLE:
        log = "onVisible";
        break;
      case ON_INVISIBLE:
        log = "onInvisible";
        break;
      case ON_PREPARE:
        log = "onPrepare";
        break;
      case ON_MEASURE:
        log = "onMeasure";
        break;
      case ON_BOUNDS_DEFINED:
        log = "onBoundsDefined";
        break;
      case ON_MOUNT:
        log = "onMount";
        break;
      case ON_BIND:
        log = "onBind";
        break;
      case ON_UNBIND:
        log = "onUnbind";
        break;
      case ON_UNMOUNT:
        log = "onUnmount";
        break;
      default:
        log = "invalid type=" + type;
        break;
    }
    return log;
  }

  public static String prefix(Thread thread, long timestamp, String id) {
    final Date date = new Date(timestamp);
    final String dateFormatted = DATE_FORMAT.format(date);
    String prefix = dateFormatted + " [" + thread.getName() + "][id=" + id + "] ";

    return prefix;
  }

  public static void onDelegateMethodCalled(
      @Nullable DelegateListener delegateListener,
      @Nullable DelegateListener consoleDelegateListener,
      int type,
      String id) {
    if (delegateListener != null) {
      delegateListener.onDelegateMethodCalled(
          type, Thread.currentThread(), SystemClock.elapsedRealtime(), id);
    }
    if (consoleDelegateListener != null) {
      consoleDelegateListener.onDelegateMethodCalled(
          type, Thread.currentThread(), SystemClock.elapsedRealtime(), id);
    }
  }
}
