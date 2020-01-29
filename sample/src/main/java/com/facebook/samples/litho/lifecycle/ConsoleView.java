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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.facebook.samples.litho.lifecycle.LifecycleDelegateComponentSpec.DelegateListener.ON_ATTACHED;
import static com.facebook.samples.litho.lifecycle.LifecycleDelegateComponentSpec.DelegateListener.ON_CREATE_INITIAL_STATE;
import static com.facebook.samples.litho.lifecycle.LifecycleDelegateComponentSpec.DelegateListener.ON_CREATE_LAYOUT;
import static com.facebook.samples.litho.lifecycle.LifecycleDelegateComponentSpec.DelegateListener.ON_CREATE_TRANSITION;
import static com.facebook.samples.litho.lifecycle.LifecycleDelegateComponentSpec.DelegateListener.ON_CREATE_TREE_PROP;
import static com.facebook.samples.litho.lifecycle.LifecycleDelegateComponentSpec.DelegateListener.ON_DETACHED;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConsoleView extends ScrollView {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd hh:mm:ss.SSS");

  public ConsoleView(Context context) {
    super(context);

    final LinearLayout parent = new LinearLayout(context);
    parent.setOrientation(LinearLayout.VERTICAL);
    final LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
    params.setMargins(0, 2, 0, 2);
    parent.setLayoutParams(params);
    parent.setBackgroundColor(Color.DKGRAY);
    addView(parent);
  }

  public void log(int type, Thread thread, long timestamp, int id) {
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
      default:
        log = "invalid type=" + type;
        break;
    }
    final Date date = new Date(timestamp);
    final String dateFormatted = DATE_FORMAT.format(date);
    post(new LogRunnable(this, dateFormatted + " [" + thread.getName() + "][id=" + id + "] ", log));
  }

  private static class LogRunnable implements Runnable {
    private final WeakReference<ConsoleView> mRootRef;
    private final String mPrefix;
    private final String mLog;

    LogRunnable(ConsoleView root, String prefix, String log) {
      mRootRef = new WeakReference<>(root);
      mPrefix = prefix;
      mLog = log;
    }

    @Override
    public void run() {
      final ConsoleView root = mRootRef.get();
      if (root == null) {
        return;
      }

      android.util.Log.e("ConsoleView", "root=" + root + ", runnable=" + this);

      final Context context = root.getContext();
      final LinearLayout row = new LinearLayout(context);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

      final TextView timestampView = new TextView(context);
      timestampView.setText(mPrefix);
      timestampView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      timestampView.setTextColor(Color.LTGRAY);
      final LinearLayout.LayoutParams params1 =
          new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
      params1.weight = 0;
      timestampView.setLayoutParams(params1);
      row.addView(timestampView);

      final TextView logView = new TextView(context);
      logView.setText(mLog);
      logView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      logView.setTextColor(Color.LTGRAY);
      final LinearLayout.LayoutParams params2 =
          new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
      params2.weight = 1;
      logView.setLayoutParams(params2);
      row.addView(logView);
      ((LinearLayout) root.getChildAt(0)).addView(row);
    }
  }
}
