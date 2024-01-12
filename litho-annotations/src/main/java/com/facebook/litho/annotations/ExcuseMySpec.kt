/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.annotations

/**
 * Spec API usage in Kotlin is deprecated and new Specs will be generally treated as errors. But
 * there are certain cases when Specs in Kotlin should be allowed. This annotation is used to mark
 * such usecases and suppress the linter, an explicit [reason] is required.
 */
@Retention(AnnotationRetention.SOURCE) annotation class ExcuseMySpec(val reason: Reason)

enum class Reason {
  /**
   * Used then Spec is an output of (auto)conversion from Java to Kotlin, landing such Spec should
   * be allowed to simplify the review process.
   */
  J2K_CONVERSION,

  /**
   * Used when devs need to use WorkingRanges API, which is not yet supported in Litho Kotlin API.
   */
  USES_WORKING_RANGES,

  /**
   * Used when devs need to use [OnUpdateStateWithTransition] API, which is not yet supported in
   * Litho Kotlin API.
   */
  USES_ON_UPDATE_STATE_WITH_TRANSITION,

  /** Used only to mark Kotlin Specs that were created before the Spec API deprecation. */
  @Deprecated("Don't add this manually!") LEGACY,
}
