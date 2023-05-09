// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import static com.facebook.rendercore.RenderUnit.DelegateBinder.createDelegateBinder;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import com.facebook.rendercore.TestBinder.TestBinder1;
import com.facebook.rendercore.TestBinder.TestBinder2;
import com.facebook.rendercore.TestBinder.TestBinder3;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RenderUnitTest {

  private final List<TestBinder<?>> mBindOrder = new ArrayList<>();
  private final List<TestBinder<?>> mUnbindOrder = new ArrayList<>();
  private final Context mContext = RuntimeEnvironment.application;
  private final View mContent = new View(mContext);
  private final Systracer mTracer = RenderCoreSystrace.getInstance();

  @Before
  public void setup() {
    mBindOrder.clear();
    mUnbindOrder.clear();
  }

  @Test
  public void testAddMountBinders_WithSameBinderType_WontAddDuplicates() {
    final TestRenderUnit renderUnit = new TestRenderUnit();

    final TestBinder1 mountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 mountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 mountBinder3sameTypeAs1 = new TestBinder1(mBindOrder, mUnbindOrder);

    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2),
        createDelegateBinder(renderUnit, mountBinder3sameTypeAs1));

    renderUnit.mountBinders(mContext, mContent, null, mTracer);

    assertThat(mBindOrder).containsExactly(mountBinder2, mountBinder3sameTypeAs1);
    assertThat(mUnbindOrder).isEmpty();
  }

  @Test
  public void testMountExtensions() {
    final TestRenderUnit renderUnit = new TestRenderUnit();

    final TestBinder1 attachBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 attachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 mountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 mountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);

    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2));
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2));

    renderUnit.mountBinders(mContext, mContent, null, mTracer);

    assertThat(mBindOrder).containsExactly(mountBinder1, mountBinder2);
    assertThat(mUnbindOrder).isEmpty();
  }

  @Test
  public void testUnmountExtensions() {
    final TestRenderUnit renderUnit = new TestRenderUnit();

    final TestBinder1 attachBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 attachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 mountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 mountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);

    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2));
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2));

    renderUnit.unmountBinders(mContext, mContent, null, mTracer);

    assertThat(mBindOrder).isEmpty();
    assertThat(mUnbindOrder).containsExactly(mountBinder2, mountBinder1);
  }

  @Test
  public void testAttachExtensions() {
    final TestRenderUnit renderUnit = new TestRenderUnit();

    final TestBinder1 attachBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 attachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 mountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 mountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);

    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2));
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2));

    renderUnit.attachBinders(mContext, mContent, null, mTracer);

    assertThat(mBindOrder).containsExactly(attachBinder1, attachBinder2);
    assertThat(mUnbindOrder).isEmpty();
  }

  @Test
  public void testDetachExtensions() {
    final TestRenderUnit renderUnit = new TestRenderUnit();

    final TestBinder1 attachBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 attachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 mountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 mountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);

    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2));
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2));

    renderUnit.detachBinders(mContext, mContent, null, mTracer);

    assertThat(mBindOrder).isEmpty();
    assertThat(mUnbindOrder).containsExactly(attachBinder2, attachBinder1);
  }

  @Test
  public void testAddFixedMountBinders_WithSameBinderType_WillAddDuplicates() {
    final TestBinder1 fixedMountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 fixedMountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 fixedMountBinder3sameTypeAs1 = new TestBinder1(mBindOrder, mUnbindOrder);

    final TestRenderUnit renderUnit =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder2),
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder3sameTypeAs1)));

    renderUnit.mountBinders(mContext, mContent, null, mTracer);

    assertThat(mBindOrder)
        .containsExactly(fixedMountBinder1, fixedMountBinder2, fixedMountBinder3sameTypeAs1);
    assertThat(mUnbindOrder).isEmpty();
  }

  @Test
  public void testMountUnmountBinders_WillBeDoneInCorrectOrder() {
    final TestBinder1 fixedMountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 fixedMountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 mountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 mountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);

    final TestRenderUnit renderUnit =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder2)));

    renderUnit.addOptionalMountBinder(createDelegateBinder(renderUnit, mountBinder1));
    renderUnit.addOptionalMountBinder(createDelegateBinder(renderUnit, mountBinder2));

    renderUnit.mountBinders(mContext, mContent, null, mTracer);
    renderUnit.unmountBinders(mContext, mContent, null, mTracer);

    assertThat(mBindOrder)
        .containsExactly(fixedMountBinder1, fixedMountBinder2, mountBinder1, mountBinder2);
    assertThat(mUnbindOrder)
        .containsExactly(mountBinder2, mountBinder1, fixedMountBinder2, fixedMountBinder1);
  }

  @Test
  public void testUpdateExtensionsWithFixedBinders_WillBeDoneInCorrectOrder() {
    final List<TestBinder<?>> binders = new ArrayList<>();
    final List<RenderUnit.DelegateBinder<?, ? super View>> fixedMountBinders = new ArrayList<>();
    for (int i = 0; i < 64; i++) {
      final TestBinder1 binder = new TestBinder1(mBindOrder, mUnbindOrder);
      binders.add(binder);
      fixedMountBinders.add(createDelegateBinder(new TestRenderUnit(), binder));
    }

    final TestRenderUnit currentRU = new TestRenderUnit(fixedMountBinders);
    final TestRenderUnit nextRU = new TestRenderUnit(fixedMountBinders);

    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, true, mTracer);

    assertThat(mBindOrder).containsExactlyElementsOf(binders);
    Collections.reverse(binders);
    assertThat(mUnbindOrder).containsExactlyElementsOf(binders);
  }

  @Test
  public void testUpdateExtensionsWithDifferentTypesOfBinders_WillBeDoneInCorrectOrder() {
    final TestBinder1 attachBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 attachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder2 mountBinder1 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder3 mountBinder2 = new TestBinder3(mBindOrder, mUnbindOrder);
    final TestBinder3 fixedMountBinder1 = new TestBinder3(mBindOrder, mUnbindOrder);
    final TestBinder3 fixedMountBinder2 = new TestBinder3(mBindOrder, mUnbindOrder);

    final TestRenderUnit currentRU =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder2)));

    final TestRenderUnit nextRU =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder2)));

    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder1));
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder2));
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder1));
    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder2));

    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder1));
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder2));
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder1));
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder2));

    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, true, mTracer);

    assertThat(mUnbindOrder)
        .containsExactly(
            attachBinder2,
            attachBinder1,
            mountBinder2,
            mountBinder1,
            fixedMountBinder2,
            fixedMountBinder1);
    assertThat(mBindOrder)
        .containsExactly(
            fixedMountBinder1,
            fixedMountBinder2,
            mountBinder1,
            mountBinder2,
            attachBinder1,
            attachBinder2);
  }

  @Test(expected = IllegalStateException.class)
  public void testUpdateExtensions_withDifferentFixedBindersCount_shouldCrash() {
    final TestBinder1 fixedMountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder1 fixedMountBinder2 = new TestBinder1(mBindOrder, mUnbindOrder);

    final TestRenderUnit currentRU =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder1),
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder2)));

    final TestRenderUnit nextRU =
        new TestRenderUnit(
            Collections.singletonList(
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder1)));

    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, true, mTracer);
  }

  @Test(expected = IllegalStateException.class)
  public void testUpdateExtensions_withRemovedFixedBinder_shouldCrash() {
    final TestBinder1 fixedMountBinder = new TestBinder1(mBindOrder, mUnbindOrder);

    final TestRenderUnit currentRU =
        new TestRenderUnit(
            Collections.singletonList(
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder)));

    final TestRenderUnit nextRU = new TestRenderUnit();

    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, true, mTracer);
  }

  @Test
  public void testUpdateExtensions_withFixedBinderAndDifferentModelTypes_shouldNotCrash() {
    final TestBinder<Drawable> fixedDrawableMountBinder =
        new TestBinder<>(mBindOrder, mUnbindOrder);
    final ColorDrawable drawable1 = new ColorDrawable(Color.CYAN);
    final GradientDrawable drawable2 = new GradientDrawable();

    final TestRenderUnit currentRU =
        new TestRenderUnit(
            Collections.singletonList(createDelegateBinder(drawable1, fixedDrawableMountBinder)));

    final TestRenderUnit nextRU =
        new TestRenderUnit(
            Collections.singletonList(createDelegateBinder(drawable2, fixedDrawableMountBinder)));

    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, true, mTracer);

    assertThat(mBindOrder).containsExactly(fixedDrawableMountBinder);
    assertThat(mUnbindOrder).containsExactly(fixedDrawableMountBinder);
  }

  @Test
  public void testAdd64FixedMountBinders_shouldNotCrash() {
    final List<RenderUnit.DelegateBinder<?, ? super View>> fixedMountBinders = new ArrayList<>();
    for (int i = 0; i < 64; i++) {
      fixedMountBinders.add(createDelegateBinder(new TestRenderUnit(), new TestBinder1()));
    }
    new TestRenderUnit(fixedMountBinders);
  }

  @Test(expected = IllegalStateException.class)
  public void testAdd65FixedMountBinders_shouldCrash() {
    final List<RenderUnit.DelegateBinder<?, ? super View>> fixedMountBinders = new ArrayList<>();
    for (int i = 0; i < 65; i++) {
      fixedMountBinders.add(createDelegateBinder(new TestRenderUnit(), new TestBinder1()));
    }
    new TestRenderUnit(fixedMountBinders);
  }

  @Test
  public void testUpdateExtensions_WithDifferentExtensionLists() {
    final TestRenderUnit currentRU = new TestRenderUnit();
    final TestRenderUnit nextRU = new TestRenderUnit();

    final TestBinder1 attachBinder = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder1 mountBinder = new TestBinder1(mBindOrder, mUnbindOrder);

    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder));
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder));

    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, true, mTracer);

    assertThat(mUnbindOrder).containsExactly(mountBinder);
    assertThat(mBindOrder).containsExactly(attachBinder);
  }

  @Test
  public void testUpdateExtensions_withAlreadyDetachedItem_shouldNotUnbind() {
    final TestRenderUnit currentRU = new TestRenderUnit();
    final TestRenderUnit nextRU = new TestRenderUnit();

    final TestBinder1 attachBinder = new TestBinder1(mBindOrder, mUnbindOrder);

    currentRU.addAttachBinder(createDelegateBinder(currentRU, attachBinder));
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder));

    // Pass false to isAttached, indicating currentRU is already unbound
    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, false, mTracer);

    // unbind should not happen, so unbind-order should be empty.
    assertThat(mUnbindOrder).isEmpty();

    // bind should still happen normally
    assertThat(mBindOrder).containsExactly(attachBinder);
  }

  @Test
  public void testUpdateExtensions_WithDifferentNumberOfExtensions2_shouldUpdateTrue() {
    final TestRenderUnit currentRU = new TestRenderUnit();
    final TestRenderUnit nextRU = new TestRenderUnit();

    final TestBinder1 currentAttachBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 currentAttachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 currentMountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);

    final TestBinder2 nextAttachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder2 nextMountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);

    currentRU.addAttachBinders(
        createDelegateBinder(currentRU, currentAttachBinder1),
        createDelegateBinder(currentRU, currentAttachBinder2));
    currentRU.addOptionalMountBinders(createDelegateBinder(currentRU, currentMountBinder1));
    nextRU.addAttachBinders(createDelegateBinder(nextRU, nextAttachBinder2));
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, nextMountBinder2));

    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, true, mTracer);

    assertThat(mUnbindOrder)
        .containsExactly(currentAttachBinder2, currentAttachBinder1, currentMountBinder1);
    assertThat(mBindOrder).containsExactly(nextMountBinder2, nextAttachBinder2);
  }

  @Test
  public void testUpdateExtensions_WithDifferentNumberOfExtensions_shouldUpdateFalse() {
    final TestRenderUnit currentRU = new TestRenderUnit();
    final TestRenderUnit nextRU = new TestRenderUnit();

    final TestBinder1 currentAttachBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);
    final TestBinder2 currentAttachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder1 currentMountBinder1 = new TestBinder1(mBindOrder, mUnbindOrder);

    final TestBinder2 nextAttachBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);
    final TestBinder2 nextMountBinder2 = new TestBinder2(mBindOrder, mUnbindOrder);

    currentRU.addAttachBinders(
        createDelegateBinder(currentRU, currentAttachBinder1),
        createDelegateBinder(currentRU, currentAttachBinder2));
    currentRU.addOptionalMountBinders(createDelegateBinder(currentRU, currentMountBinder1));
    nextRU.addAttachBinders(createDelegateBinder(nextRU, nextAttachBinder2));
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, nextMountBinder2));

    nextRU.updateBinders(mContext, mContent, currentRU, null, null, null, true, mTracer);

    assertThat(mUnbindOrder).containsExactly(currentAttachBinder1, currentMountBinder1);
    assertThat(mBindOrder).containsExactly(nextMountBinder2);
  }

  @Test
  public void testUpdateExtensions_withDifferentModelTypes_shouldNotCrash() {
    final TestRenderUnit currentRU = new TestRenderUnit();
    final TestRenderUnit nextRU = new TestRenderUnit();

    final TestBinder<Drawable> drawableMountBinder = new TestBinder<>(mBindOrder, mUnbindOrder);

    final ColorDrawable drawable1 = new ColorDrawable(Color.CYAN);
    final GradientDrawable drawable2 = new GradientDrawable();

    currentRU.addOptionalMountBinders(createDelegateBinder(drawable1, drawableMountBinder));
    nextRU.addOptionalMountBinders(createDelegateBinder(drawable2, drawableMountBinder));

    nextRU.updateBinders(mContext, mContent, currentRU, null, new Object(), null, true, mTracer);

    assertThat(mBindOrder).containsExactly(drawableMountBinder);
    assertThat(mUnbindOrder).containsExactly(drawableMountBinder);
  }
}
