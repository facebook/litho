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
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.getSize;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.animation.StateListAnimator;
import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.Config;

@PrepareForTest({LayoutState.class})
@PowerMockIgnore({
  "org.mockito.*",
  "org.robolectric.*",
  "android.*",
  "androidx.*",
  "com.facebook.yoga.*"
})
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(LithoTestRunner.class)
public class LayoutStateCalculateTest {

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  private ComponentContext mBaseContext;

  @Before
  public void setup() {
    mBaseContext = new ComponentContext(getApplicationContext());
  }

  @Test
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public void testLayoutOutputsWithStateListAnimator() {
    final StateListAnimator stateListAnimator = new StateListAnimator();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(SimpleMountSpecTester.create(c))
                        .stateListAnimator(stateListAnimator))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mBaseContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(1)).getComponent())
        .isExactlyInstanceOf(HostComponent.class);
    assertThat(
            getLayoutOutput(layoutState.getMountableOutputAt(1))
                .getViewNodeInfo()
                .getStateListAnimator())
        .isSameAs(stateListAnimator);

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(2)).getComponent())
        .isExactlyInstanceOf(SimpleMountSpecTester.class);
  }

  @Test
  public void testLayoutOutputWithCachedLayoutSpecDelegate() throws Exception {
    final ComponentTree componentTree = ComponentTree.create(mBaseContext).build();
    final ComponentContext c = ComponentContext.withComponentTree(mBaseContext, componentTree);
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateContext(new LayoutStateContext(layoutState, componentTree));

    final int widthSpecContainer = makeSizeSpec(300, EXACTLY);
    final int heightSpec = makeSizeSpec(0, UNSPECIFIED);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent =
        makeSizeSpec(getSize(widthSpecContainer) - horizontalPadding - horizontalPadding, EXACTLY);

    final Component component = TestLayoutComponent.create(c, 0, 0, true, true, true, true).build();

    final Size sizeOutput = new Size();
    component.measure(c, widthMeasuredComponent, heightSpec, sizeOutput);

    // Check the cached measured component tree
    assertThat(layoutState.getCachedLayout(component)).isNotNull();
    final LithoLayoutResult cachedLayout = layoutState.getCachedLayout(component);
    assertThat(cachedLayout.getChildCount()).isEqualTo(0);
    assertThat(cachedLayout.getInternalNode().getTailComponent())
        .isInstanceOf(TestDrawableComponent.class);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).paddingPx(HORIZONTAL, horizontalPadding).child(component).build();
          }
        };

    // Here we make sure that we try to remeasure during the same LayoutState calculation to check
    // if the cached InternalNode is reused.
    final LayoutState layoutStateSpy = spy(layoutState);
    // spy(ComponentsPools.class);
    whenNew(LayoutState.class).withAnyArguments().thenReturn(layoutStateSpy);

    calculateLayoutState(mBaseContext, rootContainer, -1, widthSpecContainer, heightSpec);

    // Make sure we reused the cached layout and it wasn't released.
    verify(layoutStateSpy, times(1)).clearCachedLayout(component);

    // Check total layout outputs.
    assertThat(layoutStateSpy.getMountableOutputCount()).isEqualTo(2);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutStateSpy, 0))).isTrue();
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(0)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 300, sizeOutput.height));
    assertThat(getComponentAt(layoutStateSpy, 1)).isInstanceOf(TestDrawableComponent.class);
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(1)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(20, 0, 280, 0));

    validateMockitoUsage();
  }

  @Test
  public void testLayoutOutputWithCachedLayoutSpecWithMeasureDelegate() throws Exception {
    final ComponentTree componentTree = ComponentTree.create(mBaseContext).build();
    final ComponentContext c = ComponentContext.withComponentTree(mBaseContext, componentTree);
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateContext(new LayoutStateContext(layoutState, componentTree));

    final int widthSpecContainer = makeSizeSpec(300, EXACTLY);
    final int heightSpec = makeSizeSpec(0, UNSPECIFIED);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent =
        makeSizeSpec(getSize(widthSpecContainer) - horizontalPadding - horizontalPadding, EXACTLY);

    final Component sizeDependentComponentSpy =
        spy(TestSizeDependentComponent.create(c).setFixSizes(false).setDelegate(true).build());
    final Size sizeOutput = new Size();
    sizeDependentComponentSpy.measure(c, widthMeasuredComponent, heightSpec, sizeOutput);

    // Reset the checks for layout release and clearing that happen during measurement
    reset(sizeDependentComponentSpy);
    doReturn(sizeDependentComponentSpy).when(sizeDependentComponentSpy).makeShallowCopy();

    // Check the cached measured component tree
    assertThat(layoutState.getCachedLayout(sizeDependentComponentSpy)).isNotNull();
    final LithoLayoutResult cachedLayout = layoutState.getCachedLayout(sizeDependentComponentSpy);
    assertThat(cachedLayout.getChildCount()).isEqualTo(0);
    assertThat(cachedLayout.getInternalNode().getTailComponent())
        .isInstanceOf(TestDrawableComponent.class);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .paddingPx(HORIZONTAL, horizontalPadding)
                .child(sizeDependentComponentSpy)
                .build();
          }
        };

    final LayoutState layoutStateSpy = spy(layoutState);
    whenNew(LayoutState.class).withAnyArguments().thenReturn(layoutStateSpy);

    calculateLayoutState(mBaseContext, rootContainer, -1, widthSpecContainer, heightSpec);

    verify(sizeDependentComponentSpy, times(1)).makeShallowCopy();

    // Make sure we reused the cached layout and it wasn't released.
    verify(layoutStateSpy, times(1)).clearCachedLayout(sizeDependentComponentSpy);

    // Check total layout outputs.
    assertThat(layoutStateSpy.getMountableOutputCount()).isEqualTo(3);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutStateSpy, 0))).isTrue();
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(0)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 300, sizeOutput.height));
    // Check NestedTree
    assertThat(getComponentAt(layoutStateSpy, 1)).isInstanceOf(DrawableComponent.class);
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(1)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(20, 0, 280, 0));
    assertThat(getComponentAt(layoutStateSpy, 2)).isInstanceOf(TestDrawableComponent.class);
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(2)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(20, 0, 280, 0));

    validateMockitoUsage();
  }

  @Test
  public void testLayoutOutputWithCachedLayoutSpecWithMeasure() throws Exception {
    final ComponentTree componentTree = ComponentTree.create(mBaseContext).build();
    final ComponentContext c = ComponentContext.withComponentTree(mBaseContext, componentTree);
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateContext(new LayoutStateContext(layoutState, componentTree));

    final int widthSpecContainer = makeSizeSpec(300, EXACTLY);
    final int heightSpec = makeSizeSpec(0, UNSPECIFIED);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent =
        makeSizeSpec(getSize(widthSpecContainer) - horizontalPadding - horizontalPadding, EXACTLY);

    final Component sizeDependentComponentSpy =
        spy(TestSizeDependentComponent.create(c).setFixSizes(false).setDelegate(false).build());
    final Size sizeOutput = new Size();
    sizeDependentComponentSpy.measure(c, widthMeasuredComponent, heightSpec, sizeOutput);

    // Reset the checks for layout release and clearing that happen during measurement
    reset(sizeDependentComponentSpy);
    doReturn(sizeDependentComponentSpy).when(sizeDependentComponentSpy).makeShallowCopy();

    // Check the cached measured component tree
    assertThat(layoutState.getCachedLayout(sizeDependentComponentSpy)).isNotNull();
    final LithoLayoutResult cachedLayout = layoutState.getCachedLayout(sizeDependentComponentSpy);
    assertThat(cachedLayout.getChildCount()).isEqualTo(2);
    assertThat(((InternalNode) cachedLayout.getChildAt(0)).getTailComponent())
        .isInstanceOf(TestDrawableComponent.class);
    assertThat(((InternalNode) cachedLayout.getChildAt(1)).getTailComponent())
        .isInstanceOf(TestViewComponent.class);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .flexShrink(0)
                .paddingPx(HORIZONTAL, horizontalPadding)
                .child(sizeDependentComponentSpy)
                .build();
          }
        };

    final LayoutState layoutStateSpy = spy(layoutState);
    whenNew(LayoutState.class).withAnyArguments().thenReturn(layoutStateSpy);

    calculateLayoutState(mBaseContext, rootContainer, -1, widthSpecContainer, heightSpec);

    verify(sizeDependentComponentSpy, times(1)).makeShallowCopy();

    // Make sure we reused the cached layout and it wasn't released.
    verify(layoutStateSpy, times(1)).clearCachedLayout(sizeDependentComponentSpy);

    // Check total layout outputs.
    assertThat(layoutStateSpy.getMountableOutputCount()).isEqualTo(4);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutStateSpy, 0))).isTrue();
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(0)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 300, sizeOutput.height));
    // Check NestedTree
    assertThat(getComponentAt(layoutStateSpy, 1)).isInstanceOf(DrawableComponent.class);
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(1)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(25, 5, 275, 5));
    assertThat(getComponentAt(layoutStateSpy, 2)).isInstanceOf(TestDrawableComponent.class);
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(2)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(25, 5, 275, 5));
    assertThat(getComponentAt(layoutStateSpy, 3)).isInstanceOf(TestViewComponent.class);
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(3)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(28, 8, 272, 8));

    validateMockitoUsage();
  }

  @Test
  public void testLayoutOutputWithCachedLayoutSpec() throws Exception {
    final ComponentTree componentTree = ComponentTree.create(mBaseContext).build();
    final ComponentContext c = ComponentContext.withComponentTree(mBaseContext, componentTree);
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateContext(new LayoutStateContext(layoutState, componentTree));

    final int widthSpecContainer = makeSizeSpec(300, EXACTLY);
    final int heightSpec = makeSizeSpec(0, UNSPECIFIED);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent =
        makeSizeSpec(getSize(widthSpecContainer) - horizontalPadding - horizontalPadding, EXACTLY);

    final Component componentSpy =
        spy(TestLayoutComponent.create(c, 0, 0, true, true, true, false).build());
    final Size sizeOutput = new Size();
    componentSpy.measure(c, widthMeasuredComponent, heightSpec, sizeOutput);

    // Reset the checks for layout release and clearing that happen during measurement
    reset(componentSpy);
    doReturn(componentSpy).when(componentSpy).makeShallowCopy();

    // Check the cached measured component tree
    assertThat(layoutState.getCachedLayout(componentSpy)).isNotNull();
    final LithoLayoutResult cachedLayout = layoutState.getCachedLayout(componentSpy);
    assertThat(cachedLayout.getChildCount()).isEqualTo(1);
    assertThat(((InternalNode) cachedLayout.getChildAt(0)).getTailComponent())
        .isInstanceOf(TestDrawableComponent.class);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).paddingPx(HORIZONTAL, horizontalPadding).child(componentSpy).build();
          }
        };

    final LayoutState layoutStateSpy = spy(layoutState);
    whenNew(LayoutState.class).withAnyArguments().thenReturn(layoutStateSpy);

    calculateLayoutState(mBaseContext, rootContainer, -1, widthSpecContainer, heightSpec);

    verify(componentSpy, times(1)).makeShallowCopy();

    // Make sure we reused the cached layout and it wasn't released.
    verify(layoutStateSpy, times(1)).clearCachedLayout(componentSpy);

    // Check total layout outputs.
    assertThat(layoutStateSpy.getMountableOutputCount()).isEqualTo(2);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutStateSpy, 0))).isTrue();
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(0)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 300, sizeOutput.height));
    assertThat(getComponentAt(layoutStateSpy, 1)).isInstanceOf(TestDrawableComponent.class);
    getLayoutOutput(layoutStateSpy.getMountableOutputAt(1)).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(20, 0, 280, 0));

    validateMockitoUsage();
  }

  private static LayoutState calculateLayoutState(
      final ComponentContext context,
      final Component component,
      final int componentTreeId,
      final int widthSpec,
      final int heightSpec) {

    return LayoutState.calculate(
        context,
        component,
        componentTreeId,
        widthSpec,
        heightSpec,
        LayoutState.CalculateLayoutSource.TEST);
  }

  private static ComponentLifecycle getComponentAt(final LayoutState layoutState, final int index) {
    return getLayoutOutput(layoutState.getMountableOutputAt(index)).getComponent();
  }

  private static boolean isHostComponent(final ComponentLifecycle component) {
    return component instanceof HostComponent;
  }
}
