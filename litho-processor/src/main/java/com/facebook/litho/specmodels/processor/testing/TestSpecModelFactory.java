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

package com.facebook.litho.specmodels.processor.testing;

import com.facebook.litho.annotations.TestSpec;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.testing.DefaultTestSpecGenerator;
import com.facebook.litho.specmodels.model.testing.TestSpecGenerator;
import com.facebook.litho.specmodels.model.testing.TestSpecModel;
import com.facebook.litho.specmodels.processor.ComponentsProcessingException;
import com.facebook.litho.specmodels.processor.InterStageStore;
import com.facebook.litho.specmodels.processor.JavadocExtractor;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.facebook.litho.specmodels.processor.MountSpecModelFactory;
import com.facebook.litho.specmodels.processor.SpecModelFactory;
import com.facebook.litho.specmodels.processor.TestTargetExtractor;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

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
   * to {@link SpecModelFactory#create(Elements, Types, TypeElement, Messager, EnumSet,
   * DependencyInjectionHelper, InterStageStore)}.
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
      Types types,
      TypeElement element,
      Messager messager,
      EnumSet<RunMode> runMode,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore interStageStore) {

    validateElementIsInterface(element);
    validateElementIsEmpty(element);

    final TypeElement valueElement = TestTargetExtractor.getTestSpecValue(element);
    if (valueElement == null) {
      throw new ComponentsProcessingException(
          element,
          "Failed to extract referenced class in TestSpec. "
              + "Please report this error to the Litho team.");
    }

    final SpecModel enclosedSpecModel =
        getEnclosedSpecModel(
            elements,
            types,
            valueElement,
            messager,
            dependencyInjectionHelper,
            interStageStore,
            runMode);

    if (enclosedSpecModel == null) {
      final String error;
      if (!valueElement.toString().endsWith("Spec")) {
        error =
            String.format(
                "It looks like the class you referenced for your TestSpec is not a "
                    + "Spec itself as '%s' doesn't end in 'Spec'. Make sure that you reference the "
                    + "Spec and not the generated Component.",
                valueElement);
      } else {
        error =
            String.format(
                "Failed to extract enclosed spec model: %s. "
                    + "Please ensure that the class is a spec annotated with either @LayoutSpec or "
                    + "@MountSpec.",
                valueElement);
      }

      throw new ComponentsProcessingException(element, error);
    }

    return new TestSpecModel(
        element.getQualifiedName().toString(),
        "",
        enclosedSpecModel.getProps(),
        enclosedSpecModel.getInjectProps(),
        enclosedSpecModel.getExtraBuilderMethods(),
        enclosedSpecModel.getPropJavadocs(),
        enclosedSpecModel.getTypeVariables(),
        enclosedSpecModel,
        mTestSpecGenerator,
        JavadocExtractor.getClassJavadoc(elements, element),
        enclosedSpecModel.getDependencyInjectionHelper());
  }

  private void validateElementIsEmpty(TypeElement element) {
    if (!element.getEnclosedElements().isEmpty()) {
      final String elements =
          element.getEnclosedElements().stream()
              .map(el -> el.asType() + " " + el.getSimpleName())
              .collect(Collectors.joining(", "));

      final String decl =
          element.getEnclosedElements().size() == 1
              ? "this function declaration"
              : "these function declarations";

      throw new ComponentsProcessingException(
          element,
          String.format(
              "TestSpec interfaces must not contain any members. Please remove %s: %s",
              decl, elements));
    }
  }

  private void validateElementIsInterface(TypeElement element) {
    if (element.getKind() != ElementKind.INTERFACE) {
      throw new ComponentsProcessingException(
          element,
          String.format(
              "Specs annotated with @TestSpecs must be interfaces and cannot be of kind %s.",
              element.getKind()));
    }
  }

  /** @return List of props for the original, annotated Spec. */
  @Nullable
  private static SpecModel getEnclosedSpecModel(
      Elements elements,
      Types types,
      TypeElement element,
      Messager messager,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper,
      @Nullable InterStageStore interStageStore,
      EnumSet<RunMode> runMode) {
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
        return factory.create(
            elements,
            types,
            element,
            messager,
            runMode,
            dependencyInjectionHelper,
            interStageStore);
      }
    }
    return null;
  }
}
