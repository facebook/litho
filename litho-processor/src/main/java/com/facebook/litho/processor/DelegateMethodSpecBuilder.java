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

  DelegateMethodSpecBuilder implClassName(String implClassName) {
    mImplClassName = implClassName;
    return this;
  }

  DelegateMethodSpecBuilder abstractImplType(TypeName abstractImplType) {
    mAbstractImplType = abstractImplType;
    return this;
  }

  DelegateMethodSpecBuilder implParameters(List<Parameter> implParameters) {
    mImplParameters = implParameters;
    return this;
  }

  DelegateMethodSpecBuilder implParameter(Parameter implParameter) {
    mImplParameters.add(implParameter);
    return this;
  }

  DelegateMethodSpecBuilder checkedExceptions(List<TypeName> checkedExceptions) {
    mCheckedExceptions = checkedExceptions;
    return this;
  }

  DelegateMethodSpecBuilder checkedException(TypeName checkedException) {
    mCheckedExceptions.add(checkedException);
    return this;
  }

  DelegateMethodSpecBuilder overridesSuper(boolean overridesSuper) {
    mOverridesSuper = overridesSuper;
    return this;
  }

  DelegateMethodSpecBuilder visibility(Visibility visibility) {
    mVisibility = visibility;
    return this;
  }

  DelegateMethodSpecBuilder fromName(String fromName) {
    mFromName = fromName;
    return this;
  }

  DelegateMethodSpecBuilder fromReturnType(TypeName fromReturnType) {
    mFromReturnType = fromReturnType;
    return this;
  }

  DelegateMethodSpecBuilder fromParams(List<Parameter> fromParams) {
    mFromParams = fromParams;
    return this;
  }

  DelegateMethodSpecBuilder fromParam(Parameter fromParam) {
    mFromParams.add(fromParam);
    return this;
  }

  DelegateMethodSpecBuilder target(String target) {
    mTarget = target;
    return this;
  }

  DelegateMethodSpecBuilder toName(String toName) {
    mToName = toName;
    return this;
  }

  DelegateMethodSpecBuilder toReturnType(TypeName toReturnType) {
    mToReturnType = toReturnType;
    return this;
  }

  DelegateMethodSpecBuilder toParams(List<Parameter> toParams) {
    mToParams = toParams;
    return this;
  }

  DelegateMethodSpecBuilder toParam(Parameter toParam) {
    mToParams.add(toParam);
    return this;
  }

  DelegateMethodSpecBuilder stateParams(Set<String> stateParams) {
    if (stateParams != null) {
      mStateParamNames = stateParams;
    }
    return this;
  }

  public DelegateMethodSpecBuilder parameterTranslation(Map<String, String> parameterTranslation) {
    mParameterTranslation = parameterTranslation;
    return this;
  }

  MethodSpec build() {
    final MethodSpec.Builder delegate = MethodSpec.methodBuilder(mFromName);

    for (TypeName exception : mCheckedExceptions) {
      delegate.addException(exception);
    }

    if (mOverridesSuper) {
      delegate.addAnnotation(Override.class);
    }

    switch (mVisibility) {
      case PACKAGE:
        break;
      case PRIVATE:
        delegate.addModifiers(Modifier.PRIVATE);
        break;
      case PUBLIC:
        delegate.addModifiers(Modifier.PUBLIC);
        break;
      case PROTECTED:
        delegate.addModifiers(Modifier.PROTECTED);
        break;
    }

    delegate.returns(mFromReturnType);

    for (Parameter parameter : mFromParams) {
      delegate.addParameter(parameter.type, parameter.name);
    }

    delegate.addParameter(mAbstractImplType, ABSTRACT_IMPL_INSTANCE_NAME);
    delegate.addStatement(mImplClassName +
        " " + IMPL_INSTANCE_NAME + " = (" +
        mImplClassName +
        ") " +
        ABSTRACT_IMPL_INSTANCE_NAME);

    for (Parameter parameter : mToParams) {
      if (isOutputType(parameter.type)) {
        delegate.addStatement("$T $L = acquireOutput()", parameter.type, parameter.name);
      }
    }

    final CodeBlock.Builder delegation = CodeBlock.builder();
    if (mFromReturnType.equals(TypeName.VOID)) {
      delegation.add("$L.$L(\n", mTarget, mToName);
    } else {
      delegation.add(
          "$T _result = ($T) $L.$L(\n",
          mFromReturnType,
          mToReturnType,
          mTarget,
          mToName);
    }

    delegation.indent();
    for (Parameter parameter : mToParams) {
      if (isOutputType(parameter.type)) {
        delegation.add("$L", parameter.name);
      } else {
        delegation.add("($T) $L", parameter.type, getMatchingInput(parameter));
      }

      final boolean isLast = parameter == mToParams.get(mToParams.size() - 1);
      if (!isLast) {
        delegation.add(",\n");
      }
    }
    delegation.add(");\n");
    delegation.unindent();

    delegate.addCode(delegation.build());

    for (Parameter parameter : mToParams) {
      if (isOutputType(parameter.type)) {
        final String stateContainerMember = mStateParamNames.contains(parameter.name)
            ? "." + Stages.STATE_CONTAINER_IMPL_MEMBER
            : "";
        delegate.addStatement(
            IMPL_INSTANCE_NAME + stateContainerMember + ".$L = $L.get()",
            parameter.name,
            parameter.name);
        delegate.addStatement("releaseOutput($L)", parameter.name);
      }
    }

    if (!mFromReturnType.equals(TypeName.VOID)) {
      delegate.addStatement("return _result");
    }

    return delegate.build();
  }

  private boolean isOutputType(TypeName type) {
    if (type.equals(ClassNames.OUTPUT)) {
      return true;
    } else if (type instanceof ParameterizedTypeName) {
      final ParameterizedTypeName parameterizedType = (ParameterizedTypeName) type;
      if (parameterizedType.rawType.equals(ClassNames.OUTPUT)) {
        return true;
      }
    }

    return false;
  }

  private CodeBlock getMatchingInput(Parameter output) {
    final String translatedName = mParameterTranslation != null ?
        mParameterTranslation.get(output.name) :
        null;
    final String parameterName = translatedName != null ? translatedName : output.name;

    for (Parameter parameter : mFromParams) {
      if (parameter.name.equals(parameterName)) {
