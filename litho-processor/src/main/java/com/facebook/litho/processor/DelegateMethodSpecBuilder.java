/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.processor;

import javax.lang.model.element.Modifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.facebook.litho.specmodels.model.ClassNames;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

class DelegateMethodSpecBuilder {
  private static final String ABSTRACT_IMPL_INSTANCE_NAME = "_abstractImpl";
  private static final String IMPL_INSTANCE_NAME = "_impl";

  private String mImplClassName;
  private TypeName mAbstractImplType;
  private List<Parameter> mImplParameters = new ArrayList<>();
  private List<TypeName> mCheckedExceptions = new ArrayList<>();
  private boolean mOverridesSuper;
  private Visibility mVisibility = Visibility.PACKAGE;

  private String mFromName;
  private TypeName mFromReturnType = TypeName.VOID;
  private List<Parameter> mFromParams = new ArrayList<>();

  private String mTarget;
  private String mToName;
  private TypeName mToReturnType = TypeName.VOID;
  private List<Parameter> mToParams = new ArrayList<>();
  private Set<String> mStateParamNames = new LinkedHashSet<>();
  private Map<String, String> mParameterTranslation;

