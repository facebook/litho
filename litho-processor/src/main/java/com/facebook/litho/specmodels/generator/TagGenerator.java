/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.TagModel;
import com.squareup.javapoet.ClassName;
import java.util.Set;

public class TagGenerator {
  private TagGenerator() {}

  public static TypeSpecDataHolder generate(
      SpecModel specModel, Set<ClassName> blacklistedInterfaces) {
    TypeSpecDataHolder.Builder dataHolder = TypeSpecDataHolder.newBuilder();

    for (TagModel tagModel : specModel.getTags()) {
      if (!blacklistedInterfaces.contains(tagModel.name)) {
        dataHolder.addSuperInterface(tagModel.name);
      }
    }

    return dataHolder.build();
  }
}
