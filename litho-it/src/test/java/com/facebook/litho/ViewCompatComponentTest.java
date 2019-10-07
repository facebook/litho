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

package com.facebook.litho;

import static com.facebook.litho.ViewCompatComponent.get;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.litho.testing.helper.ComponentTestHelper.unbindComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link ViewCompatComponent} */
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

  private class CustomViewCreator implements ViewCreator {
    private final String mText;

    private CustomViewCreator(String text) {
      this.mText = text;
    }

    @Override
    public View createView(Context c, ViewGroup parent) {
      final TextView textView = new TextView(c);
      textView.setText(mText);
      return textView;
    }
  }

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
          public void prepare() {}

          @Override
          public void bind(TextView view) {
            view.setText("Hello World!");
          }

          @Override
          public void unbind(TextView view) {}
        };

    LithoView lithoView =
        mountComponent(get(TEXT_VIEW_CREATOR, "TextView").create(mContext).viewBinder(binder));

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
          public void unbind(TextView view) {}
        };

    LithoView lithoView =
        mountComponent(get(TEXT_VIEW_CREATOR, "TextView").create(mContext).viewBinder(binder));

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

    LithoView lithoView =
        mountComponent(get(TEXT_VIEW_CREATOR, "TextView").create(mContext).viewBinder(binder));

    unbindComponent(lithoView);

    assertThat(lithoView.getMountItemCount()).isEqualTo(1);
    TextView view = (TextView) lithoView.getMountItemAt(0).getContent();
    assertThat(view.getText()).isEqualTo("");
  }

  @Test
  public void testTypeIdForDifferentViewCreators() {
    ViewCompatComponent compatComponent =
        get(TEXT_VIEW_CREATOR, "compat").create(mContext).viewBinder(NO_OP_VIEW_BINDER).build();
    ViewCompatComponent sameCompatComponent =
        get(TEXT_VIEW_CREATOR, "sameCompat").create(mContext).viewBinder(NO_OP_VIEW_BINDER).build();
    ViewCompatComponent differentCompatComponent =
        get(TEXT_VIEW_CREATOR_2, "differentCompat")
            .create(mContext)
            .viewBinder(NO_OP_VIEW_BINDER)
            .build();

    assertThat(compatComponent.getId()).isNotEqualTo(sameCompatComponent.getId());
    assertThat(compatComponent.getId()).isNotEqualTo(differentCompatComponent.getId());
    assertThat(sameCompatComponent.getId()).isNotEqualTo(differentCompatComponent.getId());

    assertThat(compatComponent.getTypeId()).isEqualTo(sameCompatComponent.getTypeId());
    assertThat(compatComponent.getTypeId()).isNotEqualTo(differentCompatComponent.getTypeId());
  }

  @Test
  public void testTypeIdForSameViewCreatorTypeButDifferentInstances() {
    CustomViewCreator textViewCreator1 = new CustomViewCreator("textviewcreator1");
    CustomViewCreator textViewCreator2 = new CustomViewCreator("textviewcreator2");

    ViewCompatComponent compatComponent =
        get(textViewCreator1, "compat").create(mContext).viewBinder(NO_OP_VIEW_BINDER).build();
    ViewCompatComponent sameCompatComponent =
        get(textViewCreator1, "sameCompat").create(mContext).viewBinder(NO_OP_VIEW_BINDER).build();
    ViewCompatComponent differentCompatComponent =
        get(textViewCreator2, "differentCompat")
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
