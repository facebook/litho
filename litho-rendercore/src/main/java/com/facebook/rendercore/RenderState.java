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

package com.facebook.rendercore;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Pair;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import com.facebook.rendercore.utils.ThreadUtils;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/** todo: javadocs * */
public class RenderState<State> {

  private static final int UNSET = -1;
  private static final int PROMOTION_MESSAGE = 99;
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

  public interface TreeFactory<State> {
    Pair<Node, State> createTree(Node currentTree, State currentState);
  }

  public interface Delegate<State> {
    void commit(RenderTree current, RenderTree next, State currentState, State nextState);

    void commitToUI(RenderTree tree, State state);
  }

  public interface HostListener {
    void onUIRenderTreeUpdated(RenderTree newRenderTree);
  }

  private static class RenderResult<State> {
    private final RenderTree mRenderTree;
    private final Node mNodeTree;
    private final LayoutCache mLayoutCache;
    private final State mState;

    public RenderResult(
        RenderTree renderTree, Node nodeTree, LayoutCache layoutCache, State state) {
      mRenderTree = renderTree;
      mNodeTree = nodeTree;
      mLayoutCache = layoutCache;
      mState = state;
    }
  }

  private final RenderStateHandler mUIHandler = new RenderStateHandler(Looper.getMainLooper());
  private final Context mContext;
  private final Delegate<State> mDelegate;
  private final int mId = ID_GENERATOR.incrementAndGet();

  @ThreadConfined(ThreadConfined.UI)
  private @Nullable HostListener mHostListener;

  private @Nullable RenderResult<State> mUIRenderResult;
  private @Nullable RenderResult<State> mCommittedRenderResult;
  private @Nullable TreeFactory<State> mLatestTreeFactory;
  private @Nullable RenderResultFuture<State> mRenderResultFuture;
  private int mNextSetRootId = 0;
  private int mCommittedSetRootId = UNSET;
  private int mWidthSpec = UNSET;
  private int mHeightSpec = UNSET;

  public RenderState(Context context, Delegate<State> delegate) {
    mContext = context;
    mDelegate = delegate;
  }

  @ThreadConfined(ThreadConfined.ANY)
  public void setTree(TreeFactory<State> treeFactory) {
    final int setRootId;
    final RenderResultFuture<State> future;
    synchronized (this) {
      mLatestTreeFactory = treeFactory;

      if (mWidthSpec == UNSET || mHeightSpec == UNSET) {
        return;
      }

      setRootId = mNextSetRootId++;
      future =
          new RenderResultFuture<>(
              mContext, treeFactory, mCommittedRenderResult, setRootId, mWidthSpec, mHeightSpec);
      mRenderResultFuture = future;
    }

    final RenderResult<State> result = future.runAndGet();

    boolean committedNewLayout = false;
    synchronized (this) {
      // We don't want to compute, layout, or reduce trees while holding a lock. However this means
      // that another thread could compute a layout and commit it before we get to this point. To
      // handle this, we make sure that the committed setRootId is only ever increased, meaning
      // we only go "forward in time" and will eventually get to the latest layout.
      if (setRootId > mCommittedSetRootId) {
        mDelegate.commit(
            mCommittedRenderResult.mRenderTree,
            result.mRenderTree,
            mCommittedRenderResult.mState,
            result.mState);

        mCommittedSetRootId = setRootId;
        mCommittedRenderResult = result;
        committedNewLayout = true;
      }

      if (mRenderResultFuture == future) {
        mRenderResultFuture = null;
      }
    }

    if (committedNewLayout) {
      schedulePromoteCommittedTreeToUI();
    }
  }

  @ThreadConfined(ThreadConfined.UI)
  public void measure(int widthSpec, int heightSpec, @Nullable int[] measureOutput) {
    if (mUIRenderResult != null
        && hasCompatibleSize(mUIRenderResult.mRenderTree, widthSpec, heightSpec)) {
      if (measureOutput != null) {
        measureOutput[0] = mUIRenderResult.mRenderTree.getWidth();
        measureOutput[1] = mUIRenderResult.mRenderTree.getHeight();
      }
      return;
    }

    measureImpl(widthSpec, heightSpec, measureOutput);
  }

