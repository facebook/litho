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

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JPanel;

/** Creates and manages settings UI for the Settings Dialog. */
public class AppSettingsView {
  private final JPanel mainPanel;
  private final JBCheckBox resolveRedSymbolsStatus =
      new JBCheckBox("Resolve Red Symbols Automatically");

  public AppSettingsView() {
    mainPanel =
        FormBuilder.createFormBuilder()
            .addComponent(resolveRedSymbolsStatus, 1)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
  }

  public JPanel getPanel() {
    return mainPanel;
  }

  public boolean isResolveRedSymbols() {
    return resolveRedSymbolsStatus.isSelected();
  }

  public void setResolveRedSymbols(boolean status) {
    resolveRedSymbolsStatus.setSelected(status);
  }
}
