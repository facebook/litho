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
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.completion.ComponentGenerateUtils;
import com.facebook.litho.intellij.file.ComponentScope;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.util.ThrowableRunnable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.Nullable;

/**
 * Stores generated in-memory components. Cache could be updated via {@link #maybeUpdate(PsiClass,
 * boolean)} call.
 */
public class ComponentsCacheService implements Disposable {
  private static final Logger LOG = Logger.getInstance(ComponentsCacheService.class);

  private final Map<String, PsiClass> componentNameToClass = new ConcurrentHashMap<>();
  private final Project project;

  public ComponentsCacheService(Project project) {
    this.project = project;
  }

  @Override
  public void dispose() {
    componentNameToClass.clear();
  }

  @Nullable("returns null if component was not created")
  public PsiClass maybeUpdate(PsiClass specClass, boolean forceUpdate) {
    if (!LithoPluginUtils.isLayoutSpec(specClass)) return null;

    final String componentQualifiedName =
        LithoPluginUtils.getLithoComponentNameFromSpec(specClass.getQualifiedName());
    if (componentQualifiedName == null) return null;

    final ShouldUpdateChecker checker =
        new ShouldUpdateChecker(forceUpdate, componentQualifiedName);
    maybeUpdateInReadAction(specClass, componentQualifiedName, checker);
    return getComponent(componentQualifiedName);
  }

  /**
   * @return component already present in the cache by its full-qualified name or null if it's
   *     absent.
   * @see #maybeUpdate(PsiClass, boolean)
   */
  @Nullable
  public PsiClass getComponent(String componentQualifiedName) {
    return componentNameToClass.get(componentQualifiedName);
  }

  public PsiClass[] getAllComponents() {
    return componentNameToClass.values().toArray(PsiClass.EMPTY_ARRAY);
  }

  void invalidate() {
    final List<String> componentNames = new ArrayList<>(componentNameToClass.keySet());
    LOG.debug("Invalidating " + componentNames);
    if (componentNames.isEmpty()) return;
    IntervalLogger logger = new IntervalLogger(LOG);

    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(project, "Invalidating In-Memory Files") {
              @Override
              public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                for (int i = 0, size = componentNames.size(); i < size; i++) {
                  indicator.setFraction(((double) i + 1) / size);
                  indicator.setText2(i + 1 + "/" + size);
                  String clsName = componentNames.get(i);
                  AtomicBoolean found = new AtomicBoolean(false);
                  ReadAction.run(
                      () -> {
                        final boolean foundCls =
                            Arrays.stream(PsiSearchUtils.findClasses(project, clsName))
                                .anyMatch(cls -> !ComponentScope.contains(cls.getContainingFile()));
                        found.set(foundCls);
                      });
                  if (found.get()) {
                    logger.logStep("removing " + clsName);
                    componentNameToClass.remove(clsName);
                  } else {
                    logger.logStep("keeping " + clsName);
                  }
                }
              }
            });
  }

  private void maybeUpdateInReadAction(
      PsiClass specClass, String componentQualifiedName, ShouldUpdateChecker checker) {
    final String componentShortName = StringUtil.getShortName(componentQualifiedName);
    if (componentShortName.isEmpty()) return;

    final ThrowableRunnable<RuntimeException> job =
        () -> {
          if (checker.shouldStopUpdate()) return;
          IntervalLogger logger = new IntervalLogger(LOG);
          Optional.ofNullable(ComponentGenerateUtils.createLayoutModel(specClass))
              .map(
                  specModel -> {
                    logger.logStep("model creation " + componentShortName);
                    if (checker.shouldStopUpdate()) return null;

                    return ComponentGenerateUtils.createFileFromModel(
                        componentQualifiedName, specModel, project);
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
    if (ApplicationManager.getApplication().isReadAccessAllowed()) {
      job.run();
    } else {
      ReadAction.run(job);
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
      if (project.isDisposed()) {
        return true;
      }
      if (!componentNameToClass.containsKey(componentQualifiedName)) {
        return false;
      }
      return !forceUpdate;
    }
  }
}
