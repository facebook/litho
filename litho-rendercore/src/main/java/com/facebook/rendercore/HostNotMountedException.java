// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

public class HostNotMountedException extends RuntimeException {

  public RenderUnit renderUnit;
  public RenderUnit parentRenderUnit;

  public HostNotMountedException(
      RenderUnit renderUnit, RenderUnit parentRenderUnit, String message) {
    super(message);

    this.renderUnit = renderUnit;
    this.parentRenderUnit = parentRenderUnit;
  }
}
