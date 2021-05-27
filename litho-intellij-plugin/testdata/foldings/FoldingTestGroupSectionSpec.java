@com.facebook.litho.sections.annotations.GroupSectionSpec
public class FoldingTestGroupSectionSpec {
  @com.facebook.litho.annotations.PropDefault
  final static String withDefault = "Hello";

  private void one(@com.facebook.litho.annotations.Prop String <fold text='withDefault: "Hello"'>withDefault</fold>, 
                   @com.facebook.litho.annotations.Prop int withoutDefault) {}
}
