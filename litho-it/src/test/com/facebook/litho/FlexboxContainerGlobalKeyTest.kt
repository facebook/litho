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

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.layout.flexbox.KColumn
import com.facebook.litho.layout.flexbox.KRow
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins the key-neutrality guarantee that `KColumn`/`KRow` adopt the classic
 * [com.facebook.litho.Column]/[com.facebook.litho.Row] `typeId`, so that migrating a container from
 * the builder API to the DSL does not change any auto-generated global key.
 *
 * The guarantee is load-bearing: a global key change re-identifies a node for visibility events,
 * `@State` and transitions. SEV S685701 was caused by a codemod converting only one of two
 * interchangeable render containers, so the two states produced different keys and the visibility
 * layer flapped `onInvisible`/`onVisible` forever.
 *
 * Because [ScopedComponentInfo.getChildCountAndIncrement] buckets the sibling counter by `typeId`,
 * the shared `typeId` also means a classic `Column` and a `KColumn` under the same parent share one
 * sibling bucket. That is what makes the keys position-based rather than variant-based, so any
 * subset of the siblings can be migrated independently. These tests fail if either half of that
 * pairing regresses.
 */
@RunWith(LithoTestRunner::class)
class FlexboxContainerGlobalKeyTest {

  private lateinit var context: ComponentContext
  private var wasDebugModeEnabled = false

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true)
    context = ComponentContext(getApplicationContext<Context>())
    // Off the debug path GlobalKey.toString() returns an identity string instead of the structural
    // key, which the assertions below compare against.
    wasDebugModeEnabled = LithoDebugConfigurations.isDebugModeEnabled
    LithoDebugConfigurations.isDebugModeEnabled = true
  }

  @After
  fun restoreConfiguration() {
    LithoDebugConfigurations.isDebugModeEnabled = wasDebugModeEnabled
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  @Test
  fun kFlexboxContainers_adoptClassicColumnAndRowTypeIds() {
    assertThat(KColumn().typeId).isEqualTo(Column.create(context).build().typeId)
    assertThat(KRow().typeId).isEqualTo(Row.create(context).build().typeId)
    assertThat(KColumn().typeId).isNotEqualTo(KRow().typeId)
  }

  @Test
  fun getChildCountAndIncrement_bucketsKColumnWithClassicColumn() {
    val scopedComponentInfo =
        ScopedComponentInfo(Column.create(context).build(), context, /* errorEventHandler */ null)

    assertThat(scopedComponentInfo.getChildCountAndIncrement(Column.create(context).build()))
        .isEqualTo(0)
    assertThat(scopedComponentInfo.getChildCountAndIncrement(KColumn())).isEqualTo(1)
    assertThat(scopedComponentInfo.getChildCountAndIncrement(KColumn())).isEqualTo(2)

    // Column/KColumn and Row/KRow remain two distinct buckets.
    assertThat(scopedComponentInfo.getChildCountAndIncrement(KRow())).isEqualTo(0)
    assertThat(scopedComponentInfo.getChildCountAndIncrement(Row.create(context).build()))
        .isEqualTo(1)
  }

  @Test
  fun mixedColumnSiblings_shareOneSiblingBucket() {
    val columnTypeId = Column.create(context).build().typeId
    val textTypeId = Text.create(context).text("").build().typeId

    val keys = leafGlobalKeys(
        Column.create(context)
            .child(classicColumn(leaf("a")))
            .child(KColumn(children = listOf(leaf("b"))))
            .build(),
    )

    assertThat(keys)
        .containsExactly(
            ComponentKeyUtils.getKeyWithSeparatorForTest(columnTypeId, columnTypeId, textTypeId),
            ComponentKeyUtils.getKeyWithSeparatorForTest(
                columnTypeId,
                "$columnTypeId!1",
                textTypeId,
            ),
        )
  }

  @Test
  fun mixedRowSiblings_shareOneSiblingBucket() {
    val rowTypeId = Row.create(context).build().typeId
    val textTypeId = Text.create(context).text("").build().typeId

    val keys = leafGlobalKeys(
        Row.create(context)
            .child(classicRow(leaf("a")))
            .child(KRow(children = listOf(leaf("b"))))
            .build(),
    )

    assertThat(keys)
        .containsExactly(
            ComponentKeyUtils.getKeyWithSeparatorForTest(rowTypeId, rowTypeId, textTypeId),
            ComponentKeyUtils.getKeyWithSeparatorForTest(rowTypeId, "$rowTypeId!1", textTypeId),
        )
  }

  @Test
  fun columnToKColumnMigration_leavesAutoGlobalKeysUnchanged() {
    val expected = leafGlobalKeys(
        Column.create(context)
            .child(classicColumn(leaf("a")))
            .child(classicColumn(leaf("b")))
            .build(),
    )
    assertThat(expected).hasSize(2)

    val firstMigrated = leafGlobalKeys(
        Column.create(context)
            .child(KColumn(children = listOf(leaf("a"))))
            .child(classicColumn(leaf("b")))
            .build(),
    )
    val secondMigrated = leafGlobalKeys(
        Column.create(context)
            .child(classicColumn(leaf("a")))
            .child(KColumn(children = listOf(leaf("b"))))
            .build(),
    )
    val bothMigrated = leafGlobalKeys(
        Column.create(context)
            .child(KColumn(children = listOf(leaf("a"))))
            .child(KColumn(children = listOf(leaf("b"))))
            .build(),
    )

    assertThat(firstMigrated).isEqualTo(expected)
    assertThat(secondMigrated).isEqualTo(expected)
    assertThat(bothMigrated).isEqualTo(expected)
  }

  @Test
  fun rowToKRowMigration_leavesAutoGlobalKeysUnchanged() {
    val expected = leafGlobalKeys(
        Row.create(context).child(classicRow(leaf("a"))).child(classicRow(leaf("b"))).build(),
    )
    assertThat(expected).hasSize(2)

    val firstMigrated = leafGlobalKeys(
        Row.create(context)
            .child(KRow(children = listOf(leaf("a"))))
            .child(classicRow(leaf("b")))
            .build(),
    )
    val secondMigrated = leafGlobalKeys(
        Row.create(context)
            .child(classicRow(leaf("a")))
            .child(KRow(children = listOf(leaf("b"))))
            .build(),
    )
    val bothMigrated = leafGlobalKeys(
        Row.create(context)
            .child(KRow(children = listOf(leaf("a"))))
            .child(KRow(children = listOf(leaf("b"))))
            .build(),
    )

    assertThat(firstMigrated).isEqualTo(expected)
    assertThat(secondMigrated).isEqualTo(expected)
    assertThat(bothMigrated).isEqualTo(expected)
  }

  private fun leaf(text: String): Component =
      Text.create(context).text(text).widthDip(10f).heightDip(10f).build()

  private fun classicColumn(child: Component): Component =
      Column.create(context).child(child).build()

  private fun classicRow(child: Component): Component = Row.create(context).child(child).build()

  /**
   * Renders [root] and returns the global keys of the leaf [Text]s in resolve order. The leaves are
   * read rather than the containers themselves because a leaf key embeds its whole ancestor chain,
   * including the container's sibling index.
   */
  private fun leafGlobalKeys(root: Component): List<String> {
    val lithoView = LithoView(context)
    lithoView.setComponent(root)
    lithoView.measure(unspecified(640), unspecified(480))
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
    return LithoKeyTestingUtil.getScopedComponentInfos(lithoView).getValue("Text").map {
      it.context.globalKey.toString()
    }
  }
}
