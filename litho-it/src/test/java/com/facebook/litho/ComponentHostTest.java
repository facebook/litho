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
import static android.graphics.Color.BLACK;
import static android.view.MotionEvent.obtain;
import static android.view.View.GONE;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.INVISIBLE;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.View.VISIBLE;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.collection.SparseArrayCompat;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TextInput;
import com.facebook.rendercore.MountItem;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/** Tests {@link ComponentHost} */
@RunWith(LithoTestRunner.class)
public class ComponentHostTest {

  private Component mViewGroupHost;

  private TestableComponentHost mHost;
  private Component mDrawableComponent;
  private Component mViewComponent;
  private static final String sDrawableComponentKey = "drawable_key";
  private static final String sViewComponentKey = "view_key";
  private ComponentContext mContext;

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void setup() throws Exception {
    mContext = mLithoViewRule.getContext();
    mViewComponent = TestViewComponent.create(mContext).key(sViewComponentKey).build();
    mDrawableComponent = SimpleMountSpecTester.create(mContext).key(sDrawableComponentKey).build();

    mHost = new TestableComponentHost(mContext);

    mViewGroupHost = HostComponent.create();
  }

  @Test
  public void testInvalidations() {
    assertThat(mHost.getInvalidationCount()).isEqualTo(0);
    assertThat(mHost.getInvalidationRect()).isNull();

    Drawable d1 = new ColorDrawable();
    d1.setBounds(0, 0, 1, 1);

    MountItem mountItem1 = mount(0, d1);
    assertThat(mHost.getInvalidationCount()).isEqualTo(1);
    assertThat(mHost.getInvalidationRect()).isEqualTo(d1.getBounds());

    Drawable d2 = new ColorDrawable();
    d2.setBounds(0, 0, 2, 2);

    MountItem mountItem2 = mount(1, d2);
    assertThat(mHost.getInvalidationCount()).isEqualTo(2);
    assertThat(mHost.getInvalidationRect()).isEqualTo(d2.getBounds());

    View v1 = new View(mContext.getAndroidContext());
    Rect v1Bounds = new Rect(0, 0, 10, 10);
    v1.measure(
        makeMeasureSpec(v1Bounds.width(), EXACTLY), makeMeasureSpec(v1Bounds.height(), EXACTLY));
    v1.layout(v1Bounds.left, v1Bounds.top, v1Bounds.right, v1Bounds.bottom);

    MountItem mountItem3 = mount(2, v1);
    assertThat(mHost.getInvalidationCount()).isEqualTo(3);
    assertThat(mHost.getInvalidationRect()).isEqualTo(v1Bounds);

    unmount(0, mountItem1);
    assertThat(mHost.getInvalidationCount()).isEqualTo(4);
    assertThat(mHost.getInvalidationRect()).isEqualTo(d1.getBounds());

    unmount(1, mountItem2);
    assertThat(mHost.getInvalidationCount()).isEqualTo(5);
    assertThat(mHost.getInvalidationRect()).isEqualTo(d2.getBounds());

    unmount(2, mountItem3);
    assertThat(mHost.getInvalidationCount()).isEqualTo(6);
    assertThat(mHost.getInvalidationRect()).isEqualTo(v1Bounds);
  }

  @Test
  public void testCallbacks() {
    Drawable d = new ColorDrawable();
    assertThat(d.getCallback()).isNull();

    MountItem mountItem = mount(0, d);
    assertThat(d.getCallback()).isEqualTo(mHost);

    unmount(0, mountItem);
    assertThat(d.getCallback()).isNull();
  }

  @Test
  public void testGetMountItemCount() {
    assertThat(mHost.getMountItemCount()).isEqualTo(0);

    MountItem mountItem1 = mount(0, new ColorDrawable());
    assertThat(mHost.getMountItemCount()).isEqualTo(1);

    mount(1, new ColorDrawable());
    assertThat(mHost.getMountItemCount()).isEqualTo(2);

    MountItem mountItem3 = mount(2, new View(mContext.getAndroidContext()));
    assertThat(mHost.getMountItemCount()).isEqualTo(3);

    unmount(0, mountItem1);
    assertThat(mHost.getMountItemCount()).isEqualTo(2);

    MountItem mountItem4 = mount(1, new ColorDrawable());
    assertThat(mHost.getMountItemCount()).isEqualTo(2);

    unmount(2, mountItem3);
    assertThat(mHost.getMountItemCount()).isEqualTo(1);

    unmount(1, mountItem4);
    assertThat(mHost.getMountItemCount()).isEqualTo(0);
  }

