package com.facebook.litho;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.ComponentTree.LayoutStateFuture;

/**
 * Wraps objects which should only be available for the duration of a LayoutState, to access them in
 * other classes such as ComponentContext during layout state calculation. When the layout
 * calculation finishes, all references are nullified. Using a wrapper instead of passing the
 * instances directly helps with clearing out the reference from all objects that hold on to it,
 * without having to keep track of all these objects to clear out the references.
 */
final class LayoutStateContext {
  private @Nullable LayoutState mLayoutStateRef;
  private @Nullable LayoutStateFuture mLayoutStateFuture;

  private static @Nullable LayoutState sTestLayoutState;

  public static LayoutStateContext getTestInstance(ComponentContext c) {
    if (sTestLayoutState == null) {
      sTestLayoutState = new LayoutState(c);
    }

    return new LayoutStateContext(sTestLayoutState, null);
  }

  @VisibleForTesting
  LayoutStateContext(LayoutState layoutState) {
    this(layoutState, null);
  }

  @VisibleForTesting
  LayoutStateContext(LayoutState layoutState, @Nullable LayoutStateFuture layoutStateFuture) {
    mLayoutStateRef = layoutState;
    mLayoutStateFuture = layoutStateFuture;
  }

  void releaseReference() {
    mLayoutStateRef = null;
    mLayoutStateFuture = null;
  }

  /** Returns the LayoutState instance or null if the layout state has been released. */
  @Nullable
  LayoutState getLayoutState() {
    return mLayoutStateRef;
  }

  public @Nullable LayoutStateFuture getLayoutStateFuture() {
    return mLayoutStateFuture;
  }

  boolean isLayoutInterrupted() {
    boolean isInterruptRequested =
        mLayoutStateFuture == null ? false : mLayoutStateFuture.isInterruptRequested();
    boolean isInterruptible = mLayoutStateRef == null ? false : mLayoutStateRef.isInterruptible();

    return isInterruptible && isInterruptRequested;
  }

  boolean isLayoutReleased() {
    return mLayoutStateFuture == null ? false : mLayoutStateFuture.isReleased();
  }

  public void markLayoutUninterruptible() {
    if (mLayoutStateRef != null) {
      mLayoutStateRef.setInterruptible(false);
    }
  }
}
