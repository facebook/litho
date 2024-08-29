// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

import java.util.Arrays;

public class FlexBoxStyle {
  // All methods in this class are made static to help with inlining
  public static final class Builder {
    // Must be kept in sync with FlexBoxBoxStyle default values in native code
    private static final Direction DEFAULT_DIRECTION = Direction.INHERIT;
    private static final FlexDirection DEFAULT_FLEX_DIRECTION = FlexDirection.ROW;
    private static final Justify DEFAULT_JUSTIFY_CONTENT = Justify.FLEX_START;
    private static final AlignContent DEFAULT_ALIGN_CONTENT = AlignContent.STRETCH;
    private static final AlignItems DEFAULT_ALIGN_ITEMS = AlignItems.STRETCH;
    private static final Wrap DEFAULT_WRAP = Wrap.NO_WRAP;
    private static final Overflow DEFAULT_OVERFLOW = Overflow.VISIBLE;
    private static final float DEFAULT_POINT_SCALE_FACTOR = 1f;

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

    public static void setPointScaleFactor(final Builder builder, Float pointScaleFactor) {
      if (pointScaleFactor == DEFAULT_POINT_SCALE_FACTOR) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.POINT_SCALE_FACTOR.ordinal();
      builder.storage[builder.numUsedElements++] = pointScaleFactor;
    }

    public static void setDirection(final Builder builder, Direction direction) {
      if (direction == DEFAULT_DIRECTION) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.DIRECTION.ordinal();
      builder.storage[builder.numUsedElements++] = direction.intValue();
    }

    public static void setFlexDirection(final Builder builder, final FlexDirection flexDirection) {
      if (flexDirection == DEFAULT_FLEX_DIRECTION) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.FLEX_DIRECTION.ordinal();
      builder.storage[builder.numUsedElements++] = flexDirection.intValue();
    }

    public static void setJustifyContent(final Builder builder, Justify justifyContent) {
      if (justifyContent == DEFAULT_JUSTIFY_CONTENT) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.JUSTIFY_CONTENT.ordinal();
      builder.storage[builder.numUsedElements++] = justifyContent.intValue();
    }

    public static void setAlignContent(final Builder builder, AlignContent alignContent) {
      if (alignContent == DEFAULT_ALIGN_CONTENT) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.ALIGN_CONTENT.ordinal();
      builder.storage[builder.numUsedElements++] = alignContent.ordinal();
    }

    public static void setAlignItems(final Builder builder, AlignItems alignItems) {
      if (alignItems == DEFAULT_ALIGN_ITEMS) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.ALIGN_ITEMS.ordinal();
      builder.storage[builder.numUsedElements++] = alignItems.ordinal();
    }

    public static void setWrap(final Builder builder, Wrap flexWrap) {
      if (flexWrap == DEFAULT_WRAP) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.FLEX_WRAP.ordinal();
      builder.storage[builder.numUsedElements++] = flexWrap.intValue();
    }

    public static void setOverflow(final Builder builder, Overflow overflow) {
      if (overflow == DEFAULT_OVERFLOW) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 2);
      builder.storage[builder.numUsedElements++] = Keys.OVERFLOW.ordinal();
      builder.storage[builder.numUsedElements++] = overflow.intValue();
    }

    public static void setPadding(final Builder builder, final Edge edge, final float value) {
      if (isUndefined(value)) {
        return;
      }

      increaseStorageSizeIfNeeded(builder, 3);
      builder.storage[builder.numUsedElements++] = Keys.PADDING.ordinal();
      builder.storage[builder.numUsedElements++] = edge.intValue();
      builder.storage[builder.numUsedElements++] = value;
    }

    public static void setPaddingPercent(final Builder builder, Edge edge, float value) {
      if (isUndefined(value)) {
        return;
      }
      increaseStorageSizeIfNeeded(builder, 3);
      builder.storage[builder.numUsedElements++] = Keys.PADDING_PERCENT.ordinal();
      builder.storage[builder.numUsedElements++] = edge.intValue();
      builder.storage[builder.numUsedElements++] = value;
    }

    public static void setBorder(final Builder builder, Edge edge, float value) {
      if (isUndefined(value)) {
        return;
      }
      increaseStorageSizeIfNeeded(builder, 3);
      builder.storage[builder.numUsedElements++] = Keys.BORDER.ordinal();
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
          case POINT_SCALE_FACTOR:
            {
              final float pointScaleFactor = storage[i + 1];
              styleStr.append(" pointScalingFactor: ").append(pointScaleFactor).append("\n");
              break;
            }
          case DIRECTION:
            final Direction direction = Direction.fromInt((int) storage[i + 1]);
            styleStr.append("  direction: ").append(direction).append("\n");
            i += 2;
            break;
          case FLEX_DIRECTION:
            final FlexDirection flexDirection = FlexDirection.fromInt((int) storage[i + 1]);
            styleStr.append("  flexDirection: ").append(flexDirection).append("\n");
            i += 2;
            break;
          case JUSTIFY_CONTENT:
            final Justify justify = Justify.fromInt((int) storage[i + 1]);
            styleStr.append("  justifyContent: ").append(justify).append("\n");
            i += 2;
            break;
          case ALIGN_CONTENT:
            final AlignContent alignContent = AlignContent.values()[(int) storage[i + 1]];
            styleStr.append("  alignContent: ").append(alignContent).append("\n");
            i += 2;
            break;
          case ALIGN_ITEMS:
            final AlignItems alignItems = AlignItems.values()[(int) storage[i + 1]];
            styleStr.append("  alignItems: ").append(alignItems).append("\n");
            i += 2;
            break;
          case FLEX_WRAP:
            final Wrap flexWrap = Wrap.values()[(int) storage[i + 1]];
            styleStr.append("  flexWrap: ").append(flexWrap).append("\n");
            i += 2;
            break;
          case OVERFLOW:
            final Overflow overflow = Overflow.values()[(int) storage[i + 1]];
            styleStr.append("  overflow: ").append(overflow).append("\n");
            i += 2;
            break;
          case PADDING:
            {
              final Edge edge = Edge.fromInt((int) storage[i + 1]);
              final float padding = storage[i + 2];
              styleStr.append("  padding").append(edge).append(": ").append(padding).append("\n");
              i += 3;
            }
            break;
          case PADDING_PERCENT:
            {
              final Edge edge = Edge.fromInt((int) storage[i + 1]);
              final float padding = storage[i + 2];
              styleStr.append("  padding").append(edge).append(": ").append(padding).append("%\n");
              i += 3;
            }
            break;
          case BORDER:
            final Edge edge = Edge.fromInt((int) storage[i + 1]);
            final float border = storage[i + 2];
            styleStr.append("  border").append(edge).append(": ").append(border).append("\n");
            i += 3;
            break;
        }
      }

      return styleStr.length() > 0 ? "{\n" + styleStr.toString() + "}" : "";
    }
  }

  // This should be in sync with FlexBoxStyleKeys in the JNI code
  public enum Keys {
    DIRECTION,
    FLEX_DIRECTION,
    JUSTIFY_CONTENT,
    ALIGN_CONTENT,
    ALIGN_ITEMS,
    FLEX_WRAP,
    OVERFLOW,
    PADDING,
    PADDING_PERCENT,
    BORDER,
    POINT_SCALE_FACTOR
  }
}
