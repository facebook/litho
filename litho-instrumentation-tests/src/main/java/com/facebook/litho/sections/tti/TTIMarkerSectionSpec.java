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

package com.facebook.litho.sections.tti;

import android.widget.Toast;
import com.facebook.litho.RenderCompleteEvent;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import java.util.List;

@GroupSectionSpec
public class TTIMarkerSectionSpec {

  static final String RENDER_MARKER = "renderMaker";

  @OnCreateChildren
  static Children onCreateChildren(final SectionContext c, @Prop List<Object> data) {
    return Children.create()
        .child(
            DataDiffSection.create(c).data(data).renderEventHandler(TTIMarkerSection.onRender(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(final SectionContext c, @FromEvent String model) {
    return ComponentRenderInfo.create()
        .component(Text.create(c).text(model).textSizeSp(14).build())
        .renderCompleteHandler(TTIMarkerSection.onRenderComplete(c, RENDER_MARKER))
        .build();
  }

  @OnEvent(RenderCompleteEvent.class)
  static void onRenderComplete(
      SectionContext c,
      @Param String renderMarker,
      @FromEvent RenderCompleteEvent.RenderState renderState,
      @FromEvent long timestampMillis) {
    final boolean hasMounted = renderState == RenderCompleteEvent.RenderState.RENDER_DRAWN;
    Toast.makeText(c.getAndroidContext(), renderMarker, Toast.LENGTH_SHORT).show();
  }
}
