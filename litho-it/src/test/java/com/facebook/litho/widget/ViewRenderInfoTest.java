package com.facebook.litho.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.viewcompat.SimpleViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

@RunWith(ComponentsTestRunner.class)
public class ViewRenderInfoTest {

  private static final ViewCreator VIEW_CREATOR_1 =
      new ViewCreator() {
        @Override
        public View createView(Context c, ViewGroup parent) {
          return mock(View.class);
        }
      };

  @Test(expected = UnsupportedOperationException.class)
  public void testThrowWhenUsingIsFullSpan() {
    ViewRenderInfo viewRenderInfo = ViewRenderInfo
        .create()
        .viewBinder(new SimpleViewBinder())
        .viewCreator(VIEW_CREATOR_1)
        .isFullSpan(true /* actual value does not matter */)
        .build();
  }

}
