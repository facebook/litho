package com.facebook.litho.widget;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** A test {@link SectionsRecyclerView} class used for unit testing. */
public class TestSectionsRecyclerView extends SectionsRecyclerView {

  private final List<Runnable> removeCallbackRunnableList = new ArrayList<>();
  private final List<Runnable> postRunnableList = new ArrayList<>();
  private OnRefreshListener listener;
  private boolean lastRefreshValue;
  private int setRefreshingValuesCount;

  public TestSectionsRecyclerView(@NonNull Context context, RecyclerView view) {
    super(context, view);
  }

  public int getSetRefreshingValuesCount() {
    return setRefreshingValuesCount;
  }

  public List<Runnable> getRemoveCallbackRunnableList() {
    return removeCallbackRunnableList;
  }

  public List<Runnable> getPostRunnableList() {
    return postRunnableList;
  }

  @Override
  public void setRefreshing(boolean refreshing) {
    super.setRefreshing(refreshing);
    setRefreshingValuesCount++;
    lastRefreshValue = refreshing;
  }

  public boolean getLastRefreshValue() {
    return lastRefreshValue;
  }

  @Override
  public boolean removeCallbacks(Runnable action) {
    removeCallbackRunnableList.add(action);
    return true;
  }

  @Override
  public boolean post(Runnable action) {
    postRunnableList.add(action);
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
    lastRefreshValue = false;
    setRefreshingValuesCount = 0;
    removeCallbackRunnableList.clear();
    postRunnableList.clear();
  }
}
