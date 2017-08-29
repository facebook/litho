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
package com.facebook.samples.lithocodelab.examples.modules;

import android.support.v7.widget.OrientationHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

@LayoutSpec
public class LearningRecyclerBinderComponentSpec {
    @OnCreateLayout
    static ComponentLayout onCreateLayout(
            ComponentContext c) {
        final RecyclerBinder recyclerBinder = new RecyclerBinder.Builder()
                .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.VERTICAL, false))
                .build(c);

        for (int i = 0; i < 32; i++) {
            recyclerBinder.insertItemAt(
                    i,
                    LearningPropsComponent.create(c)
                            .text1("Item: " + i)
                            .text2("Item: " + i)
                            .build());
        }

        return Recycler.create(c)
                .binder(recyclerBinder)
                .buildWithLayout();
    }
}
