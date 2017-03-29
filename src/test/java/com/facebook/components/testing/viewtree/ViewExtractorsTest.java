// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.litho.R;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ViewExtractors}
 */
@RunWith(ComponentsTestRunner.class)
public class ViewExtractorsTest {

  private View mView;
  private TextView mTextView;
  private TextView mGoneTextView;
  private ImageView mImageView;
  private ImageView mGoneImageView;
  private Drawable mLithoDrawable;

  @Before
  public void setUp() {
    final Activity activity = Robolectric.buildActivity(Activity.class).create().get();

    mLithoDrawable = activity.getResources().getDrawable(R.drawable.litho);

    mView = new View(activity);

    mTextView = new TextView(activity);
    mTextView.setText("example");

    mGoneTextView = new TextView(activity);
    mGoneTextView.setText("gone");
    mGoneTextView.setVisibility(View.GONE);

    mImageView = new ImageView(activity);
    mImageView.setImageDrawable(mLithoDrawable);

    mGoneImageView = new ImageView(activity);
    mGoneImageView.setImageDrawable(mLithoDrawable);
    mGoneImageView.setVisibility(View.GONE);
  }

  @Test
  public void testGetTextFromTextViewHasTextContent() {
    assertThat(ViewExtractors.GET_TEXT_FUNCTION.apply(mTextView)).contains("example");
  }

  @Test
  public void testGetTextPrintsVisibity() {
    assertThat(ViewExtractors.GET_TEXT_FUNCTION.apply(mTextView))
        .contains("view is visible");
    assertThat(ViewExtractors.GET_TEXT_FUNCTION.apply(mGoneTextView))
        .contains("view is not visible");
  }

  @Test
  public void testViewWithoutText() {
    assertThat(ViewExtractors.GET_TEXT_FUNCTION.apply(mView))
        .contains("No text found");
  }

  @Test
  public void testGetDrawableOutOfImageView() {
    assertThat(ViewExtractors.GET_DRAWABLE_FUNCTION.apply(mImageView))
        .contains(mLithoDrawable.toString());
