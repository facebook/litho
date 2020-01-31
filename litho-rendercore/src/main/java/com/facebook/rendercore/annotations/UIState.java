// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used with a RenderUnit: annotates that this field is mutable state that can be accessed on
 * the UI thread only. This should only be annotated on non-primitive fields because we want to make
 * sure to maintain the same instance when cloning.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface UIState {}
