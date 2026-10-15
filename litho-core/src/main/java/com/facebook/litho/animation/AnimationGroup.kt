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

package com.facebook.litho.animation

import androidx.annotation.MainThread
import com.facebook.litho.GlobalKey
import com.facebook.litho.HookGlobalKey
import com.facebook.litho.HookKey
import com.facebook.litho.RenderScope
import com.facebook.litho.StateUpdater
import com.facebook.litho.Value
import com.facebook.litho.state.StateId
import com.facebook.litho.state.StateReadRecorder
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.ResourceResolver
import com.facebook.rendercore.graphics.Color

/**
 * Represents a directional transition segment from [initialState] to [targetState].
 *
 * Used inside `transitionSpec` lambdas to select different animation specs based on the direction
 * of a state transition. For example, an expand animation can use a bouncy spring while a collapse
 * uses a quick tween.
 *
 * ```kotlin
 * transitionSpec = {
 *     when {
 *         false.isTransitioningTo(true) -> spring(stiffness = Stiffness.Low)
 *         true.isTransitioningTo(false) -> tween(200.ms)
 *         else -> spring()
 *     }
 * }
 * ```
 */
data class Segment<S>(val initialState: S, val targetState: S) {

  /**
   * Returns `true` if the segment represents a transition from `this` value to [target].
   *
   * Must be called within a [Segment] receiver scope (e.g., inside a `transitionSpec` lambda).
   */
  infix fun S.isTransitioningTo(target: S): Boolean =
      this == initialState && target == this@Segment.targetState
}

/**
 * Immutable per-slot descriptor collected during `render()`.
 *
 * Carries no mutable state and no [AnimatedValue]: only the [slot] it claims plus the pure
 * descriptors needed to create and drive its value at commit. The long-lived [AnimatedValue]
 * runtime for the slot lives in [AnimationGroupState] and is created from this description during
 * commit.
 */
internal class AnimationDescriptor<S, T>(
    val slot: Int,
    val typeConverter: TypeConverter<T>,
    val transitionSpec: Segment<S>.() -> FiniteAnimationSpec<T>,
    val targetValueByState: (S) -> T,
    val label: String,
) {
  /**
   * Whether [other] occupies this slot with the same value type. Drives the shape-stability check:
   * a slot's long-lived `AnimatedValue<T>` is created once for type `T`, so the per-slot type must
   * stay stable across renders. (Type only — labels are debug strings and may be dynamic.)
   */
  fun shapeEquals(other: AnimationDescriptor<*, *>): Boolean =
      typeConverter::class == other.typeConverter::class
}

/**
 * The immutable per-render record of a group: the [targetValue] this render animates toward plus
 * its ordered [descriptors]. Built by [AnimationGroup] during `render()` as a pure value — no
 * runtime, no back-reference to mutable state — and committed into [AnimationGroupState], which
 * reads its target and slot descriptors from the latest committed record. `render()` never reads
 * runtime.
 */
internal class AnimationGroupRecord<S>(
    val targetValue: S,
    val descriptors: List<AnimationDescriptor<S, *>>,
)

/**
 * Persistent runtime for a coordinated multi-property animation: the per-slot [AnimatedValue]s,
 * [currentState], and run status. It owns the latest committed [AnimationGroupRecord] and reads
 * [targetState] and the slot descriptors from it. Stored in Litho hook state, it lives across
 * renders.
 *
 * Splitting the record from the runtime keeps `render()` pure: render only *describes* slots
 * (creating no [AnimatedValue] and mutating nothing here), so two concurrent resolves of the same
 * generation cannot race on creation. The long-lived [AnimatedValue]s are instead created in
 * [commit] — run from a `useEffect` body on the main thread — so creation is single-threaded and
 * needs no lock; [retarget] re-aims them and the frame loop advances [currentState] on settle. A
 * slot's value is read at bind through a [SlotValue] handle keyed by its stable [childStateId],
 * never during render.
 *
 * @param S The type of the state that drives the transition (e.g., `Boolean`, an enum).
 * @see AnimationGroupRecord
 * @see AnimationGroup
 * @see useAnimationGroup
 * @see Segment
 */
