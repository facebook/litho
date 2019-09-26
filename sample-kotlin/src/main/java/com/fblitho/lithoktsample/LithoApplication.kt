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

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.litho.ComponentsReporter
import com.facebook.soloader.SoLoader
import com.fblitho.lithoktsample.logging.SampleComponentsReporter

class LithoApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    Fresco.initialize(this)
    SoLoader.init(this, false)
    ComponentsReporter.provide(SampleComponentsReporter())
  }
}
