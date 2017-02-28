// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Whether the LayoutSpec component will perform measurement while computing its layout tree.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnCreateLayoutWithSizeSpec {

}