internal class AnimationGroupState<S>(
    initialState: S,
    private val treeId: Int,
    private val globalKey: GlobalKey,
    private val baseHookKey: HookKey,
    internal val isLayoutContext: Boolean,
    internal val stateUpdater: StateUpdater,
    val label: String,
) {

  /**
   * The latest committed immutable record. Replaced wholesale (copy-on-write) in [commit]; the
   * runtime reads [targetState] and slot descriptors from it. Read through a `@Volatile` reference.
   */
  @Volatile
  var record: AnimationGroupRecord<S> = AnimationGroupRecord(initialState, emptyList())
    private set

  /**
   * The state the group is transitioning *from*: the last settled state, or — when a target change
   * interrupts a run — the target that run was heading for. Advanced on settle from the frame loop,
   * and on interruption from [commit].
   */
  @Volatile
  var currentState: S = initialState
    internal set

  /** The state the group is animating toward — read from the committed [record]. */
  val targetState: S
    get() = record.targetValue

  /** The current directional segment from [currentState] to [targetState]. */
  val segment: Segment<S>
    get() = Segment(currentState, record.targetValue)

  /** `true` while any animation is actively running (has an animator AND receiving frames). */
  val isRunning: Boolean
    get() = animatedValues.any { it.isRunning }

  /** `true` if any animation has a pending animator, whether or not it has started. */
  internal val hasAnimator: Boolean
    get() = animatedValues.any { it.hasAnimator }

  /**
   * The long-lived per-slot [AnimatedValue] runtime, indexed by slot. Created lazily in [commit]
   * (commit phase, main thread) and never mutated from `render()`.
   */
  @Volatile
  internal var animatedValues: List<AnimatedValue<*>> = emptyList()
    private set

  private var hasCommitted: Boolean = false

  /**
   * Returns the long-lived [AnimatedValue] for [slot], or `null` if it has not been committed yet.
   */
  internal fun animatedValueAt(slot: Int): AnimatedValue<*>? = animatedValues.getOrNull(slot)

  /**
   * Commits the render's [record] and creates the per-slot values on the first commit.
   *
   * Runs from a `useEffect` body on the main thread, so slot creation is single-threaded: there is
   * no concurrent-resolve race to guard, hence no lock. The first commit establishes the slots; the
   * slot count is fixed thereafter (the [validateRecord] guard rejects shape changes). Each slot's
   * [AnimatedValue] is created exactly once and keyed by a stable [childStateId], so sequential
   * commits of concurrent resolves converge on the same instances.
   */
  @MainThread
  internal fun commit(record: AnimationGroupRecord<S>) {
    validateRecord(record)
    if (!hasCommitted) {
      val slots = record.descriptors
      animatedValues = List(slots.size) { i -> createAnimatedValue(slots[i]) }
      hasCommitted = true
    } else if (
        record.targetValue != this.record.targetValue && currentState != this.record.targetValue
    ) {
      // Interrupted mid-run: step to the target we were heading for so [segment] describes the leg
      // being animated. Must stay in the `hasCommitted` branch — [createAnimatedValue] seeds slots
      // from [currentState], so moving it earlier would change their initial values.
      currentState = this.record.targetValue
    }
    this.record = record
  }

  @Suppress("UNCHECKED_CAST")
  private fun createAnimatedValue(description: AnimationDescriptor<S, *>): AnimatedValue<*> {
    val typed = description as AnimationDescriptor<S, Any?>
    return AnimatedValue(
        initialValue = typed.targetValueByState(currentState),
        typeConverter = typed.typeConverter,
        stateId = childStateId(typed.slot),
        isLayoutContext = isLayoutContext,
        stateUpdater = stateUpdater,
        label = deriveLabel(typed.label),
    )
        .also { it.onComplete = ::checkCompletion }
  }

  private fun validateRecord(newRecord: AnimationGroupRecord<S>) {
    if (!hasCommitted) {
      return
    }
    check(newRecord.descriptors.size == record.descriptors.size) {
      "AnimationGroup '$label' changed animation count across renders: " +
          "${record.descriptors.size} -> ${newRecord.descriptors.size}"
    }
    for (slot in newRecord.descriptors.indices) {
      val previous = record.descriptors[slot]
      val next = newRecord.descriptors[slot]
      check(next.shapeEquals(previous)) {
        "AnimationGroup '$label' changed animation order or value type at slot $slot. " +
            "Keep animate* calls in a stable order across renders."
      }
    }
  }

  /**
   * Retargets all animations toward the committed [targetState], returning `true` if at least one
   * retargeted child needs a start handle. Called from the [useAnimationGroup] effect on the main
   * thread after [commit] has published the new record.
   */
  internal fun retarget(): Boolean {
    val seg = segment
    val descriptors = record.descriptors
    val values = animatedValues
    var needsRegistration = false
    for (slot in descriptors.indices) {
      val value = values.getOrNull(slot) ?: continue
      val wasActive = value.isActive
      retargetAnimation(descriptors[slot], value, seg)
      needsRegistration = needsRegistration || (!wasActive && value.hasAnimator)
    }
    return needsRegistration
  }

  @Suppress("UNCHECKED_CAST")
  private fun retargetAnimation(
      description: AnimationDescriptor<S, *>,
      value: AnimatedValue<*>,
      seg: Segment<S>,
  ) {
    val typed = description as AnimationDescriptor<S, Any?>
    val animatedValue = value as AnimatedValue<Any?>
    animatedValue.retarget(typed.targetValueByState(seg.targetState), typed.transitionSpec(seg))
  }

  /**
   * Optional callback invoked once when all animations finish and [currentState] advances to
   * [targetState]. Runs on the main thread from within the frame loop. Set to `null` to clear.
   */
  internal var onSettled: (() -> Unit)? = null

  /**
   * Checks whether all animations have finished. If so, advances [currentState] to [targetState]
   * and invokes [onSettled]. Called via [AnimatedValue.onComplete] when each child animation
   * finishes.
   *
   * The `currentState != targetState` guard makes [onSettled] fire exactly once per settle.
   */
  internal fun checkCompletion() {
    if (!hasAnimator && currentState != targetState) {
      currentState = targetState
      onSettled?.invoke()
    }
  }

  internal fun childStateId(slot: Int): StateId = StateId(
      treeId,
      HookGlobalKey(
          globalKey,
          childHookKey(slot),
          // Scope by context like useAnimatedValue/useAnimatable, so a group used in a
          // layout-scoped component keys its slots in the same namespace as that component's
          // other state rather than always in Resolve.
          if (isLayoutContext) RenderScope.Layout else RenderScope.Resolve,
      ),
  )

  /** A stable child of this group's hook key for [slot]. */
  private fun childHookKey(slot: Int): HookKey =
      HookKey.create(SLOT_CALL_SITE_ID, slot, parent = baseHookKey, intern = false)

  internal fun deriveLabel(animationLabel: String): String =
      when {
        label.isNotEmpty() && animationLabel.isNotEmpty() -> "$label/$animationLabel"
        label.isNotEmpty() -> label
        else -> animationLabel
      }

  companion object {
    // Negative so it can never collide with a compiler-assigned (non-negative) hook call-site id
    // when minting per-slot child HookKeys under hook identity.
    private const val SLOT_CALL_SITE_ID = -1
  }
}

