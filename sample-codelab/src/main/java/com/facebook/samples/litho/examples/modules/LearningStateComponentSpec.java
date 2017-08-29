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
package com.facebook.samples.litho.examples.modules;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;

import static com.facebook.yoga.YogaAlign.STRETCH;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.TOP;

@LayoutSpec
public class LearningStateComponentSpec {

    @OnCreateInitialState
    static void onCreateInitialState(
            ComponentContext c,
            StateValue<Integer> count) {
        count.set(0);
    }

    @OnCreateLayout
    static ComponentLayout onCreateLayout(
            ComponentContext c,
            @State Integer count) {
        return Text.create(c)
                .text("Clicked " + count + " times.")
                .textSizeDip(50)
                .withLayout()
                .clickHandler(LearningStateComponent.onClick(c))
                .backgroundRes(android.R.color.holo_blue_light)
                .alignSelf(STRETCH)
                .paddingDip(BOTTOM, 20)
                .paddingDip(TOP, 40)
                .build();
    }

    @OnUpdateState
    static void incrementClickCount(StateValue<Integer> count) {
        count.set(count.get() + 1);
    }

    @OnEvent(ClickEvent.class)
    static void onClick(
            ComponentContext c) {
        LearningStateComponent.incrementClickCount(c);
    }
}
