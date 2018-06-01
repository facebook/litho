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

package com.fblitho.lithoktsample.demo

import android.os.Bundle
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.fblitho.lithoktsample.NavigatableDemoActivity

class DemoListActivity : NavigatableDemoActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val index = intent.getIntExtra(INDEX, DEFAULT_INDEX)
    val dataModels = DataModels.getDataModels(index)

    val componentContext = ComponentContext(this)
    setContentView(
        LithoView.create(
            this,
            DemoListComponent.create(componentContext)
                .dataModels(dataModels)
                .build()))
  }

  companion object {
    const val INDEX = "INDEX"
    const val DEFAULT_INDEX = -1
  }
}