  @ThreadConfined(ThreadConfined.ANY)
  public void preMeasure(int widthSpec, int heightSpec, int[] measureOutput) {
    measureImpl(widthSpec, heightSpec, measureOutput);
  }

  @ThreadConfined(ThreadConfined.UI)
  public void attach(HostListener hostListener) {
    if (mHostListener != null && mHostListener != hostListener) {
      throw new RuntimeException("Must detach from previous host listener first");
    }
    mHostListener = hostListener;
  }

  @ThreadConfined(ThreadConfined.UI)
  public void detach() {
    mHostListener = null;
  }

  @ThreadConfined(ThreadConfined.UI)
  public @Nullable RenderTree getUIRenderTree() {
    return mUIRenderResult != null ? mUIRenderResult.mRenderTree : null;
  }

  private void measureImpl(int widthSpec, int heightSpec, @Nullable int[] measureOutput) {
    final int setRootId;
    final RenderResultFuture<State> future;
    synchronized (this) {
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;

      if (mCommittedRenderResult != null
          && hasCompatibleSize(mCommittedRenderResult.mRenderTree, widthSpec, heightSpec)
          && measureOutput != null) {
        measureOutput[0] = mCommittedRenderResult.mRenderTree.getWidth();
        measureOutput[1] = mCommittedRenderResult.mRenderTree.getHeight();
        return;
      }

      if (mRenderResultFuture != null && hasSameSpecs(mRenderResultFuture, widthSpec, heightSpec)) {
        future = mRenderResultFuture;
        setRootId = future.getSetRootId();
      } else {
        setRootId = mNextSetRootId++;
        future =
            new RenderResultFuture<>(
                mContext,
                mLatestTreeFactory,
                mCommittedRenderResult,
                setRootId,
                mWidthSpec,
                mHeightSpec);
        mRenderResultFuture = future;
      }
    }

    final RenderResult<State> result = future.runAndGet();

    boolean committedNewLayout = false;
    synchronized (this) {
      if (setRootId > mCommittedSetRootId) {
        mDelegate.commit(
            mCommittedRenderResult != null ? mCommittedRenderResult.mRenderTree : null,
            result.mRenderTree,
            mCommittedRenderResult != null ? mCommittedRenderResult.mState : null,
            result.mState);

        mCommittedSetRootId = setRootId;
        mCommittedRenderResult = result;
        committedNewLayout = true;
      }

      if (mRenderResultFuture == future) {
        mRenderResultFuture = null;
      }

      if (measureOutput != null) {
        measureOutput[0] = mCommittedRenderResult.mRenderTree.getWidth();
        measureOutput[1] = mCommittedRenderResult.mRenderTree.getHeight();
      }
    }

    if (committedNewLayout) {
      schedulePromoteCommittedTreeToUI();
    }
  }

  private void schedulePromoteCommittedTreeToUI() {
    if (ThreadUtils.isMainThread()) {
      promoteCommittedTreeToUI();
    } else {
      if (!mUIHandler.hasMessages(PROMOTION_MESSAGE)) {
        mUIHandler.sendEmptyMessage(PROMOTION_MESSAGE);
      }
    }
  }

  @ThreadConfined(ThreadConfined.UI)
  private void promoteCommittedTreeToUI() {
    synchronized (this) {
      mDelegate.commitToUI(mCommittedRenderResult.mRenderTree, mCommittedRenderResult.mState);

      mUIRenderResult = mCommittedRenderResult;
    }

    if (mHostListener != null) {
      mHostListener.onUIRenderTreeUpdated(mUIRenderResult.mRenderTree);
    }
  }

  private static LayoutResult layout(
      Context context, Node newTree, int widthSpec, int heightSpec, LayoutCache layoutCache) {

    return newTree.calculateLayout(context, widthSpec, heightSpec, layoutCache);
  }

