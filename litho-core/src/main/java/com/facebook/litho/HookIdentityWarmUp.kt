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

package com.facebook.litho

import com.facebook.proguard.annotations.DoNotStrip
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Executes the hook-identity code path once at startup so that Meta's production ART profile learns
 * it.
 *
 * ## The problem
 *
 * FB4A and IG4A do not ship an AGP baseline profile. Their ART profile is harvested from real
 * production sessions, and a method is AOT-compiled only if it ran in at least `threshold`% of
 * harvested sessions for some interaction; the lowest threshold either app uses is 20%.
 *
 * Hook identity is behind an experiment, so the methods that only run when it is on were observed
 * in ~4.5% (IG4A) and ~3.4% (FB4A) of sessions and are therefore absent from the profile, while
 * their index-path counterparts are present. `KStateKt.getUpdatedStateByIndex` ships;
 * `getUpdatedStateByHookKey` does not, despite the two doing the same amount of work per render.
 * Every hook-identity experiment arm consequently pays an AOT handicap the control arm does not,
 * which biases the arm contrast independently of the change under test.
 *
 * ## What this does about it
 *
 * Touching the methods once per process pushes their observed session share to ~100%, above every
 * interaction threshold, so the *next* harvest generates a profile that AOT-compiles them for
 * everybody. It deliberately runs in **all** arms including control: the goal is a profile that is
 * identical for both arms, not one that matches the treatment.
 *
 * ## What it does not do
 *
 * - **It does not take effect in the build that contains it.** The profile is a snapshot patched
 *   into `deep_data/BUCK` by an automated job. This code has to land, ship in a release, be
 *   harvested from that release's production sessions, and then be autoshipped, before any build
 *   sees the benefit.
 * - **It cannot reach the hook methods that need a live render.**
 *   `KStateKt.getUpdatedStateByHookKey`, `ComponentScope.requireCurrentHookKey` and
 *   `KStateKt.transientMutate` all need a real [ComponentScope] with a resolve context and tree
 *   state, which is far too much to construct on the startup path. Those three stay uncovered.
 *
 * @see HookKey.Companion.warmUpForProfile
 * @see KStateContainer.Companion.warmUpForProfile
 */
@DoNotStrip
object HookIdentityWarmUp {

  private val hasRun = AtomicBoolean(false)

  /**
   * Sink for the warm-up's results.
   *
   * The warm-up's return values are otherwise unused, which would let the Kotlin compiler, R8 or
   * Redex prove the whole call tree dead and delete it -- and a deleted warm-up fails silently,
   * because its only observable effect is on a profile harvested weeks later. A volatile static
   * write is a side effect none of them can elide.
   */
  @Volatile @JvmStatic var lastResult: Any? = null

  /**
   * Runs the warm-up. Idempotent, and cheap enough to call from an app-startup initializer: it
   * allocates on the order of twenty short-lived objects and runs a few dozen comparisons.
   *
   * Safe to call from any thread; the work touches no shared state beyond one entry added to
   * [HookKey]'s process-lifetime intern map under a reserved call-site id.
   */
  @JvmStatic
  @DoNotStrip
  fun warmUp() {
    if (!hasRun.compareAndSet(false, true)) {
      return
    }
    lastResult = HookKey.warmUpForProfile()
    lastResult = KStateContainer.warmUpForProfile()
  }
}
