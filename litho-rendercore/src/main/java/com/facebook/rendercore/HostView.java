/*
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

import static com.facebook.rendercore.RenderUnit.RenderType.DRAWABLE;
import static com.facebook.rendercore.RenderUnit.RenderType.VIEW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

/** A ViewGroup that can be used as a host for subtrees in a RenderCore tree. */
public class HostView extends Host {

  private static final int INITIAL_MOUNT_ITEMS_SIZE = 8;
  private final InterleavedDispatchDraw mDispatchDraw = new InterleavedDispatchDraw();
  private MountItem[] mMountItems;
  private int[] mChildDrawingOrder = new int[0];
  private boolean mIsChildDrawingOrderDirty;
  private boolean mInLayout;
  private @Nullable InterceptTouchHandler mOnInterceptTouchEventHandler;
  private @Nullable MountItem[] mScrapMountItemsArray;
  private @Nullable Object mViewTag;
  private @Nullable SparseArray<Object> mViewTags;
  private @Nullable Drawable mForeground;

  /**
   * {@link ViewGroup#getClipChildren()} was only added in API 18, will need to keep track of this
   * flag ourselves on the lower versions
   */
  private boolean mClipChildren = true;

  public HostView(Context context) {
    this(context, null);
  }

  public HostView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setWillNotDraw(false);
    setChildrenDrawingOrderEnabled(true);
    mMountItems = new MountItem[INITIAL_MOUNT_ITEMS_SIZE];
  }

  @Override
  public void mount(int index, MountItem mountItem) {
    if (mountItem.getRenderUnit().getRenderType() == DRAWABLE) {
      mountDrawable(mountItem);
    } else {
      mountView(mountItem);
    }
    ensureSize(index);
    mMountItems[index] = mountItem;
  }

  private void ensureSize(int index) {
    if (index >= mMountItems.length) {
      int newLength = mMountItems.length * 2;
      while (index >= newLength) {
        newLength = newLength * 2;
      }
      final MountItem[] tmp = new MountItem[newLength];
      System.arraycopy(mMountItems, 0, tmp, 0, mMountItems.length);
      mMountItems = tmp;
    }
  }

  @Override
  public void unmount(MountItem item) {
    final int index = findItemIndex(item);
    unmount(index, item);
  }

  private int findItemIndex(MountItem item) {
    for (int i = 0; i < mMountItems.length; i++) {
      if (mMountItems[i] == item) {
        return i;
      }
    }

    throw new IllegalStateException(
        "Mount item "
            + item
            + "Was selected for unmount but was not found in the list of mounted items");
  }

  @Override
  public void unmount(int index, MountItem mountItem) {
    if (mountItem.getRenderUnit().getRenderType() == DRAWABLE) {
      unmountDrawable(mountItem);
    } else {
      unmountView(mountItem);
      mIsChildDrawingOrderDirty = true;
    }

    MountUtils.removeItem(index, mMountItems, mScrapMountItemsArray);
    releaseScrapDataStructuresIfNeeded();
  }

  @Override
  public int getMountItemCount() {
    int size = 0;
    for (int i = 0; i < mMountItems.length; i++) {
      if (mMountItems[i] != null) {
        size++;
      }
    }

    return size;
  }

  @Override
  public MountItem getMountItemAt(int index) {
    return mMountItems[index];
  }

  @Override
  public void moveItem(MountItem item, int oldIndex, int newIndex) {
    if (item == null && mScrapMountItemsArray != null) {
      item = mScrapMountItemsArray[oldIndex];
    }

    if (item == null) {
      return;
    }
    final Object content = item.getContent();

    if (item.getRenderUnit().getRenderType() == DRAWABLE) {
      invalidate();
    } else {
      mIsChildDrawingOrderDirty = true;

      startTemporaryDetach(((View) content));
    }
    ensureSize(newIndex);

    if (mMountItems[newIndex] != null) {
      ensureScrapMountItemsArray();

      MountUtils.scrapItemAt(newIndex, mMountItems, mScrapMountItemsArray);
    }

    MountUtils.moveItem(oldIndex, newIndex, mMountItems, mScrapMountItemsArray);

    releaseScrapDataStructuresIfNeeded();

    if (item.getRenderUnit().getRenderType() == VIEW) {
      finishTemporaryDetach(((View) content));
    }
  }

  /**
   * Sets an InterceptTouchHandler that will be invoked when {@link HostView#onInterceptTouchEvent}
   * is called.
   *
   * @param interceptTouchEventHandler the handler to be set on this host.
   */
  public void setInterceptTouchEventHandler(
      @Nullable InterceptTouchHandler interceptTouchEventHandler) {
    mOnInterceptTouchEventHandler = interceptTouchEventHandler;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (mOnInterceptTouchEventHandler != null) {
      return mOnInterceptTouchEventHandler.onInterceptTouchEvent(this, ev);
    }

    return super.onInterceptTouchEvent(ev);
  }

  private void mountView(MountItem mountItem) {
    final View view = (View) mountItem.getContent();
    mIsChildDrawingOrderDirty = true;

    // A host has been recycled and is already attached.
    if (view instanceof HostView && view.getParent() == this) {
      finishTemporaryDetach(view);
      view.setVisibility(VISIBLE);
      return;
    }

    LayoutParams lp = view.getLayoutParams();
    if (lp == null) {
      lp = generateDefaultLayoutParams();
      view.setLayoutParams(lp);
    }

    if (mInLayout) {
      super.addViewInLayout(view, -1, view.getLayoutParams(), true);
    } else {
      super.addView(view, -1, view.getLayoutParams());
    }
  }

  private void unmountView(MountItem mountItem) {
    final View view = (View) mountItem.getContent();
    mIsChildDrawingOrderDirty = true;

    if (mInLayout) {
      super.removeViewInLayout(view);
    } else {
      super.removeView(view);
    }
  }

  @Override
  public void dispatchDraw(Canvas canvas) {
    mDispatchDraw.start(canvas);

    super.dispatchDraw(canvas);

    // Cover the case where the host has no child views, in which case
    // getChildDrawingOrder() will not be called and the draw index will not
    // be incremented. This will also cover the case where drawables must be
    // painted after the last child view in the host.
    if (mDispatchDraw.isRunning()) {
      mDispatchDraw.drawNext();
    }

    mDispatchDraw.end();
  }

  @Override
  protected int getChildDrawingOrder(int childCount, int i) {
    updateChildDrawingOrderIfNeeded();

    // This method is called in very different contexts within a ViewGroup
    // e.g. when handling input events, drawing, etc. We only want to call
    // the draw methods if the InterleavedDispatchDraw is active.
    if (mDispatchDraw.isRunning()) {
      mDispatchDraw.drawNext();
    }

    return mChildDrawingOrder[i];
  }

  @Override
  public boolean shouldDelayChildPressedState() {
    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {

    boolean handled = false;

    if (isEnabled()) {
      // Iterate drawable from last to first to respect drawing order.
      for (int i = ((mMountItems == null) ? 0 : mMountItems.length) - 1; i >= 0; i--) {
        final MountItem item = mMountItems[i];

        if (item != null
            && item.getRenderUnit().getRenderType() == DRAWABLE
            && item.getContent() instanceof Touchable) {
          final Touchable t = (Touchable) item.getContent();
          if (t.shouldHandleTouchEvent(event) && t.onTouchEvent(event, this)) {
            handled = true;
            break;
          }
        }
      }
    }

    if (!handled) {
      handled = super.onTouchEvent(event);
    }

    return handled;
  }

  void performLayout(boolean changed, int l, int t, int r, int b) {}

  @Override
  protected final void onLayout(boolean changed, int l, int t, int r, int b) {
    mInLayout = true;
    performLayout(changed, l, t, r, b);
    mInLayout = false;
  }

  @Override
  public void requestLayout() {
    // Don't request a layout if it will be blocked by any parent. Requesting a layout that is
    // then ignored by an ancestor means that this host will remain in a state where it thinks that
    // it has requested layout, and will therefore ignore future layout requests. This will lead to
    // problems if a child (e.g. a ViewPager) requests a layout later on, since the request will be
    // wrongly ignored by this host.
    ViewParent parent = this;
    while (parent instanceof HostView) {
      final HostView host = (HostView) parent;
      if (!host.shouldRequestLayout()) {
        return;
      }

      parent = parent.getParent();
    }

    super.requestLayout();
  }

  protected boolean shouldRequestLayout() {
    // Don't bubble during layout.
    return !mInLayout;
  }

  @Override
  @SuppressLint("MissingSuperCall")
  protected boolean verifyDrawable(Drawable who) {
    return true;
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();

    for (int i = 0, size = (mMountItems == null) ? 0 : mMountItems.length; i < size; i++) {
      final MountItem mountItem = mMountItems[i];
      if (mountItem != null && mountItem.getRenderUnit().getRenderType() == DRAWABLE) {
        MountUtils.maybeSetDrawableState(this, (Drawable) mountItem.getContent());
      }
    }

    if (mForeground != null) {
      mForeground.setState(getDrawableState());
    }
  }

  @Override
  public void jumpDrawablesToCurrentState() {
    super.jumpDrawablesToCurrentState();

    for (int i = 0, size = (mMountItems == null) ? 0 : mMountItems.length; i < size; i++) {
      final MountItem mountItem = mMountItems[i];
      if (mountItem != null && mountItem.getRenderUnit().getRenderType() == DRAWABLE) {
        final Drawable drawable = (Drawable) mountItem.getContent();
        DrawableCompat.jumpToCurrentState(drawable);
      }
    }

    if (mForeground != null) {
      mForeground.jumpToCurrentState();
    }
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);

    for (int i = 0, size = (mMountItems == null) ? 0 : mMountItems.length; i < size; i++) {
      MountItem mountItem = mMountItems[i];
      if (mountItem != null) {
        if (mountItem.getRenderUnit().getRenderType() == DRAWABLE) {
          final Drawable drawable = (Drawable) mountItem.getContent();
          drawable.setVisible(visibility == View.VISIBLE, false);
        }
      }
    }
  }

  /**
   * Sets view tag on this host.
   *
   * @param viewTag the object to set as tag.
   */
  public void setViewTag(Object viewTag) {
    mViewTag = viewTag;
  }

  /**
   * Sets view tags on this host.
   *
   * @param viewTags the map containing the tags by id.
   */
  public void setViewTags(SparseArray<Object> viewTags) {
    mViewTags = viewTags;
  }

  @Override
  public Object getTag() {
    if (mViewTag != null) {
      return mViewTag;
    }

    return super.getTag();
  }

  @Override
  public Object getTag(int key) {
    if (mViewTags != null) {
      final Object value = mViewTags.get(key);
      if (value != null) {
        return value;
      }
    }

    return super.getTag(key);
  }

  @Override
  public boolean getClipChildren() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      // There is no ViewGroup.getClipChildren() method on API < 18
      return mClipChildren;
    } else {
      return super.getClipChildren();
    }
  }

  @Override
  public void setClipChildren(boolean clipChildren) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      // There is no ViewGroup.getClipChildren() method on API < 18, will keep track this way
      mClipChildren = clipChildren;
    }
    super.setClipChildren(clipChildren);
  }

  private static void startTemporaryDetach(View view) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // Cancel any pending clicks.
      view.cancelPendingInputEvents();
    }

    // The HostView's parent will send an ACTION_CANCEL if it's going to receive
    // other motion events for the recycled child.
    ViewCompat.dispatchStartTemporaryDetach(view);
  }

  private static void finishTemporaryDetach(View view) {
    ViewCompat.dispatchFinishTemporaryDetach(view);
  }

  private static String getMountItemName(MountItem mountItem) {
    return mountItem.getRenderUnit().getClass().getSimpleName();
  }

  private void updateChildDrawingOrderIfNeeded() {
    if (!mIsChildDrawingOrderDirty) {
      return;
    }

    final int childCount = getChildCount();
    if (mChildDrawingOrder.length < childCount) {
      mChildDrawingOrder = new int[childCount + 5];
    }

    int index = 0;
    final int mountItemCount = mMountItems == null ? 0 : mMountItems.length;
    for (int i = 0; i < mountItemCount; i++) {
      final MountItem mountItem = mMountItems[i];
      if (mountItem != null
          && mountItem.getRenderUnit().getRenderType() == RenderUnit.RenderType.VIEW) {
        final View child = (View) mountItem.getContent();
        mChildDrawingOrder[index++] = indexOfChild(child);
      }
    }

    mIsChildDrawingOrderDirty = false;
  }

  private void ensureScrapMountItemsArray() {
    if (mScrapMountItemsArray == null) {
      mScrapMountItemsArray = new MountItem[mMountItems.length];
    }
  }

  private void releaseScrapDataStructuresIfNeeded() {
    if (mScrapMountItemsArray != null && isEmpty(mScrapMountItemsArray)) {
      mScrapMountItemsArray = null;
    }
  }

  private boolean isEmpty(MountItem[] scrapMountItemsArray) {
    for (int i = 0; i < scrapMountItemsArray.length; i++) {
      if (scrapMountItemsArray[i] != null) {
        return false;
      }
    }

    return true;
  }

  private void mountDrawable(MountItem mountItem) {
    final Drawable drawable = (Drawable) mountItem.getContent();

    MountUtils.mountDrawable(this, drawable);
  }

  private void unmountDrawable(MountItem mountItem) {
    final Drawable drawable = (Drawable) mountItem.getContent();
    drawable.setCallback(null);
    invalidate(drawable.getBounds());
  }

  /**
   * Encapsulates the logic for drawing a set of views and drawables respecting their drawing order
   * withing the component host i.e. allow interleaved views and drawables to be drawn with the
   * correct z-index.
   */
  private class InterleavedDispatchDraw {

    private Canvas mCanvas;
    private int mDrawIndex;
    private int mItemsToDraw;

    private InterleavedDispatchDraw() {}

    private void start(Canvas canvas) {
      mCanvas = canvas;
      mDrawIndex = 0;
      mItemsToDraw = mMountItems == null ? 0 : getMountItemCount();
    }

    private boolean isRunning() {
      return (mCanvas != null && mDrawIndex < mItemsToDraw);
    }

    private void drawNext() {
      if (mCanvas == null) {
        return;
      }

      for (int i = mDrawIndex, size = (mMountItems == null) ? 0 : getMountItemCount();
          i < size;
          i++) {
        final MountItem mountItem = mMountItems[i];

        // During a ViewGroup's dispatchDraw() call with children drawing order enabled,
        // getChildDrawingOrder() will be called before each child view is drawn. This
        // method will only draw the drawables "between" the child views and the let
        // the host draw its children as usual. This is why views are skipped here.
        if (mountItem.getRenderUnit().getRenderType() == RenderUnit.RenderType.VIEW) {
          mDrawIndex = i + 1;
          return;
        }

        if (!mountItem.isBound()) {
          continue;
        }

        ((Drawable) mountItem.getContent()).draw(mCanvas);
      }

      mDrawIndex = mItemsToDraw;
    }

    private void end() {
      mCanvas = null;
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (mForeground != null) {
      mForeground.setBounds(0, 0, getRight(), getBottom());
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    if (mForeground != null) {
      mForeground.draw(canvas);
    }
  }

  public void setForegroundCompat(@Nullable Drawable drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      MarshmallowHelper.setForeground(this, drawable);
    } else {
      setForegroundLollipop(drawable);
    }
  }

  /**
   * Copied over from FrameLayout#setForeground from API Version 16 with some differences: supports
   * only fill gravity and does not support padded foreground
   */
  private void setForegroundLollipop(@Nullable Drawable newForeground) {
    if (mForeground != newForeground) {
      if (mForeground != null) {
        mForeground.setCallback(null);
        unscheduleDrawable(mForeground);
      }

      mForeground = newForeground;

      if (newForeground != null) {
        newForeground.setCallback(this);
        if (newForeground.isStateful()) {
          newForeground.setState(getDrawableState());
        }
      }
      invalidate();
    }
  }

  static class MarshmallowHelper {
    @RequiresApi(api = Build.VERSION_CODES.M)
    static void setForeground(HostView hostView, @Nullable Drawable newForeground) {
      hostView.setForeground(newForeground);
    }
  }

  static void performLayoutOnChildrenIfNecessary(HostView host) {
    for (int i = 0, count = host.getChildCount(); i < count; i++) {
      final View child = host.getChildAt(i);

      if (child.isLayoutRequested()) {
        // The hosting view doesn't allow children to change sizes dynamically as
        // this would conflict with the component's own layout calculations.
        child.measure(
            MeasureSpec.makeMeasureSpec(child.getWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(child.getHeight(), MeasureSpec.EXACTLY));
        child.layout(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
      }

      if (child instanceof HostView) {
        performLayoutOnChildrenIfNecessary((HostView) child);
      }
    }
  }
}
