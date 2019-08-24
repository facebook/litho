/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.generator;

/** Constants to be used when generating a Component. */
public interface GeneratorConstants {
  String ABSTRACT_PARAM_NAME = "_abstract";
  String REF_VARIABLE_NAME = "_ref";
  String STATE_CONTAINER_FIELD_NAME = "mStateContainer";
  String PREVIOUS_RENDER_DATA_FIELD_NAME = "mPreviousRenderData";
  String STATE_CONTAINER_NAME_SUFFIX = "StateContainer";
  String STATE_UPDATE_NAME_SUFFIX = "StateUpdate";
  String STATE_TRANSITION_FIELD_NAME = "_transition";
  String DYNAMIC_PROPS = "mDynamicProps";
}
