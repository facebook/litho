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
import static com.facebook.rendercore.RenderUnitTest.createBindData;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import android.content.Context;
import android.view.View;
import androidx.core.util.Pair;
import com.facebook.rendercore.TestBinder.TestBinder1;
import com.facebook.rendercore.TestBinder.TestBinder2;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData1;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData2;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData3;
import com.facebook.rendercore.TestBinderWithBindData.TestBinderWithBindData4;
import java.util.ArrayList;
import java.util.Arrays;
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
  private final BindData bindData = new BindData();

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

    final TestBinder2 wrapperMountBinder = new TestBinder2(bindOrder, unbindOrder);
    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);
    wrapperRenderUnit.addOptionalMountBinder(
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder));

    wrapperRenderUnit.mountBinders(context, content, null, bindData, tracer);
    assertThat(bindOrder).containsExactly(originalMountBinder, wrapperMountBinder);

    wrapperRenderUnit.unmountBinders(context, content, null, bindData, tracer);
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

    final TestBinder2 wrapperAttachBinder = new TestBinder2(bindOrder, unbindOrder);
    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);
    wrapperRenderUnit.addAttachBinder(
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder));

    wrapperRenderUnit.attachBinders(context, content, null, bindData, tracer);
    assertThat(bindOrder).containsExactly(originalAttachBinder, wrapperAttachBinder);

    wrapperRenderUnit.detachBinders(context, content, null, bindData, tracer);
    assertThat(unbindOrder).containsExactly(wrapperAttachBinder, originalAttachBinder);
  }

  @Test
  public void addSameTypeOptionalMountBinderToOriginalAndWrapperRenderUnit_throwsAnException() {
    TestBinder1 mountBinder = new TestBinder1(bindOrder, unbindOrder);
    final TestRenderUnit originalRenderUnit = new TestRenderUnit();
    originalRenderUnit.addOptionalMountBinder(
        createDelegateBinder(new TestRenderUnit(), mountBinder));

    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);

    assertThatThrownBy(
            () ->
                wrapperRenderUnit.addOptionalMountBinder(
                    createDelegateBinder(new TestRenderUnit(), mountBinder)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Binder <cls>com.facebook.rendercore.TestBinder$TestBinder1</cls> already exists in the wrapped <cls>com.facebook.rendercore.TestRenderUnit</cls>");
  }

  @Test
  public void addSameTypeAttachBinderToOriginalAndWrapperRenderUnit_throwsAnException() {
    TestBinder1 attachBinder = new TestBinder1(bindOrder, unbindOrder);
    final TestRenderUnit originalRenderUnit = new TestRenderUnit();
    originalRenderUnit.addAttachBinder(createDelegateBinder(new TestRenderUnit(), attachBinder));

    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);

    assertThatThrownBy(
            () ->
                wrapperRenderUnit.addAttachBinder(
                    createDelegateBinder(new TestRenderUnit(), attachBinder)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Binder <cls>com.facebook.rendercore.TestBinder$TestBinder1</cls> already exists in the wrapped <cls>com.facebook.rendercore.TestRenderUnit</cls>");
  }

  @Test
  public void mountBinders_withBindData_createsBindDataContainingOriginalAndWrapperBinders() {
    final TestBinderWithBindData1 originalFixedBinder1 = new TestBinderWithBindData1(1);
    final TestBinderWithBindData2 originalFixedBinder2 = new TestBinderWithBindData2(2);
    final TestBinderWithBindData1 originalMountBinder1 = new TestBinderWithBindData1(3);
    final TestBinderWithBindData2 originalMountBinder2 = new TestBinderWithBindData2(4);

    final TestRenderUnit originalRenderUnit =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), originalFixedBinder1),
                createDelegateBinder(new TestRenderUnit(), originalFixedBinder2)));
    originalRenderUnit.addOptionalMountBinders(
        createDelegateBinder(new TestRenderUnit(), originalMountBinder1),
        createDelegateBinder(new TestRenderUnit(), originalMountBinder2));

    final TestBinderWithBindData3 wrapperMountBinder1 = new TestBinderWithBindData3(5);
    final TestBinderWithBindData4 wrapperMountBinder2 = new TestBinderWithBindData4(6);

    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);
    wrapperRenderUnit.addOptionalMountBinders(
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder1),
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder2));

    final BindData wrapperBindData = new BindData();

    wrapperRenderUnit.mountBinders(context, content, null, wrapperBindData, tracer);

    // assert fixed binders bind data is correct
    assertThat(wrapperBindData.getFixedBindersBindData()).hasSize(2);
    assertThat(wrapperBindData.getFixedBindersBindData()).containsExactly(1, 2);

    // assert optional mount binders bind data is correct
    assertThat(wrapperBindData.getOptionalMountBindersBindData()).hasSize(4);
    assertThat(
            wrapperBindData.getOptionalMountBindersBindData().get(originalMountBinder1.getClass()))
        .isEqualTo(3);
    assertThat(
            wrapperBindData.getOptionalMountBindersBindData().get(originalMountBinder2.getClass()))
        .isEqualTo(4);
    assertThat(
            wrapperBindData.getOptionalMountBindersBindData().get(wrapperMountBinder1.getClass()))
        .isEqualTo(5);
    assertThat(
            wrapperBindData.getOptionalMountBindersBindData().get(wrapperMountBinder2.getClass()))
        .isEqualTo(6);
  }

  @Test
  public void attachBinders_withBindData_createsBindDataContainingOriginalAndWrapperBinders() {
    final TestBinderWithBindData1 originalAttachBinder1 = new TestBinderWithBindData1(1);
    final TestBinderWithBindData2 originalAttachBinder2 = new TestBinderWithBindData2(2);

    final TestRenderUnit originalRenderUnit = new TestRenderUnit();
    originalRenderUnit.addAttachBinders(
        createDelegateBinder(new TestRenderUnit(), originalAttachBinder1),
        createDelegateBinder(new TestRenderUnit(), originalAttachBinder2));

    final TestBinderWithBindData3 wrapperAttachBinder1 = new TestBinderWithBindData3(3);
    final TestBinderWithBindData4 wrapperAttachBinder2 = new TestBinderWithBindData4(4);

    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);
    wrapperRenderUnit.addAttachBinders(
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder1),
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder2));

    final BindData wrapperBindData = new BindData();
    wrapperRenderUnit.attachBinders(context, content, null, wrapperBindData, tracer);

    // assert no fixed mount binders bind data is present when calling attachBinders
    assertThat(wrapperBindData.getFixedBindersBindData()).isNull();

    // assert no optional mount binders bind data is present when calling attachBinders
    assertThat(wrapperBindData.getOptionalMountBindersBindData()).isNull();

    // assert attach binders bind data is correct
    assertThat(wrapperBindData.getAttachBindersBindData()).hasSize(4);
    assertThat(wrapperBindData.getAttachBindersBindData().get(originalAttachBinder1.getClass()))
        .isEqualTo(1);
    assertThat(wrapperBindData.getAttachBindersBindData().get(originalAttachBinder2.getClass()))
        .isEqualTo(2);
    assertThat(wrapperBindData.getAttachBindersBindData().get(wrapperAttachBinder1.getClass()))
        .isEqualTo(3);
    assertThat(wrapperBindData.getAttachBindersBindData().get(wrapperAttachBinder2.getClass()))
        .isEqualTo(4);
  }

  @Test
  public void unmountBinders_withBindData_passesBindDataToOriginalAndWrapperBinders() {
    List<Pair<Object, Object>> originalUnbindOrder = new ArrayList();
    final TestBinderWithBindData1 originalFixedBinder1 =
        new TestBinderWithBindData1(bindOrder, originalUnbindOrder);
    final TestBinderWithBindData2 originalFixedBinder2 =
        new TestBinderWithBindData2(bindOrder, originalUnbindOrder);
    final TestBinderWithBindData1 originalMountBinder1 =
        new TestBinderWithBindData1(bindOrder, originalUnbindOrder);
    final TestBinderWithBindData2 originalMountBinder2 =
        new TestBinderWithBindData2(bindOrder, originalUnbindOrder);

    final TestRenderUnit originalRenderUnit =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), originalFixedBinder1),
                createDelegateBinder(new TestRenderUnit(), originalFixedBinder2)));
    originalRenderUnit.addOptionalMountBinders(
        createDelegateBinder(new TestRenderUnit(), originalMountBinder1),
        createDelegateBinder(new TestRenderUnit(), originalMountBinder2));

    List<Pair<Object, Object>> wrapperUnbindOrder = new ArrayList();
    final TestBinderWithBindData3 wrapperMountBinder1 =
        new TestBinderWithBindData3(bindOrder, wrapperUnbindOrder);
    final TestBinderWithBindData4 wrapperMountBinder2 =
        new TestBinderWithBindData4(bindOrder, wrapperUnbindOrder);

    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);
    wrapperRenderUnit.addOptionalMountBinders(
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder1),
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder2));

    // Create BindData that will be passed to updateBinders
    final BindData bindData =
        createBindData(
            Arrays.asList(1, 2),
            Arrays.asList(
                new Pair<>(originalMountBinder1.getClass(), 3),
                new Pair<>(originalMountBinder2.getClass(), 4),
                new Pair<>(wrapperMountBinder1.getClass(), 7),
                new Pair<>(wrapperMountBinder2.getClass(), 8)),
            null);

    wrapperRenderUnit.unmountBinders(context, content, null, bindData, tracer);

    // assert that unbind was called on original RU in correct order and with correct bind data
    assertThat(originalUnbindOrder).hasSize(4);
    assertThat(originalUnbindOrder.get(0).second).isEqualTo(4);
    assertThat(originalUnbindOrder.get(1).second).isEqualTo(3);
    assertThat(originalUnbindOrder.get(2).second).isEqualTo(2);
    assertThat(originalUnbindOrder.get(3).second).isEqualTo(1);

    // assert that unbind was called on wrapper RU in correct order and with correct bind data
    assertThat(wrapperUnbindOrder).hasSize(2);
    assertThat(wrapperUnbindOrder.get(0).second).isEqualTo(8);
    assertThat(wrapperUnbindOrder.get(1).second).isEqualTo(7);
  }

  @Test
  public void detachBinders_withBindData_passesBindDataToOriginalAndWrapperBinders() {
    List<Pair<Object, Object>> originalUnbindOrder = new ArrayList();
    new TestBinderWithBindData2(bindOrder, originalUnbindOrder);
    final TestBinderWithBindData1 originalAttachBinder1 =
        new TestBinderWithBindData1(bindOrder, originalUnbindOrder);
    final TestBinderWithBindData2 originalAttachBinder2 =
        new TestBinderWithBindData2(bindOrder, originalUnbindOrder);

    final TestRenderUnit originalRenderUnit = new TestRenderUnit();
    originalRenderUnit.addAttachBinders(
        createDelegateBinder(new TestRenderUnit(), originalAttachBinder1),
        createDelegateBinder(new TestRenderUnit(), originalAttachBinder2));

    List<Pair<Object, Object>> wrapperUnbindOrder = new ArrayList();
    final TestBinderWithBindData3 wrapperAttachBinder1 =
        new TestBinderWithBindData3(bindOrder, wrapperUnbindOrder);
    final TestBinderWithBindData4 wrapperAttachBinder2 =
        new TestBinderWithBindData4(bindOrder, wrapperUnbindOrder);

    final WrapperRenderUnit<View> wrapperRenderUnit = new WrapperRenderUnit<>(originalRenderUnit);
    wrapperRenderUnit.addAttachBinders(
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder1),
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder2));

    // Create BindData that will be passed to updateBinders
    final BindData bindData =
        createBindData(
            null,
            null,
            Arrays.asList(
                new Pair<>(originalAttachBinder1.getClass(), 1),
                new Pair<>(originalAttachBinder2.getClass(), 2),
                new Pair<>(wrapperAttachBinder1.getClass(), 3),
                new Pair<>(wrapperAttachBinder2.getClass(), 4)));

    wrapperRenderUnit.detachBinders(context, content, null, bindData, tracer);

    // assert that unbind was called on original RU in correct order and with correct bind data
    assertThat(originalUnbindOrder).hasSize(2);
    assertThat(originalUnbindOrder.get(0).second).isEqualTo(2);
    assertThat(originalUnbindOrder.get(1).second).isEqualTo(1);

    // assert that unbind was called on wrapper RU in correct order and with correct bind data
    assertThat(wrapperUnbindOrder).hasSize(2);
    assertThat(wrapperUnbindOrder.get(0).second).isEqualTo(4);
    assertThat(wrapperUnbindOrder.get(1).second).isEqualTo(3);
  }

  @Test
  public void updateBinders_withBindData_passesBindDataToOriginalAndWrapperBindersAndUpdatesIt() {
    final List<Pair<Object, Object>> originalUnbindOrder = new ArrayList<>();
    final TestBinderWithBindData1 originalAttachBinder1 =
        new TestBinderWithBindData1(bindOrder, originalUnbindOrder, 100);
    final TestBinderWithBindData2 originalAttachBinder2 =
        new TestBinderWithBindData2(bindOrder, originalUnbindOrder, 200);
    final TestBinderWithBindData2 originalMountBinder1 =
        new TestBinderWithBindData2(bindOrder, originalUnbindOrder, 300);
    final TestBinderWithBindData3 originalMountBinder2 =
        new TestBinderWithBindData3(bindOrder, originalUnbindOrder, 400);
    final TestBinderWithBindData3 originalFixedMountBinder1 =
        new TestBinderWithBindData3(bindOrder, originalUnbindOrder, 500);
    final TestBinderWithBindData3 originalFixedMountBinder2 =
        new TestBinderWithBindData3(bindOrder, originalUnbindOrder, 600);

    final TestRenderUnit currentOriginalRU =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), originalFixedMountBinder1),
                createDelegateBinder(new TestRenderUnit(), originalFixedMountBinder2)));
    currentOriginalRU.addOptionalMountBinder(
        createDelegateBinder(currentOriginalRU, originalMountBinder1));
    currentOriginalRU.addOptionalMountBinder(
        createDelegateBinder(currentOriginalRU, originalMountBinder2));
    currentOriginalRU.addAttachBinder(
        createDelegateBinder(currentOriginalRU, originalAttachBinder1));
    currentOriginalRU.addAttachBinder(
        createDelegateBinder(currentOriginalRU, originalAttachBinder2));

    final List<Pair<Object, Object>> wrapperUnbindOrder = new ArrayList<>();
    final TestBinderWithBindData3 wrapperAttachBinder1 =
        new TestBinderWithBindData3(bindOrder, wrapperUnbindOrder, 700);
    final TestBinderWithBindData4 wrapperAttachBinder2 =
        new TestBinderWithBindData4(bindOrder, wrapperUnbindOrder, 800);
    final TestBinderWithBindData1 wrapperMountBinder1 =
        new TestBinderWithBindData1(bindOrder, wrapperUnbindOrder, 900);
    final TestBinderWithBindData4 wrapperMountBinder2 =
        new TestBinderWithBindData4(bindOrder, wrapperUnbindOrder, 1000);

    final WrapperRenderUnit<View> currentWrapperRU = new WrapperRenderUnit<>(currentOriginalRU);
    currentWrapperRU.addOptionalMountBinders(
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder1),
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder2));
    currentWrapperRU.addAttachBinders(
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder1),
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder2));

    final TestRenderUnit nextRU =
        new TestRenderUnit(
            Arrays.asList(
                createDelegateBinder(new TestRenderUnit(), originalFixedMountBinder1),
                createDelegateBinder(new TestRenderUnit(), originalFixedMountBinder2)));
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, originalMountBinder1));
    nextRU.addOptionalMountBinders(createDelegateBinder(nextRU, originalMountBinder2));
    nextRU.addAttachBinder(createDelegateBinder(nextRU, originalAttachBinder1));
    nextRU.addAttachBinder(createDelegateBinder(nextRU, originalAttachBinder2));

    final WrapperRenderUnit<View> nextWrapperRU = new WrapperRenderUnit<>(nextRU);
    nextWrapperRU.addOptionalMountBinders(
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder1),
        createDelegateBinder(new TestRenderUnit(), wrapperMountBinder2));
    nextWrapperRU.addAttachBinders(
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder1),
        createDelegateBinder(new TestRenderUnit(), wrapperAttachBinder2));

    // Create BindData that will be passed to updateBinders
    final BindData bindData =
        createBindData(
            Arrays.asList(1, 2),
            Arrays.asList(
                new Pair<>(originalMountBinder1.getClass(), 5),
                new Pair<>(originalMountBinder2.getClass(), 6),
                new Pair<>(wrapperMountBinder1.getClass(), 7),
                new Pair<>(wrapperMountBinder2.getClass(), 8)),
            Arrays.asList(
                new Pair<>(originalAttachBinder1.getClass(), 9),
                new Pair<>(originalAttachBinder2.getClass(), 10),
                new Pair<>(wrapperAttachBinder1.getClass(), 11),
                new Pair<>(wrapperAttachBinder2.getClass(), 12)));

    nextWrapperRU.updateBinders(
        context, content, currentWrapperRU, null, new Object(), null, bindData, true, tracer);

    // assert that unbind was called on original RU in correct order and with correct bind data
    assertThat(originalUnbindOrder).hasSize(6);
    assertThat(originalUnbindOrder.get(0).second).isEqualTo(10);
    assertThat(originalUnbindOrder.get(1).second).isEqualTo(9);
    assertThat(originalUnbindOrder.get(2).second).isEqualTo(6);
    assertThat(originalUnbindOrder.get(3).second).isEqualTo(5);
    assertThat(originalUnbindOrder.get(4).second).isEqualTo(2);
    assertThat(originalUnbindOrder.get(5).second).isEqualTo(1);

    // assert that unbind was called on wrapper RU in correct order and with correct bind data
    assertThat(wrapperUnbindOrder).hasSize(4);
    assertThat(wrapperUnbindOrder.get(0).second).isEqualTo(12);
    assertThat(wrapperUnbindOrder.get(1).second).isEqualTo(11);
    assertThat(wrapperUnbindOrder.get(2).second).isEqualTo(8);
    assertThat(wrapperUnbindOrder.get(3).second).isEqualTo(7);

    // assert fixed binders bind data is correct
    assertThat(bindData.getFixedBindersBindData()).containsExactly(500, 600);

    // assert optional mount binders bind data is correct
    assertThat(bindData.getOptionalMountBindersBindData()).hasSize(4);
    assertThat(bindData.getOptionalMountBindersBindData().get(originalMountBinder1.getClass()))
        .isEqualTo(300);
    assertThat(bindData.getOptionalMountBindersBindData().get(originalMountBinder2.getClass()))
        .isEqualTo(400);
    assertThat(bindData.getOptionalMountBindersBindData().get(wrapperMountBinder1.getClass()))
        .isEqualTo(900);
    assertThat(bindData.getOptionalMountBindersBindData().get(wrapperMountBinder2.getClass()))
        .isEqualTo(1000);

    // assert attach binders bind data is correct
    assertThat(bindData.getAttachBindersBindData()).hasSize(4);
    assertThat(bindData.getAttachBindersBindData().get(originalAttachBinder1.getClass()))
        .isEqualTo(100);
    assertThat(bindData.getAttachBindersBindData().get(originalAttachBinder2.getClass()))
        .isEqualTo(200);
    assertThat(bindData.getAttachBindersBindData().get(wrapperAttachBinder1.getClass()))
        .isEqualTo(700);
    assertThat(bindData.getAttachBindersBindData().get(wrapperAttachBinder2.getClass()))
        .isEqualTo(800);
  }
}
