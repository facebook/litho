/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.Binder;
import javax.annotation.Nullable;

/**
 * A configuration object the {@link RecyclerCollectionComponent} will use to determine which layout
 * manager should be used for the {@link RecyclerView}
 */
public interface RecyclerConfiguration {

  <E extends Binder<RecyclerView> & SectionTree.Target> E buildTarget(ComponentContext c);

  @Nullable
  SnapHelper getSnapHelper();
}
