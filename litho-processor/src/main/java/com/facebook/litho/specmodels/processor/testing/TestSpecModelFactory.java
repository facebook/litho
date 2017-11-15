/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor.testing;

import com.facebook.litho.annotations.TestSpec;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.testing.DefaultTestSpecGenerator;
import com.facebook.litho.specmodels.model.testing.TestSpecGenerator;
import com.facebook.litho.specmodels.model.testing.TestSpecModel;
import com.facebook.litho.specmodels.processor.InterStageStore;
import com.facebook.litho.specmodels.processor.JavadocExtractor;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.facebook.litho.specmodels.processor.MountSpecModelFactory;
import com.facebook.litho.specmodels.processor.SpecModelFactory;
import com.facebook.litho.specmodels.processor.TestTargetExtractor;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class TestSpecModelFactory implements SpecModelFactory<TestSpecModel> {

  private final TestSpecGenerator mTestSpecGenerator;

  public TestSpecModelFactory() {
    this(new DefaultTestSpecGenerator());
  }

  public TestSpecModelFactory(TestSpecGenerator testSpecGenerator) {
    mTestSpecGenerator = testSpecGenerator;
  }

  /**
   * Extract the relevant Elements to work with from the round environment before they're passed on
   * to {@link SpecModelFactory#create(Elements, TypeElement, DependencyInjectionHelper,
   * InterStageStore)}.
   */
  @Override
  public Set<Element> extract(RoundEnvironment roundEnvironment) {
    return (Set<Element>) roundEnvironment.getElementsAnnotatedWith(TestSpec.class);
  }

  /**
   * Create a {@link SpecModel} from the given {@link TypeElement} and an optional {@link
   * DependencyInjectionHelper}.
   */
  @Override
  public TestSpecModel create(
      Elements elements,
      TypeElement element,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore interStageStore) {
    final TypeElement valueElement = TestTargetExtractor.getTestSpecValue(element);
    if (valueElement == null) {
      throw new NullPointerException(
          "Failed to extract referenced class in TestSpec. "
              + "Please report this error to the Litho team.");
    }

    final SpecModel enclosedSpecModel =
        getEnclosedSpecModel(elements, valueElement, dependencyInjectionHelper, interStageStore);

    return new TestSpecModel(
        element.getQualifiedName().toString(),
        "",
        enclosedSpecModel.getProps(),
        enclosedSpecModel.getExtraBuilderMethods(),
        enclosedSpecModel.getPropJavadocs(),
        enclosedSpecModel,
        mTestSpecGenerator,
        JavadocExtractor.getClassJavadoc(elements, element));
  }

  /** @return List of props for the original, annotated Spec. */
  @Nullable
  private static SpecModel getEnclosedSpecModel(
      Elements elements,
      TypeElement element,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore interStageStore) {
    final List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

    for (AnnotationMirror annotationMirror : annotationMirrors) {
      final String annotationName = annotationMirror.getAnnotationType().toString();

      final SpecModelFactory factory;
      if (ClassNames.LAYOUT_SPEC.toString().equals(annotationName)) {
        factory = new LayoutSpecModelFactory();
      } else if (ClassNames.MOUNT_SPEC.toString().equals(annotationName)) {
        factory = new MountSpecModelFactory();
      } else {
        factory = null;
      }

      if (factory != null) {
        return factory.create(elements, element, dependencyInjectionHelper, interStageStore);
      }
    }
    return null;
  }
}
