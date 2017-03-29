/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.MountSpecModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.facebook.litho.specmodels.processor.MountSpecModelFactory;

import com.squareup.javapoet.JavaFile;

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
        SpecModel specModel = null;
        final TypeElement typeElement = (TypeElement) element;
        if (element.getAnnotation(LayoutSpec.class) != null) {
          specModel = LayoutSpecModelFactory.create(
              processingEnv.getElementUtils(),
              typeElement,
              getDependencyInjectionGenerator(typeElement));
        } else if (element.getAnnotation(MountSpec.class) != null) {
          final MountSpecModel mountSpecModel =
              MountSpecModelFactory.create(
                  processingEnv.getElementUtils(),
                  (TypeElement) element,
                  getDependencyInjectionGenerator((TypeElement) element));
          validate(mountSpecModel);

          final MountSpecHelper mountSpecHelper =
              new MountSpecHelper(processingEnv, (TypeElement) element, mountSpecModel);
          closeable = mountSpecHelper;
          generate(mountSpecHelper);
        }

        if (specModel != null) {
          validate(specModel);
          generate(specModel);
        }

        if (closeable != null) {
