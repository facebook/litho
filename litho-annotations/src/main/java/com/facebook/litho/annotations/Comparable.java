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

package com.facebook.litho.annotations;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates a parameter (Prop, TreeProp or State) indicating what kind of equivalence should be
 * used in the "isEquivalentTo" call.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Comparable {

  int FLOAT = 0;
  int DOUBLE = 1;
  int ARRAY = 2;
  int PRIMITIVE = 3;
  int COMPARABLE_DRAWABLE = 4;
  int COLLECTION_COMPLEVEL_0 = 5;
  int COLLECTION_COMPLEVEL_1 = 6;
  int COLLECTION_COMPLEVEL_2 = 7;
  int COLLECTION_COMPLEVEL_3 = 8;
  int COLLECTION_COMPLEVEL_4 = 9;
  int COMPONENT = 10;
  int EVENT_HANDLER = 11;
  int EVENT_HANDLER_IN_PARAMETERIZED_TYPE = 12;
  int OTHER = 13;
  int STATE_CONTAINER = 14;
  int SECTION = 15;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    FLOAT,
    DOUBLE,
    ARRAY,
    PRIMITIVE,
    COMPARABLE_DRAWABLE,
    COLLECTION_COMPLEVEL_0,
    COLLECTION_COMPLEVEL_1,
    COLLECTION_COMPLEVEL_2,
    COLLECTION_COMPLEVEL_3,
    COLLECTION_COMPLEVEL_4,
    COMPONENT,
    EVENT_HANDLER,
    EVENT_HANDLER_IN_PARAMETERIZED_TYPE,
    OTHER,
    STATE_CONTAINER,
    SECTION
  })
  @interface Type {}

  @Comparable.Type
  int type();
}
