/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.specmodels.model;

import javax.lang.model.element.Modifier;

import java.util.List;

import com.facebook.common.internal.ImmutableList;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.components.specmodels.generator.BuilderGenerator;
import com.facebook.components.specmodels.generator.ComponentImplGenerator;
import com.facebook.components.specmodels.generator.DelegateMethodGenerator;
import com.facebook.components.specmodels.generator.EventGenerator;
import com.facebook.components.specmodels.generator.JavadocGenerator;
import com.facebook.components.specmodels.generator.PreambleGenerator;
import com.facebook.components.specmodels.generator.PureRenderGenerator;
import com.facebook.components.specmodels.generator.StateGenerator;
import com.facebook.components.specmodels.generator.TreePropGenerator;
import com.facebook.components.specmodels.generator.TypeSpecDataHolder;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

/**
 * Model that is an abstract representation of a
