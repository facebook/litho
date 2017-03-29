/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow.springs;

/**
 * Data structure for storing spring configuration.
 *
 * This code was forked from the facebook/rebound repository.
 */
public class SpringConfig {
  public double friction;
  public double tension;

  // Taken from the default Quartz Composer spring config of tension=40, friction=7 (in QC units)
  public static SpringConfig defaultConfig = new SpringConfig(230.2, 22.0);

  /**
   * constructor for the SpringConfig
   * @param tension tension value for the SpringConfig
   * @param friction friction value for the SpringConfig
   */
  public SpringConfig(double tension, double friction) {
    this.tension = tension;
    this.friction = friction;
  }
}
