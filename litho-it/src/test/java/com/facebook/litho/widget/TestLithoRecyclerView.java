package com.facebook.litho.widget;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** A test {@link LithoRecylerView} class used for unit testing. */
public class TestLithoRecyclerView extends LithoRecylerView {

  private RecyclerView.ItemAnimator itemAnimator;
  private final List<OnScrollListener> removeOnScrollListeners = new ArrayList<>();
  private final List<OnScrollListener> addOnScrollListeners = new ArrayList<>();
  private TouchInterceptor touchInterceptor;
  private boolean layoutRequested;

  public TestLithoRecyclerView(Context context) {
    super(context);
  }

  @Override
  @Nullable
  public RecyclerView.ItemAnimator getItemAnimator() {
    return itemAnimator;
  }

  @Override
  public void setItemAnimator(@Nullable RecyclerView.ItemAnimator animator) {
    this.itemAnimator = animator;
  }

  @Override
  public void removeOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
    removeOnScrollListeners.add(onScrollListener);
    super.removeOnScrollListener(onScrollListener);
  }

  public int getRemoveOnScrollListenersCount() {
    return removeOnScrollListeners.size();
  }

  @Override
  public void addOnScrollListener(RecyclerView.OnScrollListener onScrollListener) {
    addOnScrollListeners.add(onScrollListener);
    super.addOnScrollListener(onScrollListener);
  }

  public int getAddOnScrollListenersCount() {
    return addOnScrollListeners.size();
  }

  @Override
  public void setTouchInterceptor(@Nullable TouchInterceptor touchInterceptor) {
    super.setTouchInterceptor(touchInterceptor);
    this.touchInterceptor = touchInterceptor;
  }

  public TouchInterceptor getTouchInterceptor() {
    return touchInterceptor;
  }

  @Override
  public void requestLayout() {
    super.requestLayout();
    layoutRequested = true;
  }

  @Override
  public boolean isLayoutRequested() {
    return layoutRequested;
  }
}
