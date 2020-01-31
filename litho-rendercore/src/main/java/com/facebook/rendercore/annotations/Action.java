// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used with a RenderUnit: annotates that this method is an action that can be taken on this
 * RenderUnit from the UI thread.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Action {}
