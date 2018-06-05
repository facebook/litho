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

import com.fblitho.lithoktsample.animations.animatedbadge.AnimatedBadgeActivity
import com.fblitho.lithoktsample.animations.animationcomposition.ComposedAnimationsActivity
import com.fblitho.lithoktsample.animations.bounds.BoundsAnimationActivity
import com.fblitho.lithoktsample.animations.expandableelement.ExpandableElementActivity
import com.fblitho.lithoktsample.bordereffects.BorderEffectsActivity
import com.fblitho.lithoktsample.errors.ErrorHandlingActivity
import com.fblitho.lithoktsample.lithography.LithographyActivity

object DataModels {

  val DATA_MODELS = listOf(
      DemoListDataModel(
          name = "Lithography",
          klass = LithographyActivity::class.java
      ),
      DemoListDataModel(
          name = "Border effects",
          klass = BorderEffectsActivity::class.java
      ),
      DemoListDataModel(
          name = "Error boundaries",
          klass = ErrorHandlingActivity::class.java
      ),
      DemoListDataModel(
          name = "Animations",
          datamodels = listOf(
              DemoListDataModel(
                  name = "Animations Composition",
                  klass = ComposedAnimationsActivity::class.java
              ),
              DemoListDataModel(
                  name = "Expandable Element",
                  klass = ExpandableElementActivity::class.java
              ),
              DemoListDataModel(
                  name = "Animated Badge",
                  klass = AnimatedBadgeActivity::class.java
              ),
              DemoListDataModel(
                  name = "Bounds Animation",
                  klass = BoundsAnimationActivity::class.java)
          )
      )
  )

  fun getDataModels(indices: IntArray?): List<DemoListDataModel> {
    if (indices == null) {
      return DATA_MODELS
    }

    var dataModels: List<DemoListDataModel> = DATA_MODELS

    indices.forEach {
      dataModels = dataModels[it].datamodels ?: dataModels
    }

    return dataModels
  }
}
