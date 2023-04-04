/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import com.facebook.rendercore.utils.ThreadUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.ThreadSafe;

/** todo: javadocs * */
public class RenderState<State, RenderContext> implements StateUpdateReceiver<State> {

  private static final int UNSET = -1;
  private static final int PROMOTION_MESSAGE = 99;
  private static final int UPDATE_STATE_MESSAGE = 100;
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

  public static int NO_ID = -1;

  /**
   * Represents a function capable of creating a tree. The tree is lazy so that the creation can be
   * done inline with the layout pass.
   *
   * @param <State> Represents the State that this tree would like to commit when the tree itself is
   *     committed
   */
  @ThreadSafe
  public interface ResolveFunc<State, RenderContext> {
    /**
     * Resolves the tree represented by this ResolveFunc. Results for resolve might be cached. The
     * assumption is that multiple resolve calls on a ResolveFunc would return equivalent trees.
     *
     * @param resolveContext
     * @param committedTree
     * @param committedState
     * @param stateUpdatesToApply
     */
    Pair<Node<RenderContext>, State> resolve(
        ResolveContext<RenderContext> resolveContext,
        @Nullable Node<RenderContext> committedTree,
        @Nullable State committedState,
        List<? extends StateUpdate> stateUpdatesToApply);
  }

  public interface Delegate<State> {
    void commit(
        int layoutVersion,
        @Nullable RenderTree current,
        RenderTree next,
        @Nullable State currentState,
        @Nullable State nextState);

    void commitToUI(RenderTree tree, @Nullable State state);
  }

  public interface HostListener {
    void onUIRenderTreeUpdated(RenderTree newRenderTree);
  }

  private final RenderStateHandler mUIHandler = new RenderStateHandler(Looper.getMainLooper());
  private final Context mContext;
  private final Delegate<State> mDelegate;
  private final @Nullable RenderContext mRenderContext;
  private final @Nullable RenderCoreExtension<?, ?>[] mExtensions;
  private final int mId = ID_GENERATOR.incrementAndGet();

  @ThreadConfined(ThreadConfined.UI)
  private @Nullable HostListener mHostListener;

  private @Nullable RenderTree mUIRenderTree;
  private @Nullable ResolveFunc<State, RenderContext> mLatestResolveFunc;
  private @Nullable ResolveFuture<State, RenderContext> mResolveFuture;
  private @Nullable LayoutFuture<State, RenderContext> mLayoutFuture;

  private @Nullable Node<RenderContext> mCommittedResolvedTree;
  private @Nullable State mCommittedState;

  private final List<StateUpdate> mPendingStateUpdates = new ArrayList<>();

  private @Nullable RenderResult<State, RenderContext> mCommittedRenderResult;

  private int mResolveVersionCounter = 0;
  private int mLayoutVersionCounter = 0;
  private int mCommittedResolveVersion = UNSET;
  private int mCommittedLayoutVersion = UNSET;

  private int mWidthSpec = UNSET;
  private int mHeightSpec = UNSET;

  public RenderState(
      final Context context,
      final Delegate<State> delegate,
      final @Nullable RenderContext renderContext,
      final @Nullable RenderCoreExtension<?, ?>[] extensions) {
    mContext = context;
    mDelegate = delegate;
    mRenderContext = renderContext;
    mExtensions = extensions;
  }

  @ThreadConfined(ThreadConfined.ANY)
  public void setTree(ResolveFunc<State, RenderContext> resolveFunc) {
    setTree(resolveFunc, null);
  }

  @ThreadConfined(ThreadConfined.ANY)
  public void setTree(ResolveFunc<State, RenderContext> resolveFunc, @Nullable Executor executor) {
    requestResolve(resolveFunc, executor);
  }

  @ThreadConfined(ThreadConfined.ANY)
  @Override
  public void enqueueStateUpdate(StateUpdate stateUpdate) {
    synchronized (this) {
      mPendingStateUpdates.add(stateUpdate);
      if (mLatestResolveFunc == null) {
        return;
      }
    }

    if (!mUIHandler.hasMessages(UPDATE_STATE_MESSAGE)) {
      mUIHandler.sendEmptyMessage(UPDATE_STATE_MESSAGE);
    }
  }

  private void flushStateUpdates() {
    requestResolve(null, null);
  }

