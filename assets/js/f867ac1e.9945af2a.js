"use strict";(self.webpackChunk=self.webpackChunk||[]).push([[3454,6972,2304,4882,6127,7940,1646,4980,6206],{3905:(e,n,t)=>{t.r(n),t.d(n,{MDXContext:()=>c,MDXProvider:()=>m,mdx:()=>f,useMDXComponents:()=>d,withMDXComponents:()=>p});var a=t(67294);function o(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function r(){return r=Object.assign||function(e){for(var n=1;n<arguments.length;n++){var t=arguments[n];for(var a in t)Object.prototype.hasOwnProperty.call(t,a)&&(e[a]=t[a])}return e},r.apply(this,arguments)}function l(e,n){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);n&&(a=a.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),t.push.apply(t,a)}return t}function i(e){for(var n=1;n<arguments.length;n++){var t=null!=arguments[n]?arguments[n]:{};n%2?l(Object(t),!0).forEach((function(n){o(e,n,t[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):l(Object(t)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(t,n))}))}return e}function s(e,n){if(null==e)return{};var t,a,o=function(e,n){if(null==e)return{};var t,a,o={},r=Object.keys(e);for(a=0;a<r.length;a++)t=r[a],n.indexOf(t)>=0||(o[t]=e[t]);return o}(e,n);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);for(a=0;a<r.length;a++)t=r[a],n.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(o[t]=e[t])}return o}var c=a.createContext({}),p=function(e){return function(n){var t=d(n.components);return a.createElement(e,r({},n,{components:t}))}},d=function(e){var n=a.useContext(c),t=n;return e&&(t="function"==typeof e?e(n):i(i({},n),e)),t},m=function(e){var n=d(e.components);return a.createElement(c.Provider,{value:n},e.children)},u={inlineCode:"code",wrapper:function(e){var n=e.children;return a.createElement(a.Fragment,{},n)}},v=a.forwardRef((function(e,n){var t=e.components,o=e.mdxType,r=e.originalType,l=e.parentName,c=s(e,["components","mdxType","originalType","parentName"]),p=d(t),m=o,v=p["".concat(l,".").concat(m)]||p[m]||u[m]||r;return t?a.createElement(v,i(i({ref:n},c),{},{components:t})):a.createElement(v,i({ref:n},c))}));function f(e,n){var t=arguments,o=n&&n.mdxType;if("string"==typeof e||o){var r=t.length,l=new Array(r);l[0]=v;var i={};for(var s in n)hasOwnProperty.call(n,s)&&(i[s]=n[s]);i.originalType=e,i.mdxType="string"==typeof e?e:o,l[1]=i;for(var c=2;c<r;c++)l[c]=t[c];return a.createElement.apply(null,l)}return a.createElement.apply(null,t)}v.displayName="MDXCreateElement"},85162:(e,n,t)=>{t.r(n),t.d(n,{default:()=>l});var a=t(67294),o=t(34334);const r="tabItem_Ymn6";function l(e){var n=e.children,t=e.hidden,l=e.className;return a.createElement("div",{role:"tabpanel",className:(0,o.Z)(r,l),hidden:t},n)}},65488:(e,n,t)=>{t.r(n),t.d(n,{default:()=>u});var a=t(83117),o=t(67294),r=t(34334),l=t(72389),i=t(67392),s=t(7094),c=t(12466);const p="tabList__CuJ",d="tabItem_LNqP";function m(e){var n,t,l=e.lazy,m=e.block,u=e.defaultValue,v=e.values,f=e.groupId,h=e.className,y=o.Children.map(e.children,(function(e){if((0,o.isValidElement)(e)&&"value"in e.props)return e;throw new Error("Docusaurus error: Bad <Tabs> child <"+("string"==typeof e.type?e.type:e.type.name)+'>: all children of the <Tabs> component should be <TabItem>, and every <TabItem> should have a unique "value" prop.')})),g=null!=v?v:y.map((function(e){var n=e.props;return{value:n.value,label:n.label,attributes:n.attributes}})),b=(0,i.l)(g,(function(e,n){return e.value===n.value}));if(b.length>0)throw new Error('Docusaurus error: Duplicate values "'+b.map((function(e){return e.value})).join(", ")+'" found in <Tabs>. Every value needs to be unique.');var k=null===u?u:null!=(n=null!=u?u:null==(t=y.find((function(e){return e.props.default})))?void 0:t.props.value)?n:y[0].props.value;if(null!==k&&!g.some((function(e){return e.value===k})))throw new Error('Docusaurus error: The <Tabs> has a defaultValue "'+k+'" but none of its children has the corresponding value. Available values are: '+g.map((function(e){return e.value})).join(", ")+". If you intend to show no default tab, use defaultValue={null} instead.");var x=(0,s.U)(),C=x.tabGroupChoices,E=x.setTabGroupChoices,S=(0,o.useState)(k),T=S[0],j=S[1],N=[],O=(0,c.o5)().blockElementScrollPositionUntilNextRender;if(null!=f){var w=C[f];null!=w&&w!==T&&g.some((function(e){return e.value===w}))&&j(w)}var P=function(e){var n=e.currentTarget,t=N.indexOf(n),a=g[t].value;a!==T&&(O(n),j(a),null!=f&&E(f,String(a)))},D=function(e){var n,t=null;switch(e.key){case"ArrowRight":var a,o=N.indexOf(e.currentTarget)+1;t=null!=(a=N[o])?a:N[0];break;case"ArrowLeft":var r,l=N.indexOf(e.currentTarget)-1;t=null!=(r=N[l])?r:N[N.length-1]}null==(n=t)||n.focus()};return o.createElement("div",{className:(0,r.Z)("tabs-container",p)},o.createElement("ul",{role:"tablist","aria-orientation":"horizontal",className:(0,r.Z)("tabs",{"tabs--block":m},h)},g.map((function(e){var n=e.value,t=e.label,l=e.attributes;return o.createElement("li",(0,a.Z)({role:"tab",tabIndex:T===n?0:-1,"aria-selected":T===n,key:n,ref:function(e){return N.push(e)},onKeyDown:D,onFocus:P,onClick:P},l,{className:(0,r.Z)("tabs__item",d,null==l?void 0:l.className,{"tabs__item--active":T===n})}),null!=t?t:n)}))),l?(0,o.cloneElement)(y.filter((function(e){return e.props.value===T}))[0],{className:"margin-top--md"}):o.createElement("div",{className:"margin-top--md"},y.map((function(e,n){return(0,o.cloneElement)(e,{key:n,hidden:e.props.value!==T})}))))}function u(e){var n=(0,l.default)();return o.createElement(m,(0,a.Z)({key:String(n)},e))}},7772:(e,n,t)=>{t.d(n,{Z:()=>u});var a=t(83117),o=t(67294),r=t(23746),l=t(7694),i=t(13618),s="0.47.0",c="0.48.0-SNAPSHOT",p="0.10.4",d="0.142.0",m=t(86668);const u=function(e){var n=e.language,t=e.code.replace(/{{site.lithoVersion}}/g,s).replace(/{{site.soloaderVersion}}/g,p).replace(/{{site.lithoSnapshotVersion}}/g,c).replace(/{{site.flipperVersion}}/g,d).trim(),u=(0,m.L)().isDarkTheme?i.Z:l.Z;return o.createElement(r.ZP,(0,a.Z)({},r.lG,{code:t,language:n,theme:u}),(function(e){var n=e.className,t=e.style,a=e.tokens,r=e.getLineProps,l=e.getTokenProps;return o.createElement("pre",{className:n,style:t},a.map((function(e,n){return o.createElement("div",r({line:e,key:n}),e.map((function(e,n){return o.createElement("span",l({token:e,key:n}))})))})))}))}},41748:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>m,contentTitle:()=>p,default:()=>f,frontMatter:()=>c,metadata:()=>d,toc:()=>u});var a=t(83117),o=t(80102),r=(t(67294),t(3905)),l=t(65488),i=t(85162),s=(t(7772),["components"]),c={id:"event-handling",title:"Event Handling"},p=void 0,d={unversionedId:"kotlin/event-handling",id:"kotlin/event-handling",title:"Event Handling",description:"There are three scenarios in which the use of Event Handlers is different in the Kotlin API:",source:"@site/../docs/kotlin/event-handling.mdx",sourceDirName:"kotlin",slug:"/kotlin/event-handling",permalink:"/docs/kotlin/event-handling",draft:!1,editUrl:"https://github.com/facebook/litho/edit/master/website/../docs/kotlin/event-handling.mdx",tags:[],version:"current",frontMatter:{id:"event-handling",title:"Event Handling"},sidebar:"mainSidebar",previous:{title:"Flexbox Containers",permalink:"/docs/kotlin/kotlin-flexbox-containers"},next:{title:"Cheatsheet",permalink:"/docs/kotlin/kotlin-api-cheatsheet"}},m={},u=[{value:"Supplying event handlers",id:"supplying-event-handlers",level:2},{value:"Events in common props",id:"events-in-common-props",level:3},{value:"Custom events in specs",id:"custom-events-in-specs",level:3},{value:"Accepting event handlers",id:"accepting-event-handlers",level:2}],v={toc:u};function f(e){var n=e.components,t=(0,o.Z)(e,s);return(0,r.mdx)("wrapper",(0,a.Z)({},v,t,{components:n,mdxType:"MDXLayout"}),(0,r.mdx)("p",null,"There are three scenarios in which the use of Event Handlers is different in the Kotlin API:"),(0,r.mdx)("ol",null,(0,r.mdx)("li",{parentName:"ol"},(0,r.mdx)("a",{parentName:"li",href:"#events-in-common-props"},"Events in common props")," - ",(0,r.mdx)("inlineCode",{parentName:"li"},"clickHandlers")," are replaced by ",(0,r.mdx)("inlineCode",{parentName:"li"},"lambdas"),"."),(0,r.mdx)("li",{parentName:"ol"},(0,r.mdx)("a",{parentName:"li",href:"#custom-events-in-specs"},"Custom events in specs")," - accepts EventHandlers, where ",(0,r.mdx)("inlineCode",{parentName:"li"},"eventHandler")," or ",(0,r.mdx)("inlineCode",{parentName:"li"},"eventHandlerWithReturn")," can be used."),(0,r.mdx)("li",{parentName:"ol"},(0,r.mdx)("a",{parentName:"li",href:"#accepting-event-handlers"},"Accepting event handlers")," - custom ",(0,r.mdx)("inlineCode",{parentName:"li"},"Event")," classes are replaced by ",(0,r.mdx)("inlineCode",{parentName:"li"},"lambdas")," passed as props.")),(0,r.mdx)("p",null,"Each of these scenarios is detailed in the following sections."),(0,r.mdx)("h2",{id:"supplying-event-handlers"},"Supplying event handlers"),(0,r.mdx)("h3",{id:"events-in-common-props"},"Events in common props"),(0,r.mdx)("p",null,"Events that were exposed in common props in the Java Spec API (such as ",(0,r.mdx)("inlineCode",{parentName:"p"},"clickHandler"),") are now exposed on ",(0,r.mdx)("inlineCode",{parentName:"p"},"Style"),"."),(0,r.mdx)("p",null,"Style properties accept ",(0,r.mdx)("a",{parentName:"p",href:"https://kotlinlang.org/docs/lambdas.html"},"lambdas")," instead of a reference to a generated ",(0,r.mdx)("inlineCode",{parentName:"p"},"EventHandler"),"."),(0,r.mdx)(l.default,{groupId:"event-handling",defaultValue:"kotlin",values:[{label:"Kotlin",value:"kotlin"},{label:"Java",value:"java"}],mdxType:"Tabs"},(0,r.mdx)(i.default,{value:"kotlin",mdxType:"TabItem"},(0,r.mdx)("pre",null,(0,r.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/EventComponent.kt start=start_example end=end_example",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/EventComponent.kt",start:"start_example",end:"end_example"},'class EventComponent : KComponent() {\n  override fun ComponentScope.render(): Component? {\n    return Column(\n        style = Style.onClick { onClick() }.width(80.dp).height(80.dp).backgroundColor(YELLOW))\n  }\n}\n\nprivate fun onClick() {\n  Log.d("EventComponent", "click")\n}\n'))),(0,r.mdx)(i.default,{value:"java",mdxType:"TabItem"},(0,r.mdx)("pre",null,(0,r.mdx)("code",{parentName:"pre",className:"language-java",metastring:"file=sample/src/main/java/com/facebook/samples/litho/java/events/EventComponentSpec.java start=start_example end=end_example",file:"sample/src/main/java/com/facebook/samples/litho/java/events/EventComponentSpec.java",start:"start_example",end:"end_example"},'@LayoutSpec\npublic class EventComponentSpec {\n\n  @OnCreateLayout\n  static Component onCreateLayout(ComponentContext c) {\n    return Column.create(c)\n        .clickHandler(EventComponent.onClickEvent(c))\n        .widthDip(80)\n        .heightDip(80)\n        .backgroundColor(YELLOW)\n        .build();\n  }\n\n  @OnEvent(ClickEvent.class)\n  static void onClickEvent(ComponentContext c) {\n    Log.d("EventComponentSpec", "click");\n  }\n}\n')))),(0,r.mdx)("h3",{id:"custom-events-in-specs"},"Custom events in specs"),(0,r.mdx)("p",null,"When using pre-existing Spec Components or Sections that accept custom events (such as RenderEvent in DataDiffSection), there is still a need to pass in an ",(0,r.mdx)("inlineCode",{parentName:"p"},"EventHandler")," for compatibility. Use either ",(0,r.mdx)("inlineCode",{parentName:"p"},"eventHandler")," or ",(0,r.mdx)("inlineCode",{parentName:"p"},"eventHandlerWithReturn"),", depending on whether the code handling the event needs to return a value. Both of these functions accept a lambda, which is invoked when the event occurs."),(0,r.mdx)(l.default,{groupId:"event-handling",defaultValue:"kotlin",values:[{label:"Kotlin",value:"kotlin"},{label:"Java",value:"java"}],mdxType:"Tabs"},(0,r.mdx)(i.default,{value:"kotlin",mdxType:"TabItem"},(0,r.mdx)("pre",null,(0,r.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/SectionComponent.kt start=start_example end=end_example",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/SectionComponent.kt",start:"start_example",end:"end_example"},"class SectionComponent(private val words: List<String>) : KComponent() {\n  override fun ComponentScope.render(): Component? {\n    return Column(style = Style.width(80.dp).height(80.dp)) {\n      child(\n          RecyclerCollectionComponent(\n              section =\n                  DataDiffSection.create<String>(SectionContext(context))\n                      .data(words)\n                      .renderEventHandler(eventHandlerWithReturn { event -> onRender(event) })\n                      .build()))\n    }\n  }\n\n  private fun ResourcesScope.onRender(event: RenderEvent<String>): RenderInfo {\n    return ComponentRenderInfo.create().component(Text(text = event.model)).build()\n  }\n}\n"))),(0,r.mdx)(i.default,{value:"java",mdxType:"TabItem"},(0,r.mdx)("pre",null,(0,r.mdx)("code",{parentName:"pre",className:"language-java",metastring:"file=sample/src/main/java/com/facebook/samples/litho/java/events/SectionComponentSpec.java start=start_example end=end_example",file:"sample/src/main/java/com/facebook/samples/litho/java/events/SectionComponentSpec.java",start:"start_example",end:"end_example"},"@LayoutSpec\nclass SectionComponentSpec {\n\n  @OnCreateLayout\n  static Component onCreateLayout(ComponentContext c, @Prop List<String> words) {\n    return Column.create(c)\n        .widthDip(80)\n        .heightDip(80)\n        .child(\n            RecyclerCollectionComponent.create(c)\n                .section(\n                    DataDiffSection.<String>create(new SectionContext(c))\n                        .data(words)\n                        .renderEventHandler(SectionComponent.onRender(c))))\n        .build();\n  }\n\n  @OnEvent(RenderEvent.class)\n  static RenderInfo onRender(ComponentContext context, @FromEvent String model) {\n    return ComponentRenderInfo.create().component(Text.create(context).text(model)).build();\n  }\n}\n")))),(0,r.mdx)("h2",{id:"accepting-event-handlers"},"Accepting event handlers"),(0,r.mdx)("p",null,"In the Java Spec API, Spec-accepted custom event handlers could be accepted by creating an Event class, and then either providing a value to the ",(0,r.mdx)("inlineCode",{parentName:"p"},"events")," param in the ",(0,r.mdx)("inlineCode",{parentName:"p"},"@LayoutSpec")," annotation or accepting an ",(0,r.mdx)("inlineCode",{parentName:"p"},"EventHandler")," as a prop, as detailed in the ",(0,r.mdx)("a",{parentName:"p",href:"/docs/codegen/events-for-specs"},"Events for Specs")," page. In the Kotlin API, simply accept a lambda as a prop to be invoked when the event happens:"),(0,r.mdx)(l.default,{groupId:"event-handling",defaultValue:"kotlin",values:[{label:"Kotlin",value:"kotlin"},{label:"Java",value:"java"}],mdxType:"Tabs"},(0,r.mdx)(i.default,{value:"kotlin",mdxType:"TabItem"},(0,r.mdx)("pre",null,(0,r.mdx)("code",{parentName:"pre",className:"language-kotlin",metastring:"file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/ClickEventComponent.kt start=start_example end=end_example",file:"sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/ClickEventComponent.kt",start:"start_example",end:"end_example"},'class ClickEventComponent(\n    private val onButtonClicked: ((String) -> Unit),\n) : KComponent() {\n  override fun ComponentScope.render(): Component? {\n    return Column {\n      child(Text(text = "OK", style = Style.onClick { onButtonClicked("OK clicked") }))\n      child(Text(text = "Cancel", style = Style.onClick { onButtonClicked("Cancel clicked") }))\n    }\n  }\n}\n'))),(0,r.mdx)(i.default,{value:"java",mdxType:"TabItem"},(0,r.mdx)("pre",null,(0,r.mdx)("code",{parentName:"pre",className:"language-java",metastring:"file=sample/src/main/java/com/facebook/samples/litho/java/events/ClickEventComponentSpec.java start=start_example end=end_example",file:"sample/src/main/java/com/facebook/samples/litho/java/events/ClickEventComponentSpec.java",start:"start_example",end:"end_example"},'@LayoutSpec(events = {ClickTextEvent.class})\nclass ClickEventComponentSpec {\n\n  @OnCreateLayout\n  static Component onCreateLayout(ComponentContext c) {\n    return Column.create(c)\n        .child(Text.create(c).text("OK").clickHandler(ClickEventComponent.onButtonClick(c, "OK")))\n        .child(\n            Text.create(c)\n                .text("Cancel")\n                .clickHandler(ClickEventComponent.onButtonClick(c, "Cancel")))\n        .build();\n  }\n\n  @OnEvent(ClickEvent.class)\n  protected static void onButtonClick(ComponentContext c, @Param String text) {\n    EventHandler handler = ClickEventComponent.getClickTextEventHandler(c);\n    if (handler != null) {\n      ClickEventComponent.dispatchClickTextEvent(handler, text);\n    }\n  }\n}\n')))))}f.isMDXComponent=!0},23746:(e,n,t)=>{t.d(n,{ZP:()=>v,lG:()=>l});var a=t(87410);const o={plain:{backgroundColor:"#2a2734",color:"#9a86fd"},styles:[{types:["comment","prolog","doctype","cdata","punctuation"],style:{color:"#6c6783"}},{types:["namespace"],style:{opacity:.7}},{types:["tag","operator","number"],style:{color:"#e09142"}},{types:["property","function"],style:{color:"#9a86fd"}},{types:["tag-id","selector","atrule-id"],style:{color:"#eeebff"}},{types:["attr-name"],style:{color:"#c4b9fe"}},{types:["boolean","string","entity","url","attr-value","keyword","control","directive","unit","statement","regex","atrule","placeholder","variable"],style:{color:"#ffcc99"}},{types:["deleted"],style:{textDecorationLine:"line-through"}},{types:["inserted"],style:{textDecorationLine:"underline"}},{types:["italic"],style:{fontStyle:"italic"}},{types:["important","bold"],style:{fontWeight:"bold"}},{types:["important"],style:{color:"#c4b9fe"}}]};var r=t(67294),l={Prism:a.Z,theme:o};function i(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function s(){return s=Object.assign||function(e){for(var n=1;n<arguments.length;n++){var t=arguments[n];for(var a in t)Object.prototype.hasOwnProperty.call(t,a)&&(e[a]=t[a])}return e},s.apply(this,arguments)}var c=/\r\n|\r|\n/,p=function(e){0===e.length?e.push({types:["plain"],content:"\n",empty:!0}):1===e.length&&""===e[0].content&&(e[0].content="\n",e[0].empty=!0)},d=function(e,n){var t=e.length;return t>0&&e[t-1]===n?e:e.concat(n)},m=function(e,n){var t=e.plain,a=Object.create(null),o=e.styles.reduce((function(e,t){var a=t.languages,o=t.style;return a&&!a.includes(n)||t.types.forEach((function(n){var t=s({},e[n],o);e[n]=t})),e}),a);return o.root=t,o.plain=s({},t,{backgroundColor:null}),o};function u(e,n){var t={};for(var a in e)Object.prototype.hasOwnProperty.call(e,a)&&-1===n.indexOf(a)&&(t[a]=e[a]);return t}const v=function(e){function n(){for(var n=this,t=[],a=arguments.length;a--;)t[a]=arguments[a];e.apply(this,t),i(this,"getThemeDict",(function(e){if(void 0!==n.themeDict&&e.theme===n.prevTheme&&e.language===n.prevLanguage)return n.themeDict;n.prevTheme=e.theme,n.prevLanguage=e.language;var t=e.theme?m(e.theme,e.language):void 0;return n.themeDict=t})),i(this,"getLineProps",(function(e){var t=e.key,a=e.className,o=e.style,r=s({},u(e,["key","className","style","line"]),{className:"token-line",style:void 0,key:void 0}),l=n.getThemeDict(n.props);return void 0!==l&&(r.style=l.plain),void 0!==o&&(r.style=void 0!==r.style?s({},r.style,o):o),void 0!==t&&(r.key=t),a&&(r.className+=" "+a),r})),i(this,"getStyleForToken",(function(e){var t=e.types,a=e.empty,o=t.length,r=n.getThemeDict(n.props);if(void 0!==r){if(1===o&&"plain"===t[0])return a?{display:"inline-block"}:void 0;if(1===o&&!a)return r[t[0]];var l=a?{display:"inline-block"}:{},i=t.map((function(e){return r[e]}));return Object.assign.apply(Object,[l].concat(i))}})),i(this,"getTokenProps",(function(e){var t=e.key,a=e.className,o=e.style,r=e.token,l=s({},u(e,["key","className","style","token"]),{className:"token "+r.types.join(" "),children:r.content,style:n.getStyleForToken(r),key:void 0});return void 0!==o&&(l.style=void 0!==l.style?s({},l.style,o):o),void 0!==t&&(l.key=t),a&&(l.className+=" "+a),l})),i(this,"tokenize",(function(e,n,t,a){var o={code:n,grammar:t,language:a,tokens:[]};e.hooks.run("before-tokenize",o);var r=o.tokens=e.tokenize(o.code,o.grammar,o.language);return e.hooks.run("after-tokenize",o),r}))}return e&&(n.__proto__=e),n.prototype=Object.create(e&&e.prototype),n.prototype.constructor=n,n.prototype.render=function(){var e=this.props,n=e.Prism,t=e.language,a=e.code,o=e.children,r=this.getThemeDict(this.props),l=n.languages[t];return o({tokens:function(e){for(var n=[[]],t=[e],a=[0],o=[e.length],r=0,l=0,i=[],s=[i];l>-1;){for(;(r=a[l]++)<o[l];){var m=void 0,u=n[l],v=t[l][r];if("string"==typeof v?(u=l>0?u:["plain"],m=v):(u=d(u,v.type),v.alias&&(u=d(u,v.alias)),m=v.content),"string"==typeof m){var f=m.split(c),h=f.length;i.push({types:u,content:f[0]});for(var y=1;y<h;y++)p(i),s.push(i=[]),i.push({types:u,content:f[y]})}else l++,n.push(u),t.push(m),a.push(0),o.push(m.length)}l--,n.pop(),t.pop(),a.pop(),o.pop()}return p(i),s}(void 0!==l?this.tokenize(n,a,l,t):[a]),className:"prism-code language-"+t,style:void 0!==r?r.root:{},getLineProps:this.getLineProps,getTokenProps:this.getTokenProps})},n}(r.Component)},13618:(e,n,t)=>{t.d(n,{Z:()=>a});const a={plain:{color:"#F8F8F2",backgroundColor:"#282A36"},styles:[{types:["prolog","constant","builtin"],style:{color:"rgb(189, 147, 249)"}},{types:["inserted","function"],style:{color:"rgb(80, 250, 123)"}},{types:["deleted"],style:{color:"rgb(255, 85, 85)"}},{types:["changed"],style:{color:"rgb(255, 184, 108)"}},{types:["punctuation","symbol"],style:{color:"rgb(248, 248, 242)"}},{types:["string","char","tag","selector"],style:{color:"rgb(255, 121, 198)"}},{types:["keyword","variable"],style:{color:"rgb(189, 147, 249)",fontStyle:"italic"}},{types:["comment"],style:{color:"rgb(98, 114, 164)"}},{types:["attr-name"],style:{color:"rgb(241, 250, 140)"}}]}},7694:(e,n,t)=>{t.d(n,{Z:()=>a});const a={plain:{color:"#393A34",backgroundColor:"#f6f8fa"},styles:[{types:["comment","prolog","doctype","cdata"],style:{color:"#999988",fontStyle:"italic"}},{types:["namespace"],style:{opacity:.7}},{types:["string","attr-value"],style:{color:"#e3116c"}},{types:["punctuation","operator"],style:{color:"#393A34"}},{types:["entity","url","symbol","number","boolean","variable","constant","property","regex","inserted"],style:{color:"#36acaa"}},{types:["atrule","keyword","attr-name","selector"],style:{color:"#00a4db"}},{types:["function","deleted","tag"],style:{color:"#d73a49"}},{types:["function-variable"],style:{color:"#6f42c1"}},{types:["tag","selector","keyword"],style:{color:"#00009f"}}]}}}]);