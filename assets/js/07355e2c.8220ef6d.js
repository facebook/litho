"use strict";(self.webpackChunk=self.webpackChunk||[]).push([[7283],{74646:(e,n,a)=>{a.d(n,{A:()=>u});var i=a(58168),t=a(96540),o=a(68382),m=a(45603),l=a(76062),d="0.50.2",r="0.51.0-SNAPSHOT",p="0.10.5",s="0.142.0",c=a(6342);const u=function(e){var n=e.language,a=e.code.replace(/{{site.lithoVersion}}/g,d).replace(/{{site.soloaderVersion}}/g,p).replace(/{{site.lithoSnapshotVersion}}/g,r).replace(/{{site.flipperVersion}}/g,s).trim(),u=(0,c.p)().isDarkTheme?l.A:m.A;return t.createElement(o.Ay,(0,i.A)({},o.Gs,{code:a,language:n,theme:u}),(function(e){var n=e.className,a=e.style,i=e.tokens,o=e.getLineProps,m=e.getTokenProps;return t.createElement("pre",{className:n,style:a},i.map((function(e,n){return t.createElement("div",o({line:e,key:n}),e.map((function(e,n){return t.createElement("span",m({token:e,key:n}))})))})))}))}},43189:(e,n,a)=>{a.r(n),a.d(n,{assets:()=>s,contentTitle:()=>r,default:()=>y,frontMatter:()=>d,metadata:()=>p,toc:()=>c});var i=a(58168),t=a(98587),o=(a(96540),a(15680)),m=a(86025),l=(a(11470),a(19365),a(74646),["components"]),d={id:"dynamic-props-bindto",title:"Animating View Properties with Dynamic Props"},r=void 0,p={unversionedId:"animations/dynamic-props-bindto",id:"animations/dynamic-props-bindto",title:"Animating View Properties with Dynamic Props",description:"Dynamic props are properties that are applied directly to a View or Drawable. They are updated without computing a layout or remounting. This makes them efficient for use in animations or other dynamic UIs. Dynamic props are initialised and updated using DynamicValue.",source:"@site/../docs/animations/dynamic-props-bindto.mdx",sourceDirName:"animations",slug:"/animations/dynamic-props-bindto",permalink:"/docs/animations/dynamic-props-bindto",draft:!1,editUrl:"https://github.com/facebook/litho/edit/master/website/../docs/animations/dynamic-props-bindto.mdx",tags:[],version:"current",frontMatter:{id:"dynamic-props-bindto",title:"Animating View Properties with Dynamic Props"},sidebar:"mainSidebar",previous:{title:"Customizing Transitions",permalink:"/docs/animations/transition-choreography"},next:{title:"Transition Key Scoping",permalink:"/docs/animations/transition-key-types"}},s={},c=[{value:"<code>DynamicValue</code>",id:"dynamicvalue",level:2},{value:"Common dynamic props",id:"common-dynamic-props",level:2},{value:"Custom dynamic drops for <code>PrimitiveComponent</code>",id:"custom-dynamic-drops-for-primitivecomponent",level:2},{value:"<code>bindDynamic</code> - dynamic props in practice",id:"binddynamic---dynamic-props-in-practice",level:3},{value:"Key points for the <code>bindDynamic</code>",id:"key-points-for-the-binddynamic",level:3},{value:"Animating dynamic props",id:"animating-dynamic-props",level:2}],u={toc:c},h="wrapper";function y(e){var n=e.components,a=(0,t.A)(e,l);return(0,o.mdx)(h,(0,i.A)({},u,a,{components:n,mdxType:"MDXLayout"}),(0,o.mdx)("admonition",{type:"note"},(0,o.mdx)("p",{parentName:"admonition"},"Dynamic props are properties that are applied directly to a ",(0,o.mdx)("inlineCode",{parentName:"p"},"View")," or ",(0,o.mdx)("inlineCode",{parentName:"p"},"Drawable"),". They are updated without computing a layout or remounting. This makes them efficient for use in animations or other dynamic UIs. Dynamic props are initialised and updated using ",(0,o.mdx)("a",{parentName:"p",href:"pathname:///javadoc/com/facebook/litho/DynamicValue.html"},"DynamicValue"),".")),(0,o.mdx)("h2",{id:"dynamicvalue"},(0,o.mdx)("inlineCode",{parentName:"h2"},"DynamicValue")),(0,o.mdx)("p",null,"In ",(0,o.mdx)("inlineCode",{parentName:"p"},"KComponents"),", a ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue")," can be created using ",(0,o.mdx)("inlineCode",{parentName:"p"},"useBinding()"),". You can then keep a reference to the ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue")," and use it to directly set values (such as in a callback or an ",(0,o.mdx)("inlineCode",{parentName:"p"},"Animator"),"). Use the ",(0,o.mdx)("inlineCode",{parentName:"p"},"set()")," function to set new values from the main thread."),(0,o.mdx)("admonition",{type:"caution"},(0,o.mdx)("p",{parentName:"admonition"},(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue"),"s should only be updated on the main thread.")),(0,o.mdx)("h2",{id:"common-dynamic-props"},"Common dynamic props"),(0,o.mdx)("p",null,"The dynamic properties that are available for all ",(0,o.mdx)("inlineCode",{parentName:"p"},"Component"),"s are:"),(0,o.mdx)("ul",null,(0,o.mdx)("li",{parentName:"ul"},"Alpha"),(0,o.mdx)("li",{parentName:"ul"},"Scale X/Y"),(0,o.mdx)("li",{parentName:"ul"},"Translation X/Y"),(0,o.mdx)("li",{parentName:"ul"},"Background Color"),(0,o.mdx)("li",{parentName:"ul"},"Foreground Color"),(0,o.mdx)("li",{parentName:"ul"},"Rotation"),(0,o.mdx)("li",{parentName:"ul"},"Elevation")),(0,o.mdx)("p",null,"For ",(0,o.mdx)("inlineCode",{parentName:"p"},"KComponent"),"s they should be applied as a ",(0,o.mdx)("inlineCode",{parentName:"p"},"Style")," item:"),(0,o.mdx)("pre",null,(0,o.mdx)("code",{parentName:"pre",className:"language-kotlin"},"MyKComponent(style = Style.alpha(dynamicAlpha))\n")),(0,o.mdx)("p",null,"The ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue")," can be updated by calling its ",(0,o.mdx)("a",{parentName:"p",href:"pathname:///javadoc/com/facebook/litho/DynamicValue.html#set-T-"},"set()")," function."),(0,o.mdx)("p",null,"The following code sample shows a ",(0,o.mdx)("inlineCode",{parentName:"p"},"Component")," that renders a square in the middle of the screen. The ",(0,o.mdx)("inlineCode",{parentName:"p"},"alpha")," and ",(0,o.mdx)("inlineCode",{parentName:"p"},"scale")," props have been set to the ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue"),"s, which are updated by two ",(0,o.mdx)("inlineCode",{parentName:"p"},"SeekBar"),"s."),(0,o.mdx)("pre",null,(0,o.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/animations/dynamicprops/CommonDynamicPropsKComponent.kt  start=start_example end=end_example",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/animations/dynamicprops/CommonDynamicPropsKComponent.kt","":!0,start:"start_example",end:"end_example"},"class CommonDynamicPropsKComponent : KComponent() {\n\n  override fun ComponentScope.render(): Component? {\n    val scale = useBinding(1f)\n    val alpha = useBinding(1f)\n\n    val square =\n        Column(\n            style =\n                Style.width(100.dp)\n                    .height(100.dp)\n                    .backgroundColor(colorRes(R.color.primaryColor))\n                    .alignSelf(YogaAlign.CENTER)\n                    .scaleX(scale)\n                    .scaleY(scale)\n                    .alpha(alpha))\n\n    return Column(justifyContent = YogaJustify.SPACE_BETWEEN, style = Style.padding(all = 20.dp)) {\n      child(SeekBar(onProgressChanged = { alpha.set(it) }))\n      child(square)\n      child(SeekBar(onProgressChanged = { scale.set(it) }))\n    }\n  }\n}\n")),(0,o.mdx)("p",null,"The following short animation illustrates the component in action."),(0,o.mdx)("video",{loop:"true",autoplay:"true",class:"video",width:"100%",height:"500px",muted:"true"},(0,o.mdx)("source",{type:"video/webm",src:(0,m.default)("/videos/common_dynamic_props.webm")}),(0,o.mdx)("p",null,"Your browser does not support the video element.")),(0,o.mdx)("p",null,"To see how other common dynamic props can be modified, see the ",(0,o.mdx)("a",{parentName:"p",href:"https://github.com/facebook/litho/tree/master/sample/src/main/java/com/facebook/samples/litho/kotlin/animations/dynamicprops/AllCommonDynamicPropsKComponent.kt"},"All Common Dynamic Props")," example in the Sample app, which is illustrated in the following animation."),(0,o.mdx)("video",{loop:"true",autoplay:"true",class:"video",width:"100%",height:"500px",muted:"true"},(0,o.mdx)("source",{type:"video/webm",src:(0,m.default)("/videos/all_dynamic_props.webm")}),(0,o.mdx)("p",null,"Your browser does not support the video element.")),(0,o.mdx)("h2",{id:"custom-dynamic-drops-for-primitivecomponent"},"Custom dynamic drops for ",(0,o.mdx)("inlineCode",{parentName:"h2"},"PrimitiveComponent")),(0,o.mdx)("p",null,"Dynamic Mount Props property types enable the value of the property, on the content mounted by the ",(0,o.mdx)("inlineCode",{parentName:"p"},"PrimitiveComponent"),", to be updated without triggering a new layout (such as when animating the text colour of a Text component)."),(0,o.mdx)("h3",{id:"binddynamic---dynamic-props-in-practice"},(0,o.mdx)("inlineCode",{parentName:"h3"},"bindDynamic")," - dynamic props in practice"),(0,o.mdx)("p",null,"To illustrate the use of ",(0,o.mdx)("inlineCode",{parentName:"p"},"bindDynamic")," in practice, you will implement a simple ",(0,o.mdx)("inlineCode",{parentName:"p"},"ImageViewComponent")," that will have ",(0,o.mdx)("inlineCode",{parentName:"p"},"background"),", ",(0,o.mdx)("inlineCode",{parentName:"p"},"rotation")," and ",(0,o.mdx)("inlineCode",{parentName:"p"},"scale")," properties animated, based on the ",(0,o.mdx)("inlineCode",{parentName:"p"},"SeekBar")," value."),(0,o.mdx)("p",null,"Start off by defining each ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue")," by using the ",(0,o.mdx)("inlineCode",{parentName:"p"},"useBinding")," hook and attaching it to the ",(0,o.mdx)("inlineCode",{parentName:"p"},"SeekBar.onProgressChanged")," callback that will change them accordingly:"),(0,o.mdx)("pre",null,(0,o.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/primitives/bindto/PrimitiveBindToExampleComponent.kt start=start_bindTo_seekbar_code end=end_bindTo_seekbar_code",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/primitives/bindto/PrimitiveBindToExampleComponent.kt",start:"start_bindTo_seekbar_code",end:"end_bindTo_seekbar_code"},'override fun ComponentScope.render(): Component? {\n  val background = useBinding(50f)\n  val rotation = useBinding(0f)\n  val scale = useBinding(1f)\n  return Column(style = Style.padding(all = 20.dp)) {\n    child(\n        SeekBar(\n            initialValue = 0f,\n            label = "background",\n            onProgressChanged = { backgroundValue -> background.set(backgroundValue) }))\n    child(\n        SeekBar(\n            initialValue = 0f,\n            label = "rotation",\n            onProgressChanged = { rotationValue ->\n              rotation.set(evaluate(rotationValue, 0f, 360f))\n            }))\n    child(\n        SeekBar(\n            initialValue = 1f,\n            label = "scale",\n            onProgressChanged = { scaleValue -> scale.set(evaluate(scaleValue, .75f, 1.25f)) }))\n')),(0,o.mdx)("p",null,"The ",(0,o.mdx)("inlineCode",{parentName:"p"},"PrimitiveComponent"),", ",(0,o.mdx)("inlineCode",{parentName:"p"},"ImageViewComponent"),", will be defined as a child below the ",(0,o.mdx)("inlineCode",{parentName:"p"},"SeekBar")," children in the ",(0,o.mdx)("inlineCode",{parentName:"p"},"render()")," function and will take each ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue")," as a constructor parameter:"),(0,o.mdx)("pre",null,(0,o.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/primitives/bindto/PrimitiveBindToExampleComponent.kt start=start_bindTo_imagecomponent_def end=end_bindTo_imagecomponent_def",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/primitives/bindto/PrimitiveBindToExampleComponent.kt",start:"start_bindTo_imagecomponent_def",end:"end_bindTo_imagecomponent_def"},"child(\n    Column(style = Style.width(100.dp).height(100.dp).margin(all = 50.dp)) {\n      child(ImageViewComponent(background = background, rotation = rotation, scale = scale))\n    })\n")),(0,o.mdx)("pre",null,(0,o.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/primitives/bindto/ImageViewComponent.kt start=start_bindTo_imagecomponent_code end=end_bindTo_imagecomponent_code",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/primitives/bindto/ImageViewComponent.kt",start:"start_bindTo_imagecomponent_code",end:"end_bindTo_imagecomponent_code"},"class ImageViewComponent(\n    private val rotation: DynamicValue<Float>,\n    private val background: DynamicValue<Float>,\n    private val scale: DynamicValue<Float>,\n    private val style: Style? = null\n) : PrimitiveComponent() {\n")),(0,o.mdx)("p",null,"Now, in the ",(0,o.mdx)("inlineCode",{parentName:"p"},"PrimitiveComponent.render()")," call, use the ",(0,o.mdx)("inlineCode",{parentName:"p"},"bindDynamic")," method to bind each ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue")," to the ",(0,o.mdx)("inlineCode",{parentName:"p"},"ImageView")," properties."),(0,o.mdx)("p",null,"There are two ways of using ",(0,o.mdx)("inlineCode",{parentName:"p"},"bindDynamic"),":"),(0,o.mdx)("ol",null,(0,o.mdx)("li",{parentName:"ol"},"The simpler way is to create a binding between the ",(0,o.mdx)("inlineCode",{parentName:"li"},"DynamicValue")," and function reference to the setter of the property.",(0,o.mdx)("ul",{parentName:"li"},(0,o.mdx)("li",{parentName:"ul"},"The setter will be invoked for every update of the ",(0,o.mdx)("inlineCode",{parentName:"li"},"DynamicValue"),"."))),(0,o.mdx)("li",{parentName:"ol"},"The more complex binding can be achieved by using a lambda and accessing the view directly, as shown in the following snippet.")),(0,o.mdx)("pre",null,(0,o.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/primitives/bindto/ImageViewComponent.kt start=start_bindTo_binding_code end=end_bindTo_binding_code",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/primitives/bindto/ImageViewComponent.kt",start:"start_bindTo_binding_code",end:"end_bindTo_binding_code"},"override fun PrimitiveComponentScope.render(): LithoPrimitive {\n  return LithoPrimitive(\n      layoutBehavior = ImageLayoutBehavior,\n      mountBehavior =\n          MountBehavior(ViewAllocator { context -> ImageView(context) }) {\n            bind(R.drawable.ic_launcher) { imageView ->\n              imageView.setImageDrawable(\n                  ContextCompat.getDrawable(context.androidContext, R.drawable.ic_launcher))\n              onUnbind { imageView.setImageResource(0) }\n            }\n\n            // simple binding\n            bindDynamic(rotation, ImageView::setRotation, 0f)\n            bindDynamic(scale, ImageView::setScaleX, 1f)\n            bindDynamic(scale, ImageView::setScaleY, 1f)\n\n            // complex binding\n            bindDynamic(background) { imageView: ImageView, value ->\n              imageView.setBackgroundColor(\n                  Color.HSVToColor(floatArrayOf(evaluate(value, 0f, 360f), 1f, 1f)))\n\n              onUnbindDynamic { imageView.setBackgroundColor(Color.BLACK) }\n            }\n          },\n      style)\n}\n")),(0,o.mdx)("p",null,"The following short video shows the ",(0,o.mdx)("inlineCode",{parentName:"p"},"bindDynamic")," in action:"),(0,o.mdx)("video",{loop:"true",autoplay:"true",class:"video",width:"100%",height:"500px",muted:"true"},(0,o.mdx)("source",{type:"video/webm",src:(0,m.default)("/videos/bindToAPI.mov")}),(0,o.mdx)("p",null,"Your browser does not support the video element.")),(0,o.mdx)("h3",{id:"key-points-for-the-binddynamic"},"Key points for the ",(0,o.mdx)("inlineCode",{parentName:"h3"},"bindDynamic")),(0,o.mdx)("ul",null,(0,o.mdx)("li",{parentName:"ul"},"A ",(0,o.mdx)("inlineCode",{parentName:"li"},"DynamicValue")," has to be bound to the ",(0,o.mdx)("inlineCode",{parentName:"li"},"PrimitiveComponent")," in ",(0,o.mdx)("inlineCode",{parentName:"li"},"MountConfigurationScope")," which is passed as a trailing lambda to ",(0,o.mdx)("inlineCode",{parentName:"li"},"MountBehavior"),"."),(0,o.mdx)("li",{parentName:"ul"},"A ",(0,o.mdx)("inlineCode",{parentName:"li"},"PrimitiveComponent")," can have several dynamic props."),(0,o.mdx)("li",{parentName:"ul"},"It is possible to automatically unbind the ",(0,o.mdx)("inlineCode",{parentName:"li"},"DynamicValue")," after ",(0,o.mdx)("inlineCode",{parentName:"li"},"unmount()")," is called by setting the default value or using ",(0,o.mdx)("inlineCode",{parentName:"li"},"onUnbindDynamic {}")," block.")),(0,o.mdx)("h2",{id:"animating-dynamic-props"},"Animating dynamic props"),(0,o.mdx)("p",null,"Dynamic props values can be used with Android Animators to create custom animations. The following example uses a ",(0,o.mdx)("a",{parentName:"p",href:"https://developer.android.com/reference/android/animation/ValueAnimator"},(0,o.mdx)("inlineCode",{parentName:"a"},"ValueAnimator"))," to animate the dynamic value ",(0,o.mdx)("inlineCode",{parentName:"p"},"time"),", defined in the previous value."),(0,o.mdx)("pre",null,(0,o.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/animations/dynamicprops/AnimateDynamicPropsKComponent.kt  start=start_example end=end_example",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/animations/dynamicprops/AnimateDynamicPropsKComponent.kt","":!0,start:"start_example",end:"end_example"},'class AnimateDynamicPropsKComponent : KComponent() {\n\n  override fun ComponentScope.render(): Component {\n    val time = useBinding(0L)\n    val animator = useRef<ValueAnimator?> { null }\n\n    val startAnimator: (ClickEvent) -> Unit = {\n      animator.value?.cancel()\n      animator.value =\n          ValueAnimator.ofInt(0, TimeUnit.HOURS.toMillis(12).toInt()).apply {\n            duration = 2000\n            interpolator = AccelerateDecelerateInterpolator()\n            addUpdateListener { time.set((it.animatedValue as Int).toLong()) }\n          }\n      animator.value?.start()\n    }\n\n    return Column(alignItems = YogaAlign.CENTER, style = Style.padding(all = 20.dp)) {\n      child(Text("Click to Start Animation", style = Style.onClick(action = startAnimator)))\n      child(\n          ClockFace.create(context)\n              .time(time)\n              .widthDip(200f)\n              .heightDip(200f)\n              .marginDip(YogaEdge.TOP, 20f)\n              .build())\n    }\n  }\n}\n')),(0,o.mdx)("p",null,"A ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue")," is used to represent time.  This is passed to the ",(0,o.mdx)("inlineCode",{parentName:"p"},"Component")," as a prop and kept as a reference to it so it can be updated. In a click event, a ",(0,o.mdx)("inlineCode",{parentName:"p"},"ValueAnimator")," is set up that updates the time ",(0,o.mdx)("inlineCode",{parentName:"p"},"DynamicValue")," each frame (see the following animation). The ",(0,o.mdx)("inlineCode",{parentName:"p"},"ValueAnimator")," is stored in a reference so that it can be cancelled if necessary."),(0,o.mdx)("video",{loop:"true",autoplay:"true",class:"video",width:"100%",height:"500px",muted:"true"},(0,o.mdx)("source",{type:"video/webm",src:(0,m.default)("/videos/custom_prop_animation.webm")}),(0,o.mdx)("p",null,"Your browser does not support the video element.")),(0,o.mdx)("p",null,"For more examples of creating Animations using Common Dynamic Props, see the ",(0,o.mdx)("a",{parentName:"p",href:"https://github.com/facebook/litho/tree/master/sample/src/main/java/com/facebook/samples/litho/java/animations/animationcookbook"},"Animations Cook Book")," in the Sample App."))}y.isMDXComponent=!0}}]);