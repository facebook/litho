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

package com.facebook.litho.intellij.completion;

import com.facebook.litho.intellij.extensions.BuildInfoProvider;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * Provides build component information.
 *
 * @see BuildInfoProvider#provideGeneratedComponentDir(String, String)
 */
class ComponentBuildInfoProvider {
  private static final ExtensionPointName<BuildInfoProvider> EP_NAME =
      ExtensionPointName.create("com.facebook.litho.intellij.buildInfoProvider");

  private static final ComponentBuildInfoProvider INSTANCE = new ComponentBuildInfoProvider();

  public static ComponentBuildInfoProvider getInstance() {
    return INSTANCE;
  }

  private final BuildInfoProvider[] providers = Extensions.getExtensions(EP_NAME);

  /**
   * Accesses external implementations.
   *
   * @see BuildInfoProvider#provideGeneratedComponentDir(String, String)
   */
  Stream<PsiDirectory> provideGeneratedComponentDirs(
      String specDirPath, String specPackageName, Project project) {
    VirtualFile baseDir = project.getBaseDir();
    PsiManager psiManager = PsiManager.getInstance(project);
    return provideGeneratedComponentDirs(
        providers, specDirPath, specPackageName, baseDir, psiManager);
  }

  @VisibleForTesting
  static Stream<PsiDirectory> provideGeneratedComponentDirs(
      BuildInfoProvider[] providers,
      String specDirPath,
      String specPackageName,
      VirtualFile baseDir,
      PsiManager psiManager) {
    return Arrays.stream(providers)
        .map(provider -> provider.provideGeneratedComponentDir(specDirPath, specPackageName))
        .filter(path -> isValidDirForPackage(path, specPackageName))
        .map(path -> findTargetDirectory(path, baseDir))
        .filter(Objects::nonNull)
        .map(psiManager::findDirectory)
        .filter(Objects::nonNull);
  }

  @VisibleForTesting
  static boolean isValidDirForPackage(@Nullable String dirPath, String packageName) {
    return dirPath != null && dirPath.endsWith(packageName.replace('.', '/'));
  }

  @Nullable
  @VisibleForTesting
  static VirtualFile findTargetDirectory(String path, VirtualFile baseDir) {
    VirtualFile currentDir = baseDir;
    List<String> dirs = StringUtil.split(path, "/");
    for (String dir : dirs) {
      VirtualFile child = currentDir.findChild(dir);
      if (child == null) {
        return null;
      }
      currentDir = child;
    }
    return currentDir;
  }
}
