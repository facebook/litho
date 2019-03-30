package com.facebook.litho.widget;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** A test {@link RecyclerView} class used for unit testing. */
public class TestRecyclerView extends RecyclerView {

  private final List<Runnable> postAnimationRunnableList = new ArrayList<>();

  public TestRecyclerView(@NonNull Context context) {
    super(context);
  }

  @Override
  public void postOnAnimation(Runnable r) {
    postAnimationRunnableList.add(r);
  }

  public List<Runnable> getPostAnimationRunnableList() {
    return postAnimationRunnableList;
  }
}
