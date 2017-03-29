/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

/**
 * To avoid referring to classes directly in the processor (which would be a circular dependency and
 * would mean the annotation processor might need to process Android-specific class definitions)
 * we provide class name constants instead.
 */
public class SectionClassNames {

  public static final ClassName STATE_CONTAINER_SECTION =
      ClassName.bestGuess("com.facebook.litho.list.SectionLifecycle.StateContainer");
  public static final ClassName SECTION =
      ClassName.bestGuess("com.facebook.litho.list.Section");
