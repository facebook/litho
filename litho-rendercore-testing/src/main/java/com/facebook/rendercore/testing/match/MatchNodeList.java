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

package com.facebook.rendercore.testing.match;

import java.util.Arrays;
import java.util.List;

/**
 * A list of match nodes -- this can be used to match a list of actual objects against a list of
 * match nodes.
 */
public class MatchNodeList {

  private final List<MatchNode> mList;

  public MatchNodeList(MatchNode... nodes) {
    mList = Arrays.asList(nodes);
  }

  public List<MatchNode> getList() {
    return mList;
  }

  @Override
  public String toString() {
    return mList.toString();
  }
}
