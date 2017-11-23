/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.processor;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.DELEGATE_FIELD_NAME;

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.processor.AbstractComponentsProcessor;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.facebook.litho.specmodels.processor.MountSpecModelFactory;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

/**
 * A bare-minimum implementation of an {@link AbstractComponentsProcessor} with DI support. This
 * allows testing generic specs which only make sense for injectable components.
 *
 * <p>This is only to be used for tests as the generated code is rather useless for any production
 * use cases.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class TestingDIComponentProcessor extends AbstractComponentsProcessor {

  public TestingDIComponentProcessor() {
    super(
        ImmutableList.of(new LayoutSpecModelFactory(), new MountSpecModelFactory()),
        typeElement -> new TestingDependencyInjectionHelper());
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new LinkedHashSet<>(
        Arrays.asList(ClassNames.LAYOUT_SPEC.toString(), ClassNames.MOUNT_SPEC.toString()));
  }

  private static class TestingDependencyInjectionHelper implements DependencyInjectionHelper {
    @Override
    public List<SpecModelValidationError> validate(SpecModel specModel) {
      return ImmutableList.of();
    }

    @Override
    public boolean isValidGeneratedComponentAnnotation(AnnotationSpec annotation) {
      return true;
    }

    @Override
    public TypeSpecDataHolder generateSourceDelegate(SpecModel specModel) {
      final FieldSpec.Builder builder =
          FieldSpec.builder(specModel.getSpecTypeName(), DELEGATE_FIELD_NAME)
              .addModifiers(Modifier.PRIVATE);

      return TypeSpecDataHolder.newBuilder().addField(builder.build()).build();
    }

    @Override
    public MethodSpec generateConstructor(SpecModel specModel) {
      return MethodSpec.constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameter(specModel.getSpecTypeName(), "spec")
          .addStatement("$N = spec", DELEGATE_FIELD_NAME)
          .build();
    }

    @Override
    public CodeBlock generateFactoryMethodsComponentInstance(SpecModel specModel) {
      return CodeBlock.of(
          "$L instance = new $L(new $L());\n",
          specModel.getComponentName(),
          specModel.getComponentName(),
          specModel.getSpecTypeName());
    }
  }
}
