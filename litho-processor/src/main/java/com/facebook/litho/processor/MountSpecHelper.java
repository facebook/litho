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
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import com.facebook.litho.annotations.FromBind;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.GetExtraAccessibilityNodeAt;
import com.facebook.litho.annotations.GetExtraAccessibilityNodesCount;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMeasureBaseline;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPopulateExtraAccessibilityNode;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.SpecModel;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

public class MountSpecHelper extends ComponentSpecHelper {

  private enum MountType {
    NONE,
    DRAWABLE,
    VIEW,
  }

  private static final Class<Annotation>[] STAGE_ANNOTATIONS = new Class[] {
      /* Methods that can have inter-stage props - these MUST come first in this list */
      OnPrepare.class,
      OnMeasure.class,
      OnBoundsDefined.class,
      OnBind.class,
      /* Methods that do not support inter-stage props */
      OnMount.class,
      OnPopulateAccessibilityNode.class,
      GetExtraAccessibilityNodesCount.class,
      OnPopulateExtraAccessibilityNode.class,
      GetExtraAccessibilityNodeAt.class,
      OnUnbind.class,
      OnUnmount.class,
  };

  private static final Class<Annotation>[] INTER_STAGE_INPUT_ANNOTATIONS = new Class[] {
      FromPrepare.class,
      FromMeasure.class,
      FromBoundsDefined.class,
      FromBind.class,
  };

  public MountSpecHelper(
      ProcessingEnvironment processingEnv,
      TypeElement specElement,
      SpecModel specModel) {
