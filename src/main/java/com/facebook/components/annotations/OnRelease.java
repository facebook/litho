// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A method annotation used in classes that are annotated with {@link ReferenceSpec}.
 * <p>It is optional (though usually necessary) to have a method annotated with {@link OnRelease}.
 * <p>Methods annotated with {@link OnRelease} take an Android Context as the first parameter, the
 * object to be released as the second parameter, and then any number of {@link Prop}s. The second
 * parameter must have the same type as the return type of the method annotated with
 * {@link OnAcquire} that is mandatory for {@link ReferenceSpec}s.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnRelease {

}
