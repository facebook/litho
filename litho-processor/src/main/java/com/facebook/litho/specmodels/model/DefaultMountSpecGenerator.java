/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.ClassAnnotationsGenerator;
import com.facebook.litho.specmodels.generator.ComponentBodyGenerator;
import com.facebook.litho.specmodels.generator.DelegateMethodGenerator;
import com.facebook.litho.specmodels.generator.EventGenerator;
import com.facebook.litho.specmodels.generator.JavadocGenerator;
import com.facebook.litho.specmodels.generator.MountSpecGenerator;
import com.facebook.litho.specmodels.generator.PreambleGenerator;
import com.facebook.litho.specmodels.generator.PureRenderGenerator;
import com.facebook.litho.specmodels.generator.RenderDataGenerator;
import com.facebook.litho.specmodels.generator.StateGenerator;
import com.facebook.litho.specmodels.generator.TagGenerator;
import com.facebook.litho.specmodels.generator.TreePropGenerator;
import com.facebook.litho.specmodels.generator.TriggerGenerator;
import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.Modifier;

public class DefaultMountSpecGenerator implements SpecGenerator<MountSpecModel> {

  private final Set<ClassName> mBlacklistedTagInterfaces;

  public DefaultMountSpecGenerator() {
    this(new LinkedHashSet<>());
  }

  public DefaultMountSpecGenerator(Set<ClassName> blacklistedTagInterfaces) {
    mBlacklistedTagInterfaces = blacklistedTagInterfaces;
  }

  @Override
  public TypeSpec generate(MountSpecModel mountSpecModel) {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(mountSpecModel.getComponentName())
            .superclass(ClassNames.COMPONENT)
            .addTypeVariables(mountSpecModel.getTypeVariables());

    if (mountSpecModel.isPublic()) {
      typeSpec.addModifiers(Modifier.PUBLIC);
    }

    if (!mountSpecModel.hasInjectedDependencies()) {
      typeSpec.addModifiers(Modifier.FINAL);
    }

    TypeSpecDataHolder.newBuilder()
        .addTypeSpecDataHolder(JavadocGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(ClassAnnotationsGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(PreambleGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(ComponentBodyGenerator.generate(mountSpecModel, null))
        .addTypeSpecDataHolder(TreePropGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(
            DelegateMethodGenerator.generateDelegates(
                mountSpecModel, DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP))
        .addTypeSpecDataHolder(MountSpecGenerator.generateGetMountType(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generatePoolSize(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCanPreallocate(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCanMountIncrementally(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateShouldUseDisplayList(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateIsMountSizeDependent(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCallsShouldUpdateOnMount(mountSpecModel))
        .addTypeSpecDataHolder(PureRenderGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(EventGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(TriggerGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(StateGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(RenderDataGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(BuilderGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(TagGenerator.generate(mountSpecModel, mBlacklistedTagInterfaces))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
  }
}
