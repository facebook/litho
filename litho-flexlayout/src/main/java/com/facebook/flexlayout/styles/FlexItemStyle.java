// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

import java.util.Arrays;

public class FlexItemStyle {
  // All methods in this class are made static to help with inlining
  public static class Builder {
    // Must be kept in sync with FlexItemStyleBase default values in native code
    private static final float DEFAULT_FLEX_GROW = 0;
    private static final float DEFAULT_FLEX_SHRINK = 1;
    private static final AlignSelf DEFAULT_ALIGN_SELF = AlignSelf.AUTO;
    private static final PositionType DEFAULT_POSITION_TYPE = PositionType.RELATIVE;
    private static final Display DEFAULT_DISPLAY = Display.FLEX;

    private float[] storage = new float[0];
    private int numUsedElements = 0;

    private static void increaseStorageSizeIfNeeded(
        final Builder builder, final int sizeIncrement) {
      final int numElementsNeeded = builder.numUsedElements + sizeIncrement;
      if (numElementsNeeded <= builder.storage.length) {
        return;
      }

      int newLength = builder.storage.length * 2;
      if (newLength < numElementsNeeded) {
        newLength += numElementsNeeded - newLength;
      }
      builder.storage = Arrays.copyOf(builder.storage, newLength);
    }

    public static void setFlex(final Builder builder, float flex) {
      if (isUndefined(flex)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.FLEX.ordinal();
      builder.storage[builder.numUsedElements++] = flex;
    }

    public static void setFlexGrow(final Builder builder, float flexGrow) {
      if (flexGrow == DEFAULT_FLEX_GROW) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.FLEX_GROW.ordinal();
      builder.storage[builder.numUsedElements++] = flexGrow;
    }

    public static void setFlexShrink(final Builder builder, float flexShrink) {
      if (flexShrink == DEFAULT_FLEX_SHRINK) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.FLEX_SHRINK.ordinal();
      builder.storage[builder.numUsedElements++] = flexShrink;
    }

    public static void setFlexBasis(final Builder builder, float flexBasis) {
      if (isUndefined(flexBasis)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.FLEX_BASIS.ordinal();
      builder.storage[builder.numUsedElements++] = flexBasis;
    }

    public static void setFlexBasisPercent(final Builder builder, float flexBasisPercent) {
      if (isUndefined(flexBasisPercent)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.FLEX_BASIS_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = flexBasisPercent;
    }

    public static void setAspectRatio(final Builder builder, float aspectRatio) {
      if (isUndefined(aspectRatio)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.ASPECT_RATIO.ordinal();
      builder.storage[builder.numUsedElements++] = aspectRatio;
    }

    public static void setAlignSelf(final Builder builder, AlignSelf alignSelf) {
      if (alignSelf == DEFAULT_ALIGN_SELF) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.ALIGN_SELF.ordinal();
      builder.storage[builder.numUsedElements++] = alignSelf.ordinal();
    }

    public static void setPositionType(final Builder builder, PositionType positionType) {
      if (positionType == DEFAULT_POSITION_TYPE) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.POSITION_TYPE.ordinal();
      builder.storage[builder.numUsedElements++] = positionType.intValue();
    }

    public static void setDisplay(final Builder builder, Display display) {
      if (display == DEFAULT_DISPLAY) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.DISPLAY.ordinal();
      builder.storage[builder.numUsedElements++] = display.intValue();
    }

    public static void setWidth(final Builder builder, float width) {
      if (isUndefined(width)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.WIDTH.ordinal();
      builder.storage[builder.numUsedElements++] = width;
    }

    public static void setWidthPercent(final Builder builder, float widthPercent) {
      if (isUndefined(widthPercent)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.WIDTH_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = widthPercent;
    }

    public static void setMinWidth(final Builder builder, float minWidth) {
      if (isUndefined(minWidth)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MIN_WIDTH.ordinal();
      builder.storage[builder.numUsedElements++] = minWidth;
    }

    public static void setMinWidthPercent(final Builder builder, float minWidthPercent) {
      if (isUndefined(minWidthPercent)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MIN_WIDTH_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = minWidthPercent;
    }

    public static void setMaxWidth(final Builder builder, float maxWidth) {
      if (isUndefined(maxWidth)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MAX_WIDTH.ordinal();
      builder.storage[builder.numUsedElements++] = maxWidth;
    }

    public static void setMaxWidthPercent(final Builder builder, float maxWidthPercent) {
      if (isUndefined(maxWidthPercent)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MAX_WIDTH_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = maxWidthPercent;
    }

    public static void setHeight(final Builder builder, float height) {
      if (isUndefined(height)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.HEIGHT.ordinal();
      builder.storage[builder.numUsedElements++] = height;
    }

    public static void setHeightPercent(final Builder builder, float heightPercent) {
      if (isUndefined(heightPercent)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.HEIGHT_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = heightPercent;
    }

    public static void setMinHeight(final Builder builder, float minHeight) {
      if (isUndefined(minHeight)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MIN_HEIGHT.ordinal();
      builder.storage[builder.numUsedElements++] = minHeight;
    }

    public static void setMinHeightPercent(final Builder builder, float minHeightPercent) {
      if (isUndefined(minHeightPercent)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MIN_HEIGHT_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = minHeightPercent;
    }

    public static void setMaxHeight(final Builder builder, float maxHeight) {
      if (isUndefined(maxHeight)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MAX_HEIGHT.ordinal();
      builder.storage[builder.numUsedElements++] = maxHeight;
    }

    public static void setMaxHeightPercent(final Builder builder, float maxHeightPercent) {
      if (isUndefined(maxHeightPercent)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MAX_HEIGHT_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = maxHeightPercent;
    }

    public static void setMargin(final Builder builder, Edge edge, float value) {
      if (isUndefined(value)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 3);
      builder.storage[builder.numUsedElements++] = Keys.MARGIN.ordinal();
      builder.storage[builder.numUsedElements++] = edge.intValue();
      builder.storage[builder.numUsedElements++] = value;
    }

    public static void setMarginPercent(final Builder builder, Edge edge, float value) {
      if (isUndefined(value)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 3);
      builder.storage[builder.numUsedElements++] = Keys.MARGIN_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = edge.intValue();
      builder.storage[builder.numUsedElements++] = value;
    }

    public static void setMarginAuto(final Builder builder, Edge edge) {
      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.MARGIN_AUTO.ordinal();
      builder.storage[builder.numUsedElements++] = edge.intValue();
    }

    public static void setPosition(final Builder builder, Edge edge, float value) {
      if (isUndefined(value)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 3);
      builder.storage[builder.numUsedElements++] = Keys.POSITION.ordinal();
      builder.storage[builder.numUsedElements++] = edge.intValue();
      builder.storage[builder.numUsedElements++] = value;
    }

    public static void setPositionPercent(final Builder builder, Edge edge, float value) {
      if (isUndefined(value)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 3);
      builder.storage[builder.numUsedElements++] = Keys.POSITION_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = edge.intValue();
      builder.storage[builder.numUsedElements++] = value;
    }

    private static boolean isUndefined(float value) {
      return Float.compare(value, Float.NaN) == 0;
    }

    public static float[] serialize(Builder builder) {
      return Arrays.copyOf(builder.storage, builder.numUsedElements);
    }

    @Override
    public String toString() {
      final StringBuilder styleStr = new StringBuilder();
      for (int i = 0; i < numUsedElements; ) {
        final Keys key = Keys.values()[(int) storage[i]];
        switch (key) {
          case FLEX:
            styleStr.append("  flex: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case FLEX_GROW:
            styleStr.append("  flexGrow: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case FLEX_SHRINK:
            styleStr.append("  flexShrink: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case FLEX_BASIS:
            styleStr.append("  flexBasis: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case FLEX_BASIS_PERCENT:
            styleStr.append("  flexBasis: ").append(storage[i + 1]).append("%\n");
            i += 2;
            break;
          case FLEX_BASIS_AUTO:
            break;
          case WIDTH:
            styleStr.append("  width: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case WIDTH_PERCENT:
            styleStr.append("  width: ").append(storage[i + 1]).append("%\n");
            i += 2;
            break;
          case WIDTH_AUTO:
            break;
          case MIN_WIDTH:
            styleStr.append("  minWidth: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case MIN_WIDTH_PERCENT:
            styleStr.append("  minWidth: ").append(storage[i + 1]).append("%\n");
            i += 2;
            break;
          case MAX_WIDTH:
            styleStr.append("  maxWidth: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case MAX_WIDTH_PERCENT:
            styleStr.append("  maxWidth: ").append(storage[i + 1]).append("%\n");
            i += 2;
            break;
          case HEIGHT:
            styleStr.append("  height: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case HEIGHT_PERCENT:
            styleStr.append("  height: ").append(storage[i + 1]).append("%\n");
            i += 2;
            break;
          case HEIGHT_AUTO:
            break;
          case MIN_HEIGHT:
            styleStr.append("  minHeight: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case MIN_HEIGHT_PERCENT:
            styleStr.append("  minHeight: ").append(storage[i + 1]).append("%\n");
            i += 2;
            break;
          case MAX_HEIGHT:
            styleStr.append("  maxHeight: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case MAX_HEIGHT_PERCENT:
            styleStr.append("  maxHeight: ").append(storage[i + 1]).append("%\n");
            i += 2;
            break;
          case ALIGN_SELF:
            final AlignSelf alignSelf = AlignSelf.values()[(int) storage[i + 1]];
            styleStr.append("  alignSelf: ").append(alignSelf).append("\n");
            i += 2;
            break;
          case POSITION_TYPE:
            final PositionType positionType = PositionType.values()[(int) storage[i + 1]];
            styleStr.append("  positionType: ").append(positionType).append("\n");
            i += 2;
            break;
          case ASPECT_RATIO:
            styleStr.append("  aspectRatio: ").append(storage[i + 1]).append("\n");
            i += 2;
            break;
          case DISPLAY:
            final Display display = Display.values()[(int) storage[i + 1]];
            styleStr.append("  display: ").append(display).append("\n");
            i += 2;
            break;
          case MARGIN:
            {
              final Edge edge = Edge.fromInt((int) storage[i + 1]);
              final float margin = storage[i + 2];
              styleStr.append("  margin").append(edge).append(": ").append(margin).append("\n");
              i += 3;
            }
            break;
          case MARGIN_PERCENT:
            {
              final Edge edge = Edge.fromInt((int) storage[i + 1]);
              final float margin = storage[i + 2];
              styleStr.append("  margin").append(edge).append(": ").append(margin).append("%\n");
              i += 3;
            }
            break;
          case MARGIN_AUTO:
            {
              final Edge edge = Edge.fromInt((int) storage[i + 1]);
              styleStr.append("  margin").append(edge).append(": ").append("auto\n");
              i += 2;
            }
            break;
          case POSITION:
            {
              final Edge edge = Edge.fromInt((int) storage[i + 1]);
              final float position = storage[i + 2];
              styleStr.append("  position").append(edge).append(": ").append(position).append("\n");
              i += 3;
            }
            break;
          case POSITION_PERCENT:
            {
              final Edge edge = Edge.fromInt((int) storage[i + 1]);
              final float position = storage[i + 2];
              styleStr
                  .append("  position")
                  .append(edge)
                  .append(": ")
                  .append(position)
                  .append("%\n");
              i += 3;
            }
            break;
          case HAS_MEASURE_FUNCTION:
            styleStr.append("  hasMeasureFunction: true\n");
            i += 1;
            break;
          case HAS_BASELINE_FUNCTION:
            styleStr.append("  hasBaselineFunction: true\n");
            i += 1;
            break;
          case ENABLE_TEXT_ROUNDING:
            styleStr.append("  enableTextRounding: true\n");
            i += 1;
            break;
        }
      }

      return styleStr.length() > 0 ? "{\n" + styleStr.toString() + "}" : "";
    }
  }

  // Must be kept in sync with FlexItemStyleKeys in JNI code
  public enum Keys {
    FLEX,
    FLEX_GROW,
    FLEX_SHRINK,
    FLEX_BASIS,
    FLEX_BASIS_PERCENT,
    FLEX_BASIS_AUTO,
    WIDTH,
    WIDTH_PERCENT,
    WIDTH_AUTO,
    MIN_WIDTH,
    MIN_WIDTH_PERCENT,
    MAX_WIDTH,
    MAX_WIDTH_PERCENT,
    HEIGHT,
    HEIGHT_PERCENT,
    HEIGHT_AUTO,
    MIN_HEIGHT,
    MIN_HEIGHT_PERCENT,
    MAX_HEIGHT,
    MAX_HEIGHT_PERCENT,
    ALIGN_SELF,
    POSITION_TYPE,
    ASPECT_RATIO,
    DISPLAY,
    MARGIN,
    MARGIN_PERCENT,
    MARGIN_AUTO,
    POSITION,
    POSITION_PERCENT,
    HAS_MEASURE_FUNCTION,
    HAS_BASELINE_FUNCTION,
    ENABLE_TEXT_ROUNDING,
  }
}
