package com.facebook.samples.lithobarebones;

import android.util.Log;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController;

/**
 * Wrapper Layout, so we can set the visibility handler.
 */
@LayoutSpec
final class MyLayoutSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop RecyclerCollectionEventsController eventsController,
      @Prop MySimpleCallback callback) {
    return RecyclerCollectionComponent.create(c)
        .disablePTR(true)
        .eventsController(eventsController)
        .visibleHandler(MyLayout.onVisible(c))
        .section(ListSection.create(new SectionContext(c)).callback(callback).build())
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(
      ComponentContext c,
      @Prop RecyclerCollectionEventsController eventsController,
      @Prop ItemTouchHelper itemTouchHelper) {
    // At this point, we should get a not-null RecyclerView.
    RecyclerView rv = eventsController.getRecyclerView();
    itemTouchHelper.attachToRecyclerView(rv);
  }
}
