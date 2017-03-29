// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho;

import android.content.Context;
import android.widget.TextView;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompatcreator.ViewCompatCreator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link ViewCompatComponent}
 */
@RunWith(ComponentsTestRunner.class)
