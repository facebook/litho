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

import android.content.Context
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [useTreeProp] and [createTreeProp]. */
@Suppress("MagicNumber")
@RunWith(LithoTestRunner::class)
class KTreePropsTest {

  private lateinit var context: ComponentContext

  @Before
  fun setUp() {
    context = ComponentContext(getApplicationContext<Context>())
  }

  @Test
  fun treePropValueIsPropagatedFromParentToChild() {
    val treeProp1Ref = TreePropHolder()
    val treeProp2Ref = TreePropHolder()
    val rect = Rect()

    val parent = KComponent {
      createTreeProp { 32 }
      createTreeProp { rect }

      KComponent { // child
        treeProp1Ref.prop = useTreeProp<Int>()
        treeProp2Ref.prop = useTreeProp<Rect>()
        null
      }
    }
    ComponentTestHelper.mountComponent(context, parent)

    assertThat(treeProp1Ref.prop).isEqualTo(32)
    assertThat(treeProp2Ref.prop).isEqualTo(rect)
  }

  @Test
  fun treePropValueIsOverriddenByIntermediate() {
    val treePropRef = TreePropHolder()

    val parent = KComponent {
      createTreeProp { 18 }

      KComponent { // intermediate
        createTreeProp { 24 } // override TreeProp!

        KComponent { // child
          treePropRef.prop = useTreeProp<Int>()
          null
        }
      }
    }
    ComponentTestHelper.mountComponent(context, parent)

    assertThat(treePropRef.prop).isEqualTo(24)
  }

  @Test
  fun treePropsAreIsolatedBetweenSiblings() {
    val child1StringPropRef = TreePropHolder()
    val child1IntPropRef = TreePropHolder()
    val child2IntPropRef = TreePropHolder()

    val parent = KComponent {
      createTreeProp { "kavabanga" }

      Row(
          children =
              listOf(
                  KComponent { // child 1
                    createTreeProp { 42 }

                    child1StringPropRef.prop = useTreeProp<String>()
                    child1IntPropRef.prop = useTreeProp<Int>()
                    null
                  },
                  KComponent { // child 2
                    child2IntPropRef.prop = useTreeProp<Int>()
                    null
                  }))
    }
    ComponentTestHelper.mountComponent(context, parent)

    assertThat(child1StringPropRef.prop).isEqualTo("kavabanga")
    assertThat(child1IntPropRef.prop).isEqualTo(42)
    assertThat(child2IntPropRef.prop).isNull()
  }
}

class TreePropHolder {
  var prop: Any? = null
}
