// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho;

import android.content.Context;
import android.widget.TextView;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompatcreator.ViewCompatCreator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link ViewCompatComponent}
 */
@RunWith(ComponentsTestRunner.class)
public class ViewCompatComponentTest {

  private static final ViewCompatCreator<TextView> TEXT_VIEW_CREATOR =
      new ViewCompatCreator<TextView>() {
        @Override
        public TextView createView(Context c) {
          return new TextView(c);
        }
      };

  private ComponentContext mContext;

  @Before
  public void setUp() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testSimpleRendering() throws Exception {
    ViewCompatComponent.ViewBinder<TextView> binder =
        new ViewCompatComponent.ViewBinder<TextView>() {
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

    ComponentView componentView = ComponentTestHelper.mountComponent(
        ViewCompatComponent.get(TEXT_VIEW_CREATOR, "TextView")
            .create(mContext)
            .viewBinder(binder));

    assertEquals(1, componentView.getMountItemCount());
    TextView view = (TextView) componentView.getMountItemAt(0).getContent();
    assertEquals("Hello World!", view.getText());
  }

  @Test
  public void testPrepare() throws Exception {
    ViewCompatComponent.ViewBinder<TextView> binder =
        new ViewCompatComponent.ViewBinder<TextView>() {
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

    ComponentView componentView = ComponentTestHelper.mountComponent(
        ViewCompatComponent.get(TEXT_VIEW_CREATOR, "TextView")
            .create(mContext)
            .viewBinder(binder));

    assertEquals(1, componentView.getMountItemCount());
    TextView view = (TextView) componentView.getMountItemAt(0).getContent();
    assertEquals("Hello World!", view.getText());
  }

  @Test
  public void testUnbind() throws Exception {
    ViewCompatComponent.ViewBinder<TextView> binder =
        new ViewCompatComponent.ViewBinder<TextView>() {
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

    ComponentView componentView = ComponentTestHelper.mountComponent(
        ViewCompatComponent.get(TEXT_VIEW_CREATOR, "TextView")
            .create(mContext)
            .viewBinder(binder));

    ComponentTestHelper.unbindComponent(componentView);

    assertEquals(1, componentView.getMountItemCount());
    TextView view = (TextView) componentView.getMountItemAt(0).getContent();
    assertEquals("", view.getText());
  }
}
