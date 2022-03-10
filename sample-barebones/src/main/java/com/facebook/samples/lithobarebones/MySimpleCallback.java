package com.facebook.samples.lithobarebones;

import android.util.Log;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public final class MySimpleCallback extends ItemTouchHelper.SimpleCallback {

  /**
   * Listener interface to call from ListSectionSpec.
   */
  public interface MyListener {

    void onItemMoved(int indexFrom, int indexTo);
  }

  private MyListener listener;

  MySimpleCallback() {
    // Only drag vertically. No swipe.
    super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
  }

  public void setListener(MyListener listener) {
    this.listener = listener;
  }

  @Override
  public boolean onMove(
      RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
    int fromPosition = viewHolder.getAbsoluteAdapterPosition();
    int toPosition = target.getAbsoluteAdapterPosition();

    // Call listener.
    if (listener != null) {
      listener.onItemMoved(fromPosition, toPosition);
    }

    // Note: This is called way too many times when dragging. Dragging feels weird.
    Log.d("MY_TEST", "Move from " + fromPosition + " to " + toPosition);

    return true; // true if moved, false otherwise
  }

  @Override
  public void onSwiped(ViewHolder viewHolder, int direction) {
    // TODO: Implement Swipe.
  }
}
