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

import com.facebook.litho.intellij.LithoPluginUtils;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

class FileGeneratingAnnotator extends ExternalAnnotator<Void, Void> {

  @Nullable
  @Override
  public Void collectInformation(PsiFile file, Editor editor, boolean hasErrors) {
    return (Void)
        LithoPluginUtils.getFirstClass(file, cls -> true)
            .map(FileGenerateUtils::generateClass)
            .map(result -> null)
            .orElse(null);
  }
}
