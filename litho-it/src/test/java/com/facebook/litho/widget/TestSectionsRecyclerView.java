package com.facebook.litho.widget;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** A test {@link SectionsRecyclerView} class used for unit testing. */
public class TestSectionsRecyclerView extends SectionsRecyclerView {

  private int removeCallbackCount;
  private int postCount;
  private OnRefreshListener listener;
  private boolean lastRefreshingValue;
  private int setRefreshingValuesCount;

  public TestSectionsRecyclerView(Context context, RecyclerView view) {
    super(context, view);
  }

  public int getSetRefreshingValuesCount() {
    return setRefreshingValuesCount;
  }

  public int getRemoveCallbackCount() {
    return removeCallbackCount;
  }

  public int getPostCount() {
    return postCount;
  }
  @Override
  public void setRefreshing(boolean refreshing) {
    super.setRefreshing(refreshing);
    setRefreshingValuesCount++;
    lastRefreshingValue = refreshing;
  }

  public boolean getLastRefreshingValue() {
    return lastRefreshingValue;
  }

  @Override
  public boolean removeCallbacks(Runnable action) {
    removeCallbackCount++;
    return true;
  }

  @Override
  public boolean post(Runnable action) {
    postCount++;
    return true;
  }

  @Override
  public void setOnRefreshListener(@Nullable OnRefreshListener listener) {
    this.listener = listener;
    super.setOnRefreshListener(listener);
  }

  public OnRefreshListener getOnRefreshListener() {
    return listener;
  }

  /** Used for resetting the fields of {@link TestSectionsRecyclerView} */
  public void reset() {
    lastRefreshingValue = false;
    setRefreshingValuesCount = 0;
    removeCallbackCount=0;
    postCount=0;
  }
}
