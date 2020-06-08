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

package com.facebook.litho.intellij.settings;

import com.facebook.litho.intellij.extensions.EventLogger;
import com.facebook.litho.intellij.logging.LithoLoggerProvider;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nls;

/** Bridges settings UI with settings data model. */
public class AppSettingsController implements Configurable {
  private final Project project;
  private AppSettingsView view;

  public AppSettingsController(Project project) {
    this.project = project;
  }

  @Nls(capitalization = Nls.Capitalization.Title)
  @Override
  public String getDisplayName() {
    return "Litho";
  }

  @Override
  public JComponent createComponent() {
    view = new AppSettingsView();
    return view.getPanel();
  }

  @Override
  public boolean isModified() {
    final AppSettingsState.Model model = AppSettingsState.getInstance(project).getState();
    return view.isResolveRedSymbols() != model.resolveRedSymbols;
  }

  @Override
  public void apply() {
    final AppSettingsState.Model model = AppSettingsState.getInstance(project).getState();
    final boolean resolveRedSymbols = view.isResolveRedSymbols();
    model.resolveRedSymbols = resolveRedSymbols;
    final Map<String, String> eventData = new HashMap<>(1);
    eventData.put(EventLogger.KEY_RED_SYMBOLS, String.valueOf(resolveRedSymbols));
    LithoLoggerProvider.getEventLogger().log(EventLogger.EVENT_SETTINGS, eventData);
  }

  @Override
  public void reset() {
    final AppSettingsState.Model model = AppSettingsState.getInstance(project).getState();
    view.setResolveRedSymbols(model.resolveRedSymbols);
  }

  @Override
  public void disposeUIResources() {
    view = null;
  }
}