/**
 * A stable, slot-scoped [Value] handed back from [AnimationGroup.animateValue] during `render()`.
 *
 * The long-lived [AnimatedValue] for a slot is not created until commit, but a `Style` binder built
 * during `render()` needs a live handle *now* (it captures the value as a dependency and reads it
 * inside the bind phase to register read tracking). `SlotValue` is that handle: identified by
 * `(state, slot)` — equivalent to the slot's stable [StateId] but allocation-free — it reads the
 * committed [AnimatedValue] once it exists and, before the first commit, falls back to the prop's
 * resting value via the [descriptor]. Reading [value] records the read against the committed slot's
 * id (or its eventual id), the same id that [AnimatedValue] signals on each frame, so per-frame
 * updates rebind exactly this binder.
 */
internal class SlotValue<S, T>(
    private val state: AnimationGroupState<S>,
    private val slot: Int,
    private val descriptor: AnimationDescriptor<S, T>,
) : Value<T> {

  override val value: T
    get() {
      @Suppress("UNCHECKED_CAST") val committed = state.animatedValueAt(slot) as AnimatedValue<T>?
      // Read-track against the committed slot's id, or its eventual id before the first commit.
      StateReadRecorder.read(committed?.stateId ?: state.childStateId(slot))
      // Until commit there is no runtime value; fall back to the prop's resting value, computed
      // from the descriptor we already hold — no extra allocation, no runtime read. Only observed
      // pre-commit, where state.targetState == this render's target, so it matches the committed
      // value's initial value.
      return committed?.currentValueRaw ?: descriptor.targetValueByState(state.targetState)
    }

  override fun equals(other: Any?): Boolean =
      other is SlotValue<*, *> && other.state === state && other.slot == slot

  override fun hashCode(): Int = 31 * System.identityHashCode(state) + slot

  override fun toString(): String = "SlotValue(slot=$slot)"
}

