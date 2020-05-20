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

package com.facebook.litho.intellij.logging;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.intellij.LithoPluginIntellijTest;
import com.facebook.litho.intellij.extensions.EventLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;

public class EventLoggerTest extends LithoPluginIntellijTest {

  public EventLoggerTest() {
    super("testdata/logging");
  }

  @Test
  public void log_syncLogged() {
    TestLogger testLogger = new TestLogger();
    EventLogger[] loggers = {testLogger};

    String event = "hello";
    new LithoLoggerProvider.LithoEventLogger(loggers, Runnable::run).log(event);
    assertThat(testLogger.event).isEqualTo(event);
  }

  @Test
  public void log_asyncBlockingLogged() throws InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    TestLogger testLogger = new TestLogger(countDownLatch);
    EventLogger[] loggers = {testLogger};

    String event = "hello";
    new LithoLoggerProvider.LithoEventLogger(loggers).log(event);
    countDownLatch.await();
    assertThat(testLogger.event).isEqualTo(event);
  }

  @Test
  public void log_asyncNotBlockingNotLogged() {
    TestLogger testLogger = new TestLogger();
    EventLogger[] loggers = {testLogger};

    String event = "hello";
    new LithoLoggerProvider.LithoEventLogger(loggers).log(event);
    assertThat(testLogger.event).isNull();
  }

  @Test
  public void log_withMetadata() {
    TestLogger testLogger = new TestLogger();
    EventLogger[] loggers = {testLogger};

    String event = "hello";
    Map<String, String> data = new HashMap<>();
    data.put("TEST2", "test2");
    new LithoLoggerProvider.LithoEventLogger(loggers).log(event, data);
    assertThat(data).containsKey("TEST2");
    assertThat(data).containsKey("TEST1");
  }

  static class TestLogger implements EventLogger {
    private final CountDownLatch lock;
    String event;
    private Map<String, String> metadata;

    TestLogger(CountDownLatch lock) {
      this.lock = lock;
    }

    TestLogger() {
      this.lock = new CountDownLatch(1);
    }

    @Override
    public void log(String event) {
      this.event = event;
      lock.countDown();
    }

    @Override
    public void log(String event, Map<String, String> metadata) {
      metadata.put("TEST1", event);
      this.metadata = metadata;
      log(event);
    }
  }
}
