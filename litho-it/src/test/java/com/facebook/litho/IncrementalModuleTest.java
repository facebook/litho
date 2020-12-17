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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.visibility.IncrementalModule;
import com.facebook.rendercore.visibility.IncrementalModuleItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LEGACY)
@RunWith(LithoTestRunner.class)
public class IncrementalModuleTest {

  private ComponentContext mContext;
  private LithoView mLithoView;
  private static final int LEFT = 0;
  private static final int RIGHT = 10;
  private FrameLayout mParent;
  private IncrementalModuleWrapper mIncrementalModule;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mLithoView = new LithoView(mContext);
    mParent = new FrameLayout(mContext.getAndroidContext());
    mParent.setLeft(0);
    mParent.setTop(0);
    mParent.setRight(10);
    mParent.setBottom(10);
    mParent.addView(mLithoView);

    ComponentTestHelper.mountComponent(
        mContext, mLithoView, Column.create(mContext).build(), 100, 100);

    mIncrementalModule = new IncrementalModuleWrapper();
  }

  /** Test processing when enter/exit range on item bounds. Ex: Simple visibility. */
  class IncrementalItemImpl implements IncrementalModuleItem {

    final Rect mBounds;
    final String mId;
    boolean mEnterRangeCalled = false;
    boolean mExitRangeCalled = false;
    private float mHeightRatio;

    IncrementalItemImpl(int left, int top, int right, int bottom, String id) {
      mBounds = new Rect(left, top, right, bottom);
      mId = id;
    }

    @Override
    public String getId() {
      return mId;
    }

    @Override
    public Rect getBounds() {
      return mBounds;
    }

    @Override
    public float getEnterRangeTop() {
      return mBounds.top + mHeightRatio * (mBounds.bottom - mBounds.top);
    }

    @Override
    public float getEnterRangeBottom() {
      return mBounds.bottom - mHeightRatio * (mBounds.bottom - mBounds.top);
    }

    @Override
    public void onEnterVisibleRange() {
      mEnterRangeCalled = true;
    }

    @Override
    public void onExitVisibleRange() {
      mExitRangeCalled = true;
    }

    @Override
    public void onLithoViewAvailable(View view) {}

    void enterRangeHeightRatio(float heightRatio) {
      mHeightRatio = heightRatio;
    }

    void clear() {
      mEnterRangeCalled = false;
      mExitRangeCalled = false;
    }
  }

  @Test
  public void testEnterRange() {
    IncrementalItemImpl item = new IncrementalItemImpl(0, 5, 10, 5, "id1");

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), item);

    assertThat(item.mEnterRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), item);

    assertThat(item.mEnterRangeCalled).isTrue();
  }

  /** See {@link VisibilityEventsTest#testVisibleEventWithHeightRatio()} */
  @Test
  public void testEnterRangeWithHeightRatio() {
    IncrementalItemImpl item = new IncrementalItemImpl(0, 5, 10, 10, "id1");
    item.enterRangeHeightRatio(0.4f);

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 1), item);
    assertThat(item.mEnterRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 2), item);
    assertThat(item.mEnterRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 3), item);
    assertThat(item.mEnterRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 4), item);
    assertThat(item.mEnterRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), item);
    assertThat(item.mEnterRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 6), item);
    assertThat(item.mEnterRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 7), item);
    assertThat(item.mEnterRangeCalled).isTrue();
  }

  /** See {@link VisibilityEventsTest#testFullImpressionEvent()} */
  @Test
  public void testInRangeAllBoundsVisible() {
    IncrementalItemImpl item = new IncrementalItemImpl(LEFT, 5, 10, 10, "id1");
    item.enterRangeHeightRatio(1.0f);

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), item);
    assertThat(item.mEnterRangeCalled).isTrue();
  }

  /** See {@link VisibilityEventsTest#testInvisibleEvent()} */
  @Test
  public void testExitRange() {
    IncrementalItemImpl item = new IncrementalItemImpl(LEFT, 5, RIGHT, 10, "id1");

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), item);
    assertThat(item.mEnterRangeCalled).isTrue();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 5), item);
    assertThat(item.mExitRangeCalled).isTrue();
  }

  /** See {@link VisibilityEventsTest#testMultipleVisibleAndInvisibleEvents()} */
  @Test
  public void testMultipleIncItems() {
    IncrementalItemImpl item1 = new IncrementalItemImpl(LEFT, 0, RIGHT, 5, "id1");
    IncrementalItemImpl item2 = new IncrementalItemImpl(LEFT, 5, RIGHT, 10, "id2");
    IncrementalItemImpl item3 = new IncrementalItemImpl(LEFT, 10, RIGHT, 15, "id3");

    final List<IncrementalModuleItem> items = new ArrayList<>();
    items.add(item1);
    items.add(item2);
    items.add(item3);

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 15), items);

    assertThat(item1.mEnterRangeCalled).isTrue();
    assertThat(item2.mEnterRangeCalled).isTrue();
    assertThat(item3.mEnterRangeCalled).isTrue();

    assertThat(item1.mExitRangeCalled).isFalse();
    assertThat(item2.mExitRangeCalled).isFalse();
    assertThat(item3.mExitRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 15), items);
    assertThat(item1.mEnterRangeCalled).isFalse();
    assertThat(item2.mEnterRangeCalled).isFalse();
    assertThat(item3.mEnterRangeCalled).isFalse();

    assertThat(item1.mExitRangeCalled).isFalse();
    assertThat(item2.mExitRangeCalled).isFalse();
    assertThat(item3.mExitRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 0), items);
    assertThat(item1.mEnterRangeCalled).isFalse();
    assertThat(item2.mEnterRangeCalled).isFalse();
    assertThat(item3.mEnterRangeCalled).isFalse();

    assertThat(item1.mExitRangeCalled).isTrue();
    assertThat(item2.mExitRangeCalled).isTrue();
    assertThat(item3.mExitRangeCalled).isTrue();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 0), items);
    assertThat(item1.mEnterRangeCalled).isFalse();
    assertThat(item2.mEnterRangeCalled).isFalse();
    assertThat(item3.mEnterRangeCalled).isFalse();

    assertThat(item1.mExitRangeCalled).isFalse();
    assertThat(item2.mExitRangeCalled).isFalse();
    assertThat(item3.mExitRangeCalled).isFalse();
    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 3), items);
    assertThat(item1.mEnterRangeCalled).isTrue();
    assertThat(item2.mEnterRangeCalled).isFalse();
    assertThat(item3.mEnterRangeCalled).isFalse();

    assertThat(item1.mExitRangeCalled).isFalse();
    assertThat(item2.mExitRangeCalled).isFalse();
    assertThat(item3.mExitRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 3, RIGHT, 11), items);
    assertThat(item1.mEnterRangeCalled).isFalse();
    assertThat(item2.mEnterRangeCalled).isTrue();
    assertThat(item3.mEnterRangeCalled).isTrue();

    assertThat(item1.mExitRangeCalled).isFalse();
    assertThat(item2.mExitRangeCalled).isFalse();
    assertThat(item3.mExitRangeCalled).isFalse();

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 5, RIGHT, 11), items);
    assertThat(item1.mEnterRangeCalled).isFalse();
    assertThat(item2.mEnterRangeCalled).isFalse();
    assertThat(item3.mEnterRangeCalled).isFalse();

    assertThat(item1.mExitRangeCalled).isTrue();
    assertThat(item2.mExitRangeCalled).isFalse();
    assertThat(item3.mExitRangeCalled).isFalse();
  }

  @Test
  public void testRemoveItem() {
    IncrementalItemImpl item = new IncrementalItemImpl(0, 0, 10, 10, "id1");

    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), item);

    assertThat(item.mEnterRangeCalled).isTrue();

    mIncrementalModule.mIsDirty = true;
    mIncrementalModule.performIncrementalMount(new Rect(LEFT, 0, RIGHT, 10), new ArrayList<>());

    assertThat(item.mExitRangeCalled).isTrue();
  }

  private class IncrementalModuleWrapper {

    private Rect mPreviousRect;
    private IncrementalModule mIncrementalModule;
    private boolean mIsDirty = true;

    IncrementalModuleWrapper() {
      mIncrementalModule = new IncrementalModule(mLithoView);
      mPreviousRect = new Rect(0, 0, 0, 0);
    }

    public void performIncrementalMount(Rect rect, IncrementalModuleItem item) {
      final List<IncrementalModuleItem> items = new ArrayList<>();
      items.add(item);

      performIncrementalMount(rect, items);
    }

    public void performIncrementalMount(Rect rect, List<IncrementalModuleItem> items) {
      for (IncrementalModuleItem item : items) {
        if (item instanceof IncrementalItemImpl) {
          IncrementalItemImpl impl = (IncrementalItemImpl) item;
          impl.clear();
        }
      }

      final List<IncrementalModuleItem> tops = new ArrayList<>(items);
      final List<IncrementalModuleItem> bottoms = new ArrayList<>(items);
      Collections.sort(tops, IncrementalModule.sTopsComparators);
      Collections.sort(bottoms, IncrementalModule.sBottomsComparator);

      mIncrementalModule.performIncrementalProcessing(mIsDirty, tops, bottoms, rect, mPreviousRect);
      mPreviousRect = rect;
      mIsDirty = false;
    }
  }
}
