// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to mark a method that can update the state of a Component.
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnUpdateState {

}
