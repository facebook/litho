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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.facebook.litho.intellij.extensions.EventLogger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/** Provider of generated Litho files information. */
public class LithoGeneratedFileProvider {
  private final Map<String, PsiClass> componentFQNToSpec = new HashMap<>();

  public static LithoGeneratedFileProvider INSTANCE() {
    return Holder.INSTANCE;
  }

  private LithoGeneratedFileProvider() {}

  /**
   * Guesses class fully qualified names for the given short name. This method should be called
   * before {@link #createFileContent(String)}
   */
  public List<String> guessQualifiedNames(
      Project project,
      GlobalSearchScope searchScope,
      String shortName,
      Map<String, String> eventMetadata) {
    return Arrays.stream(
            PsiSearchUtils.findClassesByShortName(
                project,
                searchScope,
                LithoPluginUtils.getLithoComponentSpecNameFromComponent(shortName)))
        .filter(
            cls -> {
              if (LithoPluginUtils.isLayoutSpec(cls)) {
                eventMetadata.put(EventLogger.KEY_CLASS, "layout_spec");
                return true;
              } else if (LithoPluginUtils.isMountSpec(cls)) {
                eventMetadata.put(EventLogger.KEY_CLASS, "mount_spec");
                return true;
              } else {
                return false;
              }
            })
        .map(
            specCls -> {
              final String componentFQN =
                  LithoPluginUtils.getLithoComponentNameFromSpec(specCls.getQualifiedName());
              componentFQNToSpec.put(componentFQN, specCls);
              return componentFQN;
            })
        .collect(Collectors.toList());
  }

  /**
   * Creates new file text content for the given fully qualified name.
   *
   * @param fqn should be one of the names provided by {@link #guessQualifiedNames(Project,
   *     GlobalSearchScope, String, Map)}.
   * @return file text content.
   */
  @Nullable
  public String createFileContent(String fqn) {
    final Pair<String, String> content =
        ComponentGenerateService.getInstance().createLithoFileContent(componentFQNToSpec.get(fqn));
    if (content == null) return null;

    return content.second;
  }

  public void clear() {
    componentFQNToSpec.clear();
  }

  private static class Holder {
    static final LithoGeneratedFileProvider INSTANCE = new LithoGeneratedFileProvider();
  }
}
