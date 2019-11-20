/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.generator.BuilderGenerator;
import com.facebook.litho.specmodels.generator.CachedValueGenerator;
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
import com.facebook.litho.specmodels.generator.WorkingRangeGenerator;
import com.facebook.litho.specmodels.internal.RunMode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class DefaultMountSpecGenerator implements SpecGenerator<MountSpecModel> {

  private final Set<ClassName> mBlacklistedTagInterfaces;

  public DefaultMountSpecGenerator() {
    this(new LinkedHashSet<>());
  }

  public DefaultMountSpecGenerator(Set<ClassName> blacklistedTagInterfaces) {
    mBlacklistedTagInterfaces = blacklistedTagInterfaces;
  }

  @Override
  public TypeSpec generate(MountSpecModel mountSpecModel, EnumSet<RunMode> runMode) {
    final TypeSpec.Builder typeSpec =
        TypeSpec.classBuilder(mountSpecModel.getComponentName())
            .superclass(ClassNames.COMPONENT)
            .addTypeVariables(mountSpecModel.getTypeVariables());

    if (SpecModelUtils.isTypeElement(mountSpecModel)) {
      typeSpec.addOriginatingElement((TypeElement) mountSpecModel.getRepresentedObject());
    }

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
        .addTypeSpecDataHolder(ComponentBodyGenerator.generate(mountSpecModel, null, runMode))
        .addTypeSpecDataHolder(TreePropGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(
            DelegateMethodGenerator.generateDelegates(
                mountSpecModel,
                DelegateMethodDescriptions.MOUNT_SPEC_DELEGATE_METHODS_MAP,
                runMode))
        .addTypeSpecDataHolder(MountSpecGenerator.generateGetMountType(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generatePoolSize(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCanPreallocate(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateHasChildLithoViews(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateIsMountSizeDependent(mountSpecModel))
        .addTypeSpecDataHolder(MountSpecGenerator.generateCallsShouldUpdateOnMount(mountSpecModel))
        .addTypeSpecDataHolder(PureRenderGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(EventGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(TriggerGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(WorkingRangeGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(StateGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(RenderDataGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(BuilderGenerator.generate(mountSpecModel))
        .addTypeSpecDataHolder(TagGenerator.generate(mountSpecModel, mBlacklistedTagInterfaces))
        .addTypeSpecDataHolder(CachedValueGenerator.generate(mountSpecModel, runMode))
        .build()
        .addToTypeSpec(typeSpec);

    return typeSpec.build();
  }
}
