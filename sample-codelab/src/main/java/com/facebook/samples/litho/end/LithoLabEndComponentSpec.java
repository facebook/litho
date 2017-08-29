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
package com.facebook.samples.litho.end;

import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;

@LayoutSpec
public class LithoLabEndComponentSpec {
    @OnCreateLayout
    static ComponentLayout onCreateLayout(
            ComponentContext c) {
        return Column.create(c)
                .backgroundRes(android.R.color.darker_gray)
                .child(LithoLabStoryCardComponent.create(c)
                        .title("Title")
                        .subtitle("Subtitle")
                        .content("This is some test content. It should fill at least one line. " +
                                "This is a story card. You can interact with the menu button " +
                                "and save button."))
                .build();
    }
}
