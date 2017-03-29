// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing.viewtree;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.litho.R;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentView;
import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.components.ColorDrawableShadow;

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
