// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.litho.R;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentView;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.litho.ColorDrawableShadow;

import com.google.common.base.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ViewPredicates}
 */
@RunWith(ComponentsTestRunner.class)
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
  private ComponentView mComponentViewWithText;
