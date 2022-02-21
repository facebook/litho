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

import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.KotlinClass
import com.facebook.litho.widget.KotlinClassSpec
import com.facebook.litho.widget.KotlinObject
import com.facebook.litho.widget.KotlinObjectSpec
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class KotlinSpecPropDefaultsTest {

  @JvmField @Rule var lithoViewRule = LegacyLithoViewRule()

  @Test
  fun `Kotlin object spec with PropDefault get annotation should initialise default value`() {
    lithoViewRule.render {
      KotlinObject.create(lithoViewRule.context)
          .assertion { assertThat(it).isEqualTo(KotlinObjectSpec.propWithDefault) }
          .build()
    }
  }

  @Test
  fun `Kotlin class spec with PropDefault get annotation should initialise default value`() {
    lithoViewRule.render {
      KotlinClass.create(lithoViewRule.context)
          .assertion { assertThat(it).isEqualTo(KotlinClassSpec.propWithDefault) }
          .build()
    }
  }
}
