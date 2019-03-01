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

package com.fblitho.lithoktsample.lithography

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import com.facebook.litho.Component
import com.facebook.litho.LithoView
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.fblitho.lithoktsample.NavigatableDemoActivity
import com.fblitho.lithoktsample.lithography.data.DataFetcher
import com.fblitho.lithoktsample.lithography.data.DecadeViewModel
import com.fblitho.lithoktsample.lithography.data.Model
import com.fblitho.lithoktsample.lithography.sections.LithoFeedSection

class LithographyActivity : NavigatableDemoActivity() {

  private val sectionContext: SectionContext by lazy { SectionContext(this) }
  private val lithoView: LithoView by lazy { LithoView(sectionContext) }
  private val fetcher: DataFetcher by lazy { DataFetcher(viewModel.model) }
  private val viewModel: DecadeViewModel by lazy {
    ViewModelProviders.of(this).get(DecadeViewModel::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.model.observe(this, Observer { setList(it) })
    setContentView(lithoView)
  }

  private fun setList(model: Model?) {
    model?.let {
      if (lithoView.componentTree == null) {
        lithoView.setComponent(createRecyclerComponent(it))
      } else {
        lithoView.setComponentAsync(createRecyclerComponent(it))
      }
    }
  }

  private fun createRecyclerComponent(model: Model): Component =
      RecyclerCollectionComponent
          .create(sectionContext)
          .section(
              LithoFeedSection.create(sectionContext)
                  .decades(model.decades)
                  .fetcher(fetcher)
                  .loading(model.isLoading)
                  .build())
          .disablePTR(true)
          .build()
}
