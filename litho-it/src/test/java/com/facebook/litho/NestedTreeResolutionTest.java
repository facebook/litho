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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.annotation.Nullable;
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.NestedTreeComponentSpec.ExtraProps;
import com.facebook.litho.widget.RootComponentWithTreeProps;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class NestedTreeResolutionTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void before() {
    ComponentsConfiguration.isEndToEndTestRun = true;
    NodeConfig.sInternalNodeFactory =
        new NodeConfig.InternalNodeFactory() {
          @Override
          public InternalNode create(ComponentContext c) {
            DefaultInternalNode node = spy(new DefaultInternalNode(c));
            node.getYogaNode().setData(node);
            return node;
          }

          @Override
          public InternalNode.NestedTreeHolder createNestedTreeHolder(
              ComponentContext c, @Nullable TreeProps props) {
            DefaultNestedTreeHolder node = spy(new DefaultNestedTreeHolder(c, props));
            node.getYogaNode().setData(node);
            return node;
          }
        };
  }

  @After
  public void after() {
    ComponentsConfiguration.isEndToEndTestRun = false;
    NodeConfig.sInternalNodeFactory = null;
  }

  @Test
  public void onRenderComponentWithSizeSpec_shouldContainNestTreeHolderAndNestedProps() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component = RootComponentWithTreeProps.create(c).build();

    mLithoViewRule.attachToWindow().setSizePx(100, 100).measure().setRoot(component).layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);

    DefaultNestedTreeHolder holder = (DefaultNestedTreeHolder) root.getChildAt(1);
    assertThat(holder.mNestedTreePadding.get(YogaEdge.ALL)).isEqualTo(5.0f);
    assertThat(holder.getNestedResult().getPaddingTop()).isEqualTo(5);
  }

  @Test
  public void onRenderComponentWithSizeSpecWithoutReuse_shouldCallOnCreateLayoutTwice() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    final ExtraProps props = new ExtraProps();
    props.shouldCreateNewLayout = true;
    props.setps = new ArrayList<>();

    mLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    assertThat(props.setps)
        .containsExactly(
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);
    verify(holder.getInternalNode(), times(2)).copyInto(any(InternalNode.class));
  }

  @Test
  public void onRenderComponentWithSizeSpecWithReuse_shouldCallOnCreateLayoutOnlyOnce() {
    final ComponentContext c = mLithoViewRule.getContext();
    final RootComponentWithTreeProps component =
        RootComponentWithTreeProps.create(c).shouldNotUpdateState(true).build();

    final ExtraProps props = new ExtraProps();
    props.shouldCreateNewLayout = false;
    props.setps = new ArrayList<>();

    mLithoViewRule
        .setTreeProp(ExtraProps.class, props)
        .attachToWindow()
        .setSizePx(100, 100)
        .measure()
        .setRoot(component)
        .layout();

    final LithoLayoutResult root = mLithoViewRule.getCurrentRootNode();

    assertThat(root).isNotNull();
    assertThat(root.getChildAt(1)).isInstanceOf(NestedTreeHolderResult.class);
    assertThat(props.setps).containsExactly(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);

    NestedTreeHolderResult holder = (NestedTreeHolderResult) root.getChildAt(1);
    verify(holder.getInternalNode()).copyInto(any(InternalNode.class));
  }
}
