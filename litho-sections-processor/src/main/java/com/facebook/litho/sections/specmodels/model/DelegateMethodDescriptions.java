/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.specmodels.model;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.ABSTRACT_PARAM_NAME;
import static com.facebook.litho.specmodels.generator.GeneratorConstants.REF_VARIABLE_NAME;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.DIFF_STATE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.PROP;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.STATE_VALUE;
import static com.facebook.litho.specmodels.model.DelegateMethodDescription.OptionalParameterType.TREE_PROP;

import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.sections.annotations.OnBindService;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.sections.annotations.OnRefresh;
import com.facebook.litho.sections.annotations.OnUnbindService;
import com.facebook.litho.sections.annotations.OnViewportChanged;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.DelegateMethodDescription;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.Modifier;

public class DelegateMethodDescriptions {

  private static final DelegateMethodDescription ON_CREATE_CHILDREN =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(SectionClassNames.CHILDREN)
          .name("createChildren")
          .definedParameterTypes(ImmutableList.<TypeName>of(SectionClassNames.SECTION_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  private static final DelegateMethodDescription ON_CREATE_INITIAL_STATE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("createInitialState")
          .definedParameterTypes(ImmutableList.<TypeName>of(SectionClassNames.SECTION_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE_VALUE))
          .build();

  private static final DelegateMethodDescription ON_REFRESH =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("refresh")
          .definedParameterTypes(ImmutableList.<TypeName>of(SectionClassNames.SECTION_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  private static final DelegateMethodDescription ON_DATA_BOUND =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("dataBound")
          .definedParameterTypes(ImmutableList.<TypeName>of(SectionClassNames.SECTION_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  private static final DelegateMethodDescription ON_VIEWPORT_CHANGED =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("viewportChanged")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(
                  SectionClassNames.SECTION_CONTEXT,
                  TypeName.INT,
                  TypeName.INT,
                  TypeName.INT,
                  TypeName.INT,
                  TypeName.INT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  private static final DelegateMethodDescription ON_CREATE_SERVICE =
      DelegateMethodDescription.newBuilder()
          .accessType(Modifier.PRIVATE)
          .returnType(TypeName.OBJECT)
          .name("onCreateService")
          .definedParameterTypes(ImmutableList.<TypeName>of(SectionClassNames.SECTION_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  private static final DelegateMethodDescription ON_BIND_SERVICE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("bindService")
          .definedParameterTypes(ImmutableList.<TypeName>of(SectionClassNames.SECTION_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  private static final DelegateMethodDescription ON_UNBIND_SERVICE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("unbindService")
          .definedParameterTypes(ImmutableList.<TypeName>of(SectionClassNames.SECTION_CONTEXT))
          .optionalParameterTypes(ImmutableList.of(PROP, TREE_PROP, STATE))
          .build();

  private static final DelegateMethodDescription ON_DIFF =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.VOID)
          .name("generateChangeSet")
          .definedParameterTypes(
              ImmutableList.<TypeName>of(
                  SectionClassNames.SECTION_CONTEXT, SectionClassNames.CHANGESET))
          .optionalParameterTypes(ImmutableList.of(DIFF_PROP, DIFF_STATE))
          .extraMethods(
              ImmutableList.of(
                  MethodSpec.methodBuilder("isDiffSectionSpec")
                      .addAnnotation(Override.class)
                      .addModifiers(Modifier.PROTECTED)
                      .returns(TypeName.BOOLEAN)
                      .addStatement("return true")
                      .build()))
          .build();

  private static final DelegateMethodDescription SHOULD_UPDATE =
      DelegateMethodDescription.newBuilder()
          .annotations(ImmutableList.of(AnnotationSpec.builder(Override.class).build()))
          .accessType(Modifier.PROTECTED)
          .returnType(TypeName.BOOLEAN)
          .name("shouldUpdate")
          .definedParameterTypes(ImmutableList.of())
          .optionalParameterTypes(ImmutableList.of(DIFF_PROP, DIFF_STATE))
          .build();

  static final Map<Class<? extends Annotation>, DelegateMethodDescription>
      SERVICE_AWARE_DELEGATE_METHODS_MAP;
  static final Map<Class<? extends Annotation>, DelegateMethodDescription>
      GROUP_SECTION_SPEC_DELEGATE_METHODS_MAP;
  static final Map<Class<? extends Annotation>, DelegateMethodDescription>
      DIFF_SECTION_SPEC_DELEGATE_METHODS_MAP;
  static {
    Map<Class<? extends Annotation>, DelegateMethodDescription> serviceAwareDelegateMethodsMap =
        getTreeMap();

    serviceAwareDelegateMethodsMap.put(OnRefresh.class, ON_REFRESH);
    serviceAwareDelegateMethodsMap.put(OnDataBound.class, ON_DATA_BOUND);
    serviceAwareDelegateMethodsMap.put(OnViewportChanged.class, ON_VIEWPORT_CHANGED);
    serviceAwareDelegateMethodsMap.put(OnCreateService.class, ON_CREATE_SERVICE);
    serviceAwareDelegateMethodsMap.put(OnBindService.class, ON_BIND_SERVICE);
    serviceAwareDelegateMethodsMap.put(OnUnbindService.class, ON_UNBIND_SERVICE);

    SERVICE_AWARE_DELEGATE_METHODS_MAP =
        Collections.unmodifiableMap(serviceAwareDelegateMethodsMap);

    Map<Class<? extends Annotation>, DelegateMethodDescription> groupSectionSpecDelegateMethodsMap =
        getTreeMap();
    groupSectionSpecDelegateMethodsMap.put(OnCreateChildren.class, ON_CREATE_CHILDREN);
    groupSectionSpecDelegateMethodsMap.put(OnCreateInitialState.class, ON_CREATE_INITIAL_STATE);
    groupSectionSpecDelegateMethodsMap.put(ShouldUpdate.class, SHOULD_UPDATE);

    GROUP_SECTION_SPEC_DELEGATE_METHODS_MAP =
        Collections.unmodifiableMap(groupSectionSpecDelegateMethodsMap);

    Map<Class<? extends Annotation>, DelegateMethodDescription> diffSectionSpecDelegateMethodsMap =
        getTreeMap();

    diffSectionSpecDelegateMethodsMap.put(OnCreateInitialState.class, ON_CREATE_INITIAL_STATE);
    diffSectionSpecDelegateMethodsMap.put(OnDiff.class, ON_DIFF);
    diffSectionSpecDelegateMethodsMap.put(ShouldUpdate.class, SHOULD_UPDATE);

    DIFF_SECTION_SPEC_DELEGATE_METHODS_MAP =
        Collections.unmodifiableMap(diffSectionSpecDelegateMethodsMap);
  }

  public static Map<Class<? extends Annotation>, DelegateMethodDescription>
  getGroupSectionSpecDelegatesMap(GroupSectionSpecModel specModel) {
    Map<Class<? extends Annotation>, DelegateMethodDescription> groupSectionSpecDelegateMethodsMap =
        getTreeMap();

    groupSectionSpecDelegateMethodsMap.putAll(GROUP_SECTION_SPEC_DELEGATE_METHODS_MAP);
    addServiceAwareDelegateMethodDescriptions(groupSectionSpecDelegateMethodsMap, specModel);

    return Collections.unmodifiableMap(groupSectionSpecDelegateMethodsMap);
  }

  public static Map<Class<? extends Annotation>, DelegateMethodDescription>
  getDiffSectionSpecDelegatesMap(DiffSectionSpecModel specModel) {
    Map<Class<? extends Annotation>, DelegateMethodDescription> diffSectionSpecDelegateMethodsMap =
        getTreeMap();

    diffSectionSpecDelegateMethodsMap.putAll(DIFF_SECTION_SPEC_DELEGATE_METHODS_MAP);
    addServiceAwareDelegateMethodDescriptions(diffSectionSpecDelegateMethodsMap, specModel);

    return Collections.unmodifiableMap(diffSectionSpecDelegateMethodsMap);
  }

  private static Map<Class<? extends Annotation>, DelegateMethodDescription> getTreeMap() {
    return new TreeMap<>(
        new Comparator<Class<? extends Annotation>>() {
          @Override
          public int compare(Class<? extends Annotation> lhs, Class<? extends Annotation> rhs) {
            return lhs.toString().compareTo(rhs.toString());
          }
        });
  }

  private static <S extends SpecModel & HasService> void addServiceAwareDelegateMethodDescriptions(
      Map<Class<? extends Annotation>, DelegateMethodDescription> map, S specModel) {
    final MethodParamModel serviceParam = specModel.getServiceParam();

    if (serviceParam == null) {
      map.putAll(SERVICE_AWARE_DELEGATE_METHODS_MAP);
      return;
    }

    for (Map.Entry<Class<? extends Annotation>, DelegateMethodDescription> entry :
        SERVICE_AWARE_DELEGATE_METHODS_MAP.entrySet()) {
      DelegateMethodDescription methodDescription =
          entry.getKey().equals(OnCreateService.class)
              ? getOnCreateServiceDelegateMethodDescription(entry.getValue(), specModel)
              : DelegateMethodDescription.fromDelegateMethodDescription(entry.getValue())
                  .optionalParameters(ImmutableList.of(serviceParam))
                  .build();

      map.put(entry.getKey(), methodDescription);
    }
  }

  private static <S extends SpecModel & HasService>
      DelegateMethodDescription getOnCreateServiceDelegateMethodDescription(
          DelegateMethodDescription onCreateService, S specModel) {
    final MethodParamModel serviceParam = specModel.getServiceParam();
    final String componentName = specModel.getComponentName();
    return DelegateMethodDescription.fromDelegateMethodDescription(onCreateService)
        .returnType(serviceParam.getType())
        .extraMethods(
            ImmutableList.<MethodSpec>of(
                createService(componentName, serviceParam.getName()),
                transferService(componentName, serviceParam.getName()),
                getService(componentName, serviceParam.getName())))
        .build();
  }

  private static MethodSpec createService(String implClass, String serviceInstanceName) {
    return MethodSpec.methodBuilder("createService")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ParameterSpec.builder(SectionClassNames.SECTION_CONTEXT, "context").build())
        .addParameter(
            ParameterSpec.builder(SectionClassNames.SECTION, ABSTRACT_PARAM_NAME).build())
        .addStatement(
            "$L $L = ($L) $L", implClass, REF_VARIABLE_NAME, implClass, ABSTRACT_PARAM_NAME)
        .addStatement(
            "$L.$L = onCreateService(context, $L)",
            REF_VARIABLE_NAME,
            serviceInstanceName,
            ABSTRACT_PARAM_NAME)
        .build();
  }

  private static MethodSpec transferService(String implClass, String serviceInstanceName) {
    return MethodSpec.methodBuilder("transferService")
        .addAnnotation(Override.class)
        .returns(TypeName.VOID)
        .addModifiers(Modifier.PROTECTED)
        .addParameter(ParameterSpec.builder(SectionClassNames.SECTION_CONTEXT, "c").build())
        .addParameter(ParameterSpec.builder(SectionClassNames.SECTION, "previous").build())
        .addParameter(ParameterSpec.builder(SectionClassNames.SECTION, "next").build())
        .addStatement("$L $L = ($L) previous", implClass, "previousSection", implClass)
        .addStatement("$L $L = ($L) next", implClass, "nextSection", implClass)
        .addStatement(
            "nextSection.$L = previousSection.$L", serviceInstanceName, serviceInstanceName)
        .build();
  }

  private static MethodSpec getService(String implClass, String serviceInstanceName) {
    return MethodSpec.methodBuilder("getService")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PROTECTED)
        .returns(TypeName.OBJECT)
        .addParameter(ParameterSpec.builder(SectionClassNames.SECTION, "section").build())
        .addStatement("return (($L) section).$L", implClass, serviceInstanceName)
        .build();
  }
}
