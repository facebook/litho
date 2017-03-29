// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho;

import java.util.LinkedList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.TransitionProperties.PropertyChangeHolder;
import com.facebook.litho.TransitionProperties.PropertyType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowSystemClock;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
