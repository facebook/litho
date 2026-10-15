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

package com.facebook.litho.specmodels.generator

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.EmptyComponent
import com.facebook.litho.annotations.Event
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.FromTrigger
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.Reason
import com.facebook.litho.specmodels.internal.RunMode
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory
import com.google.testing.compile.CompilationRule
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName
import java.util.Date
import java.util.EnumSet
import javax.lang.model.element.Modifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

/**
 * Under `RunMode.ABI` the processor blanks every value it takes from a spec's `@Event` classes —
 * unconditionally, whether or not the class is on the reduced classpath, which is why the events
 * declared here in the spec's own compilation unit are blanked too. This pins which parts of the
 * generated API are allowed to notice: only the package-private `dispatch<X>` helpers, which are
 * omitted outright, may differ from a normal run. Anything else drifting means an ABI jar that
 * advertises a member the library jar does not have — the shape that compiles clean and throws
 * `NoSuchMethodError`.
 */
@RunWith(JUnit4::class)
class SourceOnlyAbiEventParityTest {

  @Rule @JvmField val compilationRule = CompilationRule()

  private val normalMode by lazy { generateComponent(RunMode.normal()) }
  private val abiMode by lazy { generateComponent(EnumSet.of(RunMode.ABI)) }

  @Test
  fun `abi mode keeps every referenceable member of a normal run, descriptor for descriptor`() {
    assertThat(abiMode.keys).containsAll(normalMode.filterReferenceable().keys)
  }

  @Test
  fun `abi mode adds nothing a normal run does not have`() {
    assertThat(normalMode.keys).containsAll(abiMode.keys)
  }

  @Test
  fun `the dispatch helpers are the only members an abi run omits`() {
    assertThat(normalMode.keys - abiMode.keys)
        .containsExactlyInAnyOrder(
            "dispatchDateLikeEvent(com.facebook.litho.EventHandler,java.util.Date):void",
            "dispatchFieldlessEvent(com.facebook.litho.EventHandler):void",
            "dispatchUnannotatedListedEvent(com.facebook.litho.EventHandler,java.lang.String):void",
            "dispatchTriggerEvent(com.facebook.litho.EventHandler,int):java.lang.Boolean",
            "dispatchHandledEvent(com.facebook.litho.EventHandler,java.lang.String):void",
        )
  }

  @Test
  fun `every omitted member is package-private, so no outside-package caller can reference it`() {
    val omitted = normalMode.keys - abiMode.keys
    assertThat(omitted.map { normalMode.getValue(it).modifiers }).allSatisfy { modifiers ->
      assertThat(modifiers).doesNotContain(Modifier.PUBLIC, Modifier.PROTECTED)
    }
  }

  /**
   * The exact regression: `DateLikeEvent` has a `Date` field, so the real dispatcher takes it. Left
   * to emulate the event from a blanked model, an ABI run wrote
   * `dispatchDateLikeEvent(EventHandler)` here — a descriptor the library jar has never had.
   */
  @Test
  fun `a field-bearing event keeps its real dispatcher shape in a normal run and none in an abi run`() {
    assertThat(normalMode)
        .containsKey("dispatchDateLikeEvent(com.facebook.litho.EventHandler,java.util.Date):void")
    assertThat(abiMode.keys.filter { it.startsWith("dispatchDateLikeEvent(") }).isEmpty()
  }

  /**
   * Negative control. A fieldless event's dispatcher already agreed across the two jars, so nothing
   * about it was ever unsafe; it is omitted anyway, because "the event has no fields" and "this run
   * cannot see the event" are the same blanked model, and an ABI that depended on telling them
   * apart would depend on the dep graph rather than on this source.
   */
  @Test
  fun `a fieldless event is omitted too, rather than trusted`() {
    assertThat(normalMode)
        .containsKey("dispatchFieldlessEvent(com.facebook.litho.EventHandler):void")
    assertThat(abiMode.keys.filter { it.startsWith("dispatchFieldlessEvent(") }).isEmpty()
  }

  /**
   * A class listed in `events = {...}` need not carry `@Event`; its fields still shape dispatch.
   */
  @Test
  fun `an event listed without the annotation is treated the same way`() {
    assertThat(normalMode)
        .containsKey(
            "dispatchUnannotatedListedEvent(com.facebook.litho.EventHandler,java.lang.String):void",
        )
    assertThat(abiMode.keys.filter { it.startsWith("dispatchUnannotatedListedEvent(") }).isEmpty()
  }

  /**
   * `@OnTrigger` statics are public and cross-target: their return type comes from the spec method
   * in an ABI run, so it must survive. This is the shape that would break loudly if the extractors
   * were switched to read a stubbed `@Event` class instead.
   */
  @Test
  fun `public trigger statics keep their real return type in an abi run`() {
    val triggerStatics =
        normalMode.filterReferenceable().keys.filter { it.startsWith("pullTriggerEvent(") }
    assertThat(triggerStatics)
        .containsExactlyInAnyOrder(
            "pullTriggerEvent(com.facebook.litho.ComponentContext,com.facebook.litho.Handle,int):java.lang.Boolean",
            "pullTriggerEvent(com.facebook.litho.ComponentContext,java.lang.String,int):java.lang.Boolean",
            "pullTriggerEvent(com.facebook.litho.EventTrigger,int):java.lang.Boolean",
        )
    assertThat(abiMode.keys).containsAll(triggerStatics)
  }

