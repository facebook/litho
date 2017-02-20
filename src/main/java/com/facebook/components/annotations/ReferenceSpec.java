// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class that is annotated with {@link ReferenceSpec} will be used to create a new reference
 * lifecycle. A reference lifecycle reduce the memory footprint by only acquiring objects when they
 * are required, and releasing them when they are no longer in use.
 * <p>A class that this annotated with {@link ReferenceSpec} must implement a method annotated with
 * {@link OnAcquire}, and optionally a method annotated with {@link OnRelease}.
 * <p>For example:
 * <pre>
 * {@code
 *
 * @ReferenceSpec
 * public class MyReferenceSpec {
 *
 *   private static final Pools.SynchronizedPool<MyObject> sMyObjectPool =
 *       new Pools.SynchronizedPool<>(8);
 *
 *   @OnAcquire
 *   protected MyObject onAcquire(Context c, @Prop MyProp prop) {
 *     MyObject myObject = sMyObjectPool.acquire();
 *     if (myObject == null) {
 *       myObject = new MyObject();
 *     }
 *
 *     myObject.setMyProp(prop);
 *
 *     return myObject;
 *   }
 *
 *   @OnRelease
 *   protected void onRelease(Context c, MyObject myObject) {
 *     sMyObjectPool.release(myObject);
 *   }
 *
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceSpec {
  String value() default "";

  /**
   * Whether the generated class should be public. If not, it will be package-private.
   */
  boolean isPublic() default true;
}
