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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;

public class LithoStartupActivity implements StartupActivity {

  @Override
  public void runActivity(Project project) {
    final GeneratedFilesListener listener1 = new GeneratedFilesListener(project);
    Disposer.register(project, listener1);
    final OnCodeAnalysisFinishedListener listener2 = new OnCodeAnalysisFinishedListener(project);
    Disposer.register(project, listener2);
  }
}
