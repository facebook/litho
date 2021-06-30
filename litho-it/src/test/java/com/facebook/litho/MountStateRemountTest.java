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

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.testing.TestDrawableComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.EditText;
import com.facebook.litho.widget.Text;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.MountItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateRemountTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldDisableDrawableOutputs(true);
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testMountItemsHaveMountData() {
    final TestComponent component1 = create(mContext).build();
    final TestComponent component2 = create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            mContext, Column.create(mContext).child(component1).child(component2).build());

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();

    final MountDelegateTarget mountDelegateTarget = lithoView.getMountDelegateTarget();

    for (int i = 0; i < mountDelegateTarget.getMountItemCount(); i++) {
      MountItem item = mountDelegateTarget.getMountItemAt(i);
      assertThat(item.getMountData()).isOfAnyClassIn(LithoMountData.class);
    }
  }

  @Test
  public void testRemountSameLayoutState() {
    final TestComponent component1 = create(mContext).build();
    final TestComponent component2 = create(mContext).build();
    final TestComponent component3 = create(mContext).build();
    final TestComponent component4 = create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            mContext, Column.create(mContext).child(component1).child(component2).build());

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();

    mountComponent(
        mContext, lithoView, Column.create(mContext).child(component3).child(component4).build());

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();
    assertThat(component3.isMounted()).isFalse();
    assertThat(component4.isMounted()).isFalse();

    final MountDelegateTarget mountDelegateTarget = lithoView.getMountDelegateTarget();

    final List<Component> components = new ArrayList<>();
    for (int i = 0; i < mountDelegateTarget.getMountItemCount(); i++) {
      components.add(getLayoutOutput(mountDelegateTarget.getMountItemAt(i)).getComponent());
    }

    assertThat(containsRef(components, component1)).isFalse();
    assertThat(containsRef(components, component2)).isFalse();
    assertThat(containsRef(components, component3)).isTrue();
    assertThat(containsRef(components, component4)).isTrue();
  }

  /**
   * There was a crash when mounting a drawing in place of a view. This test is here to make sure
   * this does not regress. To reproduce this crash the pools needed to be in a specific state as
   * view layout outputs and mount items were being re-used for drawables.
   */
  @Test
  public void testRemountDifferentMountType() throws IllegalAccessException, NoSuchFieldException {
    final LithoView lithoView =
        ComponentTestHelper.mountComponent(mContext, TestViewComponent.create(mContext).build());

    ComponentTestHelper.mountComponent(
        mContext, lithoView, TestDrawableComponent.create(mContext).build());
  }

  @Test
  public void testRemountNewLayoutState() {
    final TestComponent component1 = create(mContext).color(Color.RED).build();
    final TestComponent component2 = create(mContext).color(Color.BLUE).build();
    final TestComponent component3 = create(mContext).color(Color.GREEN).build();
    final TestComponent component4 = create(mContext).color(Color.YELLOW).build();

    final LithoView lithoView =
        mountComponent(
            mContext, Column.create(mContext).child(component1).child(component2).build());

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();

    mountComponent(
        mContext, lithoView, Column.create(mContext).child(component3).child(component4).build());

    assertThat(component1.isMounted()).isFalse();
    assertThat(component2.isMounted()).isFalse();
    assertThat(component3.isMounted()).isTrue();
    assertThat(component4.isMounted()).isTrue();
  }

  @Test
  public void testRemountAfterSettingNewRootTwice() {
    final TestComponent component1 =
        create(mContext).color(Color.RED).returnSelfInMakeShallowCopy().build();
    final TestComponent component2 =
        create(mContext).returnSelfInMakeShallowCopy().color(Color.BLUE).build();

    final LithoView lithoView = new LithoView(mContext);
    final ComponentTree componentTree =
        ComponentTree.create(mContext, Column.create(mContext).child(component1).build()).build();

    mountComponent(
        lithoView, componentTree, makeMeasureSpec(100, EXACTLY), makeMeasureSpec(100, EXACTLY));

    assertThat(component1.isMounted()).isTrue();

    componentTree.setRootAndSizeSpec(
        Column.create(mContext).child(component2).build(),
        makeMeasureSpec(50, EXACTLY),
        makeMeasureSpec(50, EXACTLY));

    componentTree.setSizeSpec(makeMeasureSpec(100, EXACTLY), makeMeasureSpec(100, EXACTLY));

    assertThat(component2.isMounted()).isTrue();
  }

  @Test
  public void testRemountPartiallyDifferentLayoutState() {
    final TestComponent component1 = create(mContext).build();
    final TestComponent component2 = create(mContext).build();
    final TestComponent component3 = create(mContext).build();
    final TestComponent component4 = create(mContext).build();

    final LithoView lithoView =
        mountComponent(
            mContext, Column.create(mContext).child(component1).child(component2).build());

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();

    mountComponent(
        mContext,
        lithoView,
        Column.create(mContext)
            .child(component3)
            .child(Column.create(mContext).wrapInView().child(component4))
            .build());

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isFalse();
    assertThat(component3.isMounted()).isFalse();
    assertThat(component4.isMounted()).isTrue();
  }

  @Test
  public void testRemountOnNoLayoutChanges() {
    final Component oldComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(
                EditText.create(mContext)
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.CYAN)
                    .text("Hello World")
                    .viewTag("Alpha")
                    .contentDescription("some description"))
            .build();

    final LithoView lithoView = new LithoView(mContext);
    final ComponentTree componentTree =
        ComponentTree.create(mContext, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build();

    mountComponent(
        lithoView, componentTree, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final ViewGroup oldHost = (ViewGroup) lithoView.getChildAt(0);
    final View oldView = oldHost.getChildAt(0);

    final Object oldTag = oldView.getTag();
    final String oldContentDescription = oldView.getContentDescription().toString();
    final Drawable oldBackground = oldView.getBackground();

    final Component newComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(
                EditText.create(mContext)
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.CYAN)
                    .text("Hello World")
                    .viewTag("Alpha")
                    .contentDescription("some description"))
            .build();

    componentTree.setRootAndSizeSpec(
        newComponent, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    componentTree.setSizeSpec(makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final ViewGroup newHost = (ViewGroup) lithoView.getChildAt(0);
    View newView = newHost.getChildAt(0);

    assertThat(newView).isSameAs(oldView);

    final Object newTag = newView.getTag();
    final String newContentDescription = newView.getContentDescription().toString();
    final Drawable newBackground = newView.getBackground();

    // Check that props were not set again
    assertThat(newTag).isSameAs(oldTag);
    assertThat(newContentDescription).isSameAs(oldContentDescription);
    assertThat(oldBackground).isSameAs(newBackground);
  }

  @Test
  public void testRemountOnNodeInfoLayoutChanges() {
    final Component oldComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(Text.create(mContext).textSizeSp(12).text("label:"))
            .child(
                EditText.create(mContext)
                    .text("Hello World")
                    .textSizeSp(12)
                    .viewTag("Alpha")
                    .enabled(true))
            .build();

    final LithoView lithoView = new LithoView(mContext);
    final ComponentTree componentTree =
        ComponentTree.create(mContext, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build();

    mountComponent(
        lithoView, componentTree, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final ViewGroup oldHost = (ViewGroup) lithoView.getChildAt(0);
    final View oldView = oldHost.getChildAt(0);

    final Object oldTag = oldView.getTag();
    final boolean oldIsEnabled = oldView.isEnabled();

    final Component newComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(Text.create(mContext).textSizeSp(12).text("label:"))
            .child(
                EditText.create(mContext)
                    .text("Hello World")
                    .textSizeSp(12)
                    .viewTag("Beta")
                    .enabled(false))
            .build();

    componentTree.setRootAndSizeSpec(
        newComponent, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    componentTree.setSizeSpec(makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final ViewGroup newHost = (ViewGroup) lithoView.getChildAt(0);
    final View newView = newHost.getChildAt(0);

    assertThat(newView).isSameAs(oldView);

    final Object newTag = newView.getTag();
    final boolean newIsEnabled = newView.isEnabled();

    assertThat(newTag).isNotEqualTo(oldTag);
    assertThat(newIsEnabled).isNotEqualTo(oldIsEnabled);
  }

  @Test
  public void testRemountOnViewNodeInfoLayoutChanges() {
    final Component oldComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(Text.create(mContext).textSizeSp(12).text("label:"))
            .child(
                EditText.create(mContext)
                    .text("Hello World")
                    .textSizeSp(12)
                    .backgroundColor(Color.RED))
            .build();

    final LithoView lithoView = new LithoView(mContext);
    final ComponentTree componentTree =
        ComponentTree.create(mContext, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build();

    mountComponent(
        lithoView, componentTree, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final ViewGroup oldHost = (ViewGroup) lithoView.getChildAt(0);
    final View oldView = oldHost.getChildAt(0);

    final ComparableDrawable oldDrawable = (ComparableDrawable) oldView.getBackground();

    final Component newComponent =
        Column.create(mContext)
            .backgroundColor(Color.WHITE)
            .child(Text.create(mContext).textSizeSp(12).text("label:"))
            .child(
                EditText.create(mContext)
                    .text("Hello World")
                    .textSizeSp(12)
                    .backgroundColor(Color.CYAN))
            .build();

    componentTree.setRootAndSizeSpec(
        newComponent, makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    componentTree.setSizeSpec(makeMeasureSpec(400, EXACTLY), makeMeasureSpec(400, EXACTLY));

    final ViewGroup newHost = (ViewGroup) lithoView.getChildAt(0);
    final View newView = newHost.getChildAt(0);

    assertThat(newView).isSameAs(oldView);

    final ComparableDrawable newDrawable = (ComparableDrawable) newView.getBackground();

    assertThat(oldDrawable.isEquivalentTo(newDrawable)).isFalse();
  }

  private boolean containsRef(List<?> list, Object object) {
    for (Object o : list) {
      if (o == object) {
        return true;
      }
    }
    return false;
  }

  private static void clearPool(String name) {
    final RecyclePool<?> pool = Whitebox.getInternalState(ComponentsPools.class, name);

    while (pool.acquire() != null) {
      // Run.
    }
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldDisableDrawableOutputs();
  }
}
