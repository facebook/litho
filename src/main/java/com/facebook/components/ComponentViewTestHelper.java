// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.util.Deque;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.components.config.ComponentsConfiguration;
import com.facebook.proguard.annotations.DoNotStrip;

/**
 * Helper class to access metadata from {@link ComponentView} that is relevant during end to end
 * tests. In order for the data to be collected, {@link
 * ComponentsConfiguration#isEndToEndTestRun} must be enabled.
 */
@DoNotStrip
public class ComponentViewTestHelper {

  /**
   * @see #findTestItems(ComponentView, String)
   *
   * <strong>Note:</strong> If there is more than one element mounted under the given key,
   * the last one to render will be returned.
   *
   * @param componentView The component view the component is mounted to.
   * @param testKey The unique identifier the component was constructed with.
   * @return Test item if found, null otherwise.
   * @throws UnsupportedOperationException If the e2e flag is not enabled in the configuration.
   */
  @DoNotStrip
  @Nullable
  public static TestItem findTestItem(ComponentView componentView, String testKey) {
    final Deque<TestItem> items = componentView.findTestItems(testKey);

    return items.isEmpty() ? null : items.getLast();
  }

  /**
   * Finds a {@link TestItem} given a {@link ComponentView} based on the test key it was
   * assigned during construction.
   *
   * <strong>Example use:</strong>
   * <pre>{@code
   *  final ComponentView componentView = ComponentTestHelper.mountComponent(
   *      mContext,
   *      new InlineLayoutSpec() {
   *        @Override
   *        protected ComponentLayout onCreateLayout(ComponentContext c) {
   *          return Container.create(c)
   *              .child(
   *                  Container.create(c)
   *                      .child(TestDrawableComponent.create(c))
   *                      .child(TestDrawableComponent.create(c))
   *                      .testKey("mytestkey"))
   *              .build();
   *        }
   *      });
   *  final TestItem testItem = ComponentViewTestHelper.findTestItem(componentView, "mytestkey");
   *  }
   * </pre>
   *
   * @param componentView The component view the component is mounted to.
   * @param testKey The unique identifier the component was constructed with.
   * @return Queue of mounted items in order by mount time.
   * @throws UnsupportedOperationException If the e2e flag is not enabled in the configuration.
   */
  @DoNotStrip
  @NonNull
  public static Deque<TestItem> findTestItems(ComponentView componentView, String testKey) {
    return componentView.findTestItems(testKey);
  }
}
