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

/**
 * The method annotated with this annotation will be called when the Ui rendering the <code>Section
 * </code> requests a refresh of the content.
 *
 * <p>For example:
 *
 * <pre><code>
 * {@literal @}DiffSectionSpec
 *  public class MyChangeSetSpec {
 *
 *  {@literal @}OnRefresh
 *   protected void onRefresh(
 *     ListContext c,
 *     Service service) {
 *       service.refetch();
 *   }
 * }
 * </code></pre>
 */
public @interface OnRefresh {}
