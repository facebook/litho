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

package com.facebook.rendercore.utils

import com.facebook.rendercore.Equivalence
import com.facebook.rendercore.RenderCoreConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * Covers [hasEquivalentFields], which compares two objects by reflecting over their declared
 * fields.
 *
 * Every case runs under both settings of [RenderCoreConfig.cacheEquivalenceFields]: the cached
 * field list and the per-comparison one. Both arms ship, so both have to be covered, and running
 * the same assertions against each is also what makes them provably interchangeable.
 *
 * The cached arm keys its field list per class, so the cases below check both what a comparison
 * returns and that repeating it against different instances keeps returning the right answer.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class EquivalenceUtilsTest(private val cacheEquivalenceFields: Boolean) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "cacheEquivalenceFields={0}")
    fun settings(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
  }

  private var previousSetting: Boolean = false

  @Before
  fun setUp() {
    previousSetting = RenderCoreConfig.cacheEquivalenceFields
    RenderCoreConfig.cacheEquivalenceFields = cacheEquivalenceFields
  }

  @After
  fun tearDown() {
    RenderCoreConfig.cacheEquivalenceFields = previousSetting
  }

  private class Simple(val i: Int, val s: String?, val f: Float)

  private class WithStatics(val i: Int) {
    companion object {
      @JvmStatic var mutableStatic: Int = 0
    }
  }

  private class WithArrays(val ints: IntArray, val objects: Array<String>)

  private class WithCollections(val list: List<String>, val set: Set<String>)

  private class Nested(val inner: Simple)

  private class NestedEquivalent(val inner: Equivalent)

  private class SameShapeA(val x: Int, val y: String)

  private class SameShapeB(val x: Int, val y: String)

  private class Equivalent(val id: Int, val ignored: String) : Equivalence<Equivalent> {
    override fun isEquivalentTo(other: Equivalent): Boolean = id == other.id
  }

  @Test
  fun sameInstanceIsEquivalent() {
    val simple = Simple(1, "a", 1f)
    assertThat(hasEquivalentFields(simple, simple)).isTrue
  }

  @Test
  fun nullsAreOnlyEquivalentToThemselves() {
    assertThat(hasEquivalentFields(null, null)).isTrue
    assertThat(hasEquivalentFields(Simple(1, "a", 1f), null)).isFalse
    assertThat(hasEquivalentFields(null, Simple(1, "a", 1f))).isFalse
  }

  @Test
  fun differentClassesAreNotEquivalent() {
    assertThat(hasEquivalentFields(Simple(1, "a", 1f), Nested(Simple(1, "a", 1f)))).isFalse
  }

  @Test
  fun equalFieldValuesAreEquivalent() {
    assertThat(hasEquivalentFields(Simple(1, "a", 1f), Simple(1, "a", 1f))).isTrue
  }

  @Test
  fun eachFieldIsActuallyCompared() {
    // One test per field, so a cache that dropped a field would not hide behind the others.
    assertThat(hasEquivalentFields(Simple(1, "a", 1f), Simple(2, "a", 1f))).isFalse
    assertThat(hasEquivalentFields(Simple(1, "a", 1f), Simple(1, "b", 1f))).isFalse
    assertThat(hasEquivalentFields(Simple(1, "a", 1f), Simple(1, "a", 2f))).isFalse
  }

  @Test
  fun nullFieldValuesAreHandled() {
    assertThat(hasEquivalentFields(Simple(1, null, 1f), Simple(1, null, 1f))).isTrue
    assertThat(hasEquivalentFields(Simple(1, null, 1f), Simple(1, "a", 1f))).isFalse
    assertThat(hasEquivalentFields(Simple(1, "a", 1f), Simple(1, null, 1f))).isFalse
  }

  @Test
  fun staticFieldsDoNotAffectTheResult() {
    // The cached arm drops statics; the uncached arm keeps them. Two instances of the same class
    // read the same static, so neither can be made inequivalent by one.
    val a = WithStatics(1)
    val b = WithStatics(1)
    assertThat(hasEquivalentFields(a, b)).isTrue
    WithStatics.mutableStatic = 42
    assertThat(hasEquivalentFields(a, b)).isTrue
    assertThat(hasEquivalentFields(WithStatics(1), WithStatics(2))).isFalse
  }

  @Test
  fun arrayFieldsAreComparedByContent() {
    assertThat(
        hasEquivalentFields(
            WithArrays(intArrayOf(1, 2), arrayOf("a")),
            WithArrays(intArrayOf(1, 2), arrayOf("a")),
        ),
    )
        .isTrue
    assertThat(
        hasEquivalentFields(
            WithArrays(intArrayOf(1, 2), arrayOf("a")),
            WithArrays(intArrayOf(1, 3), arrayOf("a")),
        ),
    )
        .isFalse
    assertThat(
        hasEquivalentFields(
            WithArrays(intArrayOf(1, 2), arrayOf("a")),
            WithArrays(intArrayOf(1, 2), arrayOf("b")),
        ),
    )
        .isFalse
  }

  @Test
  fun collectionFieldsAreComparedByContent() {
    assertThat(
        hasEquivalentFields(
            WithCollections(listOf("a"), setOf("b")),
            WithCollections(listOf("a"), setOf("b")),
        ),
    )
        .isTrue
    assertThat(
        hasEquivalentFields(
            WithCollections(listOf("a"), setOf("b")),
            WithCollections(listOf("a"), setOf("c")),
        ),
    )
        .isFalse
  }

  @Test
  fun nestedFieldsFallBackToEqualsRatherThanRecursingIntoFields() {
    // Simple has no equals(), so two distinct instances are not equivalent even with equal fields.
    assertThat(hasEquivalentFields(Nested(Simple(1, "a", 1f)), Nested(Simple(1, "a", 1f)))).isFalse
    val shared = Simple(1, "a", 1f)
    assertThat(hasEquivalentFields(Nested(shared), Nested(shared))).isTrue
  }

  @Test
  fun equivalenceFieldsUseIsEquivalentTo() {
    assertThat(
        hasEquivalentFields(
            NestedEquivalent(Equivalent(1, "x")),
            NestedEquivalent(Equivalent(1, "y")),
        ),
    )
        .isTrue
    assertThat(
        hasEquivalentFields(
            NestedEquivalent(Equivalent(1, "x")),
            NestedEquivalent(Equivalent(2, "x")),
        ),
    )
        .isFalse
  }

  @Test
  fun repeatedComparisonsOnTheSameClassStayCorrect() {
    // The cached arm resolves the field list once; make sure later comparisons on fresh instances
    // still see per-instance values and not whatever the first pair held.
    assertThat(hasEquivalentFields(Simple(1, "a", 1f), Simple(1, "a", 1f))).isTrue
    for (i in 0 until 100) {
      assertThat(hasEquivalentFields(Simple(i, "s$i", i.toFloat()), Simple(i, "s$i", i.toFloat())))
          .isTrue
      assertThat(
          hasEquivalentFields(Simple(i, "s$i", i.toFloat()), Simple(i + 1, "s$i", i.toFloat())),
      )
          .isFalse
    }
  }

  @Test
  fun differentClassesWithIdenticalShapesDoNotShareCachedFields() {
    // Two classes whose fields line up exactly, compared alternately so a cache that keyed
    // everything to one entry would hand class A's Field objects to a class B instance.
    for (i in 0 until 10) {
      assertThat(hasEquivalentFields(SameShapeA(i, "a"), SameShapeA(i, "a"))).isTrue
      assertThat(hasEquivalentFields(SameShapeB(i, "a"), SameShapeB(i, "a"))).isTrue
      assertThat(hasEquivalentFields(SameShapeA(i, "a"), SameShapeA(i + 1, "a"))).isFalse
      assertThat(hasEquivalentFields(SameShapeB(i, "a"), SameShapeB(i, "b"))).isFalse
    }
  }

  @Test
  fun bothSettingsAgreeAndSurviveFlipping() {
    // The gate can flip after the cache is warm; alternating it on the same instances catches an
    // arm that diverges as well as one that reads state the other arm left behind.
    val a = Simple(1, "a", 1f)
    val equal = Simple(1, "a", 1f)
    val different = Simple(1, "b", 1f)

    for (cached in listOf(false, true, false, true)) {
      RenderCoreConfig.cacheEquivalenceFields = cached
      assertThat(hasEquivalentFields(a, equal)).isTrue
      assertThat(hasEquivalentFields(a, different)).isFalse
    }
  }

  @Test
  fun classesBeyondTheCacheCapStillCompareCorrectly() {
    val previousCap = maxCachedClasses
    clearInstanceFieldCacheForTesting()
    maxCachedClasses = 1
    try {
      // Takes the single slot; every class after it has to resolve its own fields, and still has
      // to be right about them.
      assertThat(hasEquivalentFields(SameShapeA(1, "a"), SameShapeA(1, "a"))).isTrue

      assertThat(hasEquivalentFields(SameShapeB(1, "a"), SameShapeB(1, "a"))).isTrue
      assertThat(hasEquivalentFields(SameShapeB(1, "a"), SameShapeB(2, "a"))).isFalse
      assertThat(hasEquivalentFields(SameShapeB(1, "a"), SameShapeB(1, "b"))).isFalse
      assertThat(hasEquivalentFields(Simple(1, "a", 1f), Simple(1, "a", 1f))).isTrue
      assertThat(hasEquivalentFields(Simple(1, "a", 1f), Simple(1, "a", 2f))).isFalse
    } finally {
      maxCachedClasses = previousCap
      clearInstanceFieldCacheForTesting()
    }
  }

  @Test
  fun theCacheStopsGrowingAtItsCap() {
    // The cap has no observable effect on results, so entry count is the only thing that can pin
    // it. Sets the flag itself rather than taking the parameter, so it also pins the gate
    // direction: flag on has to mean cache on, or a QE would measure the two arms the wrong way
    // round.
    val previousCap = maxCachedClasses
    val previousSetting = RenderCoreConfig.cacheEquivalenceFields
    clearInstanceFieldCacheForTesting()
    RenderCoreConfig.cacheEquivalenceFields = true
    maxCachedClasses = 2
    try {
      hasEquivalentFields(SameShapeA(1, "a"), SameShapeA(1, "a"))
      assertThat(cachedClassCountForTesting()).isEqualTo(1)
      hasEquivalentFields(SameShapeB(1, "a"), SameShapeB(1, "a"))
      assertThat(cachedClassCountForTesting()).isEqualTo(2)
      hasEquivalentFields(Simple(1, "a", 1f), Simple(1, "a", 1f))
      assertThat(cachedClassCountForTesting()).isEqualTo(2)
    } finally {
      maxCachedClasses = previousCap
      RenderCoreConfig.cacheEquivalenceFields = previousSetting
      clearInstanceFieldCacheForTesting()
    }
  }

  @Test
  fun isEqualOrEquivalentToPrefersEquivalence() {
    assertThat(isEqualOrEquivalentTo(Equivalent(1, "x"), Equivalent(1, "y"))).isTrue
    assertThat(isEqualOrEquivalentTo(Equivalent(1, "x"), Equivalent(2, "x"))).isFalse
    // Not Equivalence, so it falls through to the field comparison.
    assertThat(isEqualOrEquivalentTo(Simple(1, "a", 1f), Simple(1, "a", 1f))).isTrue
    assertThat(isEqualOrEquivalentTo(Simple(1, "a", 1f), Simple(1, "b", 1f))).isFalse
  }
}
