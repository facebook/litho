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

package com.facebook.rendercore;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface AuditableMountContent {
  
  /**
   * Called before a mount content is released and added to the recycling pool.
   * Implementations of this method should check the state of the content, and throw an exception
   * if the content isn't in its default state.
   */
  void auditForRelease();

  /**
   * Called when a non-crashing error occurs on the mount content before audit is called, suggesting
   * that it may be related to the audit failure. Implementations of this method should collect
   * the error, and print it out in the audit exception.
   */
  void logError(String message, Exception e);

  /**
   * Called when a standard usage of this mounted content occurs. This information could be useful
   * if audit throws an exception. Implementations of this method should collect the usage
   * description and print it out in the audit exception.
   */
  void logUsage(String usageDescription);
}
