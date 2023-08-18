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

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.InformsMountCallback;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.OnItemCallbacks;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.testing.LayoutResultWrappingNode;
import com.facebook.rendercore.testing.RenderCoreTestRule;
import com.facebook.rendercore.testing.SimpleLayoutResult;
import com.facebook.rendercore.testing.TestRenderCoreExtension;
import com.facebook.rendercore.testing.ViewAssertions;
import com.facebook.rendercore.testing.ViewWrapperUnit;
import com.facebook.rendercore.testing.match.ViewMatchNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MountDelegateTest {

  public final @Rule RenderCoreTestRule mRenderCoreTestRule = new RenderCoreTestRule();

  @Test
  public void testMountDelegateAPIs() {
    final Context c = mRenderCoreTestRule.getContext();
    final LayoutResult root =
        SimpleLayoutResult.create()
            .width(200)
            .height(300)
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 2))
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 3))
                    .y(100)
                    .width(100)
                    .height(100))
            .child(
                SimpleLayoutResult.create()
                    .renderUnit(new ViewWrapperUnit(new TextView(c), 4))
                    .y(200)
                    .width(100)
                    .height(100))
            .build();

    final UnitIdBasedMountExtension mountExtension = new UnitIdBasedMountExtension(2, 3);
    final RenderCoreExtension extension = new TestRenderCoreExtension(mountExtension);
    mRenderCoreTestRule
        .useExtensions(new RenderCoreExtension[] {extension})
        .useRootNode(new LayoutResultWrappingNode(root))
        .render();

    ViewAssertions.assertThat((View) mRenderCoreTestRule.getRootHost())
        .matches(
            ViewMatchNode.forType(Host.class)
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(0, 0, 100, 100)
                        .absoluteBoundsForRootType(0, 0, 100, 100, RootHost.class))
                .child(
                    ViewMatchNode.forType(TextView.class)
                        .bounds(0, 100, 100, 100)
                        .absoluteBoundsForRootType(0, 100, 100, 100, RootHost.class)));

    assertThat(mountExtension.beforeMountItem).isEqualTo(3);
    assertThat(mountExtension.onMountItem).isEqualTo(2 + 1 /* for the root*/);
    assertThat(mountExtension.onBindItem).isEqualTo(2 + 1 /* for the root*/);

    mRenderCoreTestRule
        .useRootNode(new LayoutResultWrappingNode(SimpleLayoutResult.create().build()))
        .render();

    // No new calls to the 'before' APIs
    assertThat(mountExtension.beforeMountItem).isEqualTo(3);
    assertThat(mountExtension.onMountItem).isEqualTo(2 + 1 /* for the root*/);
    assertThat(mountExtension.onBindItem).isEqualTo(2 + 1 /* for the root*/);

    assertThat(mountExtension.onUnmountItem).isEqualTo(2);
    assertThat(mountExtension.onUnbindItem).isEqualTo(2);
  }

  static class UnitIdBasedMountExtension extends MountExtension<Object, Map<Long, Boolean>>
      implements OnItemCallbacks<Map<Long, Boolean>>, InformsMountCallback {

    private final Map<Long, Boolean> mMap;

    int beforeMountItem;
    int onUnmountItem;
    int onMountItem;
    int onBindItem;
    int onUnbindItem;

    UnitIdBasedMountExtension(long... ids) {
      mMap = new HashMap<>(ids.length);
      for (long id : ids) {
        mMap.put(id, true);
      }
    }

    @Override
    public boolean canPreventMount() {
      return true;
    }

    @Override
    protected Map<Long, Boolean> createState() {
      return mMap;
    }

    @Override
    public void beforeMountItem(
        final ExtensionState<Map<Long, Boolean>> state,
        final RenderTreeNode node,
        final int index) {
      beforeMountItem++;
      long id = node.getRenderUnit().getId();
      if (state.getState().containsKey(id) && state.getState().get(id)) {
        state.acquireMountReference(id, false);
      }
    }

    @Override
    public void onMountItem(
        ExtensionState<Map<Long, Boolean>> extensionState,
        RenderUnit<?> renderUnit,
        Object content,
        @Nullable Object layoutData) {
      onMountItem++;
    }

    @Override
    public void onUnmountItem(
        ExtensionState<Map<Long, Boolean>> extensionState,
        RenderUnit<?> renderUnit,
        Object content,
        @Nullable Object layoutData) {
      onUnmountItem++;
    }

    @Override
    public void onBindItem(
        ExtensionState<Map<Long, Boolean>> extensionState,
        RenderUnit<?> renderUnit,
        Object content,
        @Nullable Object layoutData) {
      onBindItem++;
    }

    @Override
    public void onUnbindItem(
        ExtensionState<Map<Long, Boolean>> extensionState,
        RenderUnit<?> renderUnit,
        Object content,
        @Nullable Object layoutData) {
      onUnbindItem++;
    }

    @Override
    public boolean shouldUpdateItem(
        ExtensionState<Map<Long, Boolean>> extensionState,
        RenderUnit<?> previousRenderUnit,
        @Nullable Object previousLayoutData,
        RenderUnit<?> nextRenderUnit,
        @Nullable Object nextLayoutData) {
      return false;
    }

    @Override
    public void onBoundsAppliedToItem(
        ExtensionState<Map<Long, Boolean>> extensionState,
        RenderUnit<?> renderUnit,
        Object content,
        @Nullable Object layoutData) {}
  }
}
