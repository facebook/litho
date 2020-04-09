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
import com.facebook.litho.intellij.file.ComponentScope;
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
 * Stores generated in-memory components. Cache could be updated via {@link #maybeUpdate(PsiClass,
 * boolean)} call.
 */
public class ComponentsCacheService {
  private static final Logger LOG = Logger.getInstance(ComponentsCacheService.class);

  private final Map<String, PsiClass> componentNameToClass = new ConcurrentHashMap<>();
  private final Project project;

  public ComponentsCacheService(Project project) {
    this.project = project;
  }

  public void maybeUpdate(PsiClass specClass, boolean forceUpdate) {
    if (!LithoPluginUtils.isLayoutSpec(specClass)) return;

    final String componentQualifiedName =
        LithoPluginUtils.getLithoComponentNameFromSpec(specClass.getQualifiedName());
    if (componentQualifiedName == null) return;

    final ShouldUpdateChecker checker =
        new ShouldUpdateChecker(forceUpdate, componentQualifiedName);

    maybeUpdateAsync(specClass, componentQualifiedName, checker);
  }

  /**
   * @return component already present in the cache by its full-qualified name or null if it's
   *     absent.
   * @see #maybeUpdate(PsiClass, boolean)
   */
  @Nullable
  public PsiClass getComponent(String componentQualifiedName) {
    final PsiClass component = componentNameToClass.get(componentQualifiedName);
    if (component == null) return null;

    if (isPresentOnDisk(componentQualifiedName)) {
      componentNameToClass.remove(componentQualifiedName);
      return null;
    }
    return component;
  }

  public PsiClass[] getAllComponents() {
    componentNameToClass.values().removeIf(cls -> isPresentOnDisk(cls.getQualifiedName()));
    return componentNameToClass.values().toArray(PsiClass.EMPTY_ARRAY);
  }

  private boolean isPresentOnDisk(String qualifiedName) {
    // TODO: T56876413 clean-up to avoid conflicts with on-disk
    return false;
  }

  private void maybeUpdateAsync(
      PsiClass specClass, String componentQualifiedName, ShouldUpdateChecker checker) {
    final String componentShortName = StringUtil.getShortName(componentQualifiedName);
    if (componentShortName.isEmpty()) return;

    final ThrowableRunnable<RuntimeException> job =
        () -> {
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
                        .createFileFromText(
                            componentShortName + ".java", StdFileTypes.JAVA, fileContent);
                  })
              .flatMap(
                  file -> {
                    ComponentScope.include(file);
                    return LithoPluginUtils.getFirstClass(
                        file, cls -> componentShortName.equals(cls.getName()));
                  })
              .ifPresent(
                  inMemory -> {
                    logger.logStep("file creation " + componentShortName);
                    componentNameToClass.put(componentQualifiedName, inMemory);
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
    private final String componentQualifiedName;

    ShouldUpdateChecker(boolean forceUpdate, String componentQualifiedName) {
      this.forceUpdate = forceUpdate;
      this.componentQualifiedName = componentQualifiedName;
    }

    boolean shouldStopUpdate() {
      if (!componentNameToClass.containsKey(componentQualifiedName)) {
        return false;
      }
      return !forceUpdate;
    }
  }
}
