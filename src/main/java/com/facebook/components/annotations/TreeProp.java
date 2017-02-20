// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A prop passed silently down from a Spec's parents.
 * The parent should set the prop in a {@link OnCreateTreeProp} method.
 * Both the name and type of a child's TreeProp should match exactly to what is set in the parent.
 */
@Retention(RetentionPolicy.CLASS)
public @interface TreeProp {
}
