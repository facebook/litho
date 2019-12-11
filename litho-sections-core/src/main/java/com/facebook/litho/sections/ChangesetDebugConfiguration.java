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

package com.facebook.litho.sections;

import com.facebook.litho.sections.SectionsLogEventUtils.ApplyNewChangeSet;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Used for debugging changeset generation. If a {@link ChangesetDebugListener} instance is
 * configured, an event will be dispatched from the {@link SectionTree} whenever a new changeset is
 * generated and applied.
 */
public class ChangesetDebugConfiguration {
  private static @Nullable ChangesetDebugListener sListener;

  public static class ChangesetDebugInfo {
    final @ApplyNewChangeSet int mSource;
    final @Nullable String mAttribution;
    private StackTraceElement[] mStackTrace;
    @Nullable Section mOldSection;
    @Nullable String mUpdateStateAttribution;

    ChangesetDebugInfo(int source, @Nullable String attribution, StackTraceElement[] stackTrace) {
      this(source, attribution, null, stackTrace);
    }

    ChangesetDebugInfo(
        int source,
        @Nullable String attribution,
        @Nullable String updateStateAttribution,
        StackTraceElement[] stackTrace) {
      mSource = source;
      mAttribution = attribution;
      mUpdateStateAttribution = updateStateAttribution;
      mStackTrace = stackTrace;
    }

    /** Get the id for the type of event that triggered a new changeset generation. */
    public int getSource() {
      return mSource;
    }

    /** Get the name of the section to which a new changeset generation is attributed to. */
    @Nullable
    public String getAttribution() {
      return mAttribution;
    }

    /** Get the root section from the old tree. */
    @Nullable
    public Section getOldSection() {
      return mOldSection;
    }

    public void setOldSection(@Nullable Section section) {
      mOldSection = section;
    }

    /** Get the name of the section which triggered a state update. */
    @Nullable
    public String getUpdateStateAttribution() {
      return mUpdateStateAttribution;
    }

    public StackTraceElement[] getStackTrace() {
      return mStackTrace;
    }
  }

  public interface ChangesetDebugListener {
    void onChangesetApplied(
        @Nullable Section rootSection,
        ChangesInfo changesInfo,
        String surfaceId,
        ChangesetDebugInfo changesetDebugInfo);

    @Deprecated
    void onChangesetApplied(
        @Nullable Section rootSection,
        @Nullable Section oldSection,
        ChangesInfo changesInfo,
        String surfaceId,
        @ApplyNewChangeSet int source,
        @Nullable String attribution);
  }

  public static void setListener(ChangesetDebugListener listener) {
    sListener = listener;
  }

  static @Nullable ChangesetDebugListener getListener() {
    return sListener;
  }

  /** @return a list with the names of the RenderInfos for this change. */
  public static List<String> getRenderInfoNames(Change change) {
    final List<String> names = new ArrayList<>();
    final List<RenderInfo> renderInfos = change.getRenderInfos();

    if (renderInfos.isEmpty()) {
      names.add(change.getRenderInfo().getName());
    } else {
      for (int i = 0; i < renderInfos.size(); i++) {
        names.add(renderInfos.get(i).getName());
      }
    }

    return names;
  }

  public static boolean isSectionDirty(
      @Nullable Section previousSection, @Nullable Section currentSection) {
    /**
     * If currentSection is null, the SectionTree has been released. Mark it as dirty as something
     * did change, though at this point we don't really care about its lifecycle anymore.
     */
    if (previousSection == null || currentSection == null) {
      return true;
    }

    return currentSection.shouldComponentUpdate(previousSection, currentSection);
  }

  public static int getSectionCount(Section section) {
    return section.getCount();
  }
}
