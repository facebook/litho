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

package com.fblitho.lithoktsample.lithography.sections

import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.annotations.OnViewportChanged
import com.facebook.litho.sections.common.SingleComponentSection
import com.fblitho.lithoktsample.lithography.components.LoadingComponent
import com.fblitho.lithoktsample.lithography.data.Decade
import com.fblitho.lithoktsample.lithography.data.Fetcher

@GroupSectionSpec
object LithoFeedSectionSpec {
  @OnCreateChildren
  fun onCreateChildren(c: SectionContext,
                       @Prop decades: List<Decade>,
                       @Prop loading: Boolean): Children {
    val children = Children.create()

    decades.forEach {
      children.child(
          DecadeSection.create(c)
              .decade(it)
              .key(it.year.toString())
              .build())
    }

    if (loading) {
      children.child(
          SingleComponentSection.create(c).
              component(LoadingComponent.create(c).build()))
    }

    return children.build()
  }

  @OnViewportChanged
  fun onViewportChanged(
      c: SectionContext,
      firstVisile: Int,
      lastVisible: Int,
      totalCount: Int,
      fistFullyVisible: Int,
      lastFullyVisible: Int,
      @Prop fetcher: Fetcher,
      @Prop decades: List<Decade>) {
    val threshold = 2
    if (totalCount - lastVisible < threshold) {
      fetcher.fetchMoreData(decades.last().year)
    }
  }
}