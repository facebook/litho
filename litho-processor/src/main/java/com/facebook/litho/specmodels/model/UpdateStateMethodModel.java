/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import javax.annotation.concurrent.Immutable;
import javax.lang.model.element.Modifier;

import java.lang.annotation.Annotation;

import com.facebook.common.internal.ImmutableList;

import com.squareup.javapoet.TypeName;

/**
 * Model that is an abstract representation of a method annotated with
