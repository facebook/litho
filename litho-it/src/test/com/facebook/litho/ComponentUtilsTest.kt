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

import com.facebook.litho.annotations.Comparable
import com.facebook.litho.annotations.EventHandlerRebindMode
import com.facebook.litho.config.ComponentsConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * Every case runs under both settings of [ComponentsConfiguration.cacheComparableFields]: the
 * cached per-class list of `@Comparable` fields and the per-comparison one. Both arms ship, so both
 * have to be covered, and running the same assertions against each is also what makes them provably
 * interchangeable.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class ComponentUtilsTest(private val cacheComparableFields: Boolean) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "cacheComparableFields={0}")
    fun settings(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
  }

  private lateinit var c1: ComponentTest
  private lateinit var c2: ComponentTest
  private var previousSetting: Boolean = false

  @Before
  fun setUp() {
    previousSetting = ComponentsConfiguration.cacheComparableFields
    ComponentsConfiguration.cacheComparableFields = cacheComparableFields
    c1 = ComponentTest()
    c2 = ComponentTest()
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.cacheComparableFields = previousSetting
  }

  @Test
  fun hasEquivalentFieldsArrayIntPropTest() {
    c1.propArrayInt = intArrayOf(2, 5, 6)
    c2.propArrayInt = intArrayOf(2, 5, 6)
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propArrayInt = intArrayOf(2, 3)
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsArrayCharPropTest() {
    c1.propArrayChar = charArrayOf('a', 'c', '5')
    c2.propArrayChar = charArrayOf('a', 'c', '5')
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propArrayChar = charArrayOf('a', 'c')
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsStateContainersTest() {
    val sc1 = StateTest(true, 3f)
    var sc2: StateContainer = StateTest(true, 3f)
    assertThat(ComponentUtils.hasEquivalentState(sc1, sc2)).isTrue
    sc2 = StateTest(true, 2f)
    assertThat(ComponentUtils.hasEquivalentState(sc1, sc2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsDoublePropTest() {
    c1.propDouble = 2.0
    c2.propDouble = 2.0
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propDouble = 3.0
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsFloatPropTest() {
    c1.propFloat = 2f
    c2.propFloat = 2f
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propFloat = 3f
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsCharPropTest() {
    c1.propChar = 'c'
    c2.propChar = 'c'
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propChar = 'z'
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsBytePropTest() {
    c1.propByte = 1
    c2.propByte = 1
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propByte = 2
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsShortPropTest() {
    c1.propShort = 3
    c2.propShort = 3
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propShort = 2
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsIntPropTest() {
    c1.propInt = 3
    c2.propInt = 3
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propInt = 2
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsLongPropTest() {
    c1.propLong = 3
    c2.propLong = 3
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propLong = 2
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsBooleanPropTest() {
    c1.propBoolean = true
    c2.propBoolean = true
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propBoolean = false
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsIntBoxedPropTest() {
    c1.propIntBoxed = 3
    c2.propIntBoxed = 3
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propIntBoxed = 2
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
    c2.propIntBoxed = null
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsStringPropTest() {
    c1.propString = "string"
    c2.propString = "string"
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propString = "bla"
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
    c2.propString = null
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsCollectionPropTest() {
    c1.propCollection = listOf("1", "2", "3")
    c2.propCollection = listOf("1", "2", "3")
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propCollection = listOf("2", "3")
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsCollectionWithComponentsPropTest() {
    val innerComponent11 = ComponentTest()
    innerComponent11.propDouble = 2.0
    val innerComponent12 = ComponentTest()
    innerComponent12.propDouble = 2.0
    val innerComponent21 = ComponentTest()
    innerComponent21.propDouble = 2.0
    val innerComponent22 = ComponentTest()
    innerComponent22.propDouble = 2.0
    c1.propCollectionWithComponents = listOf(innerComponent11, innerComponent12)
    c2.propCollectionWithComponents = listOf(innerComponent21, innerComponent22)
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    innerComponent22.propDouble = 3.0
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsComponentPropTest() {
    val innerComponent1 = ComponentTest()
    innerComponent1.propDouble = 2.0
    val innerComponent2 = ComponentTest()
    innerComponent2.propDouble = 2.0
    c1.propComponent = innerComponent1
    c2.propComponent = innerComponent2
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    innerComponent2.propDouble = 3.0
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsEventHandlerPropTest() {
    // The first item of the params is skipped as explained in the EventHandler class.
    val eventDispatchInfo = EventDispatchInfo()
    c1.propEventHandler =
        EventHandler<Any?>(
            3,
            EventHandlerRebindMode.REBIND,
            eventDispatchInfo,
            arrayOf<Any>("", "1"),
        )
    c2.propEventHandler =
        EventHandler<Any?>(
            3,
            EventHandlerRebindMode.REBIND,
            eventDispatchInfo,
            arrayOf<Any>("", "1"),
        )
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.propEventHandler =
        EventHandler<Any?>(
            3,
            EventHandlerRebindMode.REBIND,
            eventDispatchInfo,
            arrayOf<Any>("", "2"),
        )
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsTreePropTest() {
    c1.treePropObject = "1"
    c2.treePropObject = "1"
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
    c2.treePropObject = "2"
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
  }

  @Test
  fun hasEquivalentFieldsIgnoresFieldsWithoutTheComparableAnnotation() {
    c1.propInt = 1
    c2.propInt = 1
    c1.notAProp = "a"
    c2.notAProp = "b"
    assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
  }

  @Test
  fun hasEquivalentFieldsKeepsClassesApart() {
    // Two classes with overlapping field shapes, compared alternately, so a per-class field cache
    // that collapsed to a single entry would hand one class's fields to the other's instance.
    val s1 = StateTest(true, 3f)
    val s2 = StateTest(true, 3f)
    for (i in 0 until 10) {
      c1.propInt = i
      c2.propInt = i
      assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue
      assertThat(ComponentUtils.hasEquivalentFields(s1, s2)).isTrue
      c2.propInt = i + 1
      assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isFalse
      s2.state1 = !s1.state1
      assertThat(ComponentUtils.hasEquivalentFields(s1, s2)).isFalse
      s2.state1 = s1.state1
    }
  }

  @Test
  fun classesBeyondTheCacheCapStillCompareCorrectly() {
    val previousCap = ComponentUtils.maxCachedClasses
    ComponentUtils.clearComparedFieldCacheForTesting()
    ComponentUtils.maxCachedClasses = 1
    try {
      // Takes the single slot; every class after it has to resolve its own fields, and still has
      // to be right about them.
      c1.propInt = 1
      c2.propInt = 1
      assertThat(ComponentUtils.hasEquivalentFields(c1, c2)).isTrue

      val s1 = StateTest(true, 3f)
      val s2 = StateTest(true, 3f)
      assertThat(ComponentUtils.hasEquivalentFields(s1, s2)).isTrue
      s2.state1 = false
      assertThat(ComponentUtils.hasEquivalentFields(s1, s2)).isFalse
      s2.state1 = true
      s2.state2 = 4f
      assertThat(ComponentUtils.hasEquivalentFields(s1, s2)).isFalse
    } finally {
      ComponentUtils.maxCachedClasses = previousCap
      ComponentUtils.clearComparedFieldCacheForTesting()
    }
  }

  @Test
  fun theCacheStopsGrowingAtItsCap() {
    // The cap has no observable effect on results, so entry count is the only thing that can pin
    // it. Sets the flag itself rather than taking the parameter, so it also pins the gate
    // direction: flag on has to mean cache on, or a QE would measure the two arms the wrong way
    // round.
    val previousCap = ComponentUtils.maxCachedClasses
    val previousFlag = ComponentsConfiguration.cacheComparableFields
    ComponentUtils.clearComparedFieldCacheForTesting()
    ComponentsConfiguration.cacheComparableFields = true
    ComponentUtils.maxCachedClasses = 1
    try {
      ComponentUtils.hasEquivalentFields(c1, c2)
      assertThat(ComponentUtils.cachedClassCountForTesting()).isEqualTo(1)
      ComponentUtils.hasEquivalentFields(StateTest(true, 3f), StateTest(true, 3f))
      assertThat(ComponentUtils.cachedClassCountForTesting()).isEqualTo(1)
    } finally {
      ComponentUtils.maxCachedClasses = previousCap
      ComponentsConfiguration.cacheComparableFields = previousFlag
      ComponentUtils.clearComparedFieldCacheForTesting()
    }
  }

  @Test
  fun hasEquivalentFieldsRejectsMismatchedClasses() {
    // Asserting the message too: without the explicit check, reflecting one class's fields off
    // the other instance also throws IllegalArgumentException, so the type alone proves nothing.
    assertThatThrownBy { ComponentUtils.hasEquivalentFields(c1, StateTest(true, 3f)) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("The input is invalid.")
  }

  private class ComponentTest : Component() {
    @field:Comparable(type = Comparable.ARRAY) var propArrayInt: IntArray = intArrayOf()

    @field:Comparable(type = Comparable.ARRAY) var propArrayChar: CharArray = charArrayOf()

    @field:Comparable(type = Comparable.DOUBLE) var propDouble: Double = 0.0

    @field:Comparable(type = Comparable.FLOAT) var propFloat: Float = 0f

    @field:Comparable(type = Comparable.PRIMITIVE) var propChar: Char = 0.toChar()

    @field:Comparable(type = Comparable.PRIMITIVE) var propByte: Byte = 0

    @field:Comparable(type = Comparable.PRIMITIVE) var propShort: Short = 0

    @field:Comparable(type = Comparable.PRIMITIVE) var propInt: Int = 0

    @field:Comparable(type = Comparable.PRIMITIVE) var propLong: Long = 0

    @field:Comparable(type = Comparable.PRIMITIVE) var propBoolean: Boolean = false

    @field:Comparable(type = Comparable.OTHER) var propIntBoxed: Int? = null

    @field:Comparable(type = Comparable.OTHER) var propString: String? = null

    @field:Comparable(type = Comparable.COLLECTION_COMPLEVEL_0)
    var propCollection: Collection<String>? = null

    @field:Comparable(type = Comparable.COLLECTION_COMPLEVEL_1)
    var propCollectionWithComponents: Collection<Component>? = null

    @field:Comparable(type = Comparable.COMPONENT) var propComponent: Component? = null

    @field:Comparable(type = Comparable.EVENT_HANDLER) var propEventHandler: EventHandler<*>? = null

    @field:Comparable(type = Comparable.OTHER) var treePropObject: Any? = null

    /** Deliberately not annotated: `hasEquivalentFields` must not look at it. */
    var notAProp: String? = null

    override fun resolve(
        resolveContext: ResolveContext,
        scopedComponentInfo: ScopedComponentInfo,
        parentWidthSpec: Int,
        parentHeightSpec: Int,
    ): ComponentResolveResult {
      error("Not yet implemented")
    }

    override fun resolveDeferred(
        calculationContext: CalculationContext,
        componentContext: ComponentContext,
        parentContext: ComponentContext,
    ): ComponentResolveResult {
      error("Not yet implemented")
    }
  }

  private class StateTest(
      @field:Comparable(type = Comparable.PRIMITIVE) var state1: Boolean = false,
      @field:Comparable(type = Comparable.FLOAT) var state2: Float = 0f,
  ) : StateContainer() {
    override fun applyStateUpdate(stateUpdate: StateUpdate) = Unit
  }
}
