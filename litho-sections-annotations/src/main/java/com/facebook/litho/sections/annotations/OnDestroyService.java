/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The method annotated with this annotation will be called when the Component is about to
 * disappear from the tree. This is where the Service created in the {@link OnCreateService}
 * should be disposed.
 *
 * The method will take as parameters only the {@link com.facebook.litho.sections.SectionContext}
 * and the Service created in {@link OnCreateService}.
 *
 * <p>For example:
 * <pre>
 * {@code
 *
 *   @OnDestroyServices
 *   protected void onDestroyService(
 *     SectionContext c,
 *     SomeService someService) {
 *     someService.stop();
 *     someService.dispose();
 *   }
 * }
 * </pre>
 *
 * DEPRECATED:
 * Do not use rely on this method to clean up your object. If you need to use an object
 * that requires explicit clean up, consider creating it on the Activity/Fragment level,
 * and clean it up when your context is finished.
 */
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface OnDestroyService {

}
