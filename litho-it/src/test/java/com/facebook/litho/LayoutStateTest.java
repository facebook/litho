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

import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutStateTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Test
  public void toRenderTree_emptyMountableOutputs_hasPositionForRoot() {
    ComponentContext context = mLithoViewRule.getContext();
    context.setLayoutStateContext(LayoutStateContext.getTestInstance(context));

    final LayoutState layoutState = new LayoutState(context);
    final RenderTree renderTree = layoutState.toRenderTree();

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(1);

    final RenderTreeNode renderTreeNode = layoutState.getMountableOutputAt(0);
    final long id = renderTreeNode.getRenderUnit().getId();
    assertThat(layoutState.getPositionForId(id)).isEqualTo(0);
  }
}
