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

package com.facebook.litho

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for props equivalence of KComponent classes. */
@RunWith(AndroidJUnit4::class)
class KComponentEquivalenceTest {

  @Test
  fun kcomponentWithPrivateProps_isEquivalentTo_checksAllFields() {
    class ComponentWithPrivateProps(private val counter: Int, private val name: String) :
        KComponent() {
      override fun ComponentScope.render() = null
    }

    assertThat(
            ComponentWithPrivateProps(0, "Ada").isEquivalentTo(ComponentWithPrivateProps(0, "Ada")))
        .isTrue()
    assertThat(
            ComponentWithPrivateProps(0, "Ada").isEquivalentTo(ComponentWithPrivateProps(1, "Ada")))
        .isFalse()
    assertThat(
            ComponentWithPrivateProps(0, "Ada")
                .isEquivalentTo(ComponentWithPrivateProps(0, "Grace")))
        .isFalse()
  }

  @Test
  fun kcomponentWithPublicProps_isEquivalentTo_checksAllFields() {
    class ComponentWithPublicProps(val counter: Int, val name: String) : KComponent() {
      override fun ComponentScope.render() = null
    }

    assertThat(
            ComponentWithPublicProps(0, "Ada").isEquivalentTo(ComponentWithPublicProps(0, "Ada")))
        .isTrue()
    assertThat(
            ComponentWithPublicProps(0, "Ada").isEquivalentTo(ComponentWithPublicProps(1, "Ada")))
        .isFalse()
    assertThat(
            ComponentWithPublicProps(0, "Ada").isEquivalentTo(ComponentWithPublicProps(0, "Grace")))
        .isFalse()
  }

  @Test
  fun kcomponentWithPublicJVMFieldProps_isEquivalentTo_checksAllFields() {
    class ComponentWithPublicProps(@JvmField val counter: Int, @JvmField val name: String) :
        KComponent() {
      override fun ComponentScope.render() = null
    }

    assertThat(
            ComponentWithPublicProps(0, "Ada").isEquivalentTo(ComponentWithPublicProps(0, "Ada")))
        .isTrue()
    assertThat(
            ComponentWithPublicProps(0, "Ada").isEquivalentTo(ComponentWithPublicProps(1, "Ada")))
        .isFalse()
    assertThat(
            ComponentWithPublicProps(0, "Ada").isEquivalentTo(ComponentWithPublicProps(0, "Grace")))
        .isFalse()
  }

  @Test
  fun kcomponentWithNoProps_isEquivalentTo_isTrue() {
    class ComponentWithNoProps : KComponent() {
      override fun ComponentScope.render() = null
    }

    assertThat(ComponentWithNoProps().isEquivalentTo(ComponentWithNoProps())).isTrue()
  }

  @Test
  fun kcomponentWithNoProps_isEquivalentToWithDifferentType_isFalse() {
    class ComponentWithNoProps : KComponent() {
      override fun ComponentScope.render() = null
    }

    class OtherComponentWithNoProps : KComponent() {
      override fun ComponentScope.render() = null
    }

    assertThat(ComponentWithNoProps().isEquivalentTo(OtherComponentWithNoProps())).isFalse()
  }

  @Test
  fun kcomponentWithComplexProps_isEquivalentTo_usesEqualsMethod() {
    class Counter(val count: Int) {
      override fun equals(other: Any?): Boolean {
        return other is Counter && count == other.count
      }

      override fun hashCode(): Int {
        return count
      }
    }

    data class Name(val name: String)

    class ComponentWithComplexProps(private val counter: Counter, private val name: Name) :
        KComponent() {
      override fun ComponentScope.render() = null
    }

    assertThat(
            ComponentWithComplexProps(Counter(0), Name("Ada"))
                .isEquivalentTo(ComponentWithComplexProps(Counter(0), Name("Ada"))))
        .isTrue()
    assertThat(
            ComponentWithComplexProps(Counter(0), Name("Ada"))
                .isEquivalentTo(ComponentWithComplexProps(Counter(1), Name("Ada"))))
        .isFalse()
    assertThat(
            ComponentWithComplexProps(Counter(0), Name("Ada"))
                .isEquivalentTo(ComponentWithComplexProps(Counter(0), Name("Grace"))))
        .isFalse()
  }
}
