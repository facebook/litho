public class DefaultPropertyFoldingTest {
  @com.facebook.litho.annotations.PropDefault
  String var = "Hello";

  private void one(@com.facebook.litho.annotations.Prop String <fold text='var: "Hello"'>var</fold>) {}
}
