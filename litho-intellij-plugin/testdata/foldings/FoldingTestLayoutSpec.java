@com.facebook.litho.annotations.LayoutSpec
public class FoldingTestLayoutSpec {
  @com.facebook.litho.annotations.PropDefault
  final static String withDefault = "Hello";

  private void one(@com.facebook.litho.annotations.Prop String <fold text='withDefault: "Hello"'>withDefault</fold>, 
                   @com.facebook.litho.annotations.Prop int withoutDefault) {}
}
