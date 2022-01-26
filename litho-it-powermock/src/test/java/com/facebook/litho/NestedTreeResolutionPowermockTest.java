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

package com.facebook.litho;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.RootComponentWithTreeProps;
import com.facebook.yoga.YogaDirection;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

/** Tests {@link Component} */
@PrepareForTest({
  DiffNode.class,
  Layout.class,
  InternalNodeUtils.class,
})
@PowerMockIgnore({
  "org.mockito.*",
  "org.robolectric.*",
  "androidx.*",
  "android.*",
  "com.facebook.yoga.*"
})
@RunWith(LithoTestRunner.class)
public class NestedTreeResolutionPowermockTest {

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  private static final int NODE_LIST_SIZE = 100;

  private final List<LithoNode> mNodes = new ArrayList<>(NODE_LIST_SIZE);
  private final List<NestedTreeHolder> mInputOnlyNestedTreeHolders =
      new ArrayList<>(NODE_LIST_SIZE);
  private int mNextInternalNode;
  private int mNextInputOnlyNestedTreeHolder;

  private LithoNode getNextInternalNode() {
    if (mNextInternalNode >= NODE_LIST_SIZE) {
      throw new IllegalStateException("Increase NODE_LIST_SIZE");
    }

    return mNodes.get(mNextInternalNode++);
  }

  private NestedTreeHolder getNextInputOnlyNestedTreeHolder() {
    if (mNextInputOnlyNestedTreeHolder >= NODE_LIST_SIZE) {
      throw new IllegalStateException("Increase NODE_LIST_SIZE");
    }

    return mInputOnlyNestedTreeHolders.get(mNextInputOnlyNestedTreeHolder++);
  }

  private void createSpyNodes() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    for (int i = 0, size = NODE_LIST_SIZE; i < size; i++) {
      mNodes.add(spy(new LithoNode(c)));
      mInputOnlyNestedTreeHolders.add(spy(new NestedTreeHolder(c, null)));
    }
  }

  private void clearSpyNodes() {
    mInputOnlyNestedTreeHolders.clear();
    mNodes.clear();
    mNextInternalNode = 0;
    mNextInputOnlyNestedTreeHolder = 0;
  }

  @Before
  public void setUp() {
    createSpyNodes();

    ComponentsConfiguration.isEndToEndTestRun = true;

    try {
      whenNew(LithoNode.class)
          .withArguments((ComponentContext) any())
          .thenAnswer(
              new Answer<LithoNode>() {
                @Override
                public LithoNode answer(InvocationOnMock invocation) throws Throwable {
                  final ComponentContext c = (ComponentContext) invocation.getArguments()[0];
                  final LithoNode node = getNextInternalNode();
                  Whitebox.setInternalState(node, "mContext", c.getAndroidContext());
                  if (c.useStatelessComponent()) {
                    Whitebox.setInternalState(node, "mScopedComponentInfos", new ArrayList<>(2));
                  }

                  return node;
                }
              });
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      whenNew(NestedTreeHolder.class)
          .withArguments((ComponentContext) any(), (TreeProps) any())
          .thenAnswer(
              new Answer<NestedTreeHolder>() {
                @Override
                public NestedTreeHolder answer(InvocationOnMock invocation) throws Throwable {
                  final ComponentContext c = (ComponentContext) invocation.getArguments()[0];
                  final TreeProps props = (TreeProps) invocation.getArguments()[1];
                  final NestedTreeHolder node = getNextInputOnlyNestedTreeHolder();
                  Whitebox.setInternalState(node, "mContext", c.getAndroidContext());
                  if (c.useStatelessComponent()) {
                    Whitebox.setInternalState(node, "mScopedComponentInfos", new ArrayList<>(2));
                  }
                  Whitebox.setInternalState(node, "mPendingTreeProps", TreeProps.copy(props));

                  return node;
                }
              });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @After
  public void after() {
    clearSpyNodes();
  }

  @Test
  public void onRenderComponentWithSizeSpecWithoutReuse_shouldCallOnCreateLayoutTwice() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    LithoView lithoView = getLithoView(c, component, 100, 100);

    final LithoLayoutResult root = getLithoLayoutResult(lithoView);

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(LithoLayoutResult.NestedTreeHolderResult.class);

    LithoLayoutResult.NestedTreeHolderResult holder =
        (LithoLayoutResult.NestedTreeHolderResult) root.getChildAt(1);
    verify(holder.getInternalNode(), times(2)).copyInto(any(LithoNode.class));
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldTransferLayoutDirectionIfNotExplicitlySet() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();
    LithoView lithoView = getLithoView(c, component, 100, 100);

    final LithoLayoutResult root = getLithoLayoutResult(lithoView);

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(LithoLayoutResult.NestedTreeHolderResult.class);
    LithoLayoutResult.NestedTreeHolderResult holder =
        (LithoLayoutResult.NestedTreeHolderResult) root.getChildAt(1);

    assertThat(holder.getYogaNode().getLayoutDirection()).isEqualTo(YogaDirection.LTR);
    verify(holder.getNestedResult().getInternalNode(), times(1)).layoutDirection(YogaDirection.LTR);
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldInheritLayoutDirectionIfExplicitlySetOnParent() {
    final ComponentContext c = new ComponentContext(getApplicationContext());
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c)
            .shouldNotUpdateState(true)
            .layoutDirection(YogaDirection.RTL)
            .build();

    LithoView lithoView = getLithoView(c, component, 100, 100);

    final LithoLayoutResult root = getLithoLayoutResult(lithoView);

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(LithoLayoutResult.NestedTreeHolderResult.class);
    LithoLayoutResult.NestedTreeHolderResult holder =
        (LithoLayoutResult.NestedTreeHolderResult) root.getChildAt(1);

    assertThat(holder.getYogaNode().getLayoutDirection()).isEqualTo(YogaDirection.RTL);
    verify(holder.getNestedResult().getInternalNode(), times(1)).layoutDirection(YogaDirection.RTL);
  }

  private LithoView getLithoView(
      ComponentContext c, Component component, int widthPx, int heightPx) {
    LithoView lithoView = new LithoView(c);
    lithoView.onAttachedToWindowForTest();
    int widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
    int heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY);
    lithoView.measure(widthSpec, heightSpec);
    lithoView.setComponent(component);
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    return lithoView;
  }

  private LithoLayoutResult getLithoLayoutResult(LithoView lithoView) {
    return lithoView.getComponentTree().getCommittedLayoutState().getLayoutRoot();
  }
}
