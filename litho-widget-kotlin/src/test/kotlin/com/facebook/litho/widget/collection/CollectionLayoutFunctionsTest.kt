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

package com.facebook.litho.widget.collection

import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.CollectionLayoutScope
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LinearLayoutInfo
import com.facebook.litho.widget.LithoCollectionItem
import com.facebook.litho.widget.getChildSizeConstraints
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.SizeConstraints.Companion.Infinity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class CollectionLayoutFunctionsTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  // region: Cases for CollectionLayoutFunctions::getChildSizeConstraints

  // Tests child size constraints when collection has a fixed size (200x400)
  // Condition:
  //  - vertical collection
  //  - collection size is fixed (200x400)
  //  - item hasn't been measured yet
  //  - CrossAxisWrapMode.NoWrap
  // Expectation: child constraints follow layout info specs directly
  @Test
  fun `getChildSizeConstraints when collection size is fixed`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(200, 400),
            collectionSize = Size(200, 400),
            isVertical = true,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.NoWrap,
        )
    val renderInfo = ComponentRenderInfo.createEmpty()
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(100))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(100))
    val item = LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo)
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.maxWidth == 100).isTrue
    assertThat(result.maxHeight == 100).isTrue
  }

  // Tests child size constraints when collection has fixed size and item matches parent
  // Condition:
  //  - vertical collection
  //  - collection size is fixed (300x600)
  //  - item has parent percentage constraints (50% width, 50% height)
  //  - CrossAxisWrapMode.NoWrap
  // Expectation: child constraints are adjusted based on parent percentage values
  @Test
  fun `getChildSizeConstraints when collection size is fixed and item matches parent`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(300, 600),
            collectionSize = Size(300, 600),
            isVertical = true,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.NoWrap,
        )
    val renderInfo = spy(ComponentRenderInfo.createEmpty())
    whenever(renderInfo.parentWidthPercent).thenReturn(50f)
    whenever(renderInfo.parentHeightPercent).thenReturn(50f)
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(300))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(600))
    val item = LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo)
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.maxWidth).isEqualTo(150)
    assertThat(result.maxHeight).isEqualTo(300)
  }

  // Tests child size constraints with no collection size and CrossAxisWrapMode.NoWrap (vertical)
  // Condition:
  //  - vertical collection
  //  - no collection size (null)
  //  - item hasn't been measured yet
  //  - CrossAxisWrapMode.NoWrap
  // Expectation: child constraints follow layout info specs directly
  @Test
  fun `getChildSizeConstraints with no collection size and crossAxisWrapMode is NoWrap vertical`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(400, 400),
            collectionSize = null,
            isVertical = true,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.NoWrap,
        )
    val renderInfo = ComponentRenderInfo.createEmpty()
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(100))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(200))
    val item = LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo)
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.hasExactWidth).isTrue
    assertThat(result.maxWidth).isEqualTo(100)
    assertThat(result.maxHeight).isEqualTo(200)
  }

  // Tests child size constraints with no collection size and CrossAxisWrapMode.NoWrap (horizontal)
  // Condition:
  //  - horizontal collection
  //  - no collection size (null)
  //  - item hasn't been measured yet
  //  - CrossAxisWrapMode.NoWrap
  // Expectation: child constraints follow layout info specs directly
  @Test
  fun `getChildSizeConstraints with no collection size and crossAxisWrapMode is NoWrap horizontal`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(400, 400),
            collectionSize = null,
            isVertical = false,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.NoWrap,
        )
    val renderInfo = ComponentRenderInfo.createEmpty()
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(200))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(100))
    val item = LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo)
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.maxWidth).isEqualTo(200)
    assertThat(result.hasExactHeight).isTrue
    assertThat(result.maxHeight).isEqualTo(100)
  }

  // Tests child size constraints with collection size and CrossAxisWrapMode.Dynamic (vertical)
  // Condition:
  //  - vertical collection
  //  - collection size is fixed
  //  - item hasn't been measured yet
  //  - CrossAxisWrapMode.Dynamic
  // Expectation: no limitation on child's width
  @Test
  fun `getChildSizeConstraints with collection size and crossAxisWrapMode is Dynamic vertical`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(400, 400),
            collectionSize = Size(400, 400),
            isVertical = true,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.Dynamic,
        )
    val renderInfo = ComponentRenderInfo.createEmpty()
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(200))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(200))
    val item = LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo)
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.hasBoundedWidth).isFalse
    assertThat(result.maxWidth).isEqualTo(Infinity)
    assertThat(result.maxHeight).isEqualTo(200)
  }

  // Tests child size constraints with collection size and CrossAxisWrapMode.Dynamic (horizontal)
  // Condition:
  //  - horizontal collection
  //  - collection size is fixed
  //  - item hasn't been measured yet
  //  - CrossAxisWrapMode.Dynamic
  // Expectation: no limitation on child's height
  @Test
  fun `getChildSizeConstraints with collection size is fixed and crossAxisWrapMode is Dynamic horizontal`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(400, 400),
            collectionSize = Size(400, 400),
            isVertical = false,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.Dynamic,
        )
    val renderInfo = ComponentRenderInfo.createEmpty()
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(200))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(200))
    val item = LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo)
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.maxWidth).isEqualTo(200)
    assertThat(result.hasBoundedHeight).isFalse
    assertThat(result.maxHeight).isEqualTo(Infinity)
  }

  // Tests child size constraints with fixed item size in vertical collection with
  // CrossAxisWrapMode.Dynamic
  // Condition:
  //  - vertical collection
  //  - collection size is fixed
  //  - item has fixed size (150x100)
  //  - CrossAxisWrapMode.Dynamic
  // Expectation: child's width constraint should respect collection size
  @Test
  fun `getChildSizeConstraints with fixed item size in vertical Dynamic collection`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(400, 400),
            collectionSize = Size(400, 400),
            isVertical = true,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.Dynamic,
        )
    val renderInfo = spy(ComponentRenderInfo.createEmpty())
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(150))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(100))
    val item = spy(LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo))
    whenever(item.size).thenReturn(Size(150, 100))
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.hasExactWidth).isTrue
    assertThat(result.maxWidth).isEqualTo(400)
    assertThat(result.maxHeight).isEqualTo(100)
  }

  // Tests child size constraints with fixed item size in horizontal collection with
  // CrossAxisWrapMode.Dynamic
  // Condition:
  //  - horizontal collection
  //  - collection size is fixed
  //  - item has fixed size (150x100)
  //  - CrossAxisWrapMode.Dynamic
  // Expectation: child's height constraint should respect collection size
  @Test
  fun `getChildSizeConstraints with fixed item size in horizontal Dynamic collection`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(400, 400),
            collectionSize = Size(400, 400),
            isVertical = false,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.Dynamic,
        )
    val renderInfo = spy(ComponentRenderInfo.createEmpty())
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(150))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(100))
    val item = spy(LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo))
    whenever(item.size).thenReturn(Size(150, 100))
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.maxWidth).isEqualTo(150)
    assertThat(result.hasExactHeight).isTrue
    assertThat(result.maxHeight).isEqualTo(400)
  }

  // Tests child size constraints with larger item size in vertical collection with
  // CrossAxisWrapMode.Dynamic
  // Condition:
  //  - vertical collection
  //  - collection size is fixed (300x300)
  //  - item has fixed size larger than collection (500x200)
  //  - CrossAxisWrapMode.Dynamic
  // Expectation:
  //  - child's width constraint should be the largest number between collection size and
  // item size
  @Test
  fun `getChildSizeConstraints with larger item width in vertical Dynamic collection`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(300, 300),
            collectionSize = Size(300, 300),
            isVertical = true,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.Dynamic,
        )
    val renderInfo = spy(ComponentRenderInfo.createEmpty())
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(500))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(200))
    val item = spy(LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo))
    whenever(item.size).thenReturn(Size(500, 200))
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.hasExactWidth).isTrue
    assertThat(result.maxWidth).isEqualTo(500)
    assertThat(result.maxHeight).isEqualTo(200)
  }

  // Tests child size constraints with larger item size in horizontal collection with
  // CrossAxisWrapMode.Dynamic
  // Condition:
  //  - horizontal collection
  //  - collection size is fixed (300x300)
  //  - item has fixed size larger than collection (200x500)
  //  - CrossAxisWrapMode.Dynamic
  // Expectation: child's height constraint should be the largest number between collection size and
  // item size
  @Test
  fun `getChildSizeConstraints with larger item height in horizontal Dynamic collection`() {
    val linearLayoutInfo = mock<LinearLayoutInfo>()
    val scope =
        CollectionLayoutScope(
            layoutInfo = linearLayoutInfo,
            collectionConstraints = SizeConstraints.exact(300, 300),
            collectionSize = Size(300, 300),
            isVertical = false,
            wrapInMainAxis = false,
            crossAxisWrapMode = CrossAxisWrapMode.Dynamic,
        )
    val renderInfo = spy(ComponentRenderInfo.createEmpty())
    whenever(linearLayoutInfo.getChildWidthSpec(any(), eq(renderInfo))).thenReturn(exactly(200))
    whenever(linearLayoutInfo.getChildHeightSpec(any(), eq(renderInfo))).thenReturn(exactly(500))
    val item = spy(LithoCollectionItem(lithoTestRule.context, renderInfo = renderInfo))
    whenever(item.size).thenReturn(Size(200, 500))
    val result = scope.getChildSizeConstraints(item)
    assertThat(result).isNotNull
    assertThat(result.maxWidth).isEqualTo(200)
    assertThat(result.hasExactHeight).isTrue
    assertThat(result.maxHeight).isEqualTo(500)
  }

  // endregion
}
