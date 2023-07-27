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

package com.facebook.rendercore;

import static com.facebook.rendercore.RenderUnit.DelegateBinder.createDelegateBinder;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.SparseArray;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import com.facebook.rendercore.RenderUnit.Binder;
import com.facebook.rendercore.TestBinder.TestBinder1;
import com.facebook.rendercore.TestBinder.TestBinder2;
import com.facebook.rendercore.TestBinder.TestBinder3;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData1;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData2;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData3;
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
  private final BindData mBindData = new BindData();

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

    renderUnit.mountBinders(mContext, mContent, null, mBindData, mTracer);

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

    renderUnit.mountBinders(mContext, mContent, null, mBindData, mTracer);

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

    renderUnit.unmountBinders(mContext, mContent, null, mBindData, mTracer);

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

    renderUnit.attachBinders(mContext, mContent, null, mBindData, mTracer);

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

    renderUnit.detachBinders(mContext, mContent, null, mBindData, mTracer);

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

    renderUnit.mountBinders(mContext, mContent, null, mBindData, mTracer);

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

    renderUnit.mountBinders(mContext, mContent, null, mBindData, mTracer);
    renderUnit.unmountBinders(mContext, mContent, null, mBindData, mTracer);

    assertThat(mBindOrder)
        .containsExactly(fixedMountBinder1, fixedMountBinder2, mountBinder1, mountBinder2);
    assertThat(mUnbindOrder)
        .containsExactly(mountBinder2, mountBinder1, fixedMountBinder2, fixedMountBinder1);
  }

  @Test
  public void testUpdateExtensionsWithFixedBinders_WillBeDoneInCorrectOrder() {
    final List<TestBinder<?>> binders = new ArrayList<>();
    final List<RenderUnit.DelegateBinder<?, ? super View, ?>> fixedMountBinders = new ArrayList<>();
    for (int i = 0; i < 64; i++) {
      final TestBinder1 binder = new TestBinder1(mBindOrder, mUnbindOrder);
      binders.add(binder);
      fixedMountBinders.add(createDelegateBinder(new TestRenderUnit(), binder));
    }

    final TestRenderUnit currentRU = new TestRenderUnit(fixedMountBinders);
    final TestRenderUnit nextRU = new TestRenderUnit(fixedMountBinders);

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, true, mTracer);

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

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, true, mTracer);

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

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, true, mTracer);
  }

  @Test(expected = IllegalStateException.class)
  public void testUpdateExtensions_withRemovedFixedBinder_shouldCrash() {
    final TestBinder1 fixedMountBinder = new TestBinder1(mBindOrder, mUnbindOrder);

    final TestRenderUnit currentRU =
        new TestRenderUnit(
            Collections.singletonList(
                createDelegateBinder(new TestRenderUnit(), fixedMountBinder)));

    final TestRenderUnit nextRU = new TestRenderUnit();

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, true, mTracer);
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

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, true, mTracer);

    assertThat(mBindOrder).containsExactly(fixedDrawableMountBinder);
    assertThat(mUnbindOrder).containsExactly(fixedDrawableMountBinder);
  }

  @Test
  public void testAdd64FixedMountBinders_shouldNotCrash() {
    final List<RenderUnit.DelegateBinder<?, ? super View, ?>> fixedMountBinders = new ArrayList<>();
    for (int i = 0; i < 64; i++) {
      fixedMountBinders.add(createDelegateBinder(new TestRenderUnit(), new TestBinder1()));
    }
    new TestRenderUnit(fixedMountBinders);
  }

  @Test(expected = IllegalStateException.class)
  public void testAdd65FixedMountBinders_shouldCrash() {
    final List<RenderUnit.DelegateBinder<?, ? super View, ?>> fixedMountBinders = new ArrayList<>();
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

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, true, mTracer);

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
    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, false, mTracer);

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

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, true, mTracer);

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

    nextRU.updateBinders(mContext, mContent, currentRU, null, null, null, mBindData, true, mTracer);

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

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, mBindData, true, mTracer);

    assertThat(mBindOrder).containsExactly(drawableMountBinder);
    assertThat(mUnbindOrder).containsExactly(drawableMountBinder);
  }

  @Test
  public void mountBinders_withBindData_createsBindData() {
    final TestBinderWithBindData1 fixedBinder1 = new TestBinderWithBindData1(1);
    final TestBinderWithBindData2 fixedBinder2 = new TestBinderWithBindData2(2);
    final TestBinderWithBindData1 mountBinder1 = new TestBinderWithBindData1(3);
    final TestBinderWithBindData2 mountBinder2 = new TestBinderWithBindData2(4);

    final TestRenderUnit renderUnit =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), fixedBinder1),
                createDelegateBinder(new TestRenderUnit(), fixedBinder2)));
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2));
    final BindData bindData = createBindData(null, null, null);

    renderUnit.mountBinders(mContext, mContent, null, bindData, mTracer);

    // assert fixed binders bind data is correct
    assertThat(bindData.getFixedBindersBindData()).containsExactly(1, 2);

    // assert optional mount binders bind data is correct
    assertThat(bindData.getOptionalMountBindersBindData()).hasSize(2);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder1.getClass()))
        .isEqualTo(3);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder2.getClass()))
        .isEqualTo(4);

    // assert no attach binders bind data is present when calling mountBinders
    assertThat(bindData.getAttachBindersBindData()).isNull();
  }

  @Test
  public void attachBinders_withBindData_createsBindData() {
    final TestBinderWithBindData1 attachBinder1 = new TestBinderWithBindData1(1);
    final TestBinderWithBindData2 attachBinder2 = new TestBinderWithBindData2(2);

    final TestRenderUnit renderUnit = new TestRenderUnit();
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2));

    final BindData bindData = createBindData(null, null, null);

    renderUnit.attachBinders(mContext, mContent, null, bindData, mTracer);

    // assert no fixed binders bind data is present when calling attachBinders
    assertThat(bindData.getFixedBindersBindData()).isNull();

    // assert no optional mount binders bind data is present when calling attachBinders
    assertThat(bindData.getOptionalMountBindersBindData()).isNull();

    // assert attach binders bind data is correct
    assertThat(bindData.getAttachBindersBindData()).hasSize(2);

    assertThat(bindData.getAttachBindersBindData().get(attachBinder1.getClass())).isEqualTo(1);
    assertThat(bindData.getAttachBindersBindData().get(attachBinder2.getClass())).isEqualTo(2);
  }

  @Test
  public void unmountBinders_withBindData_passesBindDataToUnbind() {
    List<Pair<Object, Object>> unbindOrder = new ArrayList();
    final TestBinderWithBindData1 fixedBinder1 =
        new TestBinderWithBindData1(mBindOrder, unbindOrder);
    final TestBinderWithBindData2 fixedBinder2 =
        new TestBinderWithBindData2(mBindOrder, unbindOrder);
    final TestBinderWithBindData1 mountBinder1 =
        new TestBinderWithBindData1(mBindOrder, unbindOrder);
    final TestBinderWithBindData2 mountBinder2 =
        new TestBinderWithBindData2(mBindOrder, unbindOrder);

    final TestRenderUnit renderUnit =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), fixedBinder1),
                createDelegateBinder(new TestRenderUnit(), fixedBinder2)));
    renderUnit.addOptionalMountBinders(
        createDelegateBinder(renderUnit, mountBinder1),
        createDelegateBinder(renderUnit, mountBinder2));

    // Create BindData that will be passed to updateBinders
    final BindData bindData =
        createBindData(
            Arrays.asList(1, 2),
            Arrays.asList(
                new Pair<>(mountBinder1.getClass(), 3), new Pair<>(mountBinder2.getClass(), 4)),
            null);

    renderUnit.unmountBinders(mContext, mContent, null, bindData, mTracer);

    // assert that unbind was called in correct order and correct bind data was passed
    assertThat(unbindOrder).hasSize(4);
    assertThat(unbindOrder.get(0).second).isEqualTo(4);
    assertThat(unbindOrder.get(1).second).isEqualTo(3);
    assertThat(unbindOrder.get(2).second).isEqualTo(2);
    assertThat(unbindOrder.get(3).second).isEqualTo(1);
  }

  @Test
  public void detachBinders_withBindData_passesBindDataToUnbind() {
    List<Pair<Object, Object>> unbindOrder = new ArrayList();

    final TestBinderWithBindData1 attachBinder1 =
        new TestBinderWithBindData1(mBindOrder, unbindOrder);
    final TestBinderWithBindData2 attachBinder2 =
        new TestBinderWithBindData2(mBindOrder, unbindOrder);

    final TestRenderUnit renderUnit = new TestRenderUnit();
    renderUnit.addAttachBinders(
        createDelegateBinder(renderUnit, attachBinder1),
        createDelegateBinder(renderUnit, attachBinder2));

    // Create BindData that will be passed to updateBinders
    final BindData bindData =
        createBindData(
            null,
            null,
            Arrays.asList(
                new Pair<>(attachBinder1.getClass(), 1), new Pair<>(attachBinder2.getClass(), 2)));

    renderUnit.detachBinders(mContext, mContent, null, bindData, mTracer);

    // assert that unbind was called in correct order and correct bind data was passed
    assertThat(unbindOrder).hasSize(2);
    assertThat(unbindOrder.get(0).second).isEqualTo(2);
    assertThat(unbindOrder.get(1).second).isEqualTo(1);
  }

  @Test
  public void updateBinders_withBindData_passesBindDataToUnbindAndUpdatesIt() {
    final List<Pair<Object, Object>> unbindOrder = new ArrayList<>();
    final TestBinderWithBindData1 attachBinder1 =
        new TestBinderWithBindData1(mBindOrder, unbindOrder, 100);
    final TestBinderWithBindData2 attachBinder2 =
        new TestBinderWithBindData2(mBindOrder, unbindOrder, 200);
    final TestBinderWithBindData2 mountBinder1 =
        new TestBinderWithBindData2(mBindOrder, unbindOrder, 300);
    final TestBinderWithBindData3 mountBinder2 =
        new TestBinderWithBindData3(mBindOrder, unbindOrder, 400);
    final TestBinderWithBindData3 fixedMountBinder1 =
        new TestBinderWithBindData3(mBindOrder, unbindOrder, 500);
    final TestBinderWithBindData3 fixedMountBinder2 =
        new TestBinderWithBindData3(mBindOrder, unbindOrder, 600);

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

    // Create BindData that will be passed to updateBinders
    final BindData bindData =
        createBindData(
            Arrays.asList(1, 2),
            Arrays.asList(
                new Pair<>(mountBinder1.getClass(), 3), new Pair<>(mountBinder2.getClass(), 4)),
            Arrays.asList(
                new Pair<>(attachBinder1.getClass(), 5), new Pair<>(attachBinder2.getClass(), 6)));

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, bindData, true, mTracer);

    // assert that unbind was called in correct order and correct bind data was passed
    assertThat(unbindOrder).hasSize(6);
    assertThat(unbindOrder.get(0).second).isEqualTo(6);
    assertThat(unbindOrder.get(1).second).isEqualTo(5);
    assertThat(unbindOrder.get(2).second).isEqualTo(4);
    assertThat(unbindOrder.get(3).second).isEqualTo(3);
    assertThat(unbindOrder.get(4).second).isEqualTo(2);
    assertThat(unbindOrder.get(5).second).isEqualTo(1);

    // assert fixed binders bind data is correct
    assertThat(bindData.getFixedBindersBindData()).containsExactly(500, 600);

    // assert optional mount binders bind data is correct
    assertThat(bindData.getOptionalMountBindersBindData()).hasSize(2);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder1.getClass()))
        .isEqualTo(300);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder2.getClass()))
        .isEqualTo(400);

    // assert attach binders bind data is correct
    assertThat(bindData.getAttachBindersBindData()).hasSize(2);
    assertThat(bindData.getAttachBindersBindData().get(attachBinder1.getClass())).isEqualTo(100);
    assertThat(bindData.getAttachBindersBindData().get(attachBinder2.getClass())).isEqualTo(200);
  }

  @Test
  public void
      updateBinders_withBindDataAndDifferentNumberOfBinders_passesBindDataToUnbindAndUpdatedsIt() {
    final List<Pair<Object, Object>> unbindOrder = new ArrayList<>();
    final TestBinderWithBindData1 attachBinder1 =
        new TestBinderWithBindData1(mBindOrder, unbindOrder, 100);
    final TestBinderWithBindData2 attachBinder2 =
        new TestBinderWithBindData2(mBindOrder, unbindOrder, 200);
    final TestBinderWithBindData2 mountBinder1 =
        new TestBinderWithBindData2(mBindOrder, unbindOrder, 300);
    final TestBinderWithBindData3 mountBinder2 =
        new TestBinderWithBindData3(mBindOrder, unbindOrder, 400);
    final TestBinderWithBindData3 fixedMountBinder1 =
        new TestBinderWithBindData3(mBindOrder, unbindOrder, 500);
    final TestBinderWithBindData3 fixedMountBinder2 =
        new TestBinderWithBindData3(mBindOrder, unbindOrder, 600);

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
    nextRU.addAttachBinder(createDelegateBinder(nextRU, attachBinder1));

    // Create BindData that will be passed to updateBinders
    final BindData bindData =
        createBindData(
            Arrays.asList(1, 2),
            Arrays.asList(
                new Pair<>(mountBinder1.getClass(), 3), new Pair<>(mountBinder2.getClass(), 4)),
            Arrays.asList(
                new Pair<>(attachBinder1.getClass(), 5), new Pair<>(attachBinder2.getClass(), 6)));

    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, bindData, true, mTracer);

    // assert that unbind was called in correct order and correct bind data was passed
    assertThat(unbindOrder).hasSize(6);
    assertThat(unbindOrder.get(0).second).isEqualTo(6);
    assertThat(unbindOrder.get(1).second).isEqualTo(5);
    assertThat(unbindOrder.get(2).second).isEqualTo(4);
    assertThat(unbindOrder.get(3).second).isEqualTo(3);
    assertThat(unbindOrder.get(4).second).isEqualTo(2);
    assertThat(unbindOrder.get(5).second).isEqualTo(1);

    // assert fixed binders bind data is correct
    assertThat(bindData.getFixedBindersBindData()).containsExactly(500, 600);

    // assert optional mount binders bind data is correct
    assertThat(bindData.getOptionalMountBindersBindData()).hasSize(1);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder1.getClass()))
        .isEqualTo(300);

    // assert attach binders bind data is correct
    assertThat(bindData.getAttachBindersBindData()).hasSize(1);
    assertThat(bindData.getAttachBindersBindData().get(attachBinder1.getClass())).isEqualTo(100);
  }

  @Test
  public void updateBindersMultipleTimes_withBindData_passesBindDataToUnbind() {
    final boolean[] binderShouldUpdate = new boolean[] {true, false};

    final List<Pair<Object, Object>> unbindOrder = new ArrayList<>();
    final Binder<TestRenderUnit, View, Object> mountBinder1 =
        new Binder<TestRenderUnit, View, Object>() {

          @Override
          public boolean shouldUpdate(
              TestRenderUnit currentModel,
              TestRenderUnit newModel,
              @Nullable Object currentLayoutData,
              @Nullable Object nextLayoutData) {
            return binderShouldUpdate[0];
          }

          @Override
          public Object bind(
              Context context,
              View view,
              TestRenderUnit testRenderUnit,
              @Nullable Object layoutData) {
            return 100;
          }

          @Override
          public void unbind(
              Context context,
              View view,
              TestRenderUnit testRenderUnit,
              @Nullable Object layoutData,
              Object bindData) {
            unbindOrder.add(new Pair(this, bindData));
          }
        };
    final Binder<TestRenderUnit, View, Object> mountBinder2 =
        new Binder<TestRenderUnit, View, Object>() {

          @Override
          public boolean shouldUpdate(
              TestRenderUnit currentModel,
              TestRenderUnit newModel,
              @Nullable Object currentLayoutData,
              @Nullable Object nextLayoutData) {
            return binderShouldUpdate[1];
          }

          @Override
          public Object bind(
              Context context,
              View view,
              TestRenderUnit testRenderUnit,
              @Nullable Object layoutData) {
            return 200;
          }

          @Override
          public void unbind(
              Context context,
              View view,
              TestRenderUnit testRenderUnit,
              @Nullable Object layoutData,
              Object bindData) {
            unbindOrder.add(new Pair(this, bindData));
          }
        };

    final TestRenderUnit currentRU = new TestRenderUnit();
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder1));
    currentRU.addOptionalMountBinder(createDelegateBinder(currentRU, mountBinder2));

    final TestRenderUnit nextRU = new TestRenderUnit();
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder1));
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, mountBinder2));

    // Create BindData that will be passed to updateBinders
    final BindData bindData =
        createBindData(
            null,
            Arrays.asList(
                new Pair<>(mountBinder1.getClass(), 1), new Pair<>(mountBinder2.getClass(), 2)),
            null);

    // Call update -  only first binder should update
    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, bindData, true, mTracer);

    // assert that unbind was called in correct order and correct bind data was passed
    assertThat(unbindOrder).hasSize(1);
    assertThat(unbindOrder.get(0).second).isEqualTo(1);

    // assert no fixed binders bind data is present
    assertThat(bindData.getFixedBindersBindData()).isNull();

    // assert optional mount binders bind data is correct
    assertThat(bindData.getOptionalMountBindersBindData()).hasSize(2);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder1.getClass()))
        .isEqualTo(100);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder2.getClass()))
        .isEqualTo(2);

    // assert no attach binders bind data is present
    assertThat(bindData.getAttachBindersBindData()).isNull();

    // Call update -  only second binder should update
    unbindOrder.clear();
    binderShouldUpdate[0] = false;
    binderShouldUpdate[1] = true;
    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, bindData, true, mTracer);

    // assert that unbind was called in correct order and correct bind data was passed
    assertThat(unbindOrder).hasSize(1);
    assertThat(unbindOrder.get(0).second).isEqualTo(2);

    // assert no fixed binders bind data is present
    assertThat(bindData.getFixedBindersBindData()).isNull();

    // assert optional mount binders bind data is correct
    assertThat(bindData.getOptionalMountBindersBindData()).hasSize(2);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder1.getClass()))
        .isEqualTo(100);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder2.getClass()))
        .isEqualTo(200);

    // assert no attach binders bind data is present
    assertThat(bindData.getAttachBindersBindData()).isNull();

    // Call update -  both binders should update
    unbindOrder.clear();
    binderShouldUpdate[0] = true;
    binderShouldUpdate[1] = true;
    nextRU.updateBinders(
        mContext, mContent, currentRU, null, new Object(), null, bindData, true, mTracer);

    // assert that unbind was called in correct order and correct bind data was passed
    assertThat(unbindOrder).hasSize(2);
    assertThat(unbindOrder.get(0).second).isEqualTo(200);
    assertThat(unbindOrder.get(1).second).isEqualTo(100);

    // assert no fixed binders bind data is present
    assertThat(bindData.getFixedBindersBindData()).isNull();

    // assert optional mount binders bind data is correct
    assertThat(bindData.getOptionalMountBindersBindData()).hasSize(2);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder1.getClass()))
        .isEqualTo(100);
    assertThat(bindData.getOptionalMountBindersBindData().get(mountBinder2.getClass()))
        .isEqualTo(200);

    // assert no attach binders bind data is present
    assertThat(bindData.getAttachBindersBindData()).isNull();
  }

  @Test
  public void createWithExtra_getExtra_returnsCorrectValues() {
    final SparseArray<Object> extras = new SparseArray<>();
    extras.put(1, "test");
    extras.put(999, 42);
    extras.put(-50000, true);

    final TestRenderUnit renderUnit = new TestRenderUnit(extras);

    assertThat(renderUnit.<Object>getExtra(0)).isNull();
    assertThat(renderUnit.<String>getExtra(1)).isEqualTo("test");
    assertThat(renderUnit.<Integer>getExtra(999)).isEqualTo(42);
    assertThat(renderUnit.<Boolean>getExtra(-50000)).isEqualTo(true);
  }

  static BindData createBindData(
      @Nullable final List<Object> fixedBindersBindData,
      @Nullable final List<Pair<Class<?>, Object>> optionalMountBindersBindData,
      @Nullable final List<Pair<Class<?>, Object>> attachBindersBindData) {
    final BindData bindData = new BindData();
    if (fixedBindersBindData != null) {
      for (int i = 0; i < fixedBindersBindData.size(); i++) {
        bindData.setFixedBindersBindData(
            fixedBindersBindData.get(i), i, fixedBindersBindData.size());
      }
    }
    if (optionalMountBindersBindData != null) {
      for (int i = 0; i < optionalMountBindersBindData.size(); i++) {
        bindData.setOptionalMountBindersBindData(
            optionalMountBindersBindData.get(i).second,
            optionalMountBindersBindData.get(i).first,
            optionalMountBindersBindData.size());
      }
    }
    if (attachBindersBindData != null) {
      for (int i = 0; i < attachBindersBindData.size(); i++) {
        bindData.setAttachBindersBindData(
            attachBindersBindData.get(i).second,
            attachBindersBindData.get(i).first,
            attachBindersBindData.size());
      }
    }
    return bindData;
  }
}
