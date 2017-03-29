/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.processor.TypeVariablesExtractor;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

public abstract class SpecHelper implements Closeable {

  private static final Pattern JAVADOC_SANITIZER = Pattern.compile("^\\s", Pattern.MULTILINE);

  protected final Stages mStages;
  protected final ProcessingEnvironment mProcessingEnv;
  protected final TypeSpec.Builder mTypeSpec;
  protected final String mQualifiedClassName;
  protected final TypeElement mSpecElement;
  protected final SpecModel mSpecModel;

  private final AtomicBoolean mOpen = new AtomicBoolean(true);

  public SpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      String name,
      boolean isPublic,
      Class<Annotation>[] stageAnnotations,
      Class<Annotation>[] interStageInputAnnotations,
      SpecModel specModel) {
    mProcessingEnv = processingEnv;
    mSpecElement = specElement;
    mQualifiedClassName = Utils.getGenClassName(specElement, name);
    mSpecModel = specModel;

    if (name.isEmpty()) {
      name = Utils.getSimpleClassName(mQualifiedClassName);
    }

    mTypeSpec = TypeSpec.classBuilder(name).superclass(getSuperclassClass());
    final List<TypeVariableName> typeVariables =
        TypeVariablesExtractor.getTypeVariables(specElement);
    mTypeSpec.addTypeVariables(typeVariables);

    if (isPublic) {
      mTypeSpec.addModifiers(Modifier.PUBLIC);
    }

    final List<AnnotationValue> eventTypes =
        Utils.getAnnotationParameter(
            mProcessingEnv,
            mSpecElement,
            getSpecAnnotationClass(),
            "events");
    final List<TypeElement> eventTypesElements;
    if (eventTypes != null) {
      eventTypesElements = new ArrayList<>();
      for (AnnotationValue eventType : eventTypes) {
        final DeclaredType type = (DeclaredType) eventType.getValue();
        eventTypesElements.add((TypeElement) type.asElement());
      }
    } else {
      eventTypesElements = Collections.<TypeElement>emptyList();
    }

    Map<String, String> propJavadocs = null;
    if (specModel == null) {
      String javadoc = mProcessingEnv.getElementUtils().getDocComment(mSpecElement);
      if (javadoc != null && !javadoc.isEmpty()) {
        // Javadoc returns a space at the start of every line.
        String javadocContents = JAVADOC_SANITIZER.matcher(javadoc).replaceAll("");

        // Splitting the javadoc with "@prop ".
        String[] keyValuePropJavadocs = javadocContents.split("@prop ");
        propJavadocs = new HashMap<>(keyValuePropJavadocs.length);

        for (int i = 1; i < keyValuePropJavadocs.length; i++) {
          // Each prop comment line look like:
          // @prop propName comment for the prop.
          String propJavadoc[] = keyValuePropJavadocs[i].split(" ", 2);
          if (propJavadoc.length == 2) {
            propJavadocs.put(propJavadoc[0], propJavadoc[1].trim());
          }
        }

        // The first portion is the class documentation.
        mTypeSpec
            .addJavadoc(keyValuePropJavadocs[0])
            .addJavadoc("<p>\n");
      }
    }

    mStages = new Stages(
        processingEnv,
        specElement,
        mQualifiedClassName,
        stageAnnotations,
        interStageInputAnnotations,
        mTypeSpec,
        typeVariables,
        isStateSupported(),
        populateExtraStateMembers(),
        eventTypesElements,
        propJavadocs);

    validate();

    if (mSpecModel == null) {
      mStages.generateJavadoc();
    }
  }

  protected abstract void validate();

  /**
   * @return any extra member for the generated State class.
   */
  protected Map<String, TypeMirror> populateExtraStateMembers() {
    return null;
  }

  /**
   * @return true if this spec supports {@link com.facebook.litho.annotations.State} parameters
   */
  protected boolean isStateSupported() {
    return false;
  }

  protected abstract TypeName getSuperclassClass();

  protected abstract Class getSpecAnnotationClass();

  public Stages getStages() {
    return mStages;
  }

  public TypeSpec.Builder getTypeSpec() {
    return mTypeSpec;
  }

