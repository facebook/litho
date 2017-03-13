// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.MountSpec;
import com.facebook.components.annotations.ReferenceSpec;
import com.facebook.components.specmodels.model.ClassNames;
import com.facebook.components.specmodels.model.DependencyInjectionHelper;
import com.facebook.components.specmodels.model.LayoutSpecModel;
import com.facebook.components.specmodels.model.SpecModelValidation;
import com.facebook.components.specmodels.model.SpecModelValidationError;
import com.facebook.components.specmodels.processor.LayoutSpecModelFactory;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractComponentsProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      return false;
    }

    for (Element element : roundEnv.getRootElements()) {
      try {
        Closeable closeable = null;
        final TypeElement typeElement = (TypeElement) element;
        if (element.getAnnotation(LayoutSpec.class) != null) {
          processingEnv.getMessager().printMessage(
              Diagnostic.Kind.NOTE,
              "annotation_processor_invoked",
              element);

          final LayoutSpecHelper layoutSpecHelper = createLayoutSpecHelper(typeElement);
          closeable = layoutSpecHelper;
          generate(layoutSpecHelper);
        } else if (element.getAnnotation(MountSpec.class) != null) {
          processingEnv.getMessager().printMessage(
              Diagnostic.Kind.NOTE,
              "annotation_processor_invoked",
              element);

          final MountSpecHelper mountSpecHelper =
              new MountSpecHelper(processingEnv, (TypeElement) element);
          closeable = mountSpecHelper;
          generate(mountSpecHelper);
        } else if (element.getAnnotation(ReferenceSpec.class) != null) {
          final ReferenceSpecHelper referenceSpecHelper = new ReferenceSpecHelper(
              processingEnv,
              (TypeElement) element,
              true,
              element.getAnnotation(ReferenceSpec.class).value());
          closeable = referenceSpecHelper;
          generate(referenceSpecHelper);
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

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(Arrays.asList(
        ClassNames.LAYOUT_SPEC.toString(),
        ClassNames.MOUNT_SPEC.toString(),
        ClassNames.REFERENCE_SPEC.toString()));
  }

  abstract protected void generate(ReferenceSpecHelper referenceSpecHelper);

  abstract protected void generate(LayoutSpecHelper layoutSpecHelper);

  abstract protected void generate(MountSpecHelper mountSpecHelper);

  abstract protected DependencyInjectionHelper getDependencyInjectionGenerator(
      TypeElement typeElement);

  private LayoutSpecHelper createLayoutSpecHelper(TypeElement typeElement) {
    final LayoutSpecModel layoutSpecModel =
        LayoutSpecModelFactory.create(
            processingEnv.getElementUtils(),
            typeElement,
            getDependencyInjectionGenerator(typeElement));

    List<SpecModelValidationError> validationErrors =
        SpecModelValidation.validateLayoutSpecModel(layoutSpecModel);

    if (!validationErrors.isEmpty()) {
      final List<PrintableException> printableExceptions = new ArrayList<>();
      for (SpecModelValidationError validationError : validationErrors) {
        printableExceptions.add(
            new ComponentsProcessingException(
                (Element) validationError.element,
                validationError.message));
      }

      throw new MultiPrintableException(printableExceptions);
    }

    return new LayoutSpecHelper(processingEnv, typeElement, layoutSpecModel);
  }
}
