/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentsReporter.LogLevel.ERROR;
import static com.facebook.litho.ComponentsReporter.LogLevel.FATAL;
import static com.facebook.litho.ComponentsReporter.LogLevel.WARNING;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import com.facebook.litho.testing.logging.TestComponentsReporter;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.assertj.core.api.ThrowableAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests {@link ComponentsReporterTest} */
@RunWith(ComponentsTestRunner.class)
public class ComponentsReporterTest {

  private static final String FATAL_MSG = "fatal";
  private static final String ERROR_MSG = "error";
  private static final String WARNING_MSG = "warning";

  private TestComponentsReporter mReporter;

  @Before
  public void setup() {
    mReporter = new TestComponentsReporter();
    ComponentsReporter.provide(mReporter);
  }

  @After
  public void tearDown() {
    ComponentsReporter.provide(new DefaultComponentsReporter());
  }

  @Test
  public void testEmitFatalMessage() {
    final Throwable throwable =
        catchThrowable(
            new ThrowableAssert.ThrowingCallable() {
              @Override
              public void call() throws Throwable {
                ComponentsReporter.emitMessage(FATAL, FATAL_MSG);

                assertThat(mReporter.getLevel()).isEqualTo(FATAL);
                assertThat(mReporter.getMessage()).isEqualTo(FATAL_MSG);
              }
            });
    assertThat(throwable).isInstanceOf(RuntimeException.class);
    assertThat(throwable.getMessage()).isEqualTo(FATAL_MSG);
  }

  @Test
  public void testEmitErrorMessage() {
    ComponentsReporter.emitMessage(ERROR, ERROR_MSG);

    assertThat(mReporter.getLevel()).isEqualTo(ERROR);
    assertThat(mReporter.getMessage()).isEqualTo(ERROR_MSG);
  }

  @Test
  public void testEmitWarningMessage() {
    ComponentsReporter.emitMessage(WARNING, WARNING_MSG);

    assertThat(mReporter.getLevel()).isEqualTo(WARNING);
    assertThat(mReporter.getMessage()).isEqualTo(WARNING_MSG);
  }
}
