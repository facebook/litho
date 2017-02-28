// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The method annotated with this annotation will be called to instantiate the mount content for
 * the {@link MountSpec}. The onCreateMountContent method can only take a
 * {@link com.facebook.components.ComponentContext} as parameter. No props are allowed here.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnCreateMountContent {

}
