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
package com.facebook.samples.lithocodelab.end;

import static android.graphics.Typeface.DEFAULT_BOLD;
import static android.graphics.Typeface.SERIF;
import static android.widget.Toast.LENGTH_SHORT;
import static com.facebook.yoga.YogaAlign.STRETCH;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaJustify.CENTER;

import android.graphics.Color;
import android.widget.Toast;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
import com.facebook.samples.lithocodelab.R;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

/**
 * Renders a "story card" with a grey box representing an image for the author, a title, subtitle,
 * and message text props. This also has a togglable "saved" state and a menu button which just
 * Toasts to indicate that the menu button was pressed.
 *
 * <p>This does most of the interesting stuff for the approximate end state for the lab activity.
 * The lab will almost entirely be spent reproducing this.
 */
@LayoutSpec
public class LithoLabStoryCardComponentSpec {

    private static final int CARD_INSET = 12;
    private static final int CARD_INTERNAL_PADDING = 7;

    @OnCreateLayout
    static ComponentLayout onCreateLayout(
            ComponentContext c,
            @Prop String title,
            @Prop String subtitle,
            @Prop String content,
            @State Boolean saved) {
        return Column.create(c)
                .backgroundColor(Color.WHITE)
                .child(Row.create(c)
                        .paddingDip(HORIZONTAL, CARD_INSET)
                        .paddingDip(TOP, CARD_INSET)
                        .child(SolidColor.create(c)
                                .colorRes(android.R.color.darker_gray)
                                .withLayout()
                                .widthDip(40)
                                .heightDip(40)
                                .marginDip(END, CARD_INTERNAL_PADDING)
                                .marginDip(BOTTOM, CARD_INTERNAL_PADDING))
                        .child(Column.create(c)
                                .flexGrow(1f)
                                .child(Text.create(c)
                                        .text(title)
                                        .textSizeSp(18)
                                        .typeface(DEFAULT_BOLD)
                                        .withLayout()
                                        .paddingDip(BOTTOM, CARD_INTERNAL_PADDING))
                                .child(Text.create(c)
                                        .text(subtitle)
                                        .textSizeSp(8)
                                        .textColor(Color.GRAY)
                                        .withLayout()
                                        .paddingDip(BOTTOM, CARD_INTERNAL_PADDING)))
                        .child(Image.create(c)
                                .drawableRes(R.drawable.menu)
                                .withLayout()
                                .clickHandler(LithoLabStoryCardComponent.onClickMenuButton(c))
                                .widthDip(15)
                                .heightDip(15)
                                .marginDip(START, CARD_INTERNAL_PADDING)
                                .marginDip(BOTTOM, CARD_INTERNAL_PADDING))
                )
                .child(Text.create(c)
                        .text(content)
                        .textSizeSp(22)
                        .typeface(SERIF)
                        .withLayout()
                        .paddingDip(HORIZONTAL, CARD_INSET)
                        .paddingDip(BOTTOM, CARD_INTERNAL_PADDING))
                .child(Row.create(c)
                        .backgroundColor(saved ? Color.BLUE : Color.TRANSPARENT)
                        .alignSelf(STRETCH)
                        .paddingDip(HORIZONTAL, CARD_INSET)
                        .paddingDip(BOTTOM, CARD_INTERNAL_PADDING)
                        .paddingDip(TOP, CARD_INTERNAL_PADDING)
                        .justifyContent(CENTER)
                        .child(Image.create(c)
                                .drawableRes(R.drawable.save)
                                .withLayout()
                                .alignSelf(YogaAlign.CENTER)
                                .widthDip(20)
                                .heightDip(20)
                                .marginDip(END, CARD_INTERNAL_PADDING))
                        .child(Text.create(c)
                                .text("Save")
                                .textSizeSp(14))
                        .clickHandler(LithoLabStoryCardComponent.onClickSave(c))
                        .borderColor(Color.BLACK)
                        .borderWidthDip(YogaEdge.TOP, 1))
                .borderWidthDip(YogaEdge.ALL, 1)
                .borderColor(Color.BLACK)
                .build();
    }

    @OnCreateInitialState
    static void onCreateInitialState(
            ComponentContext c,
            StateValue<Boolean> saved) {
        saved.set(false);
    }

    @OnUpdateState
    static void onToggleSavedState(StateValue<Boolean> saved) {
        saved.set(!saved.get());
    }

    @OnEvent(ClickEvent.class)
    static void onClickSave(
            ComponentContext c) {
        LithoLabStoryCardComponent.onToggleSavedState(c);
    }

    @OnEvent(ClickEvent.class)
    static void onClickMenuButton(
            ComponentContext c) {
        Toast.makeText(c.getApplicationContext(), "Menu button clicked.", LENGTH_SHORT).show();
    }
}
