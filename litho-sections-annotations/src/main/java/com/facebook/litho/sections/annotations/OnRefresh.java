/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.annotations;

/**
 * The method annotated with this annotation will be called when the Ui rendering the
 * {@link com.facebook.litho.sections.Section} requests a refresh of the content.
 *
 * <p>For example:
 * <pre>
 * {@code
 *
 * @DiffSectionSpec
 * public class MyChangeSetSpec {
 *
 *   @OnRefresh
 *   protected void onRefresh(
 *     ListContext c,
 *     Service service) {
 *       service.refetch();
 *   }
 * }
 * </pre>
 *
 */
public @interface OnRefresh {

}
