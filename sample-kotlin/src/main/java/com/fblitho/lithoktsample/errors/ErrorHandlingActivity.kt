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

package com.fblitho.lithoktsample.errors

import android.os.Bundle
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.litho.config.ComponentsConfiguration
import com.fblitho.lithoktsample.NavigatableDemoActivity

class ErrorHandlingActivity : NavigatableDemoActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // This feature is currently experimental and not enabled by default.
    ComponentsConfiguration.enableOnErrorHandling = true

    setContentView(
        LithoView.create(
            this,
            ErrorRootComponent.create(ComponentContext(this))
                .dataModels(DATA)
                .build()))
  }

  companion object {
    private val DATA = listOf(ListRow("First Title", "First Subtitle"),
        ListRow("Second Title", "Second Subtitle"), ListRow("Third Title", "Third Subtitle"),
        ListRow("Fourth Title", "Fourth Subtitle"), ListRow("Fifth Title", "Fifth Subtitle"),
        ListRow("Sixth Title", "Sixth Subtitle"), ListRow("Seventh Title", "Seventh Subtitle"))
  }
}
