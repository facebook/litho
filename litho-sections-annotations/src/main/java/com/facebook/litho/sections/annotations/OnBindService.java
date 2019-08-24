/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The method with this annotation will be called whenever the Service has been created or
 * transferred from the old tree to the new tree and are therefore ready to be used.
 *
 * <p>This method is the proper place to start something like a network request or register a
 * listener on the Service.
 *
 * <p>For example:
 *
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
public @interface OnBindService {}
