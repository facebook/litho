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
