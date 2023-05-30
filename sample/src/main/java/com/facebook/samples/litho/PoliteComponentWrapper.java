@LayoutSpec
class PoliteComponentWrapper {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Component content) {

    return Wrapper.create(c)
        .delegate(content)
        .onPopulateAccessibilityEventHandler(
            PoliteComponentWrapper.onPopulateAccessibilityEvent(c))
        .build();
  }

  @OnEvent(OnPopulateAccessibilityEvent.class)
  static void onPopulateAccessibilityEvent(
      ComponentContext c,
      @FromEvent AccessibilityDelegateCompat superDelegate,
      @FromEvent View view
      @FromEvent AccessibilityEvent event) {
    superDelegate.onPopulateAccessibilityEvent(view, event);
    event.getText().add("please");
  }
}