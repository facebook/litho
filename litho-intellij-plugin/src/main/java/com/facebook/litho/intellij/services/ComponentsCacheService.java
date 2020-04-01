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
  private final Map<String, PsiClass> componentNameToClass = new ConcurrentHashMap<>();
  private final Map<String, Long> componentToTimestamp = new ConcurrentHashMap<>();
  private final Project project;
  private static final Logger LOG = Logger.getInstance(ComponentsCacheService.class);

  public ComponentsCacheService(Project project) {
    this.project = project;
  }

  @Nullable
  public PsiClass getComponentAndMaybeUpdate(PsiClass specClass, boolean forceUpdate) {
    if (!LithoPluginUtils.isLayoutSpec(specClass)) return null;

    final String componentQualifiedName =
        LithoPluginUtils.getLithoComponentNameFromSpec(specClass.getQualifiedName());
    if (componentQualifiedName == null) return null;

    final ShouldUpdateChecker checker =
        new ShouldUpdateChecker(forceUpdate, componentQualifiedName);

    maybeUpdateAsync(specClass, componentQualifiedName, checker);

    return getComponentFromCache(componentQualifiedName);
  }

  /**
   * @return component already present in the cache by its full-qualified name or null if it's
   *     absent.
   * @see #getComponentAndMaybeUpdate(PsiClass, boolean)
   */
  @Nullable
  public PsiClass getComponentFromCache(String componentQualifiedName) {
    final PsiClass component = componentNameToClass.get(componentQualifiedName);
    if (component != null) return component;

    // Check if it's a builder qualified name, as in AndroidPsiElementFinder
    final String parentName = StringUtil.getPackageName(componentQualifiedName);
    final PsiClass parentClass = componentNameToClass.get(parentName);
    if (parentClass == null) return null;

    return parentClass.findInnerClassByName(StringUtil.getShortName(componentQualifiedName), false);
  }

  private void maybeUpdateAsync(
      PsiClass specClass, String componentQualifiedName, ShouldUpdateChecker checker) {
    final String componentShortName = StringUtil.getShortName(componentQualifiedName);
    if (componentShortName.isEmpty()) return;

    final ThrowableRunnable<RuntimeException> job =
        () -> {
          final long modelTimestamp = System.currentTimeMillis();
          checker.setModelTimestamp(modelTimestamp);
          if (checker.shouldStopUpdate()) return;
          IntervalLogger logger = new IntervalLogger(LOG);

          final LayoutSpecModel layoutModel;
          try {
            layoutModel = ComponentGenerateUtils.createLayoutModel(specClass);
          } catch (RuntimeException e) {
            return;
          }
          Optional.ofNullable(layoutModel)
              .map(
                  specModel -> {
                    logger.logStep("model creation " + componentShortName);
                    if (checker.shouldStopUpdate()) return null;
                    TypeSpec typeSpec = specModel.generate(RunMode.normal());

                    logger.logStep("typeSpec generation " + typeSpec.name);
                    if (checker.shouldStopUpdate()) return null;
                    // TODO T56876413 share methods with ComponentGenerator?
                    String fileContent =
                        JavaFile.builder(
                                StringUtil.getPackageName(componentQualifiedName), typeSpec)
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
                    logger.logStep("file creation " + componentShortName);
                    updateCache(componentQualifiedName, inMemory, modelTimestamp);
                  });
        };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      job.run();
    } else {
      DumbService.getInstance(project).smartInvokeLater(() -> ReadAction.run(job));
    }
  }

  private void updateCache(
      String componentQualifiedName, PsiClass inMemoryComponent, long componentTimestamp) {
    synchronized (componentNameToClass) {
      componentNameToClass.put(componentQualifiedName, inMemoryComponent);
      componentToTimestamp.put(componentQualifiedName, componentTimestamp);
    }
  }

  /** Verifies that the update for the cached Component is needed. */
  class ShouldUpdateChecker {
    private final boolean forceUpdate;
    private final String componentQualifiedName;
    private long modelTimestamp = Long.MAX_VALUE;

    ShouldUpdateChecker(boolean forceUpdate, String componentQualifiedName) {
      this.forceUpdate = forceUpdate;
      this.componentQualifiedName = componentQualifiedName;
    }

    boolean shouldStopUpdate() {
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
