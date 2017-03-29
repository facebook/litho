/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;

import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.SpecModel;

import com.squareup.javapoet.TypeName;