  @Test
  public void testGetMountItemAt() {
    assertThat(mHost.getMountItemAt(0)).isNull();
    assertThat(mHost.getMountItemAt(1)).isNull();
    assertThat(mHost.getMountItemAt(2)).isNull();

    MountItem mountItem1 = mount(0, new ColorDrawable());
    MountItem mountItem2 = mount(1, new View(mContext.getAndroidContext()));
    MountItem mountItem3 = mount(5, new ColorDrawable());

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem2);
    assertThat(mHost.getMountItemAt(2)).isEqualTo(mountItem3);

    unmount(1, mountItem2);

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem3);

    unmount(0, mountItem1);

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem3);
  }

  @Test
  public void testOnTouchWithTouchables() {
    assertThat(mHost.getMountItemAt(0)).isNull();
    assertThat(mHost.getMountItemAt(1)).isNull();
    assertThat(mHost.getMountItemAt(2)).isNull();

    // Touchables are traversed backwards as drawing order.
    // The n.4 is the first parsed, and returning false means the n.2 will be parsed too.
    TouchableDrawable touchableDrawableOnItem2 = spy(new TouchableDrawable());
    TouchableDrawable touchableDrawableOnItem4 = spy(new TouchableDrawable());
    when(touchableDrawableOnItem2.shouldHandleTouchEvent((MotionEvent) any())).thenReturn(true);
    when(touchableDrawableOnItem4.shouldHandleTouchEvent((MotionEvent) any())).thenReturn(false);

    MountItem mountItem1 = mount(0, new ColorDrawable());
    MountItem mountItem2 = mount(1, touchableDrawableOnItem2);
    MountItem mountItem3 = mount(2, new View(mContext.getAndroidContext()));
    MountItem mountItem4 = mount(5, touchableDrawableOnItem4);

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem2);
    assertThat(mHost.getMountItemAt(2)).isEqualTo(mountItem3);
    assertThat(mHost.getMountItemAt(3)).isEqualTo(mountItem4);

    mHost.onTouchEvent(mock(MotionEvent.class));

    verify(touchableDrawableOnItem4, times(1)).shouldHandleTouchEvent((MotionEvent) any());
    verify(touchableDrawableOnItem4, never()).onTouchEvent((MotionEvent) any(), (View) any());

    verify(touchableDrawableOnItem2, times(1)).shouldHandleTouchEvent((MotionEvent) any());
    verify(touchableDrawableOnItem2, times(1)).onTouchEvent((MotionEvent) any(), (View) any());
  }

  @Test
  public void testOnTouchWithDisableTouchables() {
    assertThat(mHost.getMountItemAt(0)).isNull();
    assertThat(mHost.getMountItemAt(1)).isNull();
    assertThat(mHost.getMountItemAt(2)).isNull();

    MountItem mountItem1 = mount(0, new ColorDrawable());
    MountItem mountItem2 =
        mount(1, new TouchableDrawable(), LayoutOutput.LAYOUT_FLAG_DISABLE_TOUCHABLE);
    MountItem mountItem3 = mount(2, new View(mContext.getAndroidContext()));
    MountItem mountItem4 = mount(4, spy(new TouchableDrawable()));
    MountItem mountItem5 =
        mount(5, new TouchableDrawable(), LayoutOutput.LAYOUT_FLAG_DISABLE_TOUCHABLE);
    MountItem mountItem6 = mount(7, new View(mContext.getAndroidContext()));
    MountItem mountItem7 =
        mount(8, new TouchableDrawable(), LayoutOutput.LAYOUT_FLAG_DISABLE_TOUCHABLE);

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem2);
    assertThat(mHost.getMountItemAt(2)).isEqualTo(mountItem3);
    assertThat(mHost.getMountItemAt(3)).isEqualTo(mountItem4);
    assertThat(mHost.getMountItemAt(4)).isEqualTo(mountItem5);
    assertThat(mHost.getMountItemAt(5)).isEqualTo(mountItem6);
    assertThat(mHost.getMountItemAt(6)).isEqualTo(mountItem7);

    mHost.onTouchEvent(mock(MotionEvent.class));

    TouchableDrawable touchableDrawable = (TouchableDrawable) mountItem4.getContent();
    verify(touchableDrawable, times(1)).shouldHandleTouchEvent((MotionEvent) any());
    verify(touchableDrawable, times(1)).onTouchEvent((MotionEvent) any(), (View) any());
  }

  @Test
  public void testMoveItem() {
    MountItem mountItem1 = mount(1, new ColorDrawable());
    MountItem mountItem2 = mount(2, new View(mContext.getAndroidContext()));

    assertThat(mHost.getMountItemCount()).isEqualTo(2);

    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem2);

    mHost.moveItem(mountItem2, 2, 0);

    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem2);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem1);

    mHost.moveItem(mountItem2, 0, 1);

    assertThat(mHost.getMountItemCount()).isEqualTo(1);
    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem2);

    mHost.moveItem(mountItem2, 1, 0);

    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(mHost.getMountItemAt(1)).isEqualTo(mountItem2);
  }

  @Test
  public void testMoveItemWithoutTouchables() throws Exception {
    Drawable d1 = new ColorDrawable(BLACK);
    MountItem mountItem1 = mount(1, d1);

    Drawable d2 = new ColorDrawable(BLACK);
    MountItem mountItem2 = mount(2, d2);

    assertThat(getDrawableItemsSize()).isEqualTo(2);
    assertThat(getDrawableMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(getDrawableMountItemAt(1)).isEqualTo(mountItem2);

    mHost.moveItem(mountItem2, 2, 0);

    // There are no Touchable Drawables so this call should return false and not crash.
    assertThat(mHost.onTouchEvent(obtain(0, 0, 0, 0, 0, 0))).isFalse();
  }

  @Test
  public void testDrawableStateChangedOnDrawables() {
    Drawable d1 = mock(ColorDrawable.class);
    when(d1.getBounds()).thenReturn(new Rect());
    when(d1.isStateful()).thenReturn(false);

    MountItem mountItem1 = mount(0, d1);
    verify(d1, never()).setState((int[]) any());

    unmount(0, mountItem1);

    Drawable d2 = mock(ColorDrawable.class);
    when(d2.getBounds()).thenReturn(new Rect());
    when(d2.isStateful()).thenReturn(true);

    mount(0, d2, LAYOUT_FLAG_DUPLICATE_PARENT_STATE);
    verify(d2, times(1)).setState(eq(mHost.getDrawableState()));

    mHost.setSelected(true);

    verify(d2, times(1)).setState(eq(mHost.getDrawableState()));
  }

  @Test
  public void testMoveTouchExpansionItem() {
    View view = mock(View.class);
    when(view.getContext()).thenReturn(getApplicationContext());

    MountItem mountItem = mountTouchExpansionItem(0, view);
    mHost.moveItem(mountItem, 0, 1);

    unmount(1, mountItem);
  }

  @Test
  public void testRecursiveTouchExpansionItemShouldNotAddTouchDelegate() {
    MountItem mountItem = mountTouchExpansionItem(0, mHost);

    assertThat(mHost.getTouchExpansionDelegate()).isNull();

    unmount(0, mountItem);
  }

  @Test
  public void testDuplicateParentStateOnViews() {
    View v1 = mock(View.class);
    mount(0, v1);

    View v2 = mock(View.class);
    mount(1, v2, LAYOUT_FLAG_DUPLICATE_PARENT_STATE);

    verify(v1, times(1)).setDuplicateParentStateEnabled(eq(false));
    verify(v2, times(1)).setDuplicateParentStateEnabled(eq(true));
  }

  @Test
  public void testJumpDrawablesToCurrentState() {
    mHost.jumpDrawablesToCurrentState();

    Drawable d1 = mock(ColorDrawable.class);
    when(d1.getBounds()).thenReturn(new Rect());
    mount(0, d1);

    Drawable d2 = mock(ColorDrawable.class);
    when(d2.getBounds()).thenReturn(new Rect());
    mount(1, d2);

    View v1 = mock(View.class);
    mount(2, v1);

    mHost.jumpDrawablesToCurrentState();

    verify(d1, times(1)).jumpToCurrentState();
    verify(d2, times(1)).jumpToCurrentState();
  }

  @Test
  public void testSetVisibility() {
    Drawable d1 = mock(ColorDrawable.class);
    when(d1.getBounds()).thenReturn(new Rect());
    mount(0, d1);

    Drawable d2 = mock(ColorDrawable.class);
    when(d2.getBounds()).thenReturn(new Rect());
    mount(1, d2);

    View v1 = mock(View.class);
    mount(2, v1);

    mHost.setVisibility(GONE);
    mHost.setVisibility(INVISIBLE);
    mHost.setVisibility(VISIBLE);

    verify(d1, times(2)).setVisible(eq(true), eq(false));
    verify(d1, times(2)).setVisible(eq(false), eq(false));

    verify(d2, times(2)).setVisible(eq(true), eq(false));
    verify(d2, times(2)).setVisible(eq(false), eq(false));

    verify(v1, never()).setVisibility(anyInt());
  }

  @Test
  public void testGetDrawables() {
    Drawable d1 = new ColorDrawable();
    MountItem mountItem1 = mount(0, d1);

    Drawable d2 = new ColorDrawable();
    mount(1, d2);

    MountItem mountItem3 = mount(2, new View(mContext.getAndroidContext()));

    List<Drawable> drawables = mHost.getDrawables();
    assertThat(drawables).hasSize(2);
    assertThat(drawables.get(0)).isEqualTo(d1);
    assertThat(drawables.get(1)).isEqualTo(d2);

    unmount(0, mountItem1);

    drawables = mHost.getDrawables();
    assertThat(drawables).hasSize(1);
    assertThat(drawables.get(0)).isEqualTo(d2);

    unmount(2, mountItem3);

    drawables = mHost.getDrawables();
    assertThat(drawables).hasSize(1);
    assertThat(drawables.get(0)).isEqualTo(d2);
  }

  @Test
  public void testViewTag() {
    final Component rootComponent =
        Column.create(mContext)
            .child(SimpleMountSpecTester.create(mContext).viewTag("test_tag"))
            .build();
    final LithoView lithoView = mountComponent(mContext, rootComponent, false, false);
    assertThat((View) lithoView.findViewWithTag("test_tag")).isNotNull();
  }

  @Test
  public void testViewTags() {
    assertThat(mHost.getTag(1)).isNull();
    assertThat(mHost.getTag(2)).isNull();

    Object value1 = new Object();
    Object value2 = new Object();

    SparseArray<Object> viewTags = new SparseArray<>();
    viewTags.put(1, value1);
    viewTags.put(2, value2);

    mHost.setViewTags(viewTags);

    assertThat(mHost.getTag(1)).isEqualTo(value1);
    assertThat(mHost.getTag(2)).isEqualTo(value2);

    mHost.setViewTags(null);

    assertThat(mHost.getTag(1)).isNull();
    assertThat(mHost.getTag(2)).isNull();
  }

  @Test
  public void testComponentClickListener() {
    assertThat(mHost.getComponentClickListener()).isNull();

    ComponentClickListener listener = new ComponentClickListener();
    mHost.setComponentClickListener(listener);

    assertThat(mHost.getComponentClickListener()).isEqualTo(listener);

    mHost.setComponentClickListener(null);
    assertThat(mHost.getComponentClickListener()).isNull();
  }

  @Test
  public void testComponentLongClickListener() {
    assertThat(mHost.getComponentLongClickListener()).isNull();

    ComponentLongClickListener listener = new ComponentLongClickListener();
    mHost.setComponentLongClickListener(listener);

    assertThat(mHost.getComponentLongClickListener()).isEqualTo(listener);

    mHost.setComponentLongClickListener(null);
    assertThat(mHost.getComponentLongClickListener()).isNull();
  }

  @Test
  public void testComponentFocusChangeListener() {
    assertThat(mHost.getComponentFocusChangeListener()).isNull();

    ComponentFocusChangeListener listener = new ComponentFocusChangeListener();
    mHost.setComponentFocusChangeListener(listener);

    assertThat(mHost.getComponentFocusChangeListener()).isEqualTo(listener);

    mHost.setComponentFocusChangeListener(null);
    assertThat(mHost.getComponentFocusChangeListener()).isNull();
  }

  @Test
  public void testComponentTouchListener() {
    assertThat(mHost.getComponentTouchListener()).isNull();

    ComponentTouchListener listener = new ComponentTouchListener();
    mHost.setComponentTouchListener(listener);

    assertThat(mHost.getComponentTouchListener()).isEqualTo(listener);

    mHost.setComponentTouchListener(null);
    assertThat(mHost.getComponentTouchListener()).isNull();
  }

  @Test
  public void testGetContentDescriptions() {
    CharSequence hostContentDescription = "hostContentDescription";
    mHost.setContentDescription(hostContentDescription);

    CharSequence drawableContentDescription = "drawableContentDescription";
    MountItem mountItem0 = mount(0, new ColorDrawable(), 0, drawableContentDescription);
    CharSequence viewContentDescription = "viewContentDescription";
    mount(1, mock(View.class), 0, viewContentDescription);

    assertThat(mHost.getContentDescriptions()).contains(hostContentDescription);
    assertThat(mHost.getContentDescriptions()).contains(drawableContentDescription);
    assertThat(mHost.getContentDescriptions()).doesNotContain(viewContentDescription);

    unmount(0, mountItem0);

    assertThat(mHost.getContentDescriptions()).contains(hostContentDescription);
    assertThat(mHost.getContentDescriptions()).doesNotContain(drawableContentDescription);
    assertThat(mHost.getContentDescriptions()).doesNotContain(viewContentDescription);
  }

  @Test
  public void testGetChildDrawingOrder() {
    View v1 = new View(mContext.getAndroidContext());
    mount(2, v1);

    View v2 = new View(mContext.getAndroidContext());
    MountItem mountItem2 = mount(0, v2);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);

    View v3 = new ComponentHost(mContext);
    MountItem mountItem3 = mount(1, v3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(2);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(0);

    mHost.unmount(1, mountItem3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);

    mount(1, v3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(2);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(0);

    mHost.unmount(0, mountItem2);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);

    mHost.moveItem(mountItem3, 1, 3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(0);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(1);
  }

  /**
   * {@link ViewGroup#getClipChildren()} method was only added in API 18, but plays important role
   * here, so will need to run this test for two SDK versions. And since {@link LithoTestRunner}
   * does not support multiple values for @Config.sdk, there are two separated test methods {@see
   * #testTemporaryChildClippingDisablingJB} {@see #testTemporaryChildClippingDisablingLollipop}
   */
  private void testTemporaryChildClippingDisabling() {
    ComponentHost componentHost = new ComponentHost(mContext);

    assertThat(componentHost.getClipChildren()).isTrue();

    // 1. Testing disable > restore
    componentHost.temporaryDisableChildClipping();

    assertThat(componentHost.getClipChildren()).isFalse();

    componentHost.restoreChildClipping();

    assertThat(componentHost.getClipChildren()).isTrue();

    // 2. Testing disable > set > restore
    componentHost.temporaryDisableChildClipping();
    componentHost.setClipChildren(true);

    assertThat(componentHost.getClipChildren()).isFalse();

    componentHost.restoreChildClipping();

    assertThat(componentHost.getClipChildren()).isTrue();

    // 3. Same as 1 (disable > restore), but starting with clipping tuned off initially
    componentHost.setClipChildren(false);
    componentHost.temporaryDisableChildClipping();

    assertThat(componentHost.getClipChildren()).isFalse();

    componentHost.restoreChildClipping();

    assertThat(componentHost.getClipChildren()).isFalse();

    // 4. Same as 2 (disable > set > restore), with reverted values
    componentHost.temporaryDisableChildClipping();
    componentHost.setClipChildren(true);

    assertThat(componentHost.getClipChildren()).isFalse();

    componentHost.restoreChildClipping();

    assertThat(componentHost.getClipChildren()).isTrue();
  }

  @Config(sdk = Build.VERSION_CODES.JELLY_BEAN)
  @Test
  public void testTemporaryChildClippingDisablingJB() {
    testTemporaryChildClippingDisabling();
  }

  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  @Test
  public void testTemporaryChildClippingDisablingLollipop() {
    testTemporaryChildClippingDisabling();
  }

  @Test
  public void testDisappearingItems() {
    View v1 = new View(mContext.getAndroidContext());
    mount(0, v1);

    Drawable d1 = new ColorDrawable(BLACK);
    MountItem mountItem1 = mount(1, d1);

    View v2 = new View(mContext.getAndroidContext());
    MountItem mountItem2 = mount(2, v2);

    Drawable d2 = new ColorDrawable(BLACK);
    mount(3, d2);

    assertThat(mHost.getMountItemCount()).isEqualTo(4);
    assertThat(mHost.getChildCount()).isEqualTo(2);
    assertThat(mHost.hasDisappearingItems()).isFalse();

    mHost.startUnmountDisappearingItem(1, mountItem1);

    assertThat(mHost.getMountItemCount()).isEqualTo(3);
    assertThat(mHost.getChildCount()).isEqualTo(2);
    assertThat(mHost.hasDisappearingItems()).isTrue();

    mHost.finaliseDisappearingItem(mountItem1);

    assertThat(mHost.getMountItemCount()).isEqualTo(3);
    assertThat(mHost.getChildCount()).isEqualTo(2);
    assertThat(mHost.hasDisappearingItems()).isFalse();

    mHost.startUnmountDisappearingItem(2, mountItem2);

    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getChildCount()).isEqualTo(2);
    assertThat(mHost.hasDisappearingItems()).isTrue();

    mHost.finaliseDisappearingItem(mountItem2);

    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getChildCount()).isEqualTo(1);
    assertThat(mHost.hasDisappearingItems()).isFalse();
  }

  @Ignore("t19681984")
  @Test
  public void testDisappearingItemDrawingOrder() {
    View v1 = new View(mContext.getAndroidContext());
    mount(5, v1);

    View v2 = new View(mContext.getAndroidContext());
    mount(2, v2);

    View v3 = new View(mContext.getAndroidContext());
    MountItem mountItem3 = mount(4, v3);

    View v4 = new View(mContext.getAndroidContext());
    MountItem mountItem4 = mount(0, v4);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(3);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(2);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 3)).isEqualTo(0);

    assertThat(mHost.getMountItemCount()).isEqualTo(4);
    assertThat(mHost.getChildCount()).isEqualTo(4);

    // mountItem3 started disappearing
    mHost.startUnmountDisappearingItem(4, mountItem3);

    assertThat(mHost.getMountItemCount()).isEqualTo(3);
    assertThat(mHost.getChildCount()).isEqualTo(4);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(3);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(0);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 3)).isEqualTo(2);

    // mountItem4 started disappearing
    mHost.startUnmountDisappearingItem(0, mountItem4);

    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getChildCount()).isEqualTo(4);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(3);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 3)).isEqualTo(2);

    // mountItem4 finished disappearing
    mHost.finaliseDisappearingItem(mountItem4);
    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getChildCount()).isEqualTo(3);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 2)).isEqualTo(2);

    // mountItem3 finished disappearing
    mHost.finaliseDisappearingItem(mountItem3);
    assertThat(mHost.getMountItemCount()).isEqualTo(2);
    assertThat(mHost.getChildCount()).isEqualTo(2);

    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 0)).isEqualTo(1);
    assertThat(mHost.getChildDrawingOrder(mHost.getChildCount(), 1)).isEqualTo(0);
  }

  @Test
  public void testDrawableItemsSize() throws Exception {

    assertThat(getDrawableItemsSize()).isEqualTo(0);

    assertThat(getDrawableItemsSize()).isEqualTo(0);

    Drawable d1 = new ColorDrawable(BLACK);
    MountItem m1 = mount(0, d1);
    assertThat(getDrawableItemsSize()).isEqualTo(1);

    Drawable d2 = new ColorDrawable(BLACK);
    mount(1, d2);
    assertThat(getDrawableItemsSize()).isEqualTo(2);

    unmount(0, m1);
    assertThat(getDrawableItemsSize()).isEqualTo(1);

    Drawable d3 = new ColorDrawable(BLACK);
    MountItem m3 = mount(1, d3);
    assertThat(getDrawableItemsSize()).isEqualTo(1);

    unmount(1, m3);
    assertThat(getDrawableItemsSize()).isEqualTo(0);
  }

  @Test
  public void testGetDrawableMountItem() throws Exception {
    Drawable d1 = new ColorDrawable(BLACK);
    MountItem mountItem1 = mount(0, d1);

    Drawable d2 = new ColorDrawable(BLACK);
    MountItem mountItem2 = mount(1, d2);

    Drawable d3 = new ColorDrawable(BLACK);
    MountItem mountItem3 = mount(5, d3);

    assertThat(getDrawableMountItemAt(0)).isEqualTo(mountItem1);
    assertThat(getDrawableMountItemAt(1)).isEqualTo(mountItem2);
    assertThat(getDrawableMountItemAt(2)).isEqualTo(mountItem3);
  }

  @Test
  public void testGetLinkedDrawableForAnimation() {
    Drawable d1 = new ColorDrawable();
    MountItem mountItem1 = mount(0, d1, LAYOUT_FLAG_MATCH_HOST_BOUNDS);

    Drawable d2 = new ColorDrawable();
    MountItem mountItem2 = mount(1, d2);

    Drawable d3 = new ColorDrawable();
    MountItem mountItem3 = mount(2, d3, LAYOUT_FLAG_MATCH_HOST_BOUNDS);

    List<Drawable> drawables = mHost.getLinkedDrawablesForAnimation();
    assertThat(drawables).hasSize(2);
    assertThat(drawables).contains(d1, d3);

    unmount(0, mountItem1);

    drawables = mHost.getLinkedDrawablesForAnimation();
    assertThat(drawables).hasSize(1);
    assertThat(drawables).contains(d3);

    unmount(1, mountItem2);

    drawables = mHost.getDrawables();
    assertThat(drawables).hasSize(1);
    assertThat(drawables).contains(d3);
  }

  @Test
  public void whenTouchExpansionSetOnComponent_shouldHaveTouchDelegateOnParentOfHostView() {
    final Component component =
        Column.create(mContext)
            .paddingPx(YogaEdge.ALL, 15)
            .widthPx(100)
            .heightPx(100)
            .child(
                Text.create(mContext)
                    .text("hello-world")
                    .touchExpansionPx(YogaEdge.ALL, 5)
                    .clickHandler(NoOpEventHandler.getNoOpEventHandler()))
            .child(
                Row.create(mContext)
                    .wrapInView()
                    .child(
                        TextInput.create(mContext)
                            .touchExpansionPx(YogaEdge.ALL, 5)
                            .clickHandler(NoOpEventHandler.getNoOpEventHandler())))
            .build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    TouchExpansionDelegate delegate = mLithoViewRule.getLithoView().getTouchExpansionDelegate();
    assertThat(delegate)
        .describedAs("Should be not null for the host view of the Text")
        .isNotNull();

    final View child1 = mLithoViewRule.getLithoView().getChildAt(0);
    assertThat(child1).isInstanceOf(ComponentHost.class);
    assertThat(((ComponentHost) child1).getTouchExpansionDelegate())
        .describedAs("should be null for the Text")
        .isNull();

    final View child2 = mLithoViewRule.getLithoView().getChildAt(1);
    assertThat(child2).isInstanceOf(ComponentHost.class);
    assertThat(((ComponentHost) child2).getTouchExpansionDelegate())
        .describedAs("Should be not null for the host view of the Text Input")
        .isNotNull();
  }

  private int getDrawableItemsSize() throws Exception {
    SparseArrayCompat drawableItems = Whitebox.getInternalState(mHost, "mDrawableMountItems");
    return Whitebox.invokeMethod(drawableItems, "size");
  }

  private MountItem getDrawableMountItemAt(int index) throws Exception {
    SparseArrayCompat drawableItems = Whitebox.getInternalState(mHost, "mDrawableMountItems");
    return Whitebox.invokeMethod(drawableItems, "valueAt", index);
  }

  private MountItem mount(int index, Object content) {
    return mount(index, content, 0);
  }

  private MountItem mount(int index, Object content, int flags) {
    return mount(index, content, flags, null);
  }

  private MountItem mount(int index, Object content, int flags, CharSequence contentDescription) {
    NodeInfo nodeInfo = new DefaultNodeInfo();
    nodeInfo.setContentDescription(contentDescription);

    MountItem mountItem =
        MountItemTestHelper.create(
            content instanceof Drawable ? mDrawableComponent : mViewComponent,
            content instanceof Drawable ? sDrawableComponentKey : sViewComponentKey,
            null,
            content,
            nodeInfo,
            null,
            null,
            0,
            0,
            flags,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            ORIENTATION_PORTRAIT,
            null);

    mHost.mount(
        index,
        mountItem,
        content instanceof Drawable ? ((Drawable) content).getBounds() : new Rect());
    return mountItem;
  }

  private MountItem mountTouchExpansionItem(int index, Object content) {
    final ViewNodeInfo viewNodeInfo = new ViewNodeInfo();
    viewNodeInfo.setLayoutDirection(YogaDirection.LTR);

    LithoLayoutResult result = mock(LithoLayoutResult.class);
    InternalNode node = mock(InternalNode.class);
    when(result.getInternalNode()).thenReturn(node);
    when(node.hasTouchExpansion()).thenReturn(true);
    when(result.getTouchExpansionLeft()).thenReturn(1);
    when(result.getTouchExpansionTop()).thenReturn(1);
    when(result.getTouchExpansionRight()).thenReturn(1);
    when(result.getTouchExpansionBottom()).thenReturn(1);

    viewNodeInfo.setExpandedTouchBounds(result, node, 1, 1, 1, 1);

    MountItem viewMountItem =
        MountItemTestHelper.create(
            mViewComponent,
            sViewComponentKey,
            null,
            content,
            null,
            viewNodeInfo,
            null,
            0,
            0,
            0,
            0,
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            ORIENTATION_PORTRAIT,
            null);
    mHost.mount(index, viewMountItem, new Rect());

    return viewMountItem;
  }

  private void unmount(int index, MountItem mountItem) {
    mHost.unmount(index, mountItem);
  }

  private static class TestableComponentHost extends ComponentHost {

    private int mInvalidationCount = 0;
    private Rect mInvalidationRect = null;
    private int mFocusRequestCount = 0;

    public TestableComponentHost(ComponentContext context) {
      super(context);
    }

    public TestableComponentHost(Context context) {
      super(context);
    }

    @Override
    public void invalidate(Rect dirty) {
      super.invalidate(dirty);

      trackInvalidation(dirty.left, dirty.top, dirty.right, dirty.bottom);
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
      super.invalidate(l, t, r, b);

      trackInvalidation(l, t, r, b);
    }

    @Override
    public void invalidate() {
      super.invalidate();

      trackInvalidation(0, 0, getWidth(), getHeight());
    }

    @Override
    public void onViewAdded(View child) {
      super.onViewAdded(child);

      trackInvalidation(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
    }

    @Override
    public void onViewRemoved(View child) {
      super.onViewRemoved(child);

      trackInvalidation(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
    }

    @Override
    public int getDescendantFocusability() {
      mFocusRequestCount++;
      return super.getDescendantFocusability();
    }

    int getFocusRequestCount() {
      return mFocusRequestCount;
    }

    int getInvalidationCount() {
      return mInvalidationCount;
    }

    Rect getInvalidationRect() {
      return mInvalidationRect;
    }

    private void trackInvalidation(int l, int t, int r, int b) {
      mInvalidationCount++;

      mInvalidationRect = new Rect();
      mInvalidationRect.set(l, t, r, b);
    }
  }

  private static class TouchableDrawable extends ColorDrawable implements Touchable {

    @Override
    public boolean onTouchEvent(MotionEvent event, View host) {
      return true;
    }

    @Override
    public boolean shouldHandleTouchEvent(MotionEvent event) {
      return true;
    }
  }
}
