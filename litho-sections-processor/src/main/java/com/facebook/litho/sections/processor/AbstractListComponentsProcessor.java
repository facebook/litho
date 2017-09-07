/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor;

import static com.facebook.litho.specmodels.processor.ProcessorUtils.validate;

import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.processor.specmodels.model.DiffSectionSpecModel;
import com.facebook.litho.sections.processor.specmodels.model.GroupSectionSpecModel;
import com.facebook.litho.sections.processor.specmodels.processor.DiffSectionSpecModelFactory;
import com.facebook.litho.sections.processor.specmodels.processor.GroupSectionSpecModelFactory;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.processor.PrintableException;
import java.io.Closeable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Processor used to generate {@link SectionLifecycle} and {@link Section} classes for a {@link
 * GroupSectionSpec} or a {@link DiffSectionSpec}
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractListComponentsProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      return false;
    }

    for (Element element : roundEnv.getRootElements()) {
      try {
        Closeable closeable = null;
        if (element.getAnnotation(GroupSectionSpec.class) != null) {
          final GroupSectionSpecModel groupSectionSpecModel = GroupSectionSpecModelFactory.create(
              processingEnv.getElementUtils(),
              (TypeElement) element,
              getDependencyInjectionGenerator((TypeElement) element));
          validate(groupSectionSpecModel);
          final GroupSectionSpecHelper groupSectionSpecHelper = new GroupSectionSpecHelper(
              processingEnv,
              (TypeElement) element,
              true,
              groupSectionSpecModel);
          closeable = groupSectionSpecHelper;
          generate(groupSectionSpecHelper);
        } else if (element.getAnnotation(DiffSectionSpec.class) != null) {
          final DiffSectionSpecModel diffSectionSpecModel = DiffSectionSpecModelFactory.create(
              processingEnv.getElementUtils(),
              (TypeElement) element,
              getDependencyInjectionGenerator((TypeElement) element));
          validate(diffSectionSpecModel);
          final DiffSectionSpecHelper diffSectionSpecHelper = new DiffSectionSpecHelper(
              processingEnv,
              (TypeElement) element,
              true,
              diffSectionSpecModel);
          closeable = diffSectionSpecHelper;
          generate(diffSectionSpecHelper);
        }

        if (closeable != null) {
          closeable.close();
        }
      } catch (PrintableException e) {
        e.print(processingEnv.getMessager());
      } catch (Exception e) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            "Unexpected error thrown when generating this component spec. " +
                "Please report stack trace to the components team.",
            element);
        e.printStackTrace();
      }
    }

    return false;
  }

  @Nullable
  abstract protected DependencyInjectionHelper getDependencyInjectionGenerator(
      TypeElement typeElement);

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new LinkedHashSet<>(Arrays.asList(
        "com.facebook.litho.sections.annotations.DiffSectionSpec",
        "com.facebook.litho.sections.annotations.GroupSectionSpec"));
  }

  /**
   * Generate classes for a {@link GroupSectionSpec}
   */
  abstract protected void generate(GroupSectionSpecHelper referenceSpecHelper);

  /**
   * Generate classes for a {@link DiffSectionSpec}
   */
  abstract protected void generate(DiffSectionSpecHelper diffSectionSpecHelper);
}
