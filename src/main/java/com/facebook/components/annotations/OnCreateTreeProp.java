// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation for a Spec method that generates tree props.
 * These tree props will be passed silently to all of the Component's children.
 *
 * Tree props are stored in a map keyed on their individual class object, meaning there will only be
 * one entry for tree props of any given type. PLEASE DO NOT USE COMMON JAVA CLASSES, for example,
 * String, Integer etc; creates a wrapper class instead.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 *
 * @LayoutSpec
 * public class MySpec {
 *
 *   @OnCreateTreeProp
 *   protected SomeTreePropClass onCreateSomeTreeProp(
 *     ComponentsContext c,
 *     @Prop SomeProp prop) {
 *    return new SomeTreePropClass(prop.getSomeProperty());
 *   }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnCreateTreeProp {

}
