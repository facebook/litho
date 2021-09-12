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

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.LithoMountData.getMountData;
import static com.facebook.litho.LithoMountData.isViewClickable;
import static com.facebook.litho.LithoMountData.isViewEnabled;
import static com.facebook.litho.LithoMountData.isViewFocusable;
import static com.facebook.litho.LithoMountData.isViewLongClickable;
import static com.facebook.litho.LithoMountData.isViewSelected;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import android.util.SparseArray;
import android.view.View;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link MountItem} */
@RunWith(LithoTestRunner.class)
public class MountItemTest {
  private MountItem mMountItem;
  private Component mComponent;
  private ComponentHost mComponentHost;
  private Object mContent;
  private CharSequence mContentDescription;
  private Object mViewTag;
  private SparseArray<Object> mViewTags;
  private EventHandler mClickHandler;
  private EventHandler mLongClickHandler;
  private EventHandler mFocusChangeHandler;
  private EventHandler mTouchHandler;
  private EventHandler mDispatchPopulateAccessibilityEventHandler;
  private int mFlags;
  private ComponentContext mContext;
  private NodeInfo mNodeInfo;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(getApplicationContext());

    mComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return SimpleMountSpecTester.create(c).build();
          }
        };
    mComponentHost = new ComponentHost(getApplicationContext());
    mContent = new View(getApplicationContext());
    mContentDescription = "contentDescription";
    mViewTag = "tag";
    mViewTags = new SparseArray<>();
    mClickHandler = new EventHandler(mComponent, 5);
    mLongClickHandler = new EventHandler(mComponent, 3);
    mFocusChangeHandler = new EventHandler(mComponent, 9);
    mTouchHandler = new EventHandler(mComponent, 1);
    mDispatchPopulateAccessibilityEventHandler = new EventHandler(mComponent, 7);
    mFlags = 114;

    mNodeInfo = new DefaultNodeInfo();
    mNodeInfo.setContentDescription(mContentDescription);
    mNodeInfo.setClickHandler(mClickHandler);
    mNodeInfo.setLongClickHandler(mLongClickHandler);
    mNodeInfo.setFocusChangeHandler(mFocusChangeHandler);
    mNodeInfo.setTouchHandler(mTouchHandler);
    mNodeInfo.setViewTag(mViewTag);
    mNodeInfo.setViewTags(mViewTags);
    mMountItem = create(mContent);
  }

  MountItem create(Object content) {
    return MountItemTestHelper.create(
        mComponent,
        null,
        mComponentHost,
        content,
        mNodeInfo,
        null,
        null,
        0,
        0,
        mFlags,
        0,
        IMPORTANT_FOR_ACCESSIBILITY_YES,
        ORIENTATION_PORTRAIT,
        null);
  }

  @Test
  public void testIsBound() {
    mMountItem.setIsBound(true);
    assertThat(mMountItem.isBound()).isTrue();

    mMountItem.setIsBound(false);
    assertThat(mMountItem.isBound()).isFalse();
  }

  @Test
  public void testGetters() {
    assertThat(getLayoutOutput(mMountItem).getComponent()).isSameAs((Component) mComponent);
    assertThat(mMountItem.getHost()).isSameAs(mComponentHost);
    assertThat(mMountItem.getContent()).isSameAs(mContent);
    assertThat(getLayoutOutput(mMountItem).getNodeInfo().getContentDescription())
        .isSameAs(mContentDescription);
    assertThat(getLayoutOutput(mMountItem).getNodeInfo().getClickHandler()).isSameAs(mClickHandler);
    assertThat(getLayoutOutput(mMountItem).getNodeInfo().getFocusChangeHandler())
        .isSameAs(mFocusChangeHandler);
    assertThat(getLayoutOutput(mMountItem).getNodeInfo().getTouchHandler()).isSameAs(mTouchHandler);
    assertThat(getLayoutOutput(mMountItem).getFlags()).isEqualTo(mFlags);
    assertThat(getLayoutOutput(mMountItem).getImportantForAccessibility())
        .isEqualTo(IMPORTANT_FOR_ACCESSIBILITY_YES);
  }

  @Test
  public void testFlags() {
    mFlags =
        LayoutOutput.LAYOUT_FLAG_DUPLICATE_PARENT_STATE
            | LayoutOutput.LAYOUT_FLAG_DISABLE_TOUCHABLE;

    mMountItem = create(mContent);

    assertThat(LayoutOutput.isDuplicateParentState(getLayoutOutput(mMountItem).getFlags()))
        .isTrue();
    assertThat(LayoutOutput.isTouchableDisabled(getLayoutOutput(mMountItem).getFlags())).isTrue();

    mFlags = 0;
    mMountItem = create(mContent);

    assertThat(LayoutOutput.isDuplicateParentState(getLayoutOutput(mMountItem).getFlags()))
        .isFalse();
    assertThat(LayoutOutput.isTouchableDisabled(getLayoutOutput(mMountItem).getFlags())).isFalse();
  }

  @Test
  public void testViewFlags() {
    View view = new View(getApplicationContext());
    view.setClickable(true);
    view.setEnabled(true);
    view.setLongClickable(true);
    view.setFocusable(false);
    view.setSelected(false);

    mMountItem = create(view);

    assertThat(isViewClickable(getMountData(mMountItem).getDefaultAttributeValuesFlags())).isTrue();
    assertThat(isViewEnabled(getMountData(mMountItem).getDefaultAttributeValuesFlags())).isTrue();
    assertThat(isViewLongClickable(getMountData(mMountItem).getDefaultAttributeValuesFlags()))
        .isTrue();
    assertThat(isViewFocusable(getMountData(mMountItem).getDefaultAttributeValuesFlags()))
        .isFalse();
    assertThat(isViewSelected(getMountData(mMountItem).getDefaultAttributeValuesFlags())).isFalse();

    view.setClickable(false);
    view.setEnabled(false);
    view.setLongClickable(false);
    view.setFocusable(true);
    view.setSelected(true);

    mMountItem = create(view);

    assertThat(isViewClickable(getMountData(mMountItem).getDefaultAttributeValuesFlags()))
        .isFalse();
    assertThat(isViewEnabled(getMountData(mMountItem).getDefaultAttributeValuesFlags())).isFalse();
    assertThat(isViewLongClickable(getMountData(mMountItem).getDefaultAttributeValuesFlags()))
        .isFalse();
    assertThat(isViewFocusable(getMountData(mMountItem).getDefaultAttributeValuesFlags())).isTrue();
    assertThat(isViewSelected(getMountData(mMountItem).getDefaultAttributeValuesFlags())).isTrue();
  }

  @Test
  public void testIsAccessibleWithNullComponent() {
    final MountItem mountItem = create(mContent);

    assertThat(getLayoutOutput(mountItem).isAccessible()).isFalse();
  }

  @Test
  public void testIsAccessibleWithAccessibleComponent() {
    final MountItem mountItem =
        MountItemTestHelper.create(
            TestDrawableComponent.create(mContext, true, true, true /* implementsAccessibility */)
                .build(),
            null,
            mComponentHost,
            mContent,
            mNodeInfo,
            null,
            null,
            mFlags,
            0,
            0,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            ORIENTATION_PORTRAIT,
            null);

    assertThat(getLayoutOutput(mountItem).isAccessible()).isTrue();
  }

  @Test
  public void testIsAccessibleWithDisabledAccessibleComponent() {
    final MountItem mountItem =
        MountItemTestHelper.create(
            TestDrawableComponent.create(mContext, true, true, true /* implementsAccessibility */)
                .build(),
            null,
            mComponentHost,
            mContent,
            mNodeInfo,
            null,
            null,
            mFlags,
            0,
            0,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_NO,
            ORIENTATION_PORTRAIT,
            null);

    assertThat(getLayoutOutput(mountItem).isAccessible()).isFalse();
  }

  @Test
  public void testIsAccessibleWithAccessibilityEventHandler() {
    final MountItem mountItem =
        MountItemTestHelper.create(
            TestDrawableComponent.create(mContext, true, true, true /* implementsAccessibility */)
                .build(),
            null,
            mComponentHost,
            mContent,
            mNodeInfo,
            null,
            null,
            mFlags,
            0,
            0,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            ORIENTATION_PORTRAIT,
            null);

    assertThat(getLayoutOutput(mountItem).isAccessible()).isTrue();
  }

  @Test
  public void testIsAccessibleWithNonAccessibleComponent() {
    assertThat(getLayoutOutput(mMountItem).isAccessible()).isFalse();
  }

  @Test
  public void testUpdateDoesntChangeFlags() {
    LithoRenderUnit unit =
        LithoRenderUnit.create(
            0,
            mComponent,
            mContext,
            mNodeInfo,
            null,
            new Rect(0, 0, 0, 0),
            0,
            0,
            LayoutOutput.STATE_UNKNOWN,
            null);
    RenderTreeNode node = LithoRenderUnit.create(unit, new Rect(0, 0, 0, 0), null);

    View view = new View(getApplicationContext());

    final MountItem mountItem = new MountItem(node, mComponentHost, view);
    mountItem.setMountData(new LithoMountData(view));

    assertThat(isViewClickable(getMountData(mountItem).getDefaultAttributeValuesFlags())).isFalse();

    view.setClickable(true);

    mountItem.update(node);
    assertThat(isViewClickable(getMountData(mountItem).getDefaultAttributeValuesFlags())).isFalse();
  }
}
