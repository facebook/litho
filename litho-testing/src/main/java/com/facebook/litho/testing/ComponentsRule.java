/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.testing;

import android.app.Activity;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.R;
import com.facebook.litho.StateHandler;
import com.facebook.litho.testing.assertj.LithoRepresentation;
import org.assertj.core.api.Assertions;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.Robolectric;

public class ComponentsRule implements TestRule {

  private ComponentContext mContext;

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        final Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        mContext = new ComponentContext(activity, new StateHandler());
        setComponentStyleableAttributes();

        Assertions.useRepresentation(new LithoRepresentation(mContext));

        try {
          base.evaluate();
        } finally {
          Assertions.useDefaultRepresentation();
        }
      }
    };
  }

  /** Get a Component Context for this test instance. */
  public ComponentContext getContext() {
    return mContext;
  }

  /**
   * TODO natthu: this is just a hack around a BUCK issue whereby BUCK only generates random array
   * values. Component tests require the correct values for the ComponentLayout styleable array in
   * order to bind props to the layout. The array that we define here needs to be kept up-to-date
   * with the attrs defined in litho-core/src/main/res/*attrs.xml files.
   */
  static void setComponentStyleableAttributes() {

    System.arraycopy(
        new int[] {
          R.attr.flex_direction,
          R.attr.flex_layoutDirection,
          R.attr.flex_justifyContent,
          R.attr.flex_alignItems,
          R.attr.flex_alignSelf,
          R.attr.flex_positionType,
          R.attr.flex_wrap,
          R.attr.flex_left,
          R.attr.flex_top,
          R.attr.flex_right,
          R.attr.flex_bottom,
          R.attr.flex,
          android.R.attr.layout_width,
          android.R.attr.layout_height,
          android.R.attr.padding,
          android.R.attr.paddingLeft,
          android.R.attr.paddingTop,
          android.R.attr.paddingRight,
          android.R.attr.paddingBottom,
          android.R.attr.paddingStart,
          android.R.attr.paddingEnd,
          android.R.attr.layout_margin,
          android.R.attr.layout_marginLeft,
          android.R.attr.layout_marginTop,
          android.R.attr.layout_marginRight,
          android.R.attr.layout_marginBottom,
          android.R.attr.layout_marginStart,
          android.R.attr.layout_marginEnd,
          android.R.attr.contentDescription,
          android.R.attr.background,
          android.R.attr.foreground,
          android.R.attr.minHeight,
          android.R.attr.minWidth,
          android.R.attr.importantForAccessibility,
          android.R.attr.duplicateParentState,
        },
        /* srcPos */ 0,
        R.styleable.ComponentLayout,
        /* destPos */ 0,
        R.styleable.ComponentLayout.length);

    System.arraycopy(
        new int[] {
          android.R.attr.minLines,
          android.R.attr.maxLines,
          android.R.attr.textColor,
          android.R.attr.textColorLink,
          android.R.attr.textColorHighlight,
          android.R.attr.textSize,
          android.R.attr.lineSpacingExtra,
          android.R.attr.lineSpacingMultiplier,
          android.R.attr.ellipsize,
          android.R.attr.textAlignment,
          android.R.attr.textStyle,
          android.R.attr.text,
          android.R.attr.includeFontPadding,
          android.R.attr.singleLine,
          android.R.attr.shadowColor,
          android.R.attr.shadowDx,
          android.R.attr.shadowDy,
          android.R.attr.shadowRadius,
          android.R.attr.gravity,
          android.R.attr.minEms,
          android.R.attr.maxEms,
          android.R.attr.minWidth,
          android.R.attr.maxWidth,
          android.R.attr.inputType,
          android.R.attr.fontFamily,
          android.R.attr.imeOptions,
          android.R.attr.breakStrategy,
          android.R.attr.hyphenationFrequency,
          android.R.attr.justificationMode,
        },
        /* srcPos */ 0,
        R.styleable.Text,
        /* destPos */ 0,
        R.styleable.Text.length);

    System.arraycopy(
        new int[] {
          android.R.attr.src, android.R.attr.scaleType,
        },
        /* srcPos */ 0,
        R.styleable.Image,
        /* destPos */ 0,
        R.styleable.Image.length);
  }
}
