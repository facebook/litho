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

import com.fblitho.lithoktsample.errors.ErrorHandlingActivity
import com.fblitho.lithoktsample.lithography.LithographyActivity

object DataModels {

  val DATA_MODELS = listOf(
      DemoListDataModel("Lithography", LithographyActivity::class.java),
      DemoListDataModel("Error boundaries", ErrorHandlingActivity::class.java))

  fun getDataModels(index: Int): List<DemoListDataModel> =
      if (index == -1) {
        DATA_MODELS
      } else {
        DATA_MODELS[index].datamodels ?: emptyList()
      }
}
