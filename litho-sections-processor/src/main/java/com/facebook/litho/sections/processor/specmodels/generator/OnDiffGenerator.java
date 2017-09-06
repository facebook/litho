/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.processor.specmodels.generator;

import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.processor.SectionClassNames;
import com.facebook.litho.sections.processor.Stages;
import com.facebook.litho.specmodels.generator.ComponentImplGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethodModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelUtils;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.Modifier;

public class OnDiffGenerator {
  private static final String PREV_ABSTRACT_IMPL_INSTANCE_NAME = "_prevAbstractImpl";
  private static final String NEXT_ABSTRACT_IMPL_INSTANCE_NAME = "_nextAbstractImpl";
  private static final String PREV_IMPL_INSTANCE_NAME = "_prevImpl";
  private static final String NEXT_IMPL_INSTANCE_NAME = "_nextImpl";
  private static final String DIFF_VARIABLE_SUFFIX = "_diff_";
  private static final TypeName ABSTRACT_IMPL_TYPE = SectionClassNames.SECTION;

  public static TypeSpecDataHolder generate(SpecModel specModel) {
    final DelegateMethodModel onDiffMethod =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnDiff.class);

    MethodSpec.Builder delegate =
        buildBasic(ComponentImplGenerator.getImplClassName(specModel), onDiffMethod);

    delegate.addCode(generateAcquireDiff(onDiffMethod));

    delegate.addCode(
        generateDelegationCall(SpecModelUtils.getSpecAccessor(specModel), onDiffMethod));

    delegate.addCode(generateReleaseDiff(onDiffMethod));

    return TypeSpecDataHolder.newBuilder()
        .addMethod(delegate.build())
        .addMethod(
            MethodSpec.methodBuilder("isDiffSectionSpec")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PROTECTED)
                .returns(TypeName.BOOLEAN)
                .addStatement("return true")
                .build())
        .build();
  }

  private static MethodSpec.Builder buildBasic(
      String implClassName, DelegateMethodModel onDiffMethod) {
    final List<TypeName> definedParameterTypes =
        ImmutableList.of(SectionClassNames.SECTION_CONTEXT, SectionClassNames.CHANGESET);

    final MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("generateChangeSet");
    methodSpec.addAnnotation(Override.class);

    methodSpec.addModifiers(Modifier.PROTECTED);

    for (int i = 0, size = definedParameterTypes.size(); i < size; i++) {
      methodSpec.addParameter(
          definedParameterTypes.get(i), onDiffMethod.methodParams.get(i).getName());
    }

    methodSpec.addParameter(ABSTRACT_IMPL_TYPE, PREV_ABSTRACT_IMPL_INSTANCE_NAME);
    methodSpec.addParameter(ABSTRACT_IMPL_TYPE, NEXT_ABSTRACT_IMPL_INSTANCE_NAME);

    methodSpec.addStatement(
        "$L $L = ($L) $L",
        implClassName,
        PREV_IMPL_INSTANCE_NAME,
        implClassName,
        PREV_ABSTRACT_IMPL_INSTANCE_NAME);

    methodSpec.addStatement(
        "$L $L = ($L) $L",
        implClassName,
        NEXT_IMPL_INSTANCE_NAME,
        implClassName,
        NEXT_ABSTRACT_IMPL_INSTANCE_NAME);

    return methodSpec;
  }


  private static CodeBlock generateAcquireDiff(DelegateMethodModel onDiffMethod) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    for (MethodParamModel paramModel : onDiffMethod.methodParams) {
      if (MethodParamModelUtils.isDiffType(paramModel)) {
        final String stateContainerMember =
            MethodParamModelUtils.isAnnotatedWith(paramModel, State.class)
                ? "." + Stages.STATE_CONTAINER_IMPL_MEMBER
                : "";

        final TypeName unwrappedType =
            ((ParameterizedTypeName) paramModel.getType()).typeArguments.get(0);

        builder.addStatement(
            "$T $L$L = acquireDiff($L == null ? null : ($T) $L"
                + stateContainerMember
                + ".$L, $L == null ? null : ($T) $L"
                + stateContainerMember
                + ".$L)",
            paramModel.getType(),
            DIFF_VARIABLE_SUFFIX,
            paramModel.getName(),
            PREV_IMPL_INSTANCE_NAME,
            unwrappedType,
            PREV_IMPL_INSTANCE_NAME,
            paramModel.getName(),
            NEXT_IMPL_INSTANCE_NAME,
            unwrappedType,
            NEXT_IMPL_INSTANCE_NAME,
            paramModel.getName());
      }
    }
    return builder.build();
  }

  private static CodeBlock generateDelegationCall(
      String specAccessor, DelegateMethodModel onDiffMethod) {

    final CodeBlock.Builder delegation = CodeBlock.builder();
    delegation.add("$L.$L(\n", specAccessor, onDiffMethod.name);

    delegation.indent();
    for (MethodParamModel methodParam : onDiffMethod.methodParams) {
      delegation.add(
          "$L",
          MethodParamModelUtils.isDiffType(methodParam)
              ? DIFF_VARIABLE_SUFFIX + methodParam.getName()
              : methodParam.getName());

      final boolean isLast =
          methodParam == onDiffMethod.methodParams.get(onDiffMethod.methodParams.size() - 1);
      if (!isLast) {
        delegation.add(",\n");
      }
    }

    delegation.add(");\n");
    delegation.unindent();
    return delegation.build();
  }

  private static CodeBlock generateReleaseDiff(DelegateMethodModel onDiffMethod) {
    final CodeBlock.Builder builder = CodeBlock.builder();

    for (MethodParamModel methodParam : onDiffMethod.methodParams) {
      if (MethodParamModelUtils.isDiffType(methodParam)) {
        builder.addStatement("releaseDiff($L$L)", DIFF_VARIABLE_SUFFIX, methodParam.getName());
        }
    }
    return builder.build();
  }
}
