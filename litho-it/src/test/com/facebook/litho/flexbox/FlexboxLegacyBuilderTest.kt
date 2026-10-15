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

package com.facebook.litho.flexbox

import android.annotation.SuppressLint
import com.facebook.litho.Column
import com.facebook.litho.DynamicValue
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.it.R
import com.facebook.litho.kotlinStyle
import com.facebook.litho.layout.flexbox.Column as KColumnDsl
import com.facebook.litho.layout.flexbox.KColumn
import com.facebook.litho.layout.flexbox.KRow
import com.facebook.litho.layout.flexbox.Row as KRowDsl
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.testing.unspecified
import com.facebook.litho.view.backgroundColor
import com.facebook.rendercore.px
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the W2 wrapper: the legacy `Row.create(c)...build()` / `Column.create(c)...build()`
 * builders either accumulate a [Style] and produce a [KRow] / [KColumn] (flag ON), or retain their
 * original direct legacy-builder behaviour (flag OFF), as selected by
 * [ComponentsConfiguration.useKComponentForLegacyRowColumn].
 */
@SuppressLint("DeprecatedClass", "DeprecatedMethod")
@RunWith(LithoTestRunner::class)
class FlexboxLegacyBuilderTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  private val c
    get() = lithoTestRule.context

  private lateinit var originalConfig: ComponentsConfiguration

  @Before
  fun setUp() {
    // Default the flag ON for these tests so build() returns KRow/KColumn.
    originalConfig = ComponentsConfiguration.defaultInstance
    ComponentsConfiguration.defaultInstance =
        ComponentsConfiguration.create(originalConfig).useKComponentForLegacyRowColumn(true).build()
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.defaultInstance = originalConfig
  }

  private fun setFlag(enabled: Boolean) {
    ComponentsConfiguration.defaultInstance =
        ComponentsConfiguration.create(originalConfig)
            .useKComponentForLegacyRowColumn(enabled)
            .build()
  }

  // ===== Flag gating =====

  @Test
  fun rowCreate_flagOn_returnsKRow() {
    assertThat(Row.create(c).build()).isInstanceOf(KRow::class.java)
  }

  @Test
  fun columnCreate_flagOn_returnsKColumn() {
    assertThat(Column.create(c).build()).isInstanceOf(KColumn::class.java)
  }

  @Test
  fun rowCreate_flagOff_returnsLegacyRow() {
    setFlag(false)
    assertThat(Row.create(c).build()).isInstanceOf(Row::class.java)
  }

  @Test
  fun columnCreate_flagOff_returnsLegacyColumn() {
    setFlag(false)
    assertThat(Column.create(c).build()).isInstanceOf(Column::class.java)
  }

  // ===== simpleName =====

  @Test
  fun rowCreate_defaultSimpleName_isRow() {
    assertThat(Row.create(c).build().simpleName).isEqualTo("Row")
  }

  @Test
  fun columnCreate_defaultSimpleName_isColumn() {
    assertThat(Column.create(c).build().simpleName).isEqualTo("Column")
  }

  @Test
  fun create_customSimpleName_isCarriedThrough() {
    assertThat(Row.create(c, "MyRow").build().simpleName).isEqualTo("MyRow")
    assertThat(Column.create(c, "MyColumn").build().simpleName).isEqualTo("MyColumn")
  }

  @Test
  fun builder_flagOn_nullDynamicBackgroundIsNotSet() {
    val builder = Row.create(c).backgroundColor(null as DynamicValue<Int>?)

    assertThat(builder.hasBackgroundSet()).isFalse()
  }

  @Test
  fun builder_flagOn_fractionalElevationIsPreserved() {
    val view = lithoTestRule.render { Row.create(c).shadowElevationPx(1.5f).build() }

    assertThat(view.currentRootNode?.node?.nodeInfo?.shadowElevation).isEqualTo(1.5f)
  }

  // ===== Children =====

  @Test
  fun builder_children_areAdded() {
    val view = lithoTestRule.render {
      Row.create(c).child(Row.create(c).build()).child(Column.create(c).build()).build()
    }
    assertThat(requireNotNull(view.currentRootNode?.node).childCount).isEqualTo(2)
  }

  @Test
  fun builder_flagOff_childAddedAfterBuild_isAdded() {
    setFlag(false)
    val builder = Column.create(c)
    val component = builder.build()

    builder.child(Row.create(c).build())

    val view = lithoTestRule.render { component }
    assertThat(requireNotNull(view.currentRootNode?.node).childCount).isEqualTo(1)
  }

  @Test
  fun builder_flagOn_defStyleIsApplied() {
    val view =
        lithoTestRule.render(widthSpec = unspecified(), heightSpec = unspecified()) {
          Column.create(c, 0, R.style.ApplyStylesTest_WidthHeight).build()
        }

    assertThat(view.lithoView.width).isEqualTo(37)
    assertThat(view.lithoView.height).isEqualTo(100)
  }

  @Test
  fun builder_flagOff_nullVisibilityHandlerStillAppliesRatios() {
    setFlag(false)
    val view = lithoTestRule.render {
      Column.create(c).visibleHandler(0.25f, 0.5f, null).build()
    }

    assertThat(requireNotNull(view.currentRootNode?.node).visibleWidthRatio).isEqualTo(0.25f)
    assertThat(requireNotNull(view.currentRootNode?.node).visibleHeightRatio).isEqualTo(0.5f)
  }

  @Test
  fun builder_flagOn_nullVisibilityHandlerStillAppliesRatios() {
    val view = lithoTestRule.render {
      Column.create(c).visibleHandler(0.25f, 0.5f, null).build()
    }

    assertThat(requireNotNull(view.currentRootNode?.node).visibleWidthRatio).isEqualTo(0.25f)
    assertThat(requireNotNull(view.currentRootNode?.node).visibleHeightRatio).isEqualTo(0.5f)
  }

  // ===== Equivalence with the Kotlin DSL (flag ON) =====

  @Test
  fun containerProps_equivalentToDsl() {
    val fromBuilder =
        Row.create(c)
            .alignItems(YogaAlign.CENTER)
            .justifyContent(YogaJustify.SPACE_BETWEEN)
            .reverse(true)
            .build() as KRow
    val fromDsl = KRowDsl(
        alignItems = YogaAlign.CENTER,
        justifyContent = YogaJustify.SPACE_BETWEEN,
        isReversed = true,
    )
    assertThat(fromBuilder.isEquivalentTo(fromDsl)).isTrue()
  }

  @Test
  fun eachStyleProp_wrapperReconcilesIdenticallyToDsl() {
    // The wrapper's setters call the SAME Style extensions as the Kotlin DSL, so a builder-built
    // KRow is structurally identical to the DSL-built one. This asserts they reconcile identically
    // per prop — i.e. the wrapper is never *worse* than the launched DSL path. (A few props such as
    // flex/alignSelf/backgroundColor don't structurally reconcile even via the DSL; the wrapper
    // must
    // match that, which is what this verifies.)
    val cases: List<Triple<String, (Row.Builder) -> Row.Builder, () -> KRow>> = listOf(
        Triple("widthPx", { b -> b.widthPx(100) }, { KRowDsl(style = Style.width(100.px)) }),
        Triple("heightPx", { b -> b.heightPx(50) }, { KRowDsl(style = Style.height(50.px)) }),
        Triple(
            "paddingPx",
            { b -> b.paddingPx(YogaEdge.ALL, 8) },
            { KRowDsl(style = Style.padding(all = 8.px)) },
        ),
        Triple("flexGrow", { b -> b.flexGrow(1f) }, { KRowDsl(style = Style.flex(grow = 1f)) }),
        Triple(
            "alignSelf",
            { b -> b.alignSelf(YogaAlign.CENTER) },
            { KRowDsl(style = Style.alignSelf(YogaAlign.CENTER)) },
        ),
        Triple(
            "backgroundColor",
            { b -> b.backgroundColor(0xFF112233.toInt()) },
            { KRowDsl(style = Style.backgroundColor(0xFF112233.toInt())) },
        ),
    )
    val mismatches = mutableListOf<String>()
    for ((name, viaBuilder, viaDsl) in cases) {
      val builderReconciles =
          (viaBuilder(Row.create(c)).build() as KRow).isEquivalentTo(
              viaBuilder(Row.create(c)).build() as KRow,
          )
      val dslReconciles = viaDsl().isEquivalentTo(viaDsl())
      if (builderReconciles != dslReconciles) {
        mismatches.add("$name (builder=$builderReconciles dsl=$dslReconciles)")
      }
    }
    assertThat(mismatches).isEmpty()
  }

  @Test
  fun singleStyleProp_equivalentToDsl() {
    val fromBuilder = Column.create(c).widthPx(100).build() as KColumn
    val fromDsl = KColumnDsl(style = Style.width(100.px))
    assertThat(fromBuilder.isEquivalentTo(fromDsl)).isTrue()
  }

  @Test
  fun emptyBuilder_equivalentToDsl() {
    assertThat((Row.create(c).build() as KRow).isEquivalentTo(KRowDsl())).isTrue()
  }

  // ===== flex shorthand =====

  /**
   * Two content-sized children in a 300px row, with 150px of free space to distribute. A `0px` flex
   * basis would ignore the content widths and split the row evenly; an `auto` basis grows each
   * child from its own content width.
   */
  private fun twoChildWidths(flex: Float): String {
    val root =
        lithoTestRule
            .render {
              Row.create(c)
                  .widthPx(300)
                  .child(Row.create(c).flex(flex).child(Row.create(c).widthPx(50).heightPx(10)))
                  .child(Row.create(c).flex(flex).child(Row.create(c).widthPx(100).heightPx(10)))
                  .build()
            }
            .currentRootNode!!
    return "${root.getChildAt(0).width}/${root.getChildAt(1).width}"
  }

  /** A single content-sized child in a 300px row, so the grow factor alone decides its width. */
  private fun soleChildWidth(flex: Float): Int =
      lithoTestRule
          .render {
            Row.create(c)
                .widthPx(300)
                .child(Row.create(c).flex(flex).child(Row.create(c).widthPx(50).heightPx(10)))
                .build()
          }
          .currentRootNode!!
          .getChildAt(0)
          .width

  @Test
  fun flexPositive_identicalInBothFlagStates() {
    // Mapping the shorthand to `grow` leaves basis `auto`, and so does the legacy path: Litho's
    // YogaConfig sets useWebDefaults, under which `Node.processFlexBasis` returns `auto` rather
    // than `0px` for a positive `flex` (LithoYogaFactory.createYogaConfig). A `0px` basis would
    // give 150/150 here.
    val flagOn = twoChildWidths(1f)
    setFlag(false)
    assertThat(flagOn).isEqualTo(twoChildWidths(1f)).isEqualTo("125/175")
  }

  @Test
  fun flexNegative_identicalInBothFlagStates() {
    // Guards the `flex < 0` mapping: routing a negative to `grow` puts a negative grow factor on
    // the node, which the legacy path never produces and which stretches the child instead of
    // leaving it at its content width.
    val flagOn = soleChildWidth(-1f)
    setFlag(false)
    assertThat(flagOn).isEqualTo(soleChildWidth(-1f)).isEqualTo(50)
  }

  // ===== Nullable DynamicValue setters =====

  /**
   * The base tolerates a null [DynamicValue] and real call sites rely on it — a Mockito double of a
   * non-null Kotlin property yields null, which is how `StickerCtaAnimator.scaleX` reaches
   * `StickerCtaComponent`. A non-null Kotlin override would add an
   * `Intrinsics.checkNotNullParameter` that runs before the flag is read, so this must hold in both
   * flag states.
   */
  private fun setEveryDynamicValueToNull(b: Row.Builder): Row.Builder =
      b.backgroundColor(null as DynamicValue<Int>?)
          .backgroundDynamicDrawable(null)
          .foregroundColor(null as DynamicValue<Int>?)
          .alpha(null as DynamicValue<Float>?)
          .rotation(null as DynamicValue<Float>?)
          .rotationX(null as DynamicValue<Float>?)
          .rotationY(null as DynamicValue<Float>?)
          .scaleX(null as DynamicValue<Float>?)
          .scaleY(null as DynamicValue<Float>?)
          .translationX(null as DynamicValue<Float>?)
          .translationY(null as DynamicValue<Float>?)
          .translationZ(null as DynamicValue<Float>?)
          .shadowElevation(null as DynamicValue<Float>?)

  private fun sizedRow() = Row.create(c).widthPx(10).heightPx(10)

  @Test
  fun nullDynamicValues_flagOn_areIgnored() {
    val withNulls = setEveryDynamicValueToNull(sizedRow()).build()

    assertThat(withNulls.isEquivalentTo(sizedRow().build())).isTrue()
  }

  /**
   * The legacy path forwards the nulls to `CommonProps` rather than dropping them, so unlike the K
   * path it is not equivalent to never having called the setters — only that it still renders.
   */
  @Test
  fun nullDynamicValues_flagOff_stillRender() {
    setFlag(false)

    val view = lithoTestRule.render { setEveryDynamicValueToNull(sizedRow()).build() }

    assertThat(view.currentRootNode?.width).isEqualTo(10)
  }

  // ===== No-Style-API sinks reach the node via the Style -> CommonProps path =====

  @Test
  fun visibilityOutputTag_reachesLithoNode() {
    val view = lithoTestRule.render {
      Column.create(c).visibilityOutputTag("occlusion-tag").widthPx(10).heightPx(10).build()
    }
    assertThat(view.currentRootNode?.node?.visibilityOutputTag).isEqualTo("occlusion-tag")
  }

  // ===== kotlinStyle merges into the Style instead of the dropped scratch CommonProps =====

  @Test
  fun kotlinStyle_carriesStyle_equivalentToDsl() {
    val fromBuilder = Column.create(c).kotlinStyle(Style.width(100.px)).build() as KColumn
    val fromDsl = KColumnDsl(style = Style.width(100.px))
    assertThat(fromBuilder.isEquivalentTo(fromDsl)).isTrue()
  }
}
