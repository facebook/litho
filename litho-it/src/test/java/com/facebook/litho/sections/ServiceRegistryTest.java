/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Java6Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link ServiceRegistry} */
@RunWith(ComponentsTestRunner.class)
public class ServiceRegistryTest {

  @Before
  public void setup() {
    ServiceRegistry.getUnusedServicesToSections().clear();
    ServiceRegistry.getContextsToServicesMap().clear();
  }

  @Test
  public void testRegisterService() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service);
    final Context context = RuntimeEnvironment.application;
    final SectionContext sectionContext = new SectionContext(context);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    final Section secondSection = TestSectionCreator.createSection(0, "2", lifecycle);
    secondSection.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);
    ServiceRegistry.registerService(secondSection);

    Map<Context, Map<Object, Set<Section>>> contextsToServicesMap =
        ServiceRegistry.getContextsToServicesMap();

    assertThat(contextsToServicesMap.size()).isEqualTo(1);

    Map<Object, Set<Section>> servicesToSections =
        contextsToServicesMap.get(contextsToServicesMap.keySet().iterator().next());

    assertThat(servicesToSections.size()).isEqualTo(1);

    Set<Section> sections = servicesToSections.get(service);
    assertThat(sections.size()).isEqualTo(2);
  }

  @Test
  public void testUnregisterService() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service);
    final SectionContext sectionContext = new SectionContext(RuntimeEnvironment.application);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    final Section secondSection = TestSectionCreator.createSection(0, "2", lifecycle);
    secondSection.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);
    ServiceRegistry.registerService(secondSection);

    ServiceRegistry.unregisterService(section);

    Map<Context, Map<Object, Set<Section>>> contextsToServicesMap =
        ServiceRegistry.getContextsToServicesMap();

    assertThat(contextsToServicesMap.size()).isEqualTo(1);

    Map<Object, Set<Section>> servicesToSections =
        contextsToServicesMap.get(contextsToServicesMap.keySet().iterator().next());

    assertThat(servicesToSections.size()).isEqualTo(1);

    Set<Section> sections = servicesToSections.get(service);
    assertThat(sections.size()).isEqualTo(1);

    Map<Object, Section> unusedServicestoSections = ServiceRegistry.getUnusedServicesToSections();

    assertThat(unusedServicestoSections.isEmpty()).isTrue();

    ServiceRegistry.unregisterService(secondSection);

    contextsToServicesMap = ServiceRegistry.getContextsToServicesMap();
    unusedServicestoSections = ServiceRegistry.getUnusedServicesToSections();

    assertThat(contextsToServicesMap.isEmpty()).isFalse();
    assertThat(unusedServicestoSections.size()).isEqualTo(1);
    assertThat(unusedServicestoSections.containsKey(service)).isTrue();
    assertThat(unusedServicestoSections.get(service)).isEqualTo(secondSection);
  }

  @Test
  public void testCleanUnusedServices() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service);
    final SectionContext sectionContext = new SectionContext(RuntimeEnvironment.application);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    final Section secondSection = TestSectionCreator.createSection(0, "2", lifecycle);
    secondSection.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);
    ServiceRegistry.registerService(secondSection);

    ServiceRegistry.unregisterService(section);
    ServiceRegistry.unregisterService(secondSection);

    ServiceRegistry.cleanUnusedServices();

    Map<Object, Section> unusedServicestoSections = ServiceRegistry.getUnusedServicesToSections();

    assertThat(unusedServicestoSections.isEmpty()).isTrue();
    assertThat(lifecycle.mUnbindServiceCalled).isFalse();
    assertThat(lifecycle.mDestroyServiceCalled).isTrue();
  }

  @Test
  public void testRegisterUnusedServicePreventsCleanup() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service);
    final Context context = RuntimeEnvironment.application;
    final SectionContext sectionContext = new SectionContext(context);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);
    ServiceRegistry.unregisterService(section);

    Map<Object, Section> unusedServicestoSections = ServiceRegistry.getUnusedServicesToSections();

    assertThat(unusedServicestoSections.size()).isEqualTo(1);
    assertThat(lifecycle.mUnbindServiceCalled).isFalse();
    assertThat(lifecycle.mDestroyServiceCalled).isFalse();

    ServiceRegistry.registerService(section);

    unusedServicestoSections = ServiceRegistry.getUnusedServicesToSections();
    assertThat(unusedServicestoSections.isEmpty()).isTrue();
  }

  @Test
  public void testOnContextDestroy() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service);
    final Activity activity = new Activity();
    final SectionContext sectionContext = new SectionContext(activity);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);
    ServiceRegistry.onActivityDestroyed(activity);

    Map<Context, Map<Object, Set<Section>>> contextsToServicesMap =
        ServiceRegistry.getContextsToServicesMap();
    Map<Object, Section> unusedServicestoSections = ServiceRegistry.getUnusedServicesToSections();

    assertThat(contextsToServicesMap.isEmpty()).isTrue();
    assertThat(unusedServicestoSections.isEmpty()).isTrue();
    assertThat(lifecycle.mUnbindServiceCalled).isTrue();
    assertThat(lifecycle.mDestroyServiceCalled).isTrue();
  }

  @Test
  public void testServiceUnregistrationExitEarlyAfterContextDestroy() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service);
    final Activity activity = new Activity();
    final SectionContext sectionContext = new SectionContext(activity);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    try {
      ServiceRegistry.registerService(section);
      ServiceRegistry.onActivityDestroyed(activity);
      ServiceRegistry.unregisterService(section);
    } catch (NullPointerException e) {
      fail("Null pointer exception is thrown because all the services are already cleaned up");
    }
  }

  @Test()
  public void testUnwrapContextToFindActivityNoActivity() {
    final SectionContext sectionContext = new SectionContext(RuntimeEnvironment.application);
    Java6Assertions.assertThat(
        ServiceRegistry.unwrapContextToFindActivity(sectionContext)).isNull();
  }

  @Test
  public void testUnwrapContextToFindActivityWithActivity() {
    final Activity activity = new Activity();
    final SectionContext sectionContext = new SectionContext(activity);
    assertThat(ServiceRegistry.unwrapContextToFindActivity(sectionContext)).isEqualTo(activity);
  }

  @Test
  public void testUnwrapContextToFindActivityWithNestedActivity() {
    final Activity activity = new Activity();
    final SectionContext sectionContext =
        new SectionContext(
            new ContextWrapper(
                new ContextWrapper(activity)));
    assertThat(ServiceRegistry.unwrapContextToFindActivity(sectionContext)).isEqualTo(activity);
  }

  @Test
  public void testRemoveServiceForActivityNoService() {
      assertThat(ServiceRegistry.removeServicesForActivity(new Activity())).isEmpty();
  }

  @Test
  public void testRemoveServiceForActivityKeyIsActivity() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service);
    final Activity activity = new Activity();
    final SectionContext sectionContext = new SectionContext(activity);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);

    Map<Object, Set<Section>> serviceCount = ServiceRegistry.removeServicesForActivity(activity);
    assertThat(serviceCount.size()).isEqualTo(1);
    assertThat(serviceCount.get(lifecycle.getService(section))).contains(section);
  }

  @Test
  public void testRemoveServiceForActivityKeyIsWrappedActivity() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service);
    final Activity activity = new Activity();
    final SectionContext sectionContext =
        new SectionContext(
            new ContextWrapper(
                new ContextWrapper(activity)));

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);

    Map<Object, Set<Section>> serviceCount = ServiceRegistry.removeServicesForActivity(activity);
    assertThat(serviceCount.size()).isEqualTo(1);
    assertThat(serviceCount.get(service)).contains(section);
  }

  @Test
  public void testRemoveServiceForActivityMultipleWrappedEntries() {
    final Activity activity = new Activity();

    final SectionContext oneLevelDeepSectionContext =
        new SectionContext(
            new ContextWrapper(activity));
    final Object firstService = new Object();
    final TestLifecycle firstLifecycle = new TestLifecycle(firstService);
    final Section firstSection = TestSectionCreator.createSection(0, "1", firstLifecycle);
    firstSection.setScopedContext(oneLevelDeepSectionContext);
    ServiceRegistry.registerService(firstSection);

    final SectionContext twoLevelsDeepSectionContext =
        new SectionContext(
            new ContextWrapper(
                new ContextWrapper(activity)));
    final Object secondService = new Object();
    final TestLifecycle secondLifecycle = new TestLifecycle(secondService);
    final Section secondSection = TestSectionCreator.createSection(0, "2", secondLifecycle);
    secondSection.setScopedContext(twoLevelsDeepSectionContext);
    ServiceRegistry.registerService(secondSection);

    Map<Object, Set<Section>> serviceCount = ServiceRegistry.removeServicesForActivity(activity);
    assertThat(serviceCount.size()).isEqualTo(2);

    assertThat(serviceCount.keySet()).contains(firstService);
    assertThat(serviceCount.get(firstService)).contains(firstSection);

    assertThat(serviceCount.keySet()).contains(secondService);
    assertThat(serviceCount.get(secondService)).contains(secondSection);
  }

  @Test
  public void testRegisterSectionWithoutOnDestroyService() {
    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service, false);
    final Activity activity = new Activity();
    final SectionContext sectionContext = new SectionContext(activity);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);

    Map<Object, Set<Section>> serviceCount = ServiceRegistry.removeServicesForActivity(activity);
    assertThat(serviceCount.isEmpty()).isTrue();
  }

  @Test
  public void testUnregisterSectionWithoutOnDestroyService() {
    final Activity activity = new Activity();
    final SectionContext sectionContext = new SectionContext(activity);

    final Object service = new Object();
    final TestLifecycle lifecycle = new TestLifecycle(service, true);

    final Section section = TestSectionCreator.createSection(0, "1", lifecycle);
    section.setScopedContext(sectionContext);

    final Object secondService = new Object();
    final TestLifecycle secondLifecycle = new TestLifecycle(secondService, false);
    final Section secondSection = TestSectionCreator.createSection(0, "2", secondLifecycle);
    secondSection.setScopedContext(sectionContext);

    ServiceRegistry.registerService(section);
    ServiceRegistry.registerService(secondSection);

    ServiceRegistry.unregisterService(secondSection);

    Map<Object, Set<Section>> serviceCount = ServiceRegistry.removeServicesForActivity(activity);
    assertThat(serviceCount.size()).isEqualTo(1);
  }

  private static class TestLifecycle extends SectionLifecycle {
    private boolean mDestroyServiceCalled;
    private boolean mUnbindServiceCalled;
    private final Object mService;
    private final boolean mHasDestroyService;

    TestLifecycle(Object service) {
      this(service, true);
    }

    TestLifecycle(Object service, boolean hasDestroyService) {
      mService = service;
      mHasDestroyService = hasDestroyService;
    }

    @Override
    protected void unbindService(SectionContext c, Section section) {
      super.unbindService(c, section);
      mUnbindServiceCalled = true;
    }

    @Override
    protected void destroyService(SectionContext c, Object service) {
      super.destroyService(c, service);
      mDestroyServiceCalled = true;
    }

    @Override
    protected Object getService(Section section) {
      return mService;
    }

    @Override
    protected boolean hasDestroyService() {
      return mHasDestroyService;
    }
  }
}