/**
 * The per-render handle returned by [useAnimationGroup] and the public surface for declaring a
 * group's animations ([animateFloat], [animateDimen], …).
 *
 * Each call to [useAnimationGroup] creates a fresh `AnimationGroup` for that render, isolating
 * per-render bookkeeping (slot counter, pending descriptions) from the shared, persistent
 * [AnimationGroupState] — so concurrent resolves never race. Declaring a slot allocates no
 * [AnimatedValue] and mutates nothing here; it records a [AnimationDescriptor] (committed as an
 * [AnimationGroupRecord]) and returns a [Value] handle. It deliberately exposes no animation
 * runtime — settled state (`currentState`, `isRunning`) lives on [AnimationGroupState] and is read
 * from the main thread / effects, never during `render()`.
 *
 * ```kotlin
 * val transition = useAnimationGroup(expanded)
 *
 * val height = transition.animateDimen(
 *     transitionSpec = {
 *         when {
 *             false.isTransitioningTo(true) -> spring(stiffness = Stiffness.Low)
 *             else -> tween(200.ms)
 *         }
 *     },
 * ) { state -> if (state) 200.dp else 56.dp }
 *
 * val alpha = transition.animateFloat { state -> if (state) 1f else 0f }
 * ```
 *
 * @param S The type of the state that drives the transition.
 * @see useAnimationGroup
 * @see AnimationGroupRecord
 * @see AnimationGroupState
 * @see Segment
 */
