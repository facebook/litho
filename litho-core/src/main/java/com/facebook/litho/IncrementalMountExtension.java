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

package com.facebook.litho;

import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.litho.MountDelegate.MountDelegateInput;
import com.facebook.litho.stats.LithoStats;
import com.facebook.rendercore.RenderTreeNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Extension for performing incremental mount. */
public class IncrementalMountExtension extends MountDelegateExtension
    implements HostListenerExtension<IncrementalMountExtension.IncrementalMountExtensionInput> {

  private final Host mLithoView;
  private boolean mIsMounting;
  private int mPreviousTopsIndex;
  private int mPreviousBottomsIndex;
  private final Rect mPreviousLocalVisibleRect = new Rect();
  private final Set<Long> mComponentIdsMountedInThisFrame = new HashSet<>();
  private IncrementalMountExtensionInput mInput;

  public interface IncrementalMountExtensionInput extends MountDelegateInput {
    int getMountableOutputCount();

    List<RenderTreeNode> getMountableOutputTops();

    List<RenderTreeNode> getMountableOutputBottoms();

    int getLayoutOutputPositionForId(long id);
  }

  public IncrementalMountExtension(Host lithoView) {
    mLithoView = lithoView;
  }

  @Override
  public void beforeMount(IncrementalMountExtensionInput input) {
    mInput = input;
    mPreviousLocalVisibleRect.setEmpty();

    final Rect localVisibleRect = mLithoView.getVisibleRect();
    initIncrementalMount(localVisibleRect);
    setVisibleRect(localVisibleRect);
  }

  @Override
  public void onUnmount() {
    resetAcquiredReferences();
    mPreviousLocalVisibleRect.setEmpty();
  }

  /**
   * Called when LithoView visible bounds change to perform incremental mount. This is always called
   * on a non-dirty mount with a non-null localVisibleRect.
   */
  @Override
  public void onViewOffset() {
    assertMainThread();

    mIsMounting = true;
    final Rect localVisibleRect = mLithoView.getVisibleRect();

    // Horizontally scrolling, can't incrementally mount.
    if (localVisibleRect.left != mPreviousLocalVisibleRect.left
        || localVisibleRect.right != mPreviousLocalVisibleRect.right) {
      initIncrementalMount(localVisibleRect);
    } else {
      performIncrementalMount(localVisibleRect);
    }

    setVisibleRect(localVisibleRect);

    LithoStats.incrementComponentMountCount();

    mIsMounting = false;
  }

  private void setVisibleRect(@Nullable Rect localVisibleRect) {
    if (localVisibleRect != null) {
      mPreviousLocalVisibleRect.set(localVisibleRect);
    }
  }

  @Override
  public void onUnbind() {}

  @Override
  public void onHostVisibilityChanged(boolean isVisible) {}

  private void initIncrementalMount(Rect localVisibleRect) {
    final MountItem rootMountItem = getRootMountItem();

    for (int i = 0, size = mInput.getMountableOutputCount(); i < size; i++) {
      final LayoutOutput layoutOutput = getLayoutOutput(mInput.getMountableOutputAt(i));
      final Component component = layoutOutput.getComponent();
      // TODO: pass in the list of mounted items.
      final MountItem currentMountItem = getItemAt(i);
      final boolean isMounted = currentMountItem != null;

      // By default, a LayoutOutput passed in to mount will be mountable. Incremental mount can
      // override that if the item is outside the visible bounds.
      // TODO: extract animations logic out of this.
      final boolean isMountable =
          isMountedHostWithChildContent(currentMountItem)
              || Rect.intersects(localVisibleRect, layoutOutput.getBounds())
              || isAnimationLocked(i)
              || (currentMountItem != null && currentMountItem == rootMountItem);
      final boolean hasAcquiredMountRef = ownsReference(layoutOutput);
      if (isMountable && !hasAcquiredMountRef) {
        acquireMountReference(layoutOutput, i, mInput, mIsMounting);
      } else if (!isMountable && hasAcquiredMountRef) {
        releaseMountReference(layoutOutput, i);
      }

      if (!isMountable) {
        continue;
      }

      // TODO: extract animations logic out of this.
      if (!isMounted) {
        if (isAnimationLocked(i) && component.hasChildLithoViews()) {
          // If the component is locked for animation then we need to make sure that all the
          // children are also mounted.
          final View view = (View) getItemAt(i).getContent();
          // We're mounting everything, don't process visibility outputs as they will not be
          // accurate.
          mountViewIncrementally(view, true);
        }
      } else {
        if (component.hasChildLithoViews()) {
          mountItemIncrementally(currentMountItem);
        }
      }
    }

    setupPreviousMountableOutputData(localVisibleRect);
  }

  /**
   * @return true if this method did all the work that was necessary and there is no other content
   *     that needs mounting/unmounting in this mount step. If false then a full mount step should
   *     take place.
   */
  private boolean performIncrementalMount(Rect localVisibleRect) {
    final List<RenderTreeNode> layoutOutputTops = mInput.getMountableOutputTops();
    final List<RenderTreeNode> layoutOutputBottoms = mInput.getMountableOutputBottoms();
    final int count = mInput.getMountableOutputCount();

    if (localVisibleRect.top > 0 || mPreviousLocalVisibleRect.top > 0) {
      // View is going on/off the top of the screen. Check the bottoms to see if there is anything
      // that has moved on/off the top of the screen.
      while (mPreviousBottomsIndex < count
          && localVisibleRect.top
              >= layoutOutputBottoms.get(mPreviousBottomsIndex).getBounds().bottom) {
        final RenderTreeNode node = layoutOutputBottoms.get(mPreviousBottomsIndex);
        final LayoutOutput layoutOutput = (LayoutOutput) node.getLayoutData();
        final long id = layoutOutput.getId();
        final int layoutOutputIndex = mInput.getLayoutOutputPositionForId(id);
        if (!isAnimationLocked(layoutOutputIndex)) {
          releaseMountReference(layoutOutput, layoutOutputIndex);
        }
        mPreviousBottomsIndex++;
      }

      while (mPreviousBottomsIndex > 0
          && localVisibleRect.top
              < layoutOutputBottoms.get(mPreviousBottomsIndex - 1).getBounds().bottom) {
        mPreviousBottomsIndex--;
        final RenderTreeNode node = layoutOutputBottoms.get(mPreviousBottomsIndex);
        final LayoutOutput layoutOutput = (LayoutOutput) node.getLayoutData();
        final int layoutOutputIndex = mInput.getLayoutOutputPositionForId(layoutOutput.getId());
        if (getItemAt(layoutOutputIndex) == null) {
          acquireMountReference(
              layoutOutput,
              mInput.getLayoutOutputPositionForId(layoutOutput.getId()),
              mInput,
              mIsMounting);
          mComponentIdsMountedInThisFrame.add(layoutOutput.getId());
        }
      }
    }

    final int height = mLithoView.getHeight();
    if (localVisibleRect.bottom < height || mPreviousLocalVisibleRect.bottom < height) {
      // View is going on/off the bottom of the screen. Check the tops to see if there is anything
      // that has changed.
      while (mPreviousTopsIndex < count
          && localVisibleRect.bottom > layoutOutputTops.get(mPreviousTopsIndex).getBounds().top) {
        final RenderTreeNode node = layoutOutputTops.get(mPreviousTopsIndex);
        final LayoutOutput layoutOutput = (LayoutOutput) node.getLayoutData();
        final int layoutOutputIndex = mInput.getLayoutOutputPositionForId(layoutOutput.getId());
        if (getItemAt(layoutOutputIndex) == null) {
          acquireMountReference(
              layoutOutput,
              mInput.getLayoutOutputPositionForId(layoutOutput.getId()),
              mInput,
              mIsMounting);
          mComponentIdsMountedInThisFrame.add(layoutOutput.getId());
        }
        mPreviousTopsIndex++;
      }

      while (mPreviousTopsIndex > 0
          && localVisibleRect.bottom
              <= layoutOutputTops.get(mPreviousTopsIndex - 1).getBounds().top) {
        mPreviousTopsIndex--;
        final RenderTreeNode node = layoutOutputTops.get(mPreviousTopsIndex);
        final LayoutOutput layoutOutput = (LayoutOutput) node.getLayoutData();
        final long id = layoutOutput.getId();
        final int layoutOutputIndex = mInput.getLayoutOutputPositionForId(id);
        if (!isAnimationLocked(layoutOutputIndex)) {
          releaseMountReference(layoutOutput, layoutOutputIndex);
        }
      }
    }

    for (int i = 0, size = mInput.getMountableOutputCount(); i < size; i++) {
      final LayoutOutput layoutOutput = getLayoutOutput(mInput.getMountableOutputAt(i));
      final long layoutOutputId = layoutOutput.getId();

      if (!mComponentIdsMountedInThisFrame.contains(layoutOutputId)) {
        final Component component = layoutOutput.getComponent();
        if (component.hasChildLithoViews() && isLockedForMount(layoutOutput)) {
          final int layoutOutputPosition = mInput.getLayoutOutputPositionForId(layoutOutputId);
          if (layoutOutputPosition != -1) {
            mountItemIncrementally(getItemAt(i));
          }
        }
      }
    }

    mComponentIdsMountedInThisFrame.clear();

    return true;
  }

  private void setupPreviousMountableOutputData(Rect localVisibleRect) {
    if (localVisibleRect.isEmpty()) {
      return;
    }

    final List<RenderTreeNode> layoutOutputTops = mInput.getMountableOutputTops();
    final List<RenderTreeNode> layoutOutputBottoms = mInput.getMountableOutputBottoms();
    final int mountableOutputCount = mInput.getMountableOutputCount();

    mPreviousTopsIndex = mInput.getMountableOutputCount();
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.bottom <= layoutOutputTops.get(i).getBounds().top) {
        mPreviousTopsIndex = i;
        break;
      }
    }

    mPreviousBottomsIndex = mInput.getMountableOutputCount();
    for (int i = 0; i < mountableOutputCount; i++) {
      if (localVisibleRect.top < layoutOutputBottoms.get(i).getBounds().bottom) {
        mPreviousBottomsIndex = i;
        break;
      }
    }
  }

  private boolean isMountedHostWithChildContent(MountItem mountItem) {
    if (mountItem == null) {
      return false;
    }

    final Object content = mountItem.getContent();
    if (!(content instanceof ComponentHost)) {
      return false;
    }

    final ComponentHost host = (ComponentHost) content;
    return host.getMountItemCount() > 0;
  }

  private static void mountItemIncrementally(MountItem item) {
    final Component component = item.getComponent();

    if (!isMountViewSpec(component)) {
      return;
    }

    // We can't just use the bounds of the View since we need the bounds relative to the
    // hosting LithoView (which is what the localVisibleRect is measured relative to).
    final View view = (View) item.getContent();

    mountViewIncrementally(view, false);
  }

  private static void mountViewIncrementally(View view, boolean mountingAll) {
    assertMainThread();

    if (view instanceof LithoView) {
      final LithoView lithoView = (LithoView) view;
      if (lithoView.isIncrementalMountEnabled()) {
        if (mountingAll) {
          lithoView.performIncrementalMount(
              new Rect(0, 0, view.getWidth(), view.getHeight()), false);
        } else {
          lithoView.performIncrementalMount();
        }
      }
    } else if (view instanceof ViewGroup) {
      final ViewGroup viewGroup = (ViewGroup) view;

      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        final View childView = viewGroup.getChildAt(i);
        mountViewIncrementally(childView, mountingAll);
      }
    }
  }
}
