
@com.facebook.litho.annotations.LayoutSpec
public class DefaultPropertyFoldingSpec {
  @com.facebook.litho.annotations.PropDefault
  String withDefault = "Hello";

  private void one(@com.facebook.litho.annotations.Prop String <fold text='withDefault: "Hello"'>withDefault</fold>, 
                   @com.facebook.litho.annotations.Prop int withoutDefault) {}
}
