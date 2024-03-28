// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho

import android.content.Context
import android.view.View
import com.facebook.litho.binders.viewBinder
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.RenderUnit
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test focuses on testing the behavior around
 * [ComponentsConfiguration.unmountOnDetachedFromWindow].
 */
@RunWith(LithoTestRunner::class)
class UnmountOnDetachTest {

  @get:Rule val lithoRule = LithoViewRule()

  @Test
  fun `should not unmount on detach when unmount on detach is disabled`() {
    var mountCount = 0
    var unmountCount = 0
    val component =
        UnmountOnDetachComponent(
            "Mount Text", onBind = { mountCount++ }, onUnbind = { unmountCount++ })

    val lithoView =
        lithoRule.render(
            componentTree =
                ComponentTree.create(lithoRule.context)
                    .componentsConfiguration(
                        ComponentsConfiguration.defaultInstance.copy(
                            unmountOnDetachedFromWindow = false))
                    .build()) {
              component
            }

    LithoAssertions.assertThat(lithoView).hasVisibleText("Mount Text")

    lithoView.detachFromWindow()

    Assertions.assertThat(mountCount).isEqualTo(1)
    Assertions.assertThat(unmountCount).isEqualTo(0)
  }

  @Test
  fun `should unmount on detach when unmount on detach is enabled`() {
    var mountCount = 0
    var unmountCount = 0
    val component =
        UnmountOnDetachComponent(
            "Mount Text", onBind = { mountCount++ }, onUnbind = { unmountCount++ })

    val lithoView =
        lithoRule.render(
            componentTree =
                ComponentTree.create(lithoRule.context)
                    .componentsConfiguration(
                        ComponentsConfiguration.defaultInstance.copy(
                            unmountOnDetachedFromWindow = true))
                    .build()) {
              component
            }

    LithoAssertions.assertThat(lithoView).hasVisibleText("Mount Text")

    lithoView.detachFromWindow()

    Assertions.assertThat(mountCount).isEqualTo(1)
    Assertions.assertThat(unmountCount).isEqualTo(1)
  }

  private class UnmountOnDetachComponent(
      private val name: String,
      private val onBind: () -> Unit,
      private val onUnbind: () -> Unit
  ) : KComponent() {

    override fun ComponentScope.render(): Component? {
      return Text(
          text = name,
          style =
              Style.viewBinder(
                  RenderUnit.DelegateBinder.createDelegateBinder(
                      Unit,
                      object : RenderUnit.Binder<Any, View, Any> {
                        override fun shouldUpdate(
                            currentModel: Any,
                            newModel: Any,
                            currentLayoutData: Any?,
                            nextLayoutData: Any?
                        ): Boolean = false

                        override fun bind(
                            context: Context,
                            content: View,
                            model: Any,
                            layoutData: Any?
                        ): Any? {
                          onBind()
                          return Unit
                        }

                        override fun unbind(
                            context: Context,
                            content: View,
                            model: Any,
                            layoutData: Any?,
                            bindData: Any?
                        ) {
                          onUnbind()
                        }
                      })))
    }
  }
}
