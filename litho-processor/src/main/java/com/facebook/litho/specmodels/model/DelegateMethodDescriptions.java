/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.GetExtraAccessibilityNodeAt;
import com.facebook.litho.annotations.GetExtraAccessibilityNodesCount;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLayoutTransition;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMeasureBaseline;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPopulateExtraAccessibilityNode;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.INTER_STAGE_OUTPUT;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP_OUTPUT;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE_OUTPUT;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.TREE_PROP;
