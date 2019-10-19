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

package com.facebook.litho.dataflow.springs;

/**
 * Data structure for storing spring configuration.
 *
 * <p>This code was forked from the facebook/rebound repository.
 */
public class SpringConfig {

  public static final double DEFAULT_TENSION = 230.2;
  public static final double DEFAULT_FRICTION = 22.0;

  public double friction;
  public double tension;

  // Taken from the default Quartz Composer spring config of tension=40, friction=7 (in QC units)
  public static SpringConfig defaultConfig = new SpringConfig(DEFAULT_TENSION, DEFAULT_FRICTION);

  // Taken from the default Quartz Composer spring config of tension=70, friction=11 (in QC units)
  public static SpringConfig noOvershootConfig = new SpringConfig(338.8, 34);

  /**
   * constructor for the SpringConfig
   *
   * @param tension tension value for the SpringConfig
   * @param friction friction value for the SpringConfig
   */
  public SpringConfig(double tension, double friction) {
    this.tension = tension;
    this.friction = friction;
  }
}
