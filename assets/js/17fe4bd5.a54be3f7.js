"use strict";(self.webpackChunk=self.webpackChunk||[]).push([[6860,1534,5701,2586,2793,2138,5425,3156,2875,1894],{3905:(e,t,n)=>{n.r(t),n.d(t,{MDXContext:()=>u,MDXProvider:()=>d,mdx:()=>y,useMDXComponents:()=>p,withMDXComponents:()=>c});var r=n(67294);function o(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function a(){return a=Object.assign||function(e){for(var t=1;t<arguments.length;t++){var n=arguments[t];for(var r in n)Object.prototype.hasOwnProperty.call(n,r)&&(e[r]=n[r])}return e},a.apply(this,arguments)}function i(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function l(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?i(Object(n),!0).forEach((function(t){o(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):i(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function s(e,t){if(null==e)return{};var n,r,o=function(e,t){if(null==e)return{};var n,r,o={},a=Object.keys(e);for(r=0;r<a.length;r++)n=a[r],t.indexOf(n)>=0||(o[n]=e[n]);return o}(e,t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(r=0;r<a.length;r++)n=a[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(o[n]=e[n])}return o}var u=r.createContext({}),c=function(e){return function(t){var n=p(t.components);return r.createElement(e,a({},t,{components:n}))}},p=function(e){var t=r.useContext(u),n=t;return e&&(n="function"==typeof e?e(t):l(l({},t),e)),n},d=function(e){var t=p(e.components);return r.createElement(u.Provider,{value:t},e.children)},m="mdxType",h={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},f=r.forwardRef((function(e,t){var n=e.components,o=e.mdxType,a=e.originalType,i=e.parentName,u=s(e,["components","mdxType","originalType","parentName"]),c=p(n),d=o,m=c["".concat(i,".").concat(d)]||c[d]||h[d]||a;return n?r.createElement(m,l(l({ref:t},u),{},{components:n})):r.createElement(m,l({ref:t},u))}));function y(e,t){var n=arguments,o=t&&t.mdxType;if("string"==typeof e||o){var a=n.length,i=new Array(a);i[0]=f;var l={};for(var s in t)hasOwnProperty.call(t,s)&&(l[s]=t[s]);l.originalType=e,l[m]="string"==typeof e?e:o,i[1]=l;for(var u=2;u<a;u++)i[u]=n[u];return r.createElement.apply(null,i)}return r.createElement.apply(null,n)}f.displayName="MDXCreateElement"},85162:(e,t,n)=>{n.r(t),n.d(t,{default:()=>i});var r=n(67294),o=n(86010);const a={tabItem:"tabItem_Ymn6"};function i(e){var t=e.children,n=e.hidden,i=e.className;return r.createElement("div",{role:"tabpanel",className:(0,o.default)(a.tabItem,i),hidden:n},t)}},74866:(e,t,n)=>{n.r(t),n.d(t,{default:()=>k});var r=n(87462),o=n(67294),a=n(86010),i=n(12466),l=n(16550),s=n(91980),u=n(67392),c=n(50012);function p(e){return function(e){return o.Children.map(e,(function(e){if((0,o.isValidElement)(e)&&"value"in e.props)return e;throw new Error("Docusaurus error: Bad <Tabs> child <"+("string"==typeof e.type?e.type:e.type.name)+'>: all children of the <Tabs> component should be <TabItem>, and every <TabItem> should have a unique "value" prop.')}))}(e).map((function(e){var t=e.props;return{value:t.value,label:t.label,attributes:t.attributes,default:t.default}}))}function d(e){var t=e.values,n=e.children;return(0,o.useMemo)((function(){var e=null!=t?t:p(n);return function(e){var t=(0,u.l)(e,(function(e,t){return e.value===t.value}));if(t.length>0)throw new Error('Docusaurus error: Duplicate values "'+t.map((function(e){return e.value})).join(", ")+'" found in <Tabs>. Every value needs to be unique.')}(e),e}),[t,n])}function m(e){var t=e.value;return e.tabValues.some((function(e){return e.value===t}))}function h(e){var t=e.queryString,n=void 0!==t&&t,r=e.groupId,a=(0,l.k6)(),i=function(e){var t=e.queryString,n=void 0!==t&&t,r=e.groupId;if("string"==typeof n)return n;if(!1===n)return null;if(!0===n&&!r)throw new Error('Docusaurus error: The <Tabs> component groupId prop is required if queryString=true, because this value is used as the search param name. You can also provide an explicit value such as queryString="my-search-param".');return null!=r?r:null}({queryString:n,groupId:r});return[(0,s._X)(i),(0,o.useCallback)((function(e){if(i){var t=new URLSearchParams(a.location.search);t.set(i,e),a.replace(Object.assign({},a.location,{search:t.toString()}))}}),[i,a])]}function f(e){var t,n,r,a,i=e.defaultValue,l=e.queryString,s=void 0!==l&&l,u=e.groupId,p=d(e),f=(0,o.useState)((function(){return function(e){var t,n=e.defaultValue,r=e.tabValues;if(0===r.length)throw new Error("Docusaurus error: the <Tabs> component requires at least one <TabItem> children component");if(n){if(!m({value:n,tabValues:r}))throw new Error('Docusaurus error: The <Tabs> has a defaultValue "'+n+'" but none of its children has the corresponding value. Available values are: '+r.map((function(e){return e.value})).join(", ")+". If you intend to show no default tab, use defaultValue={null} instead.");return n}var o=null!=(t=r.find((function(e){return e.default})))?t:r[0];if(!o)throw new Error("Unexpected error: 0 tabValues");return o.value}({defaultValue:i,tabValues:p})})),y=f[0],g=f[1],v=h({queryString:s,groupId:u}),b=v[0],w=v[1],k=(t=function(e){return e?"docusaurus.tab."+e:null}({groupId:u}.groupId),n=(0,c.Nk)(t),r=n[0],a=n[1],[r,(0,o.useCallback)((function(e){t&&a.set(e)}),[t,a])]),x=k[0],O=k[1],T=function(){var e=null!=b?b:x;return m({value:e,tabValues:p})?e:null}();return(0,o.useLayoutEffect)((function(){T&&g(T)}),[T]),{selectedValue:y,selectValue:(0,o.useCallback)((function(e){if(!m({value:e,tabValues:p}))throw new Error("Can't select invalid tab value="+e);g(e),w(e),O(e)}),[w,O,p]),tabValues:p}}var y=n(72389);const g={tabList:"tabList__CuJ",tabItem:"tabItem_LNqP"};function v(e){var t=e.className,n=e.block,l=e.selectedValue,s=e.selectValue,u=e.tabValues,c=[],p=(0,i.o5)().blockElementScrollPositionUntilNextRender,d=function(e){var t=e.currentTarget,n=c.indexOf(t),r=u[n].value;r!==l&&(p(t),s(r))},m=function(e){var t,n=null;switch(e.key){case"Enter":d(e);break;case"ArrowRight":var r,o=c.indexOf(e.currentTarget)+1;n=null!=(r=c[o])?r:c[0];break;case"ArrowLeft":var a,i=c.indexOf(e.currentTarget)-1;n=null!=(a=c[i])?a:c[c.length-1]}null==(t=n)||t.focus()};return o.createElement("ul",{role:"tablist","aria-orientation":"horizontal",className:(0,a.default)("tabs",{"tabs--block":n},t)},u.map((function(e){var t=e.value,n=e.label,i=e.attributes;return o.createElement("li",(0,r.Z)({role:"tab",tabIndex:l===t?0:-1,"aria-selected":l===t,key:t,ref:function(e){return c.push(e)},onKeyDown:m,onClick:d},i,{className:(0,a.default)("tabs__item",g.tabItem,null==i?void 0:i.className,{"tabs__item--active":l===t})}),null!=n?n:t)})))}function b(e){var t=e.lazy,n=e.children,r=e.selectedValue;if(n=Array.isArray(n)?n:[n],t){var a=n.find((function(e){return e.props.value===r}));return a?(0,o.cloneElement)(a,{className:"margin-top--md"}):null}return o.createElement("div",{className:"margin-top--md"},n.map((function(e,t){return(0,o.cloneElement)(e,{key:t,hidden:e.props.value!==r})})))}function w(e){var t=f(e);return o.createElement("div",{className:(0,a.default)("tabs-container",g.tabList)},o.createElement(v,(0,r.Z)({},e,t)),o.createElement(b,(0,r.Z)({},e,t)))}function k(e){var t=(0,y.default)();return o.createElement(w,(0,r.Z)({key:String(t)},e))}},7772:(e,t,n)=>{n.d(t,{Z:()=>m});var r=n(87462),o=n(67294),a=n(23746),i=n(7694),l=n(13618),s="0.47.0",u="0.48.0-SNAPSHOT",c="0.10.5",p="0.142.0",d=n(86668);const m=function(e){var t=e.language,n=e.code.replace(/{{site.lithoVersion}}/g,s).replace(/{{site.soloaderVersion}}/g,c).replace(/{{site.lithoSnapshotVersion}}/g,u).replace(/{{site.flipperVersion}}/g,p).trim(),m=(0,d.L)().isDarkTheme?l.Z:i.Z;return o.createElement(a.ZP,(0,r.Z)({},a.lG,{code:n,language:t,theme:m}),(function(e){var t=e.className,n=e.style,r=e.tokens,a=e.getLineProps,i=e.getTokenProps;return o.createElement("pre",{className:t,style:n},r.map((function(e,t){return o.createElement("div",a({line:e,key:t}),e.map((function(e,t){return o.createElement("span",i({token:e,key:t}))})))})))}))}},67136:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>p,contentTitle:()=>u,default:()=>f,frontMatter:()=>s,metadata:()=>c,toc:()=>d});var r=n(87462),o=n(63366),a=(n(67294),n(3905)),i=(n(74866),n(85162),n(44996)),l=(n(7772),["components"]),s={id:"migration-strategies",title:"Migration Strategies"},u=void 0,c={unversionedId:"kotlin/migration-strategies",id:"kotlin/migration-strategies",title:"Migration Strategies",description:"Prerequisites",source:"@site/../docs/kotlin/migration-strategies.mdx",sourceDirName:"kotlin",slug:"/kotlin/migration-strategies",permalink:"/docs/kotlin/migration-strategies",draft:!1,editUrl:"https://github.com/facebook/litho/edit/master/website/../docs/kotlin/migration-strategies.mdx",tags:[],version:"current",frontMatter:{id:"migration-strategies",title:"Migration Strategies"},sidebar:"mainSidebar",previous:{title:"Canvas",permalink:"/docs/widgets/canvas"},next:{title:"Compatibility with Custom Views",permalink:"/docs/kotlin/custom-view-compat"}},p={},d=[{value:"Prerequisites",id:"prerequisites",level:3},{value:"Adopting Litho in your app",id:"adopting-litho-in-your-app",level:2},{value:"Bottom-up",id:"bottom-up",level:3},{value:"Top-down",id:"top-down",level:3}],m={toc:d},h="wrapper";function f(e){var t=e.components,n=(0,o.Z)(e,l);return(0,a.mdx)(h,(0,r.Z)({},m,n,{components:t,mdxType:"MDXLayout"}),(0,a.mdx)("h3",{id:"prerequisites"},"Prerequisites"),(0,a.mdx)("p",null,"Before reading this page, you may find it helpful to review the following sections of the Tutorial:"),(0,a.mdx)("ul",null,(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/tutorial/project-setup"},"Setting up the Project")," - the required settings and dependencies to add Litho to your project."),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/tutorial/first-components"},"Component and Props")," - learn the basic Litho building blocks and create a component that uses props."),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/tutorial/introducing-layout"},"Introducing Layout")," - become familiar with building layouts using Flexbox.")),(0,a.mdx)("h2",{id:"adopting-litho-in-your-app"},"Adopting Litho in your app"),(0,a.mdx)("p",null,"Using Litho in a new surface is fairly straightforward: you can put a LithoView at the root of your new Fragment or Activity and start writing your components.\nHowever, adopting Litho within an existing surface needs to be done more incrementally and can require a bit more thought."),(0,a.mdx)("p",null,"Litho Components can interoperate with Views in the same App or even in the same surface, so you can migrate View surfaces to Litho incrementally and maintain a hybrid Component-View UI."),(0,a.mdx)("p",null,"There are two common strategies for incrementally migrating to Litho: ",(0,a.mdx)("a",{parentName:"p",href:"#bottom-up"},"Bottom Up")," and ",(0,a.mdx)("a",{parentName:"p",href:"#top-down"},"Top-down"),", as detailed in the following sub-sections."),(0,a.mdx)("h3",{id:"bottom-up"},"Bottom-up"),(0,a.mdx)("p",null,"With the bottom-up approach, you break down the UI into smaller pieces that can be converted incrementally. The View or ViewGroup in the original implementation is replaced by a LithoView that you attach as child to the root ViewGroup of your UI."),(0,a.mdx)("p",null,"Consider the following UI as an example:"),(0,a.mdx)("img",{src:(0,i.default)("/images/post-breakdown.png"),alt:"post-breakdown"}),(0,a.mdx)("p",null,"You can identify three UI blocks, which can be converted independently into three Litho Components: Header, Media and Footer. You'll have three LithoViews in your UI to render the Components."),(0,a.mdx)("p",null,"These Components are composed with smaller widgets such as Text or Image, similar to how Views are arranged in a ViewGroup.\nLitho provides ",(0,a.mdx)("a",{parentName:"p",href:"/docs/widgets/builtin-widgets"},"a library of widget Components"),", which you can immediately start using. If your app has a custom design system that implements custom views for primitives such as Button, Text or Image, you can start by creating Components for these first; you can also reuse them across the app to convert multiple surfaces to Litho."),(0,a.mdx)("p",null,"Once you've completed the incremental conversion, you can coalesce all the individual Components into a single Component and use one LithoView as the root of the UI."),(0,a.mdx)("p",null,"It's recommended to use the bottom-approach when you want to leverage performance optimisations such as incremental mount and view flattening sooner than you would with the top-down approach."),(0,a.mdx)("h3",{id:"top-down"},"Top-down"),(0,a.mdx)("p",null,"With the top-down approach, you replace the root ViewGroup of your UI with a LithoView and wrap the root View representing the UI into a Mountable Component. As you convert smaller parts of the UI into Components, you extract them out of the Mountable Component and into individual LithoViews."),(0,a.mdx)("p",null,"Some scenarios when the top-down approach is suitable include:"),(0,a.mdx)("ul",null,(0,a.mdx)("li",{parentName:"ul"},"Using Litho for the architecture of your surface and for writing new features, but existing Views might not be immediately converted to Litho."),(0,a.mdx)("li",{parentName:"ul"},"Converting a list surface to ",(0,a.mdx)("a",{parentName:"li",href:"/docs/sections/start"},"Sections"),". The root of the surface is a LithoView rendering a ",(0,a.mdx)("inlineCode",{parentName:"li"},"RecyclerCollectionComponent"),", while the individual list items can be either Views or Litho Components. You can leverage the Litho Lists API for features such as asynchronous data diffing or granular RecyclerView Adapter updates before converting the entire UI to Litho.")))}f.isMDXComponent=!0},23746:(e,t,n)=>{n.d(t,{ZP:()=>h,lG:()=>i});var r=n(87410);const o={plain:{backgroundColor:"#2a2734",color:"#9a86fd"},styles:[{types:["comment","prolog","doctype","cdata","punctuation"],style:{color:"#6c6783"}},{types:["namespace"],style:{opacity:.7}},{types:["tag","operator","number"],style:{color:"#e09142"}},{types:["property","function"],style:{color:"#9a86fd"}},{types:["tag-id","selector","atrule-id"],style:{color:"#eeebff"}},{types:["attr-name"],style:{color:"#c4b9fe"}},{types:["boolean","string","entity","url","attr-value","keyword","control","directive","unit","statement","regex","atrule","placeholder","variable"],style:{color:"#ffcc99"}},{types:["deleted"],style:{textDecorationLine:"line-through"}},{types:["inserted"],style:{textDecorationLine:"underline"}},{types:["italic"],style:{fontStyle:"italic"}},{types:["important","bold"],style:{fontWeight:"bold"}},{types:["important"],style:{color:"#c4b9fe"}}]};var a=n(67294),i={Prism:r.Z,theme:o};function l(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function s(){return s=Object.assign||function(e){for(var t=1;t<arguments.length;t++){var n=arguments[t];for(var r in n)Object.prototype.hasOwnProperty.call(n,r)&&(e[r]=n[r])}return e},s.apply(this,arguments)}var u=/\r\n|\r|\n/,c=function(e){0===e.length?e.push({types:["plain"],content:"\n",empty:!0}):1===e.length&&""===e[0].content&&(e[0].content="\n",e[0].empty=!0)},p=function(e,t){var n=e.length;return n>0&&e[n-1]===t?e:e.concat(t)},d=function(e,t){var n=e.plain,r=Object.create(null),o=e.styles.reduce((function(e,n){var r=n.languages,o=n.style;return r&&!r.includes(t)||n.types.forEach((function(t){var n=s({},e[t],o);e[t]=n})),e}),r);return o.root=n,o.plain=s({},n,{backgroundColor:null}),o};function m(e,t){var n={};for(var r in e)Object.prototype.hasOwnProperty.call(e,r)&&-1===t.indexOf(r)&&(n[r]=e[r]);return n}const h=function(e){function t(){for(var t=this,n=[],r=arguments.length;r--;)n[r]=arguments[r];e.apply(this,n),l(this,"getThemeDict",(function(e){if(void 0!==t.themeDict&&e.theme===t.prevTheme&&e.language===t.prevLanguage)return t.themeDict;t.prevTheme=e.theme,t.prevLanguage=e.language;var n=e.theme?d(e.theme,e.language):void 0;return t.themeDict=n})),l(this,"getLineProps",(function(e){var n=e.key,r=e.className,o=e.style,a=s({},m(e,["key","className","style","line"]),{className:"token-line",style:void 0,key:void 0}),i=t.getThemeDict(t.props);return void 0!==i&&(a.style=i.plain),void 0!==o&&(a.style=void 0!==a.style?s({},a.style,o):o),void 0!==n&&(a.key=n),r&&(a.className+=" "+r),a})),l(this,"getStyleForToken",(function(e){var n=e.types,r=e.empty,o=n.length,a=t.getThemeDict(t.props);if(void 0!==a){if(1===o&&"plain"===n[0])return r?{display:"inline-block"}:void 0;if(1===o&&!r)return a[n[0]];var i=r?{display:"inline-block"}:{},l=n.map((function(e){return a[e]}));return Object.assign.apply(Object,[i].concat(l))}})),l(this,"getTokenProps",(function(e){var n=e.key,r=e.className,o=e.style,a=e.token,i=s({},m(e,["key","className","style","token"]),{className:"token "+a.types.join(" "),children:a.content,style:t.getStyleForToken(a),key:void 0});return void 0!==o&&(i.style=void 0!==i.style?s({},i.style,o):o),void 0!==n&&(i.key=n),r&&(i.className+=" "+r),i})),l(this,"tokenize",(function(e,t,n,r){var o={code:t,grammar:n,language:r,tokens:[]};e.hooks.run("before-tokenize",o);var a=o.tokens=e.tokenize(o.code,o.grammar,o.language);return e.hooks.run("after-tokenize",o),a}))}return e&&(t.__proto__=e),t.prototype=Object.create(e&&e.prototype),t.prototype.constructor=t,t.prototype.render=function(){var e=this.props,t=e.Prism,n=e.language,r=e.code,o=e.children,a=this.getThemeDict(this.props),i=t.languages[n];return o({tokens:function(e){for(var t=[[]],n=[e],r=[0],o=[e.length],a=0,i=0,l=[],s=[l];i>-1;){for(;(a=r[i]++)<o[i];){var d=void 0,m=t[i],h=n[i][a];if("string"==typeof h?(m=i>0?m:["plain"],d=h):(m=p(m,h.type),h.alias&&(m=p(m,h.alias)),d=h.content),"string"==typeof d){var f=d.split(u),y=f.length;l.push({types:m,content:f[0]});for(var g=1;g<y;g++)c(l),s.push(l=[]),l.push({types:m,content:f[g]})}else i++,t.push(m),n.push(d),r.push(0),o.push(d.length)}i--,t.pop(),n.pop(),r.pop(),o.pop()}return c(l),s}(void 0!==i?this.tokenize(t,r,i,n):[r]),className:"prism-code language-"+n,style:void 0!==a?a.root:{},getLineProps:this.getLineProps,getTokenProps:this.getTokenProps})},t}(a.Component)},13618:(e,t,n)=>{n.d(t,{Z:()=>r});const r={plain:{color:"#F8F8F2",backgroundColor:"#282A36"},styles:[{types:["prolog","constant","builtin"],style:{color:"rgb(189, 147, 249)"}},{types:["inserted","function"],style:{color:"rgb(80, 250, 123)"}},{types:["deleted"],style:{color:"rgb(255, 85, 85)"}},{types:["changed"],style:{color:"rgb(255, 184, 108)"}},{types:["punctuation","symbol"],style:{color:"rgb(248, 248, 242)"}},{types:["string","char","tag","selector"],style:{color:"rgb(255, 121, 198)"}},{types:["keyword","variable"],style:{color:"rgb(189, 147, 249)",fontStyle:"italic"}},{types:["comment"],style:{color:"rgb(98, 114, 164)"}},{types:["attr-name"],style:{color:"rgb(241, 250, 140)"}}]}},7694:(e,t,n)=>{n.d(t,{Z:()=>r});const r={plain:{color:"#393A34",backgroundColor:"#f6f8fa"},styles:[{types:["comment","prolog","doctype","cdata"],style:{color:"#999988",fontStyle:"italic"}},{types:["namespace"],style:{opacity:.7}},{types:["string","attr-value"],style:{color:"#e3116c"}},{types:["punctuation","operator"],style:{color:"#393A34"}},{types:["entity","url","symbol","number","boolean","variable","constant","property","regex","inserted"],style:{color:"#36acaa"}},{types:["atrule","keyword","attr-name","selector"],style:{color:"#00a4db"}},{types:["function","deleted","tag"],style:{color:"#d73a49"}},{types:["function-variable"],style:{color:"#6f42c1"}},{types:["tag","selector","keyword"],style:{color:"#00009f"}}]}}}]);