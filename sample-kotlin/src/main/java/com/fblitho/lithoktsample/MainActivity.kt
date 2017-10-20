/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.fblitho.lithoktsample

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.fblitho.lithoktsample.lithography.data.DataFetcher
import com.fblitho.lithoktsample.lithography.data.DecadeViewModel
import com.fblitho.lithoktsample.lithography.data.Model
import com.fblitho.lithoktsample.lithography.sections.LithoFeedSection


class MainActivity : AppCompatActivity() {

  private val sectionContext: SectionContext by lazy { SectionContext(this) }
  private val lithoView: LithoView by lazy { LithoView(sectionContext) }
  private val fetcher: DataFetcher by lazy { DataFetcher(viewModel.model) }
  private val viewModel: DecadeViewModel by lazy {
    ViewModelProviders.of(this).get(DecadeViewModel::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.init()
    viewModel.model.observe(this, Observer { model -> setList(model!!) })
    setContentView(lithoView)
  }

  private fun setList(model: Model) {

    if (lithoView.componentTree == null) {
      lithoView.setComponent(createRecyclerComponent(model))
    } else {
      lithoView.setComponentAsync(createRecyclerComponent(model))
    }
  }

  private fun createRecyclerComponent(model: Model): Component<RecyclerCollectionComponent> {

    return RecyclerCollectionComponent
        .create(sectionContext)
        .section(LithoFeedSection.create(sectionContext)
            .decades(model.decades)
            .fetcher(fetcher)
            .loading(model.isLoading)
            .build())
        .disablePTR(true)
        .build()
  }
}
