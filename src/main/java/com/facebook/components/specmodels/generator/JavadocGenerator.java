// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.generator;

import com.facebook.components.specmodels.generator.TypeSpecDataHolder.JavadocSpec;
import com.facebook.components.specmodels.model.PropJavadocModel;
import com.facebook.components.specmodels.model.PropModel;
import com.facebook.components.specmodels.model.SpecModel;

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
