/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.GuardedBy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.IntDef;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.ViewParent;

import com.facebook.infer.annotation.ReturnsOwnership;

import static com.facebook.litho.ComponentLifecycle.StateUpdate;
import static com.facebook.litho.ComponentsLogger.ACTION_SUCCESS;
import static com.facebook.litho.ComponentsLogger.EVENT_LAYOUT_CALCULATE;
import static com.facebook.litho.ComponentsLogger.EVENT_PRE_ALLOCATE_MOUNT_CONTENT;
import static com.facebook.litho.ComponentsLogger.PARAM_IS_BACKGROUND_LAYOUT;
import static com.facebook.litho.ComponentsLogger.PARAM_LOG_TAG;
import static com.facebook.litho.ComponentsLogger.PARAM_TREE_DIFF_ENABLED;
import static com.facebook.litho.ThreadUtils.assertHoldsLock;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;

/**
 * Represents a tree of components and controls their life cycle. ComponentTree takes in a single
 * root component and recursively invokes its OnCreateLayout to create a tree of components.
 * ComponentTree is responsible for refreshing the mounted state of a component with new props.
 *
 * The usual use case for {@link ComponentTree} is:
 * <code>
 * ComponentTree component = ComponentTree.create(context, MyComponent.create());
 * myHostView.setRoot(component);
 * <code/>
 */
public class ComponentTree {

  private static final String TAG = ComponentTree.class.getSimpleName();
  private static final int SIZE_UNINITIALIZED = -1;
  // MainThread Looper messages:
  private static final int MESSAGE_WHAT_BACKGROUND_LAYOUT_STATE_UPDATED = 1;
  private static final String DEFAULT_LAYOUT_THREAD_NAME = "ComponentLayoutThread";
  private static final int DEFAULT_LAYOUT_THREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND;

  private static final int SCHEDULE_NONE = 0;
  private static final int SCHEDULE_LAYOUT_ASYNC = 1;
  private static final int SCHEDULE_LAYOUT_SYNC = 2;
  private ComponentsStethoManager mStethoManager;

  @IntDef({SCHEDULE_NONE, SCHEDULE_LAYOUT_ASYNC, SCHEDULE_LAYOUT_SYNC})
  @Retention(RetentionPolicy.SOURCE)
  private @interface PendingLayoutCalculation {}

  private static final AtomicInteger sIdGenerator = new AtomicInteger(0);
  private static final Handler sMainThreadHandler = new ComponentMainThreadHandler();
  // Do not access sDefaultLayoutThreadLooper directly, use getDefaultLayoutThreadLooper().
  @GuardedBy("ComponentTree.class")
  private static volatile Looper sDefaultLayoutThreadLooper;

  // Helpers to track view visibility when we are incrementally
  // mounting and partially invalidating
  private static final int[] sCurrentLocation = new int[2];
  private static final int[] sParentLocation = new int[2];
  private static final Rect sParentBounds = new Rect();

  private final Runnable mCalculateLayoutRunnable = new Runnable() {
    @Override
    public void run() {
      calculateLayout(null);
    }
  };

  private final ComponentContext mContext;

  // These variables are only accessed from the main thread.
  private boolean mIsMounting;
  private boolean mIncrementalMountEnabled;
  private boolean mIsLayoutDiffingEnabled;
  private boolean mIsAttached;
  private boolean mIsAsyncUpdateStateEnabled;
  private ComponentView mComponentView;
  private LayoutHandler mLayoutThreadHandler;

  @GuardedBy("this")
  private boolean mHasViewMeasureSpec;

  // TODO(6606683): Enable recycling of mComponent.
  // We will need to ensure there are no background threads referencing mComponent. We'll need
  // to keep a reference count or something. :-/
  @GuardedBy("this")
  private Component<?> mRoot;

  @GuardedBy("this")
  private int mWidthSpec = SIZE_UNINITIALIZED;

  @GuardedBy("this")
  private int mHeightSpec = SIZE_UNINITIALIZED;

  // This is written to only by the main thread with the lock held, read from the main thread with
  // no lock held, or read from any other thread with the lock held.
  private LayoutState mMainThreadLayoutState;

  // The semantics here are tricky. Whenever you transfer mBackgroundLayoutState to a local that
  // will be accessed outside of the lock, you must set mBackgroundLayoutState to null to ensure
  // that the current thread alone has access to the LayoutState, which is single-threaded.
  @GuardedBy("this")
  private LayoutState mBackgroundLayoutState;

  @GuardedBy("this")
  private StateHandler mStateHandler;

  private Object mLayoutLock;

  protected final int mId = sIdGenerator.getAndIncrement();

  @GuardedBy("this")
  private boolean mIsMeasuring;
  @GuardedBy("this")
  private @PendingLayoutCalculation int mScheduleLayoutAfterMeasure;

  public static Builder create(ComponentContext context, Component.Builder<?> root) {
    return create(context, root.build());
  }

  public static Builder create(ComponentContext context, Component<?> root) {
    return ComponentsPools.acquireComponentTreeBuilder(context, root);
  }

  protected ComponentTree(Builder builder) {
    mContext = ComponentContext.withComponentTree(builder.context, this);
    mRoot = builder.root;

    mIncrementalMountEnabled = builder.incrementalMountEnabled;
    mIsLayoutDiffingEnabled = builder.isLayoutDiffingEnabled;
    mLayoutThreadHandler = builder.layoutThreadHandler;
    mLayoutLock = builder.layoutLock;
    mIsAsyncUpdateStateEnabled = builder.asyncStateUpdates;

    if (mLayoutThreadHandler == null) {
      mLayoutThreadHandler = new DefaultLayoutHandler(getDefaultLayoutThreadLooper());
    }

    final StateHandler builderStateHandler = builder.stateHandler;
    mStateHandler = builderStateHandler == null
        ? StateHandler.acquireNewInstance(null)
        : builderStateHandler;
  }

  LayoutState getMainThreadLayoutState() {
