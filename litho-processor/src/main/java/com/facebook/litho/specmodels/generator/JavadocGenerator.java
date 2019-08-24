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

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder.JavadocSpec;
import com.facebook.litho.specmodels.model.PropJavadocModel;
import com.facebook.litho.specmodels.model.PropModel;
import com.facebook.litho.specmodels.model.SpecModel;

/** Class that generates the state methods for a Component. */
public class JavadocGenerator {
  private JavadocGenerator() {}

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
              prop.getTypeName(),
              getPropJavadocForProp(specModel, prop)));
    }

    if (specModel.getSpecTypeName() != null) {
      // Add a link back to the spec this was build from for easier jumping.
      typeSpecDataHolder.addJavadoc(
          new JavadocSpec("\n@see $N\n", specModel.getSpecTypeName().toString()));
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
