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
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.viewcompat.ViewBinder
import com.facebook.litho.viewcompat.ViewCreator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests [ViewCompatComponent] */
@RunWith(LithoTestRunner::class)
class ViewCompatComponentTest {

  private lateinit var context: ComponentContext

  private val textViewCreator: ViewCreator<TextView> = ViewCreator { c, _ -> TextView(c) }
  private val textViewCreator2: ViewCreator<TextView> = ViewCreator { c, _ -> TextView(c) }
  private val noOpViewBinder: ViewBinder<TextView> =
      object : ViewBinder<TextView> {
        override fun prepare() = Unit

        override fun bind(view: TextView) = Unit

        override fun unbind(view: TextView) = Unit
      }

  @Before
  fun setUp() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testSimpleRendering() {
    val binder =
        object : ViewBinder<TextView> {
          override fun prepare() = Unit

          override fun bind(view: TextView) {
            view.text = "Hello World!"
          }

          override fun unbind(view: TextView) = Unit
        }
    val lithoView =
        ComponentTestHelper.mountComponent(
            ViewCompatComponent.get(textViewCreator, "TextView").create(context).viewBinder(binder))
    assertThat(lithoView.mountItemCount).isEqualTo(1)
    val view = lithoView.getMountItemAt(0).content as TextView
    assertThat(view.text).isEqualTo("Hello World!")
  }

  @Test
  fun testPrepare() {
    val binder =
        object : ViewBinder<TextView> {
          private var state: String? = null

          override fun prepare() {
            state = "Hello World!"
          }

          override fun bind(view: TextView) {
            view.text = state
          }

          override fun unbind(view: TextView) = Unit
        }
    val lithoView =
        ComponentTestHelper.mountComponent(
            ViewCompatComponent.get(textViewCreator, "TextView").create(context).viewBinder(binder))
    assertThat(lithoView.mountItemCount).isEqualTo(1)
    val view = lithoView.getMountItemAt(0).content as TextView
    assertThat(view.text).isEqualTo("Hello World!")
  }

  @Test
  fun testUnbind() {
    val binder =
        object : ViewBinder<TextView> {
          private lateinit var state: String

          override fun prepare() {
            state = "Hello World!"
          }

          override fun bind(view: TextView) {
            view.text = state
          }

          override fun unbind(view: TextView) {
            view.text = ""
          }
        }
    val lithoView =
        ComponentTestHelper.mountComponent(
            ViewCompatComponent.get(textViewCreator, "TextView").create(context).viewBinder(binder))
    ComponentTestHelper.unbindComponent(lithoView)
    assertThat(lithoView.mountItemCount).isEqualTo(1)
    val view = lithoView.getMountItemAt(0).content as TextView
    assertThat(view.text).isEqualTo("")
  }

  @Test
  fun testTypeIdForDifferentViewCreators() {
    val compatComponent: ViewCompatComponent<*> =
        ViewCompatComponent.get(textViewCreator, "compat")
            .create(context)
            .viewBinder(noOpViewBinder)
            .build()
    val sameCompatComponent: ViewCompatComponent<*> =
        ViewCompatComponent.get(textViewCreator, "sameCompat")
            .create(context)
            .viewBinder(noOpViewBinder)
            .build()
    val differentCompatComponent: ViewCompatComponent<*> =
        ViewCompatComponent.get(textViewCreator2, "differentCompat")
            .create(context)
            .viewBinder(noOpViewBinder)
            .build()
    assertThat(compatComponent.id).isNotEqualTo(sameCompatComponent.id)
    assertThat(compatComponent.id).isNotEqualTo(differentCompatComponent.id)
    assertThat(sameCompatComponent.id).isNotEqualTo(differentCompatComponent.id)
    assertThat(compatComponent.typeId).isEqualTo(sameCompatComponent.typeId)
    assertThat(compatComponent.typeId).isNotEqualTo(differentCompatComponent.typeId)
  }

  @Test
  fun testTypeIdForSameViewCreatorTypeButDifferentInstances() {
    val textViewCreator1 = CustomViewCreator("textviewcreator1")
    val textViewCreator2 = CustomViewCreator("textviewcreator2")
    val compatComponent: ViewCompatComponent<*> =
        ViewCompatComponent.get(textViewCreator1, "compat")
            .create(context)
            .viewBinder(noOpViewBinder)
            .build()
    val sameCompatComponent: ViewCompatComponent<*> =
        ViewCompatComponent.get(textViewCreator1, "sameCompat")
            .create(context)
            .viewBinder(noOpViewBinder)
            .build()
    val differentCompatComponent: ViewCompatComponent<*> =
        ViewCompatComponent.get(textViewCreator2, "differentCompat")
            .create(context)
            .viewBinder(noOpViewBinder)
            .build()
    assertThat(compatComponent.id).isNotEqualTo(sameCompatComponent.id)
    assertThat(compatComponent.id).isNotEqualTo(differentCompatComponent.id)
    assertThat(sameCompatComponent.id).isNotEqualTo(differentCompatComponent.id)
    assertThat(compatComponent.typeId).isEqualTo(sameCompatComponent.typeId)
    assertThat(compatComponent.typeId).isNotEqualTo(differentCompatComponent.typeId)
  }

  private inner class CustomViewCreator constructor(private val text: String) :
      ViewCreator<TextView> {
    override fun createView(c: Context, parent: ViewGroup): TextView {
      return TextView(c).apply { this.text = text }
    }
  }
}
