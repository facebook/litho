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

package com.facebook.samples.litho.kotlin.primitives.widgets

import android.graphics.Color
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.assertj.LithoAssertions.assertThat
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.PrimitiveCard
import com.facebook.litho.widget.SolidColor
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [PrimitiveCard] */
@RunWith(LithoTestRunner::class)
class PrimitiveCardTest {

  @Rule @JvmField val lithoViewRule = LithoTestRule()

  @Test
  fun `PrimitiveCard should render with default props`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(100.px).height(50.px)),
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
    // Card should mount at least clip and shadow plus content
    assertThat(testLithoView.lithoView.mountItemCount).isGreaterThan(0)
  }

  @Test
  fun `PrimitiveCard should render with transparency enabled`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.BLUE, style = Style.width(50.px).height(50.px)),
          transparencyEnabled = true,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
    assertThat(testLithoView.lithoView.mountItemCount).isGreaterThan(0)
  }

  @Test
  fun `PrimitiveCard should render with custom background color`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
          cardBackgroundColor = Color.GREEN,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
  }

  @Test
  fun `PrimitiveCard should render with custom clipping and shadow colors`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
          clippingColor = Color.YELLOW,
          shadowStartColor = Color.BLACK,
          shadowEndColor = Color.TRANSPARENT,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
  }

  @Test
  fun `PrimitiveCard should render with cornerRadius and elevation`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
          cornerRadius = 8.dp,
          elevation = 4.dp,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
  }

  @Test
  fun `PrimitiveCard should render with px cornerRadius and elevation`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
          cornerRadius = 16.px,
          elevation = 8.px,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
  }

  @Test
  fun `PrimitiveCard should render with disableClip flags`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
          disableClipTopLeft = true,
          disableClipTopRight = true,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
  }

  @Test
  fun `PrimitiveCard should render with all disableClip flags`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
          disableClipTopLeft = true,
          disableClipTopRight = true,
          disableClipBottomLeft = true,
          disableClipBottomRight = true,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
  }

  @Test
  fun `PrimitiveCard should render with shadow overrides`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
          shadowTopOverride = 10.px,
          shadowBottomOverride = 12.px,
          shadowLeftOverride = 6.px,
          shadowRightOverride = 6.px,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
  }

  @Test
  fun `PrimitiveCard should render with SolidColor as content and no elevation`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.GRAY, style = Style.width(50.px).height(50.px)),
          elevation = 0.px,
          style = Style.width(200.px).height(100.px),
      )
    }

    // Even with 0 elevation (no shadow), card should still render clip and content
    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
    assertThat(testLithoView.lithoView.mountItemCount).isGreaterThan(0)
  }

  @Test
  fun `PrimitiveCard should render with transparent clipping and custom background`() {
    val testLithoView = lithoViewRule.render {
      PrimitiveCard(
          content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
          transparencyEnabled = true,
          clippingColor = Color.TRANSPARENT,
          cardBackgroundColor = Color.WHITE,
          cornerRadius = 12.dp,
          elevation = 2.dp,
          style = Style.width(200.px).height(100.px),
      )
    }

    assertThat(testLithoView.findComponent(PrimitiveCard::class)).isNotNull()
  }

  @Test
  fun `same instance should be equivalent`() {
    val component = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = Style.width(50.px).height(50.px)),
        style = Style.width(200.px).height(100.px),
    )

    assertThat(component).isEquivalentTo(component)
  }

  @Test
  fun `components with same prop values should be equivalent`() {
    val color = Color.RED
    val contentStyle = Style.width(50.px).height(50.px)
    val cardStyle = Style.width(200.px).height(100.px)

    val a = PrimitiveCard(
        content = SolidColor(color = color, style = contentStyle),
        cardBackgroundColor = Color.WHITE,
        cornerRadius = 8.dp,
        elevation = 4.dp,
        style = cardStyle,
    )

    val b = PrimitiveCard(
        content = SolidColor(color = color, style = contentStyle),
        cardBackgroundColor = Color.WHITE,
        cornerRadius = 8.dp,
        elevation = 4.dp,
        style = cardStyle,
    )

    assertThat(a).isEquivalentTo(b)
  }

  @Test
  fun `components with different cardBackgroundColor should not be equivalent`() {
    val contentStyle = Style.width(50.px).height(50.px)
    val cardStyle = Style.width(200.px).height(100.px)

    val a = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        cardBackgroundColor = Color.WHITE,
        style = cardStyle,
    )

    val b = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        cardBackgroundColor = Color.BLACK,
        style = cardStyle,
    )

    assertThatThrownBy { assertThat(a).isEquivalentTo(b) }.isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `components with different transparencyEnabled should not be equivalent`() {
    val contentStyle = Style.width(50.px).height(50.px)
    val cardStyle = Style.width(200.px).height(100.px)

    val a = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        transparencyEnabled = false,
        style = cardStyle,
    )

    val b = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        transparencyEnabled = true,
        style = cardStyle,
    )

    assertThatThrownBy { assertThat(a).isEquivalentTo(b) }.isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `components with different cornerRadius should not be equivalent`() {
    val contentStyle = Style.width(50.px).height(50.px)
    val cardStyle = Style.width(200.px).height(100.px)

    val a = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        cornerRadius = 4.dp,
        style = cardStyle,
    )

    val b = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        cornerRadius = 8.dp,
        style = cardStyle,
    )

    assertThatThrownBy { assertThat(a).isEquivalentTo(b) }.isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `components with different elevation should not be equivalent`() {
    val contentStyle = Style.width(50.px).height(50.px)
    val cardStyle = Style.width(200.px).height(100.px)

    val a = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        elevation = 2.dp,
        style = cardStyle,
    )

    val b = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        elevation = 8.dp,
        style = cardStyle,
    )

    assertThatThrownBy { assertThat(a).isEquivalentTo(b) }.isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `components with different style values should not be equivalent`() {
    val contentStyle = Style.width(50.px).height(50.px)

    val a = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        style = Style.width(200.px).height(100.px),
    )

    val b = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        style = Style.width(300.px).height(300.px),
    )

    assertThatThrownBy { assertThat(a).isEquivalentTo(b) }.isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `components with different disableClip flags should not be equivalent`() {
    val contentStyle = Style.width(50.px).height(50.px)
    val cardStyle = Style.width(200.px).height(100.px)

    val a = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        disableClipTopLeft = false,
        style = cardStyle,
    )

    val b = PrimitiveCard(
        content = SolidColor(color = Color.RED, style = contentStyle),
        disableClipTopLeft = true,
        style = cardStyle,
    )

    assertThatThrownBy { assertThat(a).isEquivalentTo(b) }.isInstanceOf(AssertionError::class.java)
  }
}