  private void requestResolve(
      @Nullable ResolveFunc<State, RenderContext> resolveFunc, @Nullable Executor executor) {
    final ResolveFuture<State, RenderContext> future;

    synchronized (this) {
      // Resolve was triggered by State Update, but all pendingStateUpdates are already applied.
      if (resolveFunc == null && mPendingStateUpdates.isEmpty()) {
        return;
      }

      if (resolveFunc != null) {
        mLatestResolveFunc = resolveFunc;
      }

      future =
          new ResolveFuture<>(
              requireNonNull(mLatestResolveFunc),
              new ResolveContext<>(mRenderContext, this),
              mCommittedResolvedTree,
              mCommittedState,
              mPendingStateUpdates.isEmpty() ? emptyList() : new ArrayList<>(mPendingStateUpdates),
              mResolveVersionCounter++);
      mResolveFuture = future;
    }
    if (executor != null) {
      executor.execute(() -> resolveTreeAndMaybeCommit(future));
    } else {
      resolveTreeAndMaybeCommit(future);
    }
  }

  private void resolveTreeAndMaybeCommit(ResolveFuture<State, RenderContext> future) {
    final Pair<Node<RenderContext>, State> result = future.runAndGet();
    if (maybeCommitResolveResult(result, future)) {
      layoutAndMaybeCommitInternal(null);
    }
  }

  private synchronized boolean maybeCommitResolveResult(
      Pair<Node<RenderContext>, State> result, ResolveFuture<State, RenderContext> future) {
    // We don't want to compute, layout, or reduce trees while holding a lock. However this means
    // that another thread could compute a layout and commit it before we get to this point. To
    // handle this, we make sure that the committed resolve version is only ever increased, meaning
    // we only go "forward in time" and will eventually get to the latest layout.
    boolean didCommit = false;
    if (future.getVersion() > mCommittedResolveVersion) {
      mCommittedResolveVersion = future.getVersion();
      mCommittedResolvedTree = result.first;
      mCommittedState = result.second;
      mPendingStateUpdates.removeAll(future.getStateUpdatesToApply());
      didCommit = true;
    }

    if (mResolveFuture == future) {
      mResolveFuture = null;
    }

    return didCommit;
  }

  private void layoutAndMaybeCommitInternal(@Nullable int[] measureOutput) {
    final LayoutFuture<State, RenderContext> layoutFuture;
    final RenderResult<State, RenderContext> previousRenderResult;
    synchronized (this) {
      if (mWidthSpec == UNSET || mHeightSpec == UNSET) {
        return;
      }

      requireNonNull(
          mCommittedResolvedTree, "Tried executing the layout step before resolving a tree");

      if (mLayoutFuture == null
          || mLayoutFuture.getTree() != mCommittedResolvedTree
          || !hasSameSpecs(mLayoutFuture, mWidthSpec, mHeightSpec)) {
        mLayoutFuture =
            new LayoutFuture<>(
                mContext,
                mRenderContext,
                mCommittedResolvedTree,
                mCommittedState,
                mLayoutVersionCounter++,
                mCommittedRenderResult,
                mExtensions,
                mWidthSpec,
                mHeightSpec);
      }
      layoutFuture = mLayoutFuture;
      previousRenderResult = mCommittedRenderResult;
    }
    final RenderResult<State, RenderContext> renderResult = layoutFuture.runAndGet();

    boolean committedNewLayout = false;
    synchronized (this) {
      if (hasSameSpecs(layoutFuture, mWidthSpec, mHeightSpec)
          && layoutFuture.getVersion() > mCommittedLayoutVersion
          && mCommittedRenderResult != renderResult) {
        mCommittedLayoutVersion = layoutFuture.getVersion();
        committedNewLayout = true;
        mCommittedRenderResult = renderResult;
      }

      if (mLayoutFuture == layoutFuture) {
        mLayoutFuture = null;
      }
    }

    if (measureOutput != null) {
      measureOutput[0] = renderResult.getRenderTree().getWidth();
      measureOutput[1] = renderResult.getRenderTree().getHeight();
    }

    if (committedNewLayout) {
      mDelegate.commit(
          layoutFuture.getVersion(),
          previousRenderResult != null ? previousRenderResult.getRenderTree() : null,
          renderResult.getRenderTree(),
          previousRenderResult != null ? previousRenderResult.getState() : null,
          renderResult.getState());
      schedulePromoteCommittedTreeToUI();
    }
  }

