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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnError
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.yoga.YogaEdge
import java.util.Optional

@LayoutSpec
object ErrorBoundarySpec {

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop child: Component,
      @State error: Optional<Exception>
  ): Component = if (error.isPresent) {
    Column.create(c)
        .marginDip(YogaEdge.ALL, 16f)
        .child(
            DebugErrorComponent.create(c)
                .message("Error Boundary")
                .throwable(error.get())
                .build())
        .build()
  } else {
    child
  }

  @OnCreateInitialState
  fun createInitialState(c: ComponentContext, error: StateValue<Optional<Exception>>) {
    error.set(Optional.empty())
  }

  @OnUpdateState
  fun updateError(error: StateValue<Optional<Exception>>, @Param e: Exception) {
    error.set(Optional.of(e))
  }

  @OnError
  fun onError(c: ComponentContext, error: Exception) {
    ErrorBoundary.updateError(c, error)
  }
}
