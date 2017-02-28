// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A method annotation used in classes that are annotated with {@link ReferenceSpec}.
 * <p>Methods annotated with {@link OnAcquire} take an Android Context as the first parameter,
 * followed by any number of {@link Prop}s.
 * <p>The method should return an object. It must be the same type as the second parameter of the
 * method annotated with {@link OnRelease}, if there is one.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnAcquire {

}
