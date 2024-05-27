// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho.testing.api

import com.facebook.litho.ComponentContext
import com.facebook.litho.widget.ExperimentalRecycler
import com.facebook.litho.widget.Recycler

/**
 * Returns the name of the recycler component that is currently in use. This will depend on the
 * [com.facebook.litho.ComponentsConfiguration#primitiveRecyclerEnabled]
 */
internal fun recyclerComponentName(componentContext: ComponentContext): String {
  val recyclerInUse =
      if (componentContext.lithoConfiguration.componentsConfig.primitiveRecyclerEnabled)
          ExperimentalRecycler::class.java
      else Recycler::class.java
  return recyclerInUse.simpleName
}
