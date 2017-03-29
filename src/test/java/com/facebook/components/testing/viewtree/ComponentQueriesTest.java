// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.testing.viewtree;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentView;
import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ComponentQueries}
 */
@RunWith(ComponentsTestRunner.class)
public class ComponentQueriesTest {
