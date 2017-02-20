// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A method annotation used in classes that are annotated with {@link MountSpec}.
 * <p>Methods annotated with {@link OnUnbind} take an Android Context as the first parameter,
 * the Object that the MountSpec mounts as the second parameter, followed by any number of
 * {@link Prop}s.
 * <p>The method should return void.
 * This callback will be invoked every time the mounted object is not active anymore but has not
 * been unmounted yet. This happens for example when a ComponentView can be in a state where it's
 * not on the screen anymore but it's not been unmounted yet (to re-use items in place for example).
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnUnbind {

}
