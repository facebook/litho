/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.intellij;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LithoPluginTestHelper {
  private final String testPath;

  /** @param testPath in the form "testdata/dir" */
  public LithoPluginTestHelper(String testPath) {
    this.testPath = new File(testPath).getAbsolutePath();
  }

  public String getContent(String clsName) throws IOException {
    return String.join(" ", Files.readAllLines(Paths.get(getTestDataPath(clsName))));
  }

  public String getTestDataPath(String clsName) {
    return testPath + "/" + clsName;
  }
}
