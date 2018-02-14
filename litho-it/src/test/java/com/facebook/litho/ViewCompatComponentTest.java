/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.ViewCompatComponent.get;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.litho.testing.helper.ComponentTestHelper.unbindComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TextView;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests {@link ViewCompatComponent}
 */
@RunWith(ComponentsTestRunner.class)
public class ViewCompatComponentTest {

  private static final ViewCreator<TextView> TEXT_VIEW_CREATOR =
      new ViewCreator<TextView>() {
        @Override
        public TextView createView(Context c, ViewGroup parent) {
          return new TextView(c);
        }
      };

  private static final ViewCreator<TextView> TEXT_VIEW_CREATOR_2 =
      new ViewCreator<TextView>() {
        @Override
        public TextView createView(Context c, ViewGroup parent) {
          return new TextView(c);
        }
      };

  private static final ViewBinder<TextView> NO_OP_VIEW_BINDER =
      new ViewBinder<TextView>() {
        @Override
        public void prepare() {}

        @Override
        public void bind(TextView view) {}

        @Override
        public void unbind(TextView view) {}
      };

  private ComponentContext mContext;

  @Before
  public void setUp() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testSimpleRendering() throws Exception {
    ViewBinder<TextView> binder =
        new ViewBinder<TextView>() {
          @Override
          public void prepare() {

          }

          @Override
          public void bind(TextView view) {
            view.setText("Hello World!");
          }

          @Override
          public void unbind(TextView view) {

          }
        };

    LithoView lithoView = mountComponent(
        get(TEXT_VIEW_CREATOR, "TextView")
            .create(mContext)
            .viewBinder(binder));

    assertThat(lithoView.getMountItemCount()).isEqualTo(1);
    TextView view = (TextView) lithoView.getMountItemAt(0).getContent();
    assertThat(view.getText()).isEqualTo("Hello World!");
  }

  @Test
  public void testPrepare() throws Exception {
    ViewBinder<TextView> binder =
        new ViewBinder<TextView>() {
          private String mState;

          @Override
          public void prepare() {
            mState = "Hello World!";
          }

          @Override
          public void bind(TextView view) {
            view.setText(mState);
          }

          @Override
          public void unbind(TextView view) {

          }
        };

    LithoView lithoView = mountComponent(
        get(TEXT_VIEW_CREATOR, "TextView")
            .create(mContext)
            .viewBinder(binder));

    assertThat(lithoView.getMountItemCount()).isEqualTo(1);
    TextView view = (TextView) lithoView.getMountItemAt(0).getContent();
    assertThat(view.getText()).isEqualTo("Hello World!");
  }

  @Test
  public void testUnbind() throws Exception {
    ViewBinder<TextView> binder =
        new ViewBinder<TextView>() {
          private String mState;

          @Override
          public void prepare() {
            mState = "Hello World!";
          }

          @Override
          public void bind(TextView view) {
            view.setText(mState);
          }

          @Override
          public void unbind(TextView view) {
            view.setText("");
          }
        };

    LithoView lithoView = mountComponent(
        get(TEXT_VIEW_CREATOR, "TextView")
            .create(mContext)
            .viewBinder(binder));

    unbindComponent(lithoView);

    assertThat(lithoView.getMountItemCount()).isEqualTo(1);
    TextView view = (TextView) lithoView.getMountItemAt(0).getContent();
    assertThat(view.getText()).isEqualTo("");
  }

  @Test
  public void testTypeIdForDifferentViewCreators() {
    ViewCompatComponent compatComponent = get(TEXT_VIEW_CREATOR, "compat")
        .create(mContext)
        .viewBinder(NO_OP_VIEW_BINDER)
        .build();
    ViewCompatComponent sameCompatComponent = get(TEXT_VIEW_CREATOR, "sameCompat")
        .create(mContext)
        .viewBinder(NO_OP_VIEW_BINDER)
        .build();
    ViewCompatComponent differentCompatComponent = get(TEXT_VIEW_CREATOR_2, "differentCompat")
        .create(mContext)
        .viewBinder(NO_OP_VIEW_BINDER)
        .build();

    assertThat(compatComponent.getId()).isNotEqualTo(sameCompatComponent.getId());
    assertThat(compatComponent.getId()).isNotEqualTo(differentCompatComponent.getId());
    assertThat(sameCompatComponent.getId()).isNotEqualTo(differentCompatComponent.getId());

    assertThat(compatComponent.getTypeId()).isEqualTo(sameCompatComponent.getTypeId());
    assertThat(compatComponent.getTypeId()).isNotEqualTo(differentCompatComponent.getTypeId());
  }
}
