/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.sections.common;

import static com.facebook.litho.widget.RenderInfoDebugInfoRegistry.SONAR_SECTIONS_DEBUG_INFO_TAG;
import static com.facebook.litho.widget.RenderInfoDebugInfoRegistry.SONAR_SINGLE_COMPONENT_SECTION_DATA_NEXT;
import static com.facebook.litho.widget.RenderInfoDebugInfoRegistry.SONAR_SINGLE_COMPONENT_SECTION_DATA_PREV;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentUtils;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.Diff;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.LithoDebugConfigurations;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.utils.MapDiffUtils;
import com.facebook.litho.widget.ComponentRenderInfo;
import java.util.Map;

/**
 * This is the simplest Section within a Sections hierarchy and it can be used to represent one row
 * in a complex list.
 *
 * <pre>{@code
 * final Section loadingSection = SingleComponentSection.create(c)
 *      .component(MyComponent.create(c).build())
 *      .isFullSpan(true)
 *      .build();
 * }</pre>
 *
 * @prop component Component this section wraps.
 * @prop sticky If this section is a sticky header.
 * @prop spanSize Number of columns occupied by this section (relevant for multi-column layouts).
 * @prop isFullSpan It is {@code false} by default making section fit one column. Set it to {@code
 *     true} if this section should span all columns in a multi-column layout.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@DiffSectionSpec
public class SingleComponentSectionSpec {

  @OnDiff
  public static void onCreateChangeSet(
      SectionContext context,
      ChangeSet changeSet,
      @Prop Diff<Component> component,
      @Prop(optional = true) Diff<Boolean> sticky,
      @Prop(optional = true) Diff<Integer> spanSize,
      @Prop(optional = true) Diff<Boolean> isFullSpan,
      @Prop(optional = true) Diff<Map<String, Object>> customAttributes,
      @Prop(optional = true) Diff<Object> data,
      @Prop(optional = true) Diff<ComponentsLogger> componentsLogger,
      @Prop(optional = true) Diff<String> logTag,
      @Prop(optional = true) Diff<Boolean> shouldCompareComponentCommonProps) {
    final Object prevData = data.getPrevious();
    final Object nextData = data.getNext();
    final Component prevComponent = component.getPrevious();
    final Component nextComponent = component.getNext();

    if (prevComponent == null && nextComponent == null) {
      return;
    }

    if (prevComponent != null && nextComponent == null) {
      changeSet.delete(0, prevData);
      return;
    }

    boolean isNextSticky = false;
    if (sticky != null && sticky.getNext() != null) {
      isNextSticky = sticky.getNext();
    }

    int nextSpanSize = 1;
    if (spanSize != null && spanSize.getNext() != null) {
      nextSpanSize = spanSize.getNext();
    }

    boolean isNextFullSpan = false;
    if (isFullSpan != null && isFullSpan.getNext() != null) {
      isNextFullSpan = isFullSpan.getNext();
    }

    if (prevComponent == null) {
      changeSet.insert(
          0,
          addCustomAttributes(
                  ComponentRenderInfo.create(),
                  customAttributes.getNext(),
                  context,
                  component,
                  componentsLogger)
              .component(nextComponent)
              .isSticky(isNextSticky)
              .spanSize(nextSpanSize)
              .isFullSpan(isNextFullSpan)
              .logTag(logTag != null ? logTag.getNext() : null)
              .build(),
          context.getTreePropContainerCopy(),
          nextData);
      return;
    }

    // Both previous and next components are non-null -- check if an update is required.
    boolean isPrevSticky = false;
    if (sticky != null && sticky.getPrevious() != null) {
      isPrevSticky = sticky.getPrevious();
    }

    int prevSpanSize = 1;
    if (spanSize != null && spanSize.getPrevious() != null) {
      prevSpanSize = spanSize.getPrevious();
    }

    boolean isPrevFullSpan = false;
    if (isFullSpan != null && isFullSpan.getPrevious() != null) {
      isPrevFullSpan = isFullSpan.getPrevious();
    }
    final boolean customAttributesEqual =
        MapDiffUtils.areMapsEqual(customAttributes.getPrevious(), customAttributes.getNext());

    boolean shouldCompareComponentCommonPropsValue = false;
    if (shouldCompareComponentCommonProps != null
        && shouldCompareComponentCommonProps.getNext() != null) {
      shouldCompareComponentCommonPropsValue = shouldCompareComponentCommonProps.getNext();
    }

    if (isPrevSticky != isNextSticky
        || prevSpanSize != nextSpanSize
        || isPrevFullSpan != isNextFullSpan
        || !customAttributesEqual
        || !isComponentEquivalent(
            // NULLSAFE_FIXME[Parameter Not Nullable]
            prevComponent, nextComponent, shouldCompareComponentCommonPropsValue)) {
      changeSet.update(
          0,
          addCustomAttributes(
                  ComponentRenderInfo.create(),
                  customAttributes.getNext(),
                  context,
                  component,
                  componentsLogger)
              .component(nextComponent)
              .isSticky(isNextSticky)
              .spanSize(nextSpanSize)
              .isFullSpan(isNextFullSpan)
              .build(),
          context.getTreePropContainerCopy(),
          prevData,
          nextData);
    }
  }

  private static boolean isComponentEquivalent(
      Component prevComponent, Component nextComponent, boolean shouldCompareCommonProps) {
    return ComponentUtils.isEquivalent(
        prevComponent,
        nextComponent,
        ComponentsConfiguration.shouldCompareRootCommonPropsInSingleComponentSection
            || shouldCompareCommonProps);
  }

  private static ComponentRenderInfo.Builder addCustomAttributes(
      ComponentRenderInfo.Builder builder,
      @Nullable Map<String, Object> attributes,
      SectionContext c,
      Diff<Component> component,
      @Nullable Diff<ComponentsLogger> componentsLogger) {
    if (LithoDebugConfigurations.isRenderInfoDebuggingEnabled) {
      builder.debugInfo(SONAR_SECTIONS_DEBUG_INFO_TAG, c.getSectionScope());
      builder.debugInfo(SONAR_SINGLE_COMPONENT_SECTION_DATA_PREV, component.getPrevious());
      builder.debugInfo(SONAR_SINGLE_COMPONENT_SECTION_DATA_NEXT, component.getNext());
    }

    if (attributes != null) {
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        builder.customAttribute(entry.getKey(), entry.getValue());
      }
    }

    builder.componentsLogger(componentsLogger != null ? componentsLogger.getNext() : null);

    return builder;
  }
}
