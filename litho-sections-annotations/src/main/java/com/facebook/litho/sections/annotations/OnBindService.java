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
 * The method with this annotation will be called whenever the Service has been created or
 * transferred from the old tree to the new tree and are therefore ready to be used.
 *
 * This method is the proper place to start something like a network request or register a
 * listener on the Service.
 *
 * <p>For example:
 * <pre>
 *
 * {@literal @}DiffSectionSpec
 *  public class MyChangeSetSpec {
 *
 *   {@literal @}OnBindServices
 *    protected void onBindService(
 *      SectionContext c,
 *      SomeService someService,
 *     {@literal @}Prop SomeProp prop) {
 *      myService.startDoingSomething(prop);
 *      myService.registerListener(...);
 *    }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnBindService {

}
