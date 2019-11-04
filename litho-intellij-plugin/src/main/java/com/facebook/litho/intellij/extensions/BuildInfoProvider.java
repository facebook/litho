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

package com.facebook.litho.intellij.extensions;

/**
 * Extension point for other plugins to provide information about generated files for different
 * build systems.
 *
 * @see "plugin.xml"
 */
public interface BuildInfoProvider {

  /**
   * @param specDirPath path to the Spec file starting from the root directory with '/' separator.
   *     For example if 'MyApp/src' is the root folder with 'src' subdirectory, path starts with
   *     'src/'.
   * @param specPackageName package name of the Spec file with '.' separator 'com.example.package'
   * @return path to the generated component starting from the root directory
   */
  String provideGeneratedComponentDir(String specDirPath, String specPackageName);
}
