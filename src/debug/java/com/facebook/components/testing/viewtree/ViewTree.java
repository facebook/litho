// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.testing.viewtree;

import javax.annotation.Nullable;

import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

/**
 * This is a helper class to allow asserting on view trees and recursively
 * verify predicates on its nodes within the narrow abilities that
 * Robolectric affords us.
 */
