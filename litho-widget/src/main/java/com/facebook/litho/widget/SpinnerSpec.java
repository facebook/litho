/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.widget;

import static com.facebook.yoga.YogaAlign.CENTER;
import static com.facebook.yoga.YogaEdge.START;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import com.facebook.litho.AccessibilityRole;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaJustify;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A simple spinner (dropdown) component. Derived from the standard Android {@link
 * android.widget.Spinner}
 *
 * <p>Additionally added logic to flip the caret vertically once menu is shown.
 *
 * <p>If no optional values are provided the component will look like it's material design
 * counterpart.
 *
 * @uidocs https://fburl.com/Spinner:3bf4
 * @prop-required selectedOption The initially selected option for the spinner
 * @prop-required options The options available from the dropdown
 * @prop-required onItemSelectedListener The listener for dropdown selections
 * @prop-optional itemLayout The item layout for the drop down list
 *     android.R.layout.simple_dropdown_item_1line is used by default
 * @prop-optional caret The spinner caret icon i.e. arrow at the far right. Notice that this
 *     drawable will be flipped vertically when the dropdown menu is shown
 * @prop-optional selectedTextSize The text size of the selected value
 * @prop-optional selectedTextColor The text color of the selected value
 */
@LayoutSpec(events = ItemSelectedEvent.class)
@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
public class SpinnerSpec {

  private static final float MARGIN_SMALL = 8;

  private static final int DEFAULT_CARET_COLOR = 0x8A000000; // 54% Black
  private static final int DEFAULT_TEXT_SIZE_SP = 16;
  private static final int SPINNER_HEIGHT = 48;

  @PropDefault static final int itemLayout = android.R.layout.simple_dropdown_item_1line;
  @PropDefault static final float selectedTextSize = -1;
  @PropDefault static final int selectedTextColor = 0xDE000000; // 87% Black

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c, @Prop String selectedOption, StateValue<String> selection) {
    selection.set(selectedOption);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State String selection,
      @State boolean isShowingDropDown,
      @Prop(resType = ResType.DIMEN_TEXT, optional = true) float selectedTextSize,
      @Prop(resType = ResType.COLOR, optional = true) int selectedTextColor,
      @Prop(resType = ResType.DRAWABLE, optional = true) @Nullable Drawable caret) {
    assertAPI11orHigher();
    caret = caret == null ? new CaretDrawable(c.getAndroidContext(), DEFAULT_CARET_COLOR) : caret;
    selectedTextSize =
        selectedTextSize == -1
            ? spToPx(c.getAndroidContext(), DEFAULT_TEXT_SIZE_SP)
            : selectedTextSize;

    return Row.create(c)
        .minHeightDip(SPINNER_HEIGHT)
        .justifyContent(YogaJustify.SPACE_BETWEEN)
        .paddingDip(START, MARGIN_SMALL)
        .backgroundAttr(android.R.attr.selectableItemBackground)
        .clickHandler(Spinner.onClick(c))
        .child(createSelectedItemText(c, selection, (int) selectedTextSize, selectedTextColor))
        .child(createCaret(c, caret, isShowingDropDown))
        .accessibilityRole(AccessibilityRole.DROP_DOWN_LIST)
        .build();
  }

  private static void assertAPI11orHigher() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
      throw new RuntimeException("Spinner requires API 11 (HONEYCOMB) or greater");
    }
  }

  private static Component createCaret(
      ComponentContext c, Drawable icon, boolean isShowingDropDown) {
    return Image.create(c)
        .drawable(icon)
        .widthDip(SPINNER_HEIGHT)
        .heightDip(SPINNER_HEIGHT)
        .flexShrink(0)
        .flexGrow(0)
        .scale(isShowingDropDown ? -1 : 1)
        .build();
  }

  private static Component createSelectedItemText(
      ComponentContext c, String selection, int textSizePx, @ColorInt int textColor) {
    return Text.create(c)
        .text(selection)
        .alignSelf(CENTER)
        .textSizePx(textSizePx)
        .textColor(textColor)
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      final ComponentContext c,
      @FromEvent final View view,
      @Prop final List<String> options,
      @Prop(resType = ResType.INT, optional = true) int itemLayout) {
    final EventHandler eventHandler = Spinner.getItemSelectedEventHandler(c);
    final ListPopupWindow popup = new ListPopupWindow(c.getAndroidContext());
    popup.setAnchorView(view);
    popup.setModal(true);
    popup.setPromptPosition(ListPopupWindow.POSITION_PROMPT_ABOVE);
    popup.setAdapter(new ArrayAdapter<>(c.getAndroidContext(), itemLayout, options));
    popup.setOnItemClickListener(
        new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final String newSelection = options.get(position);
            if (eventHandler != null) {
              Spinner.dispatchItemSelectedEvent(eventHandler, newSelection);
            }
            popup.dismiss();
            Spinner.updateSelectionSync(c, newSelection);
          }
        });
    popup.setOnDismissListener(
        new PopupWindow.OnDismissListener() {
          @Override
          public void onDismiss() {
            Spinner.updateIsShowingDropDownSync(c, false);
          }
        });
    popup.show();
    Spinner.updateIsShowingDropDownSync(c, true);
  }

  @OnUpdateState
  static void updateSelection(StateValue<String> selection, @Param String newSelection) {
    selection.set(newSelection);
  }

  @OnUpdateState
  static void updateIsShowingDropDown(
      StateValue<Boolean> isShowingDropDown, @Param boolean isShowing) {
    isShowingDropDown.set(isShowing);
  }

  private static float spToPx(Context context, int spValue) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, spValue, context.getResources().getDisplayMetrics());
  }

  private static float dpToPx(Context context, int dpValue) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
  }

  /** Draws a simple triangle caret depicting if the Spinner is expanded or collapsed. */
  private static class CaretDrawable extends Drawable {

    private static final int CARET_WIDTH_DP = 5;
    private static final int CARET_HEIGHT_DP = 3;

    private final Paint paint = new Paint();
    private final int mWidth;
    private final int mHeight;

    // Triangle geometry
    private final Path mTrianglePath = new Path();
    private final Point mP1 = new Point();
    private final Point mP2 = new Point();
    private final Point mP3 = new Point();

    public CaretDrawable(Context context, @ColorInt int caretColor) {
      paint.setColor(caretColor);
      paint.setFlags(Paint.ANTI_ALIAS_FLAG);
      mWidth = (int) dpToPx(context, CARET_WIDTH_DP);
      mHeight = (int) dpToPx(context, CARET_HEIGHT_DP);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
      super.onBoundsChange(bounds);
      final int cx = bounds.centerX();
      final int cy = bounds.centerY();

      // Setup points
      mP1.set(cx - mWidth, cy - mHeight);
      mP2.set(cx + mWidth, cy - mHeight);
      mP3.set(cx, cy + mHeight);

      // Setup triangle
      mTrianglePath.reset();
      mTrianglePath.setFillType(Path.FillType.EVEN_ODD);
      mTrianglePath.moveTo(mP1.x, mP1.y);
      mTrianglePath.lineTo(mP2.x, mP2.y);
      mTrianglePath.lineTo(mP3.x, mP3.y);
      mTrianglePath.close();
    }

    @Override
    public void draw(Canvas canvas) {
      canvas.drawPath(mTrianglePath, paint);
    }

    @Override
    public void setAlpha(int alpha) {
      throw new RuntimeException("Not supported");
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
      throw new RuntimeException("Not supported");
    }

    @Override
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }
  }
}
