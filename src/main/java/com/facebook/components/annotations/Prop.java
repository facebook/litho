// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface Prop {

  boolean optional() default false;
  ResType resType() default ResType.NONE;
  String docString() default "";

}
