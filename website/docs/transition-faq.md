---
id: transition-faq
title: FAQ
---

#### How to debug Litho Transitions?

1. Go to `litho-core/src/main/java/com/facebook/litho/AnimationsDebug.java` and set value of `ENABLED` flag to `True`.
2. Look for log messages with tag `"LithoAnimationDebug"`

This will let you see how the framework does layout diffing, recognizes what transition key/property pairs need to be animated, picks up start and end values, etc.