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

package com.facebook.litho.testing.viewtree;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.shadows.ColorDrawableShadow;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import com.google.common.base.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

/** Tests {@link ViewPredicates} */
@RunWith(LithoTestRunner.class)
@Config(shadows = ColorDrawableShadow.class)
public class ViewPredicatesTest {

  private View mView;
  private TextView mTextViewWithNull;
  private TextView mTextViewWithEmptyString;
  private TextView mTextViewWithHello;
  private TextView mTextViewWithWorld;
  private ImageView mImagelessView;
  private ImageView mOtherImageView;
  private ImageView mImageView;
  private ImageView mImageViewWithCustomDrawable;
  private LithoView mLithoViewWithText;

  @Before
  public void setUp() {
    final Activity activity = Robolectric.buildActivity(Activity.class).create().get();

    mView = new View(activity);
    mTextViewWithNull = new TextView(activity);
    mTextViewWithEmptyString = new TextView(activity);
    mTextViewWithHello = new TextView(activity);
    mTextViewWithWorld = new TextView(activity);
    mImagelessView = new ImageView(activity);
    mOtherImageView = new ImageView(activity);
    mImageView = new ImageView(activity);
    mImageViewWithCustomDrawable = new ImageView(activity);

    mTextViewWithEmptyString.setText("");
    mTextViewWithHello.setText("Hello");
    mTextViewWithWorld.setText("World");
    mOtherImageView.setImageResource(R.drawable.background);
    mImageView.setImageResource(R.drawable.litho);
    mImageViewWithCustomDrawable.setImageResource(R.drawable.custom_drawable);

    final ComponentContext context = new ComponentContext(activity);
    mLithoViewWithText = ComponentTestHelper.mountComponent(Text.create(context).text("Hello"));
  }

  @Test
  public void testHasText() throws Exception {
    final Predicate<View> hasHello = ViewPredicates.hasText("Hello");

    assertThat(hasHello.apply(mView)).isFalse();
    assertThat(hasHello.apply(mTextViewWithNull)).isFalse();
    assertThat(hasHello.apply(mTextViewWithEmptyString)).isFalse();
    assertThat(hasHello.apply(mTextViewWithHello)).isTrue();
    assertThat(hasHello.apply(mTextViewWithWorld)).isFalse();
    assertThat(hasHello.apply(mLithoViewWithText)).isTrue();
  }

  @Test
  public void testHasVisibleText() throws Exception {
    final Predicate<View> hasHello = ViewPredicates.hasVisibleText("Hello");

    assertThat(hasHello.apply(mTextViewWithHello)).isTrue();
    mTextViewWithHello.setVisibility(View.GONE);
    assertThat(hasHello.apply(mTextViewWithHello)).isFalse();
  }

  @Test
  public void testMatchesTextWholeString() throws Exception {
    final Predicate<View> matchesHello = ViewPredicates.matchesText("Hello");

    assertThat(matchesHello.apply(mView)).isFalse();
    assertThat(matchesHello.apply(mTextViewWithNull)).isFalse();
    assertThat(matchesHello.apply(mTextViewWithEmptyString)).isFalse();
    assertThat(matchesHello.apply(mTextViewWithHello)).isTrue();
    assertThat(matchesHello.apply(mTextViewWithWorld)).isFalse();
    assertThat(matchesHello.apply(mLithoViewWithText)).isTrue();
  }

  @Test
  public void testMatchesTextPartOfString() throws Exception {
    final Predicate<View> matchesPartOfHello = ViewPredicates.matchesText(".*o.*");

    assertThat(matchesPartOfHello.apply(mView)).isFalse();
    assertThat(matchesPartOfHello.apply(mTextViewWithNull)).isFalse();
    assertThat(matchesPartOfHello.apply(mTextViewWithEmptyString)).isFalse();
    assertThat(matchesPartOfHello.apply(mTextViewWithHello)).isTrue();
    assertThat(matchesPartOfHello.apply(mTextViewWithWorld)).isTrue();
    assertThat(matchesPartOfHello.apply(mLithoViewWithText)).isTrue();
  }

  @Test
  public void testHasVisibleMatchingText() throws Exception {
    final Predicate<View> hasHello = ViewPredicates.hasVisibleMatchingText(".*o.*");

    assertThat(hasHello.apply(mTextViewWithHello)).isTrue();
    mTextViewWithHello.setVisibility(View.GONE);
    assertThat(hasHello.apply(mTextViewWithHello)).isFalse();
  }

  @Test
  public void testIsVisible() throws Exception {
    final Predicate<View> isVisible = ViewPredicates.isVisible();

    assertThat(isVisible.apply(mView)).isTrue();
    mView.setVisibility(View.GONE);
    assertThat(isVisible.apply(mView)).isFalse();
    mView.setVisibility(View.INVISIBLE);
    assertThat(isVisible.apply(mView)).isFalse();
  }

  @Test
  public void testHasDrawable() {
    final Resources resources = getApplicationContext().getResources();
    final Drawable noAvatar = resources.getDrawable(R.drawable.custom_drawable);
    final Predicate<View> hasDrawable = ViewPredicates.hasDrawable(noAvatar);

    assertThat(hasDrawable.apply(mView)).isFalse();
    assertThat(hasDrawable.apply(mImagelessView)).isFalse();
    assertThat(hasDrawable.apply(mOtherImageView)).isFalse();
    assertThat(hasDrawable.apply(mImageView)).isTrue();
  }

  @Test
  public void testHasDrawableForRobolectric3AndColorDrawables() {
    final Drawable black1 = new ColorDrawable(Color.BLACK);
    final Drawable black2 = new ColorDrawable(Color.BLACK);
    final Drawable white = new ColorDrawable(Color.WHITE);
    final Predicate<View> hasDrawable = ViewPredicates.hasDrawable(black1);

    mImageView.setImageDrawable(black1);
    assertThat(hasDrawable.apply(mImageView)).isTrue();

    mImageView.setImageDrawable(black2);
    assertThat(hasDrawable.apply(mImageView)).isTrue();

    mImageView.setImageDrawable(white);
    assertThat(hasDrawable.apply(mImageView)).isFalse();
  }

  @Test
  public void testHasVisibleDrawable() {
    final Resources resources = getApplicationContext().getResources();
    final Drawable noAvatar = resources.getDrawable(R.drawable.litho);
    final Predicate<View> hasVisibleDrawable = ViewPredicates.hasVisibleDrawable(noAvatar);

    assertThat(hasVisibleDrawable.apply(mImageView)).isTrue();
    mImageView.setVisibility(View.GONE);
    assertThat(hasVisibleDrawable.apply(mImageView)).isFalse();
  }

  @Test
  public void testHasVisibleCustomDrawable() {
    final Resources resources = getApplicationContext().getResources();
    final Drawable customDrawable = resources.getDrawable(R.drawable.custom_drawable);
    final Predicate<View> hasVisibleDrawable = ViewPredicates.hasVisibleDrawable(customDrawable);

    assertThat(hasVisibleDrawable.apply(mImageViewWithCustomDrawable)).isTrue();
    mImageViewWithCustomDrawable.setVisibility(View.GONE);
    assertThat(hasVisibleDrawable.apply(mImageViewWithCustomDrawable)).isFalse();
  }
}
