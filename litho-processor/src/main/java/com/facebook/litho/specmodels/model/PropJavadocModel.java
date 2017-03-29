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

/**
 * Model that is an abstract representation of the javadoc for a prop.
 */
@Immutable
public final class PropJavadocModel {
  public final String propName;
  public final String javadoc;

  public PropJavadocModel(String propName, String javadoc) {
    this.propName = propName;
    this.javadoc = javadoc;
  }
}