  @ThreadConfined(ThreadConfined.UI)
  public void measure(int widthSpec, int heightSpec, @Nullable int[] measureOutput) {
    final ResolveFuture<State, RenderContext> futureToResolveBeforeMeasuring;

    synchronized (this) {
      if (mWidthSpec != widthSpec || mHeightSpec != heightSpec) {
        mWidthSpec = widthSpec;
        mHeightSpec = heightSpec;
      }

      // The current UI tree is compatible. We might just return those values
      if (mUIRenderTree != null && hasCompatibleSize(mUIRenderTree, widthSpec, heightSpec)) {
        if (measureOutput != null) {
          measureOutput[0] = mUIRenderTree.getWidth();
          measureOutput[1] = mUIRenderTree.getHeight();
        }
        return;
      }

      if (mCommittedRenderResult != null
          && hasCompatibleSize(mCommittedRenderResult.getRenderTree(), widthSpec, heightSpec)) {
        maybePromoteCommittedTreeToUI();

        if (measureOutput != null) {
          // We have a tree that we previously resolved with these contraints. For measuring we can
          // just return it
          measureOutput[0] = mCommittedRenderResult.getRenderTree().getWidth();
          measureOutput[1] = mCommittedRenderResult.getRenderTree().getHeight();
        }
        return;
      }

      // We don't have a valid resolve function yet. Let's just bail until then.
      if (mLatestResolveFunc == null) {
        if (measureOutput != null) {
          measureOutput[0] = 0;
          measureOutput[1] = 0;
        }
        return;
      }

      // If we do have a resolve function we expect to have either a committed resolved tree or a
      // future. If we have neither something has gone wrong with the setTree call.
      if (mCommittedResolvedTree != null) {
        futureToResolveBeforeMeasuring = null;
      } else {
        futureToResolveBeforeMeasuring = requireNonNull(mResolveFuture);
      }
    }

    if (futureToResolveBeforeMeasuring != null) {
      final Pair<Node<RenderContext>, State> result = futureToResolveBeforeMeasuring.runAndGet();
      maybeCommitResolveResult(result, futureToResolveBeforeMeasuring);
    }

    layoutAndMaybeCommitInternal(measureOutput);
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
    return mUIRenderTree;
  }

  private void schedulePromoteCommittedTreeToUI() {
    if (ThreadUtils.isMainThread()) {
      maybePromoteCommittedTreeToUI();
    } else {
      if (!mUIHandler.hasMessages(PROMOTION_MESSAGE)) {
        mUIHandler.sendEmptyMessage(PROMOTION_MESSAGE);
      }
    }
  }

  @ThreadConfined(ThreadConfined.UI)
  private void maybePromoteCommittedTreeToUI() {
    synchronized (this) {
      mDelegate.commitToUI(
          mCommittedRenderResult.getRenderTree(), mCommittedRenderResult.getState());

      if (mUIRenderTree == mCommittedRenderResult.getRenderTree()) {
        return;
      }

      mUIRenderTree = mCommittedRenderResult.getRenderTree();
    }

    if (mHostListener != null) {
      mHostListener.onUIRenderTreeUpdated(mUIRenderTree);
    }
  }

  private static boolean hasCompatibleSize(RenderTree tree, int widthSpec, int heightSpec) {
    return MeasureSpecUtils.isMeasureSpecCompatible(tree.getWidthSpec(), widthSpec, tree.getWidth())
        && MeasureSpecUtils.isMeasureSpecCompatible(
            tree.getHeightSpec(), heightSpec, tree.getHeight());
  }

  private static <State, RenderContext> boolean hasSameSpecs(
      LayoutFuture<State, RenderContext> future, int widthSpec, int heightSpec) {
    return MeasureSpecUtils.areMeasureSpecsEquivalent(future.getWidthSpec(), widthSpec)
        && MeasureSpecUtils.areMeasureSpecsEquivalent(future.getHeightSpec(), heightSpec);
  }

  public int getId() {
    return mId;
  }

  @Nullable
  RenderCoreExtension<?, ?>[] getExtensions() {
    return mExtensions;
  }

  private class RenderStateHandler extends Handler {

    public RenderStateHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case PROMOTION_MESSAGE:
          maybePromoteCommittedTreeToUI();
          break;

        case UPDATE_STATE_MESSAGE:
          flushStateUpdates();
          break;
        default:
          throw new RuntimeException("Unknown message: " + msg.what);
      }
    }
  }
}
