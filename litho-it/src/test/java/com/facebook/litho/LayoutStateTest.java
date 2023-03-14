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

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.RenderTreeNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutStateTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Test
  public void toRenderTree_emptyMountableOutputs_hasPositionForRoot() {
    ComponentContext context = mLegacyLithoViewRule.getContext();

    final ResolveResult result =
        ResolveTreeFuture.resolve(
            context,
            TestLayoutComponent.create(context).build(),
            new TreeState(),
            -1,
            -1,
            null,
            null,
            null,
            null);
    final LayoutState layoutState =
        LayoutTreeFuture.layout(
            result,
            SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY),
            -1,
            -1,
            false,
            null,
            null,
            null,
            null);

    layoutState.toRenderTree();

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(1);

    final RenderTreeNode renderTreeNode = layoutState.getMountableOutputAt(0);
    final long id = renderTreeNode.getRenderUnit().getId();
    assertThat(layoutState.getPositionForId(id)).isEqualTo(0);
  }
}
