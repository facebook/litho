/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.squareup.javapoet.ClassName;

/**
 * Constants used in {@link SpecModel}s.
 */
public interface ClassNames {
  ClassName OBJECT = ClassName.bestGuess("java.lang.Object");
  ClassName STRING = ClassName.bestGuess("java.lang.String");

  ClassName VIEW = ClassName.bestGuess("android.view.View");
  ClassName DRAWABLE =
      ClassName.bestGuess("android.graphics.drawable.Drawable");