  private static RenderTree reduce(
      Context context, int widthSpec, int heightSpec, LayoutResult layoutRoot) {
    return Reducer.getReducedTree(context, layoutRoot, widthSpec, heightSpec);
  }

  private boolean hasCompatibleSize(RenderTree tree, int widthSpec, int heightSpec) {
    return MeasureSpecUtils.isMeasureSpecCompatible(tree.getWidthSpec(), widthSpec, tree.getWidth())
        && MeasureSpecUtils.isMeasureSpecCompatible(
            tree.getHeightSpec(), heightSpec, tree.getHeight());
  }

  private boolean hasSameSpecs(RenderResultFuture<State> future, int widthSpec, int heightSpec) {
    return MeasureSpecUtils.areMeasureSpecsEquivalent(future.getWidthSpec(), widthSpec)
        && MeasureSpecUtils.areMeasureSpecsEquivalent(future.getHeightSpec(), heightSpec);
  }

  public int getId() {
    return mId;
  }

  private static class RenderResultFuture<State> {

    private final TreeFactory<State> mTreeFactory;
    private final RenderResult<State> mPreviousResult;
    private final int mSetRootId;
    private final int mWidthSpec;
    private final int mHeightSpec;
    private final FutureTask<RenderResult<State>> mFutureTask;
    private final AtomicInteger mRunningThreadId = new AtomicInteger(-1);

    private RenderResultFuture(
        final Context context,
        TreeFactory<State> treeFactory,
        RenderResult<State> previousResult,
        int setRootId,
        final int widthSpec,
        final int heightSpec) {
      mTreeFactory = treeFactory;
      mPreviousResult = previousResult;
      mSetRootId = setRootId;
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
      mFutureTask =
          new FutureTask<>(
              new Callable<RenderResult<State>>() {
                @Override
                public RenderResult<State> call() {
                  final Node previousTree =
                      mPreviousResult != null ? mPreviousResult.mNodeTree : null;
                  final State previousState =
                      mPreviousResult != null ? mPreviousResult.mState : null;
                  final LayoutCache layoutCache =
                      mPreviousResult != null
                          ? new LayoutCache(mPreviousResult.mLayoutCache.getWriteCache())
                          : new LayoutCache(null);

                  Systrace.sInstance.beginSection("RC Create Tree");
                  final Pair<Node, State> result =
                      mTreeFactory.createTree(previousTree, previousState);
                  Systrace.sInstance.endSection();

                  Systrace.sInstance.beginSection("RC Layout");
                  final LayoutResult layoutResult =
                      layout(context, result.first, widthSpec, heightSpec, layoutCache);
                  Systrace.sInstance.endSection();

                  Systrace.sInstance.beginSection("RC Reduce");
                  final RenderResult renderResult =
                      new RenderResult<>(
                          reduce(context, widthSpec, heightSpec, layoutResult),
                          result.first,
                          layoutCache,
                          result.second);
                  Systrace.sInstance.endSection();
                  return renderResult;
                }
              });
    }

    @Nullable
    RenderResult<State> runAndGet() {
      if (mRunningThreadId.compareAndSet(-1, Process.myTid())) {
        mFutureTask.run();
        try {
          return mFutureTask.get();
        } catch (ExecutionException | InterruptedException e) {
          // This should never happen since we've already completed the task
          throw new RuntimeException(e);
        }
      }

      return ThreadUtils.getResultInheritingPriority(mFutureTask, mRunningThreadId.get());
    }

    public int getSetRootId() {
      return mSetRootId;
    }

    public int getWidthSpec() {
      return mWidthSpec;
    }

    public int getHeightSpec() {
      return mHeightSpec;
    }
  }

  private class RenderStateHandler extends Handler {

    public RenderStateHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case PROMOTION_MESSAGE:
          promoteCommittedTreeToUI();
          break;
        default:
          throw new RuntimeException("Unknown message: " + msg.what);
      }
    }
  }
}
