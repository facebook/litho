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

package com.facebook.litho.intellij.services;

import com.facebook.litho.intellij.IntervalLogger;
import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.completion.ComponentGenerateUtils;
import com.facebook.litho.specmodels.internal.RunMode;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.ThrowableRunnable;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * Stores generated in-memory components. There's no guarantee, that they're more recent, than
 * generated components on a local disc. Cache could be updated via {@link
 * #getComponentAndMaybeUpdate(PsiClass, boolean)} call.
 */
public class ComponentsCacheService {
  private final Map<String, PsiClass> specToComponent = new ConcurrentHashMap<>();
  private final Map<String, Long> componentToTimestamp = new ConcurrentHashMap<>();
  // We store project instance to keep files created within that project
  private final Project project;
  private static final Logger LOG = Logger.getInstance(ComponentsCacheService.class);

  public ComponentsCacheService(Project project) {
    this.project = project;
  }

  @Nullable
  public PsiClass getComponentAndMaybeUpdate(PsiClass specClass, boolean forceUpdate) {
    final String specQualifiedName = specClass.getQualifiedName();
    final String componentQualifiedName =
        LithoPluginUtils.getLithoComponentNameFromSpec(specQualifiedName);
    if (componentQualifiedName == null) {
      return null;
    }
    final ShouldUpdateChecker checker =
        new ShouldUpdateChecker(forceUpdate, componentQualifiedName);

    maybeUpdateAsync(specClass, specQualifiedName, componentQualifiedName, checker);

    return getComponentFromCache(specQualifiedName);
  }

  @Nullable
  public PsiClass getComponentFromCache(String specQualifiedName) {
    return specToComponent.get(specQualifiedName);
  }

  private void maybeUpdateAsync(
      PsiClass specClass,
      String specQualifiedName,
      String componentQualifiedName,
      ShouldUpdateChecker checker) {

    final ThrowableRunnable<RuntimeException> job =
        () -> {
          final long modelTimestamp = System.currentTimeMillis();
          IntervalLogger logger = new IntervalLogger(LOG);
          checker.setModelTimestamp(modelTimestamp);
          if (checker.shouldStopUpdate()) return;

          final LayoutSpecModel layoutModel;
          try {
            layoutModel = ComponentGenerateUtils.createLayoutModel(specClass);
          } catch (RuntimeException e) {
            return;
          }
          final String componentShortName = StringUtil.getShortName(componentQualifiedName);
          Optional.ofNullable(layoutModel)
              .map(
                  specModel -> {
                    logger.logStep("model creation " + componentShortName);
                    if (checker.shouldStopUpdate()) return null;
                    TypeSpec typeSpec = specModel.generate(RunMode.normal());

                    logger.logStep("typeSpec generation " + typeSpec.name);
                    if (checker.shouldStopUpdate()) return null;
                    final String specPackageName = StringUtil.getPackageName(specQualifiedName);
                    // TODO T56876413 share methods with ComponentGenerator?
                    String fileContent =
                        JavaFile.builder(specPackageName, typeSpec)
                            .skipJavaLangImports(true)
                            .build()
                            .toString();

                    logger.logStep("component's fileContent build " + componentShortName);
                    if (checker.shouldStopUpdate()) return null;
                    return PsiFileFactory.getInstance(project)
                        .createFileFromText(componentShortName, StdFileTypes.JAVA, fileContent);
                  })
              .flatMap(
                  file ->
                      LithoPluginUtils.getFirstClass(
                          file, cls -> componentShortName.equals(cls.getName())))
              .ifPresent(
                  inMemory -> {
                    logger.logStep("file creation " + inMemory.getName());
                    synchronized (specToComponent) {
                      specToComponent.put(specQualifiedName, inMemory);
                      componentToTimestamp.put(inMemory.getQualifiedName(), modelTimestamp);
                    }
                  });
        };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      job.run();
    } else {
      DumbService.getInstance(project).smartInvokeLater(() -> ReadAction.run(job));
    }
  }

  /** Verifies that the update for the cached Component is needed. */
  class ShouldUpdateChecker {
    private final boolean forceUpdate;
    @Nullable private final String componentQualifiedName;
    private long modelTimestamp = Long.MAX_VALUE;
    private long start;

    /**
     * @param componentQualifiedName name of the component. If it's null {@link #shouldStopUpdate()}
     *     always returns true.
     */
    ShouldUpdateChecker(boolean forceUpdate, @Nullable String componentQualifiedName) {
      this.forceUpdate = forceUpdate;
      this.componentQualifiedName = componentQualifiedName;
    }

    boolean shouldStopUpdate() {
      if (componentQualifiedName == null) {
        return true;
      }
      final Long cachedTimestamp = componentToTimestamp.get(componentQualifiedName);
      if (cachedTimestamp == null) {
        return false;
      }
      if (cachedTimestamp > modelTimestamp) {
        return true;
      }
      return !forceUpdate;
    }

    void setModelTimestamp(long timestamp) {
      this.modelTimestamp = timestamp;
    }
  }
}
