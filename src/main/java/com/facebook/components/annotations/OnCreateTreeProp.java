// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation for a Spec method that generates tree props.
 * These tree props will be passed silently to all of the Component's children.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 *
 * @LayoutSpec
 * public class MySpec {
 *
 *   @OnCreateTreeProp("someTreeProp")
 *   protected String onCreateSomeTreeProp(
 *     ComponentsContext c,
 *     @Prop SomeProp prop) {
 *    return prop.toString();
 *   }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnCreateTreeProp {

  String name();
}
