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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import java.util.List;

/** Listens for files created in file system and removes their in-memory copies from the cache. */
class GeneratedFilesListener implements BulkFileListener, Disposable {
  private static final Logger LOG = Logger.getInstance(GeneratedFilesListener.class);
  static final String BUCK_OUT_BASE = "/buck-out/annotation/";
  private static final String GRADLE_OUTPUT_BASE = "/build/generated/source/apt/debug/";
  private Project project;
  private MessageBusConnection connection;

  GeneratedFilesListener(Project project) {
    this.project = project;
    this.connection = project.getMessageBus().connect();
    connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    LOG.debug("Connected");
  }

  @Override
  public void dispose() {
    connection.disconnect();
    LOG.debug("Disposed");
  }

  @Override
  public void after(List<? extends VFileEvent> events) {
    IntervalLogger logger = new IntervalLogger(LOG);

    final boolean found =
        events.stream()
            .filter(VFileCreateEvent.class::isInstance)
            .map(VFileEvent::getFile)
            .filter(file -> file != null && file.isValid())
            .map(file -> FileUtil.toSystemIndependentName(file.getPath()))
            .anyMatch(GeneratedFilesListener::isOutput);
    logger.logStep("finding created paths: " + found);
    if (!found) return;

    // Wait for indexing
    final Runnable job =
        () -> {
          logger.logStep("start of removing files");
          ComponentsCacheService.getInstance(project).invalidate();
        };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      job.run();
    } else {
      DumbService.getInstance(project).smartInvokeLater(job);
    }
  }

  /** @param path path with "/" dividers. */
  private static boolean isOutput(String path) {
    // Guessing if the path is build output
    return containsOrEndsWithSegment(path, BUCK_OUT_BASE)
        || containsOrEndsWithSegment(path, GRADLE_OUTPUT_BASE);
  }

  /**
   * @param path1 path with "/" dividers.
   * @param path2 path with "/" dividers.
   * @return if path1 contains whole path 2 or path 1 ends with segment from path 2.
   */
  private static boolean containsOrEndsWithSegment(String path1, String path2) {
    if (path1.contains(path2)) return true;
    for (int i = path2.lastIndexOf('/'); i > 0; i = path2.lastIndexOf('/')) {
      path2 = path2.substring(0, i);
      if (path1.endsWith(path2)) return true;
    }
    return false;
  }
}
