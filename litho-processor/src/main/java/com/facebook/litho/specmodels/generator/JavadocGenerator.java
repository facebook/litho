/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder.JavadocSpec;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;

/**
 * Class that generates the state methods for a Component.
 */
public class JavadocGenerator {
  private JavadocGenerator() {
  }

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final TypeSpecDataHolder.Builder typeSpecDataHolder = TypeSpecDataHolder.newBuilder();

    final String classJavadoc = specModel.getClassJavadoc();
    if (classJavadoc != null) {
      typeSpecDataHolder
          .addJavadoc(new JavadocSpec(classJavadoc))
          .addJavadoc(new JavadocSpec("<p>\n"));
    }

    for (PropModel prop : specModel.getProps()) {
      final String propTag = prop.isOptional() ? "@prop-optional" : "@prop-required";

      // Adds javadoc with following format:
      // @prop-required name type javadoc.
      // This can be changed later to use clear demarcation for fields.
      // This is a block tag and cannot support inline tags like "{@link something}".
      typeSpecDataHolder.addJavadoc(
          new JavadocSpec(
              "$L $L $L $L\n",
              propTag,
              prop.getName(),
              prop.getType(),
              getPropJavadocForProp(specModel, prop)));
    }

    return typeSpecDataHolder.build();
  }

  private static String getPropJavadocForProp(SpecModel specModel, PropModel prop) {
    for (PropJavadocModel propJavadoc : specModel.getPropJavadocs()) {
      if (prop.getName().equals(propJavadoc.propName)) {
        return propJavadoc.javadoc;
      }
    }

    return "";
  }
}
