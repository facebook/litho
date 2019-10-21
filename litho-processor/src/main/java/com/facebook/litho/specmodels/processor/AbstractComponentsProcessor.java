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

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.processor.ProcessorUtils.getPackageName;
import static com.facebook.litho.specmodels.processor.ProcessorUtils.validate;

import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DependencyInjectionHelperFactory;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.JavaFile;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractComponentsProcessor extends AbstractProcessor {

  @Nullable private final DependencyInjectionHelperFactory mDependencyInjectionHelperFactory;
  private final List<SpecModelFactory> mSpecModelFactories;
  private final boolean mShouldSavePropNames;
  private PropNameInterStageStore mPropNameInterStageStore;
  private final EnumSet<RunMode> mRunMode = RunMode.normal();

  private final InterStageStore mInterStageStore =
      new InterStageStore() {
        @Override
        public PropNameInterStageStore getPropNameInterStageStore() {
          return mPropNameInterStageStore;
        }
      };

  protected AbstractComponentsProcessor(
      List<SpecModelFactory> specModelFactories,
      DependencyInjectionHelperFactory dependencyInjectionHelperFactory) {
    this(specModelFactories, dependencyInjectionHelperFactory, true);
  }

  protected AbstractComponentsProcessor(
      List<SpecModelFactory> specModelFactories,
      DependencyInjectionHelperFactory dependencyInjectionHelperFactory,
      boolean shouldSavePropNames) {
    mSpecModelFactories = specModelFactories;
    mDependencyInjectionHelperFactory = dependencyInjectionHelperFactory;
    mShouldSavePropNames = shouldSavePropNames;
  }

  @Override
  public void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    Map<String, String> options = processingEnv.getOptions();
    boolean isGeneratingAbi =
        Boolean.valueOf(options.getOrDefault("com.facebook.buck.java.generating_abi", "false"));
    if (isGeneratingAbi) {
      mRunMode.add(RunMode.ABI);
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      return false;
    }
    // processingEnv is not available at construction time. :(
    mPropNameInterStageStore = new PropNameInterStageStore(processingEnv.getFiler());

    for (SpecModelFactory specModelFactory : mSpecModelFactories) {
      final Set<Element> elements = specModelFactory.extract(roundEnv);

      for (Element element : elements) {
        try {
          final SpecModel specModel =
              specModelFactory.create(
                  processingEnv.getElementUtils(),
                  processingEnv.getTypeUtils(),
                  (TypeElement) element,
                  processingEnv.getMessager(),
                  mRunMode,
                  mDependencyInjectionHelperFactory == null
                      ? null
                      : mDependencyInjectionHelperFactory.create((TypeElement) element, mRunMode),
                  mInterStageStore);

          validate(specModel, mRunMode);
          generate(specModel, mRunMode);
          afterGenerate(specModel);
        } catch (PrintableException e) {
          e.print(processingEnv.getMessager());
        } catch (Exception e) {
          processingEnv
              .getMessager()
              .printMessage(
                  Diagnostic.Kind.ERROR,
                  String.format(
                      "Unexpected error thrown when generating this component spec. "
                          + "Please report stack trace to the components team.\n%s",
                      e),
                  element);
          e.printStackTrace();
        }
      }
    }

    return false;
  }

  protected void generate(SpecModel specModel, EnumSet<RunMode> runMode) throws IOException {
    final String packageName = getPackageName(specModel.getComponentTypeName());
    JavaFile.builder(packageName, specModel.generate(runMode))
        .skipJavaLangImports(true)
        .build()
        .writeTo(processingEnv.getFiler());
  }

  private void afterGenerate(SpecModel specModel) throws IOException {
    if (mShouldSavePropNames) {
      mInterStageStore.getPropNameInterStageStore().saveNames(specModel);
    }
  }
}
