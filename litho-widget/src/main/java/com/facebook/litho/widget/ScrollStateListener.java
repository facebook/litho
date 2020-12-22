package com.facebook.litho.widget;

import android.view.View;

/** Scroll change listener invoked when the scroll position changes. */
public interface ScrollStateListener {

  int SCROLL_STARTED = 0;
  int SCROLL_STOPPED = 1;

  void onScrollStateChanged(View v, int scrollStatus);
}
