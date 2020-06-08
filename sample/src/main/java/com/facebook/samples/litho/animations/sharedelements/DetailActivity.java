/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.litho.animations.sharedelements;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.transition.Slide;
import android.view.Gravity;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.Row;
import com.facebook.litho.widget.Text;
import com.facebook.samples.litho.R;
import com.facebook.yoga.YogaEdge;

public class DetailActivity extends AppCompatActivity {

  public static final String INTENT_COLOR_KEY = "INTENT_COLOR_KEY";
  public static final String INTENT_LAND_LITHO = "INTENT_LAND_LITHO";
  public static final String SQUARE_TRANSITION_NAME = "SQUARE_TRANSITION_NAME";
  public static final String TITLE_TRANSITION_NAME = "TITLE_TRANSITION_NAME";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    boolean landLitho = getIntent().getBooleanExtra(INTENT_LAND_LITHO, true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      // This transition here is used only as an example, any provided/custom Android transition can
      // be used here.
      Slide slide = new Slide(Gravity.BOTTOM);
      slide.setInterpolator(
          AnimationUtils.loadInterpolator(this, android.R.interpolator.linear_out_slow_in));
      if (getWindow() != null) {
        getWindow().setEnterTransition(slide);
      }

      // When landing on an Activity that renders a LithoView we need to postpone the enter
      // transition.
      if (landLitho) {
        postponeEnterTransition();
      }
    }

    super.onCreate(savedInstanceState);
    Spannable titleSpannable =
        new SpannableString("SHARED ELEMENT " + (landLitho ? "LITHO" : "XML"));
    titleSpannable.setSpan(
        new ForegroundColorSpan(Color.RED),
        14,
        titleSpannable.length(),
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    if (landLitho) {
      final ComponentContext componentContext = new ComponentContext(this);
      LithoView.OnDirtyMountListener dirtyMountListener =
          new LithoView.OnDirtyMountListener() {
            @Override
            public void onDirtyMount(LithoView view) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Continue the transition after Litho finishes it's dirty mount.
                startPostponedEnterTransition();
              }
            }
          };
      LithoView lithoView =
          LithoView.create(
              this,
              getContent(
                  componentContext, getIntent().getIntExtra(INTENT_COLOR_KEY, 0), titleSpannable));
      lithoView.setOnDirtyMountListener(dirtyMountListener);
      setContentView(lithoView);
    } else {
      setContentView(R.layout.detail_activity_shared_elements);

      ((ImageView) findViewById(R.id.square_imageview))
          .setImageDrawable(new ColorDrawable(getIntent().getIntExtra(INTENT_COLOR_KEY, 0)));

      ((TextView) findViewById(R.id.title_textview)).setText(titleSpannable);
    }
  }

  private static Component getContent(ComponentContext c, int color, Spannable titleText) {
    return Column.create(c)
        .child(Text.create(c).textSizeSp(25).transitionName(TITLE_TRANSITION_NAME).text(titleText))
        .child(
            Row.create(c)
                .marginDip(YogaEdge.START, 100)
                .transitionName(SQUARE_TRANSITION_NAME)
                .widthDip(200)
                .heightDip(200)
                .backgroundColor(color))
        .child(
            Text.create(c)
                .textSizeSp(12)
                .transitionName("DESCRIPTION")
                .text(
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent egestas augue venenatis suscipit maximus. Maecenas vel volutpat nunc. Etiam volutpat ultricies ante a iaculis. Fusce ultrices eleifend ligula in maximus. Fusce commodo, mauris vitae consequat tincidunt, nunc massa pharetra ante, non interdum magna sapien vel tortor. Aliquam in ultrices odio. Phasellus ac ante sit amet purus efficitur tempus fermentum in erat. Nullam auctor lorem ut justo convallis vestibulum. Fusce consequat velit eget pharetra consequat. Integer vulputate nisl eu libero luctus, id consequat ipsum eleifend. Nam quis sodales neque. Nullam nec velit sed leo feugiat imperdiet.\n"
                        + "\n"
                        + "Praesent lacinia lorem quis mauris molestie, ut placerat nisi ultricies. Sed a fringilla mi. Ut ornare a lorem quis consectetur. Pellentesque id leo id odio accumsan egestas. Proin sollicitudin turpis orci, in tempus dolor eleifend dapibus. Aenean facilisis fringilla orci, vel facilisis nunc commodo in. Sed scelerisque lectus ac diam feugiat, sit amet condimentum enim imperdiet. Integer urna arcu, aliquet quis facilisis quis, faucibus quis lorem. Nam congue augue est, ac porttitor mauris vehicula ut. Phasellus sapien tortor, euismod non dui quis, vulputate auctor orci. Maecenas a lectus in felis tincidunt pulvinar. Praesent nec laoreet ante, in sollicitudin quam. Vestibulum convallis, ante sit amet consequat varius, urna dui sagittis odio, suscipit rutrum ipsum nisi non eros. Cras interdum mattis libero at posuere. Phasellus venenatis dui massa, sed egestas mauris porta id."))
        .build();
  }
}
