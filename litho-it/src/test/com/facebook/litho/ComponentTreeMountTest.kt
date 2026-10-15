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
import android.graphics.Color
import android.graphics.Color.BLACK
import android.graphics.Color.YELLOW
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.testing.TestTransitionComponent
import com.facebook.litho.testing.eventhandler.EventHandlerTestHelper
import com.facebook.litho.testing.eventhandler.EventHandlerTestHelper.MockEventHandler
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.litho.widget.SolidColor
import com.facebook.litho.widget.Text
import com.facebook.rendercore.dp
import com.facebook.rendercore.extensions.VisibilityChangeReason
import com.facebook.rendercore.visibility.VisibleEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowView

@RunWith(LithoTestRunner::class)
class ComponentTreeMountTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testRemountsWithNewInputOnSameLayout() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            SimpleMountSpecTester(color = Color.BLACK),
        )
    (Shadow.extract(lithoView) as ShadowView).callOnAttachedToWindow()
    assertThat(lithoView.drawables).hasSize(1)
    assertThat((lithoView.drawables[0] as ColorDrawable).color).isEqualTo(Color.BLACK)
    lithoView.componentTree?.root = SimpleMountSpecTester(color = Color.YELLOW)
    assertThat(lithoView.drawables).hasSize(1)
    assertThat((lithoView.drawables[0] as ColorDrawable).color).isEqualTo(Color.YELLOW)
  }

  @Test
  fun testReentrantMounts() {
    val lithoView = ComponentTestHelper.mountComponent(context, EmptyComponent(), true, true)
    val visibleEventHandler =
        EventHandlerTestHelper.createMockEventHandler(
            VisibleEvent::class.java,
            MockEventHandler<VisibleEvent?, Void?> {
              lithoView.setComponent(
                  TestTransitionComponent.create(
                          context,
                          Row.create(context)
                              .child(Text.create(context).text("text").textSizeDip(20f))
                              .child(
                                  SolidColor(
                                      color = Color.BLUE,
                                      style = Style.width(20.dp).height(20.dp),
                                  ),
                              )
                              .build(),
                      )
                      .build(),
              )
              null
            },
        )
    lithoView.setComponent(
        TestTransitionComponent.create(
                context,
                Column.create(context)
                    .child(
                        SolidColor(
                            color = Color.YELLOW,
                            style = Style.width(10.dp).height(10.dp),
                        ),
                    )
                    .child(
                        SolidColor(
                            color = Color.GREEN,
                            style = Style.width(10.dp).height(10.dp),
                        ),
                    )
                    .child(
                        SolidColor(
                            color = Color.GRAY,
                            style = Style.width(10.dp).height(10.dp),
                        ),
                    )
                    .build(),
            )
            .visibleHandler(visibleEventHandler)
            .build(),
    )
    lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 100, 100), true, VisibilityChangeReason.UNKNOWN)
  }
}
