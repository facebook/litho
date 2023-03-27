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
package com.facebook.litho

import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.TestLayoutComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LayoutStateTest {

  @JvmField @Rule val legacyLithoViewRule: LithoViewRule = LithoViewRule()

  @Test
  fun toRenderTree_emptyMountableOutputs_hasPositionForRoot() {
    val context = legacyLithoViewRule.context
    val result =
        ResolveTreeFuture.resolve(
            context,
            TestLayoutComponent.create(context).build(),
            TreeState(),
            -1,
            -1,
            null,
            null,
            null,
            null)
    val layoutState =
        LayoutTreeFuture.layout(
            result,
            makeSizeSpec(20, SizeSpec.EXACTLY),
            makeSizeSpec(20, SizeSpec.EXACTLY),
            -1,
            -1,
            false,
            null,
            null,
            null,
            null)
    layoutState.toRenderTree()
    assertThat(layoutState.mountableOutputCount).isEqualTo(1)
    val renderTreeNode = layoutState.getMountableOutputAt(0)
    val id = renderTreeNode.renderUnit.id
    assertThat(layoutState.getPositionForId(id)).isEqualTo(0)
  }
}
