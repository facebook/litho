// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

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
