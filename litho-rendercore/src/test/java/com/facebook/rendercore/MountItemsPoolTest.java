// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class MountItemsPoolTest {
  private final Context mContext = RuntimeEnvironment.application;
  private ActivityController<Activity> mActivityController;
  private Activity mActivity;

  @Before
  public void setup() {
    MountItemsPool.clear();
    MountItemsPool.setMountContentPoolFactory(null);
    mActivityController = Robolectric.buildActivity(Activity.class).create();
    mActivity = mActivityController.get();
  }

  @Test
  public void testPrefillMountContentPool() {
    final int prefillCount = 4;
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0);
    MountItemsPool.prefillMountContentPool(mContext, prefillCount, testRenderUnit);
    assertThat(testRenderUnit.getCreatedCount()).isEqualTo(prefillCount);

    final TestRenderUnit testRenderUnitToAcquire = new TestRenderUnit(0);

    for (int i = 0; i < prefillCount; i++) {
      MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    }
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(0);

    MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(1);
  }

  @Test
  public void testPrefillMountContentPoolWithCustomPool() {
    final int prefillCount = 4;
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0, 2);
    MountItemsPool.prefillMountContentPool(mContext, prefillCount, testRenderUnit);
    assertThat(testRenderUnit.getCreatedCount()).isEqualTo(prefillCount);

    final TestRenderUnit testRenderUnitToAcquire = new TestRenderUnit(0, 2);

    for (int i = 0; i < prefillCount; i++) {
      MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    }
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(2);
  }

  @Test
  public void testPrefillMountContentPoolWithCustomPoolFactory() {
    final MountItemsPool.ItemPool customPool = new MountItemsPool.DefaultItemPool(new Object(), 10);
    MountItemsPool.setMountContentPoolFactory(() -> customPool);

    final int prefillCount = 10;
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0, 5);
    MountItemsPool.prefillMountContentPool(mContext, prefillCount, testRenderUnit);
    assertThat(testRenderUnit.getCreatedCount()).isEqualTo(prefillCount);

    final TestRenderUnit testRenderUnitToAcquire = new TestRenderUnit(0, 5);

    for (int i = 0; i < prefillCount; i++) {
      MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    }
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(0);
  }

  @Test
  public void testReleaseMountContentForDestroyedContextDoesNothing() {
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0);

    final Object content1 = MountItemsPool.acquireMountContent(mActivity, testRenderUnit);
    MountItemsPool.release(mActivity, testRenderUnit, content1);

    final Object content2 = MountItemsPool.acquireMountContent(mActivity, testRenderUnit);

    // Assert pooling was working before
    assertThat(content1).isSameAs(content2);

    MountItemsPool.release(mActivity, testRenderUnit, content2);

    // Now destroy the activity and assert pooling no longer works. Next acquire should produce
    // difference content.
    MountItemsPool.onContextDestroyed(mActivity);

    final Object content3 = MountItemsPool.acquireMountContent(mActivity, testRenderUnit);
    assertThat(content3).isNotSameAs(content1);
  }

  @Test
  public void testDestroyingActivityDoesNotAffectPoolingOfOtherContexts() {
    // Destroy activity context
    mActivityController.destroy();
    MountItemsPool.onContextDestroyed(mActivity);
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0);

    // Create content with different context
    final Object content1 = MountItemsPool.acquireMountContent(mContext, testRenderUnit);
    MountItemsPool.release(mContext, testRenderUnit, content1);

    final Object content2 = MountItemsPool.acquireMountContent(mContext, testRenderUnit);

    // Ensure different context is unaffected by destroying activity context.
    assertThat(content1).isSameAs(content2);
  }

  public static final class TestRenderUnit extends RenderUnit<View>
      implements ContentAllocator<View> {

    private final long mId;
    private final int mCustomPoolSize;
    private int mCreatedCount;

    public TestRenderUnit(long id) {
      super(RenderType.VIEW);
      mId = id;
      mCreatedCount = 0;
      mCustomPoolSize = 0;
    }

    public TestRenderUnit(long id, int customPoolSize) {
      super(RenderType.VIEW);
      mId = id;
      mCreatedCount = 0;
      mCustomPoolSize = customPoolSize;
    }

    @Override
    public View createContent(Context c) {
      mCreatedCount++;
      return new View(c);
    }

    @Nullable
    @Override
    public MountItemsPool.ItemPool createRecyclingPool() {
      if (mCustomPoolSize > 0)
        return new MountItemsPool.DefaultItemPool(new Object(), mCustomPoolSize);

      return null;
    }

    @Override
    public ContentAllocator<View> getContentAllocator() {
      return this;
    }

    @Override
    public long getId() {
      return mId;
    }

    public int getCreatedCount() {
      return mCreatedCount;
    }
  }
}
