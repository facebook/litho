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

import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.EventHandlerBindingSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests to make sure we properly dispatch events to the latest versions of event handlers when
 * eventhandlers are used across trees.
 */
@RunWith(LithoTestRunner::class)
class EventHandlerRebindTest {

  @Rule @JvmField val lithoViewRule: LithoViewRule = LithoViewRule()

  @Test
  fun `test section renders`() {
    lithoViewRule.render {
      RecyclerCollectionComponent.create(context)
          .section(EventHandlerBindingSection.create(SectionContext(context)))
          .build()
    }
  }
}
