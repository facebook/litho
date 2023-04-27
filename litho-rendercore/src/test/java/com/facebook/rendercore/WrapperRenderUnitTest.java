// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import static com.facebook.rendercore.RenderUnit.DelegateBinder.createDelegateBinder;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;
import android.view.View;
import com.facebook.rendercore.TestBinder.TestBinder1;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WrapperRenderUnitTest {

  private final List<TestBinder<?>> bindOrder = new ArrayList<>();
  private final List<TestBinder<?>> unbindOrder = new ArrayList<>();
  private final Context context = RuntimeEnvironment.getApplication();
  private final View content = new View(context);
  private final Systracer tracer = RenderCoreSystrace.getInstance();

  @Before
  public void setup() {
    bindOrder.clear();
    unbindOrder.clear();
  }

  @Test
  public void mountUnmountBinders_originalBindersMountFirstAndUnmountLast() {
    TestBinder1 originalMountBinder = new TestBinder1(bindOrder, unbindOrder);
    final TestRenderUnit originalRenderUnit =
        new TestRenderUnit(
            singletonList(createDelegateBinder(new TestRenderUnit(), originalMountBinder)));

    final TestBinder1 wrapperMountBinder = new TestBinder1(bindOrder, unbindOrder);
    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);
    wrapperRenderUnit.addOptionalMountBinder(
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder));

    wrapperRenderUnit.mountBinders(context, content, null, tracer);
    assertThat(bindOrder).containsExactly(originalMountBinder, wrapperMountBinder);

    wrapperRenderUnit.unmountBinders(context, content, null, tracer);
    assertThat(unbindOrder).containsExactly(wrapperMountBinder, originalMountBinder);
  }

  @Test
  public void attachDetachBinders_originalBindersAttachFirstAndDetachLast() {
    TestBinder1 originalAttachBinder = new TestBinder1(bindOrder, unbindOrder);
    final TestRenderUnit originalRenderUnit =
        new TestRenderUnit(
            emptyList(),
            emptyList(),
            singletonList(createDelegateBinder(new TestRenderUnit(), originalAttachBinder)));

    final TestBinder1 wrapperAttachBinder = new TestBinder1(bindOrder, unbindOrder);
    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);
    wrapperRenderUnit.addAttachBinder(
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder));

    wrapperRenderUnit.attachBinders(context, content, null, tracer);
    assertThat(bindOrder).containsExactly(originalAttachBinder, wrapperAttachBinder);

    wrapperRenderUnit.detachBinders(context, content, null, tracer);
    assertThat(unbindOrder).containsExactly(wrapperAttachBinder, originalAttachBinder);
  }
}
