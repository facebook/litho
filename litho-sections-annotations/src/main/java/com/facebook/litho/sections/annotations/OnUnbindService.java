/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * The method annotated with this annotation will be called when the Service has been transferred
 * from the old tree to the new tree. This means that this <code>SectionComponent</code> should
 * unset any listener previously set on the Service.
 *
 * <p>For example:
 *
 * <pre><code>
 *
 * {@literal @}DiffSectionSpec
 *  public class MyChangeSetSpec {
 *
 *   {@literal @}OnUnbindServices
 *    protected void onUnbindService(
 *      SectionContext c,
 *      SomeService myService,
 *     {@literal @}Prop SomeProp prop) {
 *     myService.unregisterListener(...);
 *   }
 * }
 * </code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnUnbindService {}
