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

import androidx.collection.MutableScatterSet
import com.facebook.litho.state.StateId

/**
 * Bookmark into a previous LayoutState's flat arrays. Stores the index range of a cached subtree's
 * **descendant** contributions in each output buffer (mountableOutputs, visibilityOutputs,
 * testOutputs, componentScopes, attachables).
 *
 * The current pass's baseline always re-emits the cached subtree's own root-level contributions
 * (host/bg/content before the splice block; border/fg/vis/test/scopes after it). Only the
 * descendants need to come from the cache, so the splice copies these ranges verbatim — no slice
 * math, no "skip the root from front vs end" fragility.
 *
 * Each field is `[*Start, *End)` half-open. [rootX]/[rootY] record the cached subtree root's
 * absolute origin so the splice can offset descendant bounds when the subtree shifts position.
 *
 * [stateReads] carries the subtree's layout-phase state-read attributions (collected in
 * [LithoReducer.kt]'s per-output accumulator from [LithoLayoutOutput.stateReads], which captures
 * Spec MountSpec / Primitive measure-time reads). Splice replays these into the current pass's
 * [ReductionState.stateReads] so the next pass's [LayoutTreeFuture.willSkipLayout] check sees the
 * full read graph. KComponent useState reads flow through the resolve-phase tracker ([Resolver.kt])
 * and don't appear here — they're picked up via [ResolveResult.outputs.stateReads].
 */
internal class SubtreeSnapshot(
    val mountableOutputStart: Int,
    val mountableOutputEnd: Int,
    val visibilityOutputStart: Int,
    val visibilityOutputEnd: Int,
    val testOutputStart: Int,
    val testOutputEnd: Int,
    val componentScopeStart: Int,
    val componentScopeEnd: Int,
    val attachableStart: Int,
    val attachableEnd: Int,
    val rootX: Int,
    val rootY: Int,
    // Per-direct-child layout. `isCachedLayout=true` only guarantees intrinsic measurement
    // reuse — a child's POSITION within the subtree root can still shift across passes
    // (justify-end / wrap reflow) and its SIZE can change (flex-grow cascading from a parent
    // resize). Splice corrects shifted positions with per-child offsets; size mismatches force
    // a cache miss because the child's interior can re-flow non-uniformly.
    //
    // All five arrays have length [childCount]. [childRangeStart] partitions `mountableOutputs`
    // by direct child; child i's range is `[childRangeStart[i], childRangeStart[i+1])` (or up to
    // [mountableOutputEnd] for the last child).
    val childCount: Int,
    val childPrevX: IntArray,
    val childPrevY: IntArray,
    val childPrevWidth: IntArray,
    val childPrevHeight: IntArray,
    val childRangeStart: IntArray,
    val diffNode: DiffNode,
    val stateReads: Map<StateId, MutableScatterSet<GlobalKey>>? = null,
    // External parent absolute position at recording time. The splice copies prev's
    // parent-relative RTN bounds and feeds them into a fresh RTN whose
    // `absoluteX = current_parent.absoluteX + bounds.left`. That arithmetic is correct only when
    // the external parent's absolute position is unchanged across passes — when it differs, the
    // splice over-applies `rootDx` by exactly the parent's delta and produces wrong absoluteX/Y
    // for first-level descendants. Triggered in production when a sibling above a hosted wrapper
    // grows: the wrapper itself is REUSE'd by reconcile (props unchanged), but Yoga shifts its
    // absolute position.
    val externalParentAbsoluteX: Int = 0,
    val externalParentAbsoluteY: Int = 0,
    // True iff this snapshot's recording window walked through any [DeferredLithoLayoutResult].
    // Snapshots flagged true cannot be safely spliced — see
    // [LithoReducer.shouldRecordSubtreeSnapshot]
    // for the structural reason — and the splice gate rejects them. Recording-time gate also
    // checks this so flagged snapshots are not (re)stored, but the splice gate is the load-bearing
    // check: stale snapshots from a prev LayoutState carry-forward path may still carry the flag,
    // and replaying them would be the bug.
    val containsDeferredDescendant: Boolean = false,
)
