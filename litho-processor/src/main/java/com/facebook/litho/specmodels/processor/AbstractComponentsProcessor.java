/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.processor.ProcessorUtils.getPackageName;
import static com.facebook.litho.specmodels.processor.ProcessorUtils.validate;

import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.DependencyInjectionHelperFactory;
import com.facebook.litho.specmodels.model.SpecModel;
import com.squareup.javapoet.JavaFile;
import java.io.IOException;
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
  private PropNameInterStageStore mPropNameInterStageStore;
  private RunMode mRunMode;

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
    mSpecModelFactories = specModelFactories;
    mDependencyInjectionHelperFactory = dependencyInjectionHelperFactory;
  }

  @Override
  public void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    Map<String, String> options = processingEnv.getOptions();
    boolean isGeneratingAbi =
        Boolean.valueOf(options.getOrDefault("com.facebook.buck.java.generating_abi", "false"));
    mRunMode = isGeneratingAbi ? RunMode.ABI : RunMode.NORMAL;
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
          generate(specModel);
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

  protected void generate(SpecModel specModel) throws IOException {
    final String packageName = getPackageName(specModel.getComponentTypeName());
    JavaFile.builder(packageName, specModel.generate())
        .skipJavaLangImports(true)
        .build()
        .writeTo(processingEnv.getFiler());
  }

  private void afterGenerate(SpecModel specModel) throws IOException {
    mInterStageStore.getPropNameInterStageStore().saveNames(specModel);
  }
}