  /**
   * `@OnEvent` public factories and `get<X>Handler` accessors carry the event in their signature.
   */
  @Test
  fun `public event handler factories and accessors survive an abi run`() {
    val publicEventApi =
        normalMode.filterReferenceable().keys.filter {
          it.startsWith("onHandledEvent(") || it.startsWith("getDateLikeEventHandler(")
        }
    assertThat(publicEventApi).hasSize(2)
    assertThat(abiMode.keys).containsAll(publicEventApi)
  }

  /**
   * The one referenceable API `@Event` fields still shape, and the reason this suite pins erasure
   * rather than whole signatures. Recovering a generic event's type-variable bounds needs the
   * event's field declarations, which map its type parameters onto the spec's `@FromEvent` params;
   * an ABI run has neither, so the factory's bounds can widen. Linkage resolves on the descriptor,
   * which does not move — the real `DataDiffSection.onRenderEvent` shows the widening as `<T>`
   * against `<T, T0>` between its class ABI and its source-only ABI, with both erasing to
   * `(SectionContext)EventHandler`. Recovering the bounds would make the ABI depend on whether the
   * event is on the reduced classpath, so it is deliberately not part of this change.
   */
  @Test
  fun `a generic event's public factory keeps its descriptor in an abi run`() {
    val factory =
        "onGenericPayload(com.facebook.litho.ComponentContext):com.facebook.litho.EventHandler"
    assertThat(normalMode).containsKey(factory)
    assertThat(abiMode).containsKey(factory)
    assertThat(normalMode.getValue(factory).modifiers).contains(Modifier.PUBLIC)
  }

  private fun generateComponent(runMode: EnumSet<RunMode>): Map<String, MethodSpec> {
    val typeElement =
        compilationRule.elements.getTypeElement(AbiParitySpec::class.java.canonicalName)
    val specModel =
        LayoutSpecModelFactory()
            .create(
                compilationRule.elements,
                compilationRule.types,
                typeElement,
                mock(),
                runMode,
                null,
            )
    return specModel.generate(runMode).methodsByDescriptor()
  }

  /** Keys every method of the generated class and its nested types by its erased JVM shape. */
  private fun TypeSpec.methodsByDescriptor(prefix: String = ""): Map<String, MethodSpec> {
    val here = methodSpecs.associateBy { method ->
      val params = method.parameters.joinToString(",") { erasure(it.type) }
      // A constructor has no return type in javapoet; keep the owner prefix so that nested
      // types (the Builder, the state container) cannot collide with the component's own.
      val returns = method.returnType?.let { erasure(it) } ?: "void"
      "$prefix${method.name}($params):$returns"
    }
    return here +
        typeSpecs.fold(emptyMap()) { acc, nested ->
          acc + nested.methodsByDescriptor("${nested.name}.")
        }
  }

  private fun Map<String, MethodSpec>.filterReferenceable(): Map<String, MethodSpec> =
      filterValues {
        it.modifiers.contains(Modifier.PUBLIC) || it.modifiers.contains(Modifier.PROTECTED)
      }

  /** Erasure is what linkage resolves on, so it is what the two jars have to agree about. */
  private fun erasure(type: TypeName): String =
      when (type) {
        is ParameterizedTypeName -> type.rawType.toString()
        is TypeVariableName ->
            if (type.bounds.isEmpty()) "java.lang.Object" else erasure(type.bounds[0])
        is WildcardTypeName ->
            if (type.upperBounds.isEmpty()) "java.lang.Object" else erasure(type.upperBounds[0])
        is ArrayTypeName -> "${erasure(type.componentType)}[]"
        else -> type.toString()
      }

  // A Spec is the subject under test here: this suite exists to compare what the Spec-API
  // processor emits in each run mode.
  @Suppress("DeprecatedLayoutSpecApiInKotlin", "DEPRECATION")
  @ExcuseMySpec(reason = Reason.LEGACY)
  @LayoutSpec(
      events =
          [
              DateLikeEvent::class,
              FieldlessEvent::class,
              UnannotatedListedEvent::class,
              TriggerEvent::class,
              HandledEvent::class,
          ],
  )
  private object AbiParitySpec {

    @OnCreateLayout
    fun onCreateLayout(c: ComponentContext, @Prop title: String): Component = EmptyComponent()

    @OnEvent(HandledEvent::class)
    fun onHandledEvent(c: ComponentContext, @FromEvent payload: String, @Param extra: Int) = Unit

    @OnEvent(GenericPayloadEvent::class)
    fun <T> onGenericPayload(c: ComponentContext, @FromEvent model: T) = Unit

    @OnTrigger(TriggerEvent::class)
    fun pullTriggerEvent(c: ComponentContext, @FromTrigger count: Int): Boolean = count > 0
  }

  @Event
  private class DateLikeEvent {
    @JvmField var selected: Date? = null
  }

  @Event private class FieldlessEvent

  private class UnannotatedListedEvent {
    @JvmField var label: String? = null
  }

  @Event(returnType = Boolean::class)
  private class TriggerEvent {
    @JvmField var count: Int = 0
  }

  @Event
  private class HandledEvent {
    @JvmField var payload: String? = null
  }

  /** Its field is typed by the event's own type parameter, which is what bound recovery needs. */
  @Event
  private class GenericPayloadEvent<T> {
    @JvmField var model: T? = null
  }
}