class AnimationGroup<S>
internal constructor(
    /** The persistent runtime this render's record is committed into. */
    internal val state: AnimationGroupState<S>,
    /** The state that the transition is animating toward in this render (the render prop). */
    val targetState: S,
    private val resourceResolver: ResourceResolver,
) {

  /** Per-render slot counter. Each [animateFloat]/[animateDimen]/etc. call claims one slot. */
  private var renderSlot = 0
  private val pendingDescriptors = mutableListOf<AnimationDescriptor<S, *>>()

  /**
   * Optional callback invoked once when all animations in this group finish and the group settles.
   * Runs on the main thread from within the frame loop. Register it from an effect and clear it
   * (set to `null`) on cleanup. Writes through to the shared [AnimationGroupState], so setting it
   * is stable across renders.
   */
  var onSettled: (() -> Unit)?
    get() = state.onSettled
    set(value) {
      state.onSettled = value
    }

  /** Freezes the slots declared this render into the immutable record committed into [state]. */
  internal fun createRecord(): AnimationGroupRecord<S> =
      AnimationGroupRecord(targetState, pendingDescriptors.toList())

  /**
   * Animates a [Float] property as part of this transition.
   *
   * @param transitionSpec Provides the animation spec, evaluated with the current [Segment] as
   *   receiver for directional spec selection. Defaults to [spring].
   * @param label Optional label for this animation, used for tracing attribution.
   * @param targetValueByState Maps the target state to the target float value.
   * @return A [Value] containing the current animated float.
   */
  fun animateFloat(
      transitionSpec: Segment<S>.() -> FiniteAnimationSpec<Float> = { spring() },
      label: String = "float",
      targetValueByState: (S) -> Float,
  ): Value<Float> = animateValue(FloatTypeConverter, transitionSpec, label, targetValueByState)

  /**
   * Animates an [Int] property as part of this transition.
   *
   * @param transitionSpec Provides the animation spec. Defaults to [spring].
   * @param label Optional label for this animation, used for tracing attribution.
   * @param targetValueByState Maps the target state to the target int value.
   * @return A [Value] containing the current animated int.
   */
  fun animateInt(
      transitionSpec: Segment<S>.() -> FiniteAnimationSpec<Int> = { spring() },
      label: String = "int",
      targetValueByState: (S) -> Int,
  ): Value<Int> = animateValue(IntTypeConverter, transitionSpec, label, targetValueByState)

  /**
   * Animates a [Color] property with per-channel interpolation.
   *
   * @param transitionSpec Provides the animation spec. Defaults to [spring].
   * @param label Optional label for this animation, used for tracing attribution.
   * @param targetValueByState Maps the target state to the target color.
   * @return A [Value] containing the current interpolated color.
   */
  fun animateColor(
      transitionSpec: Segment<S>.() -> FiniteAnimationSpec<Color> = { spring() },
      label: String = "color",
      targetValueByState: (S) -> Color,
  ): Value<Color> = animateValue(ColorTypeConverter, transitionSpec, label, targetValueByState)

  /**
   * Animates a [Dimen] property as part of this transition.
   *
   * @param transitionSpec Provides the animation spec. Defaults to [spring].
   * @param label Optional label for this animation, used for tracing attribution.
   * @param targetValueByState Maps the target state to the target dimension value.
   * @return A [Value] containing the current animated dimension.
   */
  fun animateDimen(
      transitionSpec: Segment<S>.() -> FiniteAnimationSpec<Dimen> = { spring() },
      label: String = "dimen",
      targetValueByState: (S) -> Dimen,
  ): Value<Dimen> =
      animateValue(DimenTypeConverter(resourceResolver), transitionSpec, label, targetValueByState)

  /**
   * Animates a value of type [T] as part of this transition using a custom [TypeConverter].
   *
   * This is the generic form used by [animateFloat], [animateInt], [animateColor], and
   * [animateDimen]. Use it directly when animating a custom type with your own [TypeConverter].
   *
   * The returned [Value] is a stable slot handle: the underlying long-lived [AnimatedValue] is
   * created during commit, not here, so this call performs no shared-state mutation and is safe
   * under concurrent resolves.
   *
   * @param typeConverter Converts between [T] and [Float] for animation computation.
   * @param transitionSpec Provides the animation spec. Defaults to [spring].
   * @param label Optional label for this animation, used for tracing attribution.
   * @param targetValueByState Maps the target state to the target value.
   * @return A [Value] containing the current animated value.
   */
  fun <T> animateValue(
      typeConverter: TypeConverter<T>,
      transitionSpec: Segment<S>.() -> FiniteAnimationSpec<T> = { spring() },
      label: String = "",
      targetValueByState: (S) -> T,
  ): Value<T> {
    val slot = renderSlot++
    val descriptor =
        AnimationDescriptor(slot, typeConverter, transitionSpec, targetValueByState, label)
    pendingDescriptors.add(descriptor)
    return SlotValue(state, slot, descriptor)
  }
}
