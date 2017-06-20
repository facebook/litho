/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.JavaFile;

import static com.facebook.litho.specmodels.processor.ProcessorUtils.validate;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractComponentsProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      return false;
    }

    Set<Element> layoutSpecElements =
        (Set<Element>) roundEnv.getElementsAnnotatedWith(LayoutSpec.class);
    Set<Element> mountSpecElements =
        (Set<Element>) roundEnv.getElementsAnnotatedWith(MountSpec.class);

    Set<Element> allSpecElements = new LinkedHashSet<>();
    allSpecElements.addAll(layoutSpecElements);
    allSpecElements.addAll(mountSpecElements);

    for (Element element : allSpecElements) {
      try {
        SpecModel specModel = null;
        final TypeElement typeElement = (TypeElement) element;
        if (element.getAnnotation(LayoutSpec.class) != null) {
          specModel = getLayoutSpecModel(typeElement);
        } else if (element.getAnnotation(MountSpec.class) != null) {
          specModel =
              MountSpecModelFactory.create(
                  processingEnv.getElementUtils(),
                  (TypeElement) element,
                  getDependencyInjectionGenerator((TypeElement) element));
        }

        if (specModel != null) {
          validate(specModel);
          generate(specModel);
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
    return new LinkedHashSet<>(Arrays.asList(
        ClassNames.LAYOUT_SPEC.toString(),
        ClassNames.MOUNT_SPEC.toString()));
  }

  protected abstract DependencyInjectionHelper getDependencyInjectionGenerator(
      TypeElement typeElement);

  protected abstract SpecModel getLayoutSpecModel(TypeElement typeElement);

  protected void generate(SpecModel specModel) throws IOException {
    JavaFile.builder(
        getPackageName(specModel.getComponentTypeName().toString()), specModel.generate())
        .skipJavaLangImports(true)
        .build()
        .writeTo(processingEnv.getFiler());
  }

  protected static String getPackageName(String qualifiedName) {
    return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
  }
}
