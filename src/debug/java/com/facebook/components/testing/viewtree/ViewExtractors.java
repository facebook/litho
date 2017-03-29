// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import javax.annotation.Nullable;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.litho.ComponentHost;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * Function objects used for extracting specific information out of Android classes
 */
final class ViewExtractors {

  private ViewExtractors() {}

  public static final Function<View, String> GET_TEXT_FUNCTION = new Function<View, String>() {
    @Override
    public String apply(@Nullable View input) {
      CharSequence text = null;
      if (input instanceof ComponentHost) {
        List<CharSequence> strings = ((ComponentHost) input).getTextContent().getTextItems();
