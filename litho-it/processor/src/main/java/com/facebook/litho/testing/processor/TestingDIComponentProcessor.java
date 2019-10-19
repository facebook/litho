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

package com.facebook.litho.testing.processor;

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.ClassNames;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.InjectPropModel;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelValidationError;
import com.facebook.litho.specmodels.processor.AbstractComponentsProcessor;
import com.facebook.litho.specmodels.processor.LayoutSpecModelFactory;
import com.facebook.litho.specmodels.processor.MountSpecModelFactory;
import com.facebook.litho.specmodels.processor.testing.TestSpecModelFactory;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
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
        ImmutableList.of(
            new LayoutSpecModelFactory(), new MountSpecModelFactory(), new TestSpecModelFactory()),
        (typeElement, runMode) -> new TestingDependencyInjectionHelper());
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new LinkedHashSet<>(
        Arrays.asList(
            ClassNames.LAYOUT_SPEC.toString(),
            ClassNames.MOUNT_SPEC.toString(),
            ClassNames.TEST_SPEC.toString()));
  }

  private static class TestingDependencyInjectionHelper implements DependencyInjectionHelper {

    private static final ClassName ANDROID_CONTEXT_CLASS_NAME =
        ClassName.get("android.content", "Context");

    @Override
    public List<SpecModelValidationError> validate(SpecModel specModel) {
      return ImmutableList.of();
    }

    @Override
    public boolean isValidGeneratedComponentAnnotation(AnnotationSpec annotation) {
      return true;
    }

    @Override
    public MethodSpec generateConstructor(SpecModel specModel) {
      return MethodSpec.constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameter(ANDROID_CONTEXT_CLASS_NAME, "context")
          .build();
    }

    @Override
    public CodeBlock generateFactoryMethodsComponentInstance(SpecModel specModel) {
      return CodeBlock.of(
          "$L instance = new $L(context.getAndroidContext());\n",
          specModel.getComponentName(),
          specModel.getComponentName());
    }

    @Override
    public TypeSpecDataHolder generateInjectedFields(
        SpecModel specModel, ImmutableList<InjectPropModel> injectPropParams) {
      final TypeSpecDataHolder.Builder builder = TypeSpecDataHolder.newBuilder();

      for (MethodParamModel injectedParam : injectPropParams) {
        final FieldSpec.Builder fieldBuilder =
            FieldSpec.builder(injectedParam.getTypeName(), injectedParam.getName());
        for (AnnotationSpec extAnnotation : injectedParam.getExternalAnnotations()) {
          fieldBuilder.addAnnotation(extAnnotation);
        }

        builder.addField(fieldBuilder.build());
      }

      return builder.build();
    }

    @Override
    public MethodSpec generateTestingFieldAccessor(
        SpecModel specModel, InjectPropModel injectedParam) {
      return MethodSpec.methodBuilder(
              "get"
                  + injectedParam.getName().substring(0, 1).toUpperCase()
                  + injectedParam.getName().substring(1))
          .returns(injectedParam.getTypeName())
          .addStatement("return $N", injectedParam.getName())
          .build();
    }

    @Override
    public String generateImplAccessor(SpecModel specModel, MethodParamModel methodParamModel) {
      return methodParamModel.getName();
    }
  }
}
