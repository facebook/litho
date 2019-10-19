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
 * The method annotated with this annotation will be called when the data corresponding to this
 * Section props is now rendered complete.
 *
 * <p>For example:
 *
 * <pre>
 *  {@literal @GroupSectionSpec}
 *   public class MyGroupSectionSpec {
 *
 *    {@literal OnDataRendered}
 *     protected void onDataRendered(
 *       SectionContext c,
 *       boolean isDataChanged,
 *      {@literal @}State GraphQLResponse response,
 *      {@literal @}State DataSource dataSource,
 *      {@literal @}Prop String someProp) {
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnDataRendered {}
