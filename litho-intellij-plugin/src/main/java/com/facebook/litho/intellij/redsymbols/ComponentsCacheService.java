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

package com.facebook.litho.intellij.redsymbols;

import com.facebook.litho.intellij.IntervalLogger;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.Nullable;

/** Stores generated in-memory components. */
class ComponentsCacheService implements Disposable {
  private static final Logger LOG = Logger.getInstance(ComponentsCacheService.class);

  private final Map<String, PsiClass> componentNameToClass = new ConcurrentHashMap<>();
  private final Project project;

  ComponentsCacheService(Project project) {
    this.project = project;
  }

  static ComponentsCacheService getInstance(Project project) {
    return ServiceManager.getService(project, ComponentsCacheService.class);
  }

  @Override
  public void dispose() {
    componentNameToClass.clear();
  }

  /**
   * @return component already present in the cache by its full-qualified name or null if it's
   *     absent.
   */
  @Nullable
  PsiClass getComponent(String componentQualifiedName) {
    return componentNameToClass.get(componentQualifiedName);
  }

  PsiClass[] getAllComponents() {
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

  /** Updates cache. */
  void update(String componentQualifiedName, PsiClass inMemory) {
    componentNameToClass.put(componentQualifiedName, inMemory);
  }
}
