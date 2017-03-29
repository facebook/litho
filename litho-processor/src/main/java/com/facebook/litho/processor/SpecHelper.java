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
