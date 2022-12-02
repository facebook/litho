"use strict";(self.webpackChunk=self.webpackChunk||[]).push([[8070],{3905:(e,t,n)=>{n.r(t),n.d(t,{MDXContext:()=>p,MDXProvider:()=>m,mdx:()=>v,useMDXComponents:()=>l,withMDXComponents:()=>d});var o=n(67294);function r(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function a(){return a=Object.assign||function(e){for(var t=1;t<arguments.length;t++){var n=arguments[t];for(var o in n)Object.prototype.hasOwnProperty.call(n,o)&&(e[o]=n[o])}return e},a.apply(this,arguments)}function i(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);t&&(o=o.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,o)}return n}function c(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?i(Object(n),!0).forEach((function(t){r(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):i(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function s(e,t){if(null==e)return{};var n,o,r=function(e,t){if(null==e)return{};var n,o,r={},a=Object.keys(e);for(o=0;o<a.length;o++)n=a[o],t.indexOf(n)>=0||(r[n]=e[n]);return r}(e,t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(o=0;o<a.length;o++)n=a[o],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(r[n]=e[n])}return r}var p=o.createContext({}),d=function(e){return function(t){var n=l(t.components);return o.createElement(e,a({},t,{components:n}))}},l=function(e){var t=o.useContext(p),n=t;return e&&(n="function"==typeof e?e(t):c(c({},t),e)),n},m=function(e){var t=l(e.components);return o.createElement(p.Provider,{value:t},e.children)},u={inlineCode:"code",wrapper:function(e){var t=e.children;return o.createElement(o.Fragment,{},t)}},f=o.forwardRef((function(e,t){var n=e.components,r=e.mdxType,a=e.originalType,i=e.parentName,p=s(e,["components","mdxType","originalType","parentName"]),d=l(n),m=r,f=d["".concat(i,".").concat(m)]||d[m]||u[m]||a;return n?o.createElement(f,c(c({ref:t},p),{},{components:n})):o.createElement(f,c({ref:t},p))}));function v(e,t){var n=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var a=n.length,i=new Array(a);i[0]=f;var c={};for(var s in t)hasOwnProperty.call(t,s)&&(c[s]=t[s]);c.originalType=e,c.mdxType="string"==typeof e?e:r,i[1]=c;for(var p=2;p<a;p++)i[p]=n[p];return o.createElement.apply(null,i)}return o.createElement.apply(null,n)}f.displayName="MDXCreateElement"},37972:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>d,contentTitle:()=>s,default:()=>u,frontMatter:()=>c,metadata:()=>p,toc:()=>l});var o=n(83117),r=n(80102),a=(n(67294),n(3905)),i=["components"],c={id:"overview",title:"Introduction"},s=void 0,p={unversionedId:"codegen/overview",id:"codegen/overview",title:"Introduction",description:"This section contains information about the old Java Spec API.",source:"@site/../docs/codegen/overview.mdx",sourceDirName:"codegen",slug:"/codegen/overview",permalink:"/docs/codegen/overview",draft:!1,editUrl:"https://github.com/facebook/litho/edit/master/website/../docs/codegen/overview.mdx",tags:[],version:"current",frontMatter:{id:"overview",title:"Introduction"},sidebar:"mainSidebar",previous:{title:"Compatibility with Custom Views",permalink:"/docs/kotlin/custom-view-compat"},next:{title:"Layout Specs",permalink:"/docs/codegen/layout-specs"}},d={},l=[],m={toc:l};function u(e){var t=e.components,n=(0,r.Z)(e,i);return(0,a.mdx)("wrapper",(0,o.Z)({},m,n,{components:t,mdxType:"MDXLayout"}),(0,a.mdx)("admonition",{type:"caution"},(0,a.mdx)("p",{parentName:"admonition"},"This section contains information about the old Java Spec API.\nFor new development, the Kotlin API is recommended (see the ",(0,a.mdx)("a",{parentName:"p",href:"/docs/mainconcepts/components-basics"},"Components")," page in the 'Main Concepts' section).")),(0,a.mdx)("p",null,"The documentation for Codegen APIs includes the following pages:"),(0,a.mdx)("ul",null,(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/codegen/layout-specs"},"Layout Specs")," - the logical equivalents of composite views on Android."),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/codegen/mount-specs"},"Mount Specs")," - components that can render views or drawables."),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/sections/start"},"Sections API")," - documentation for the Sections API for writing lists."),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/codegen/passing-data-to-components/spec-props"},"Props")," and ",(0,a.mdx)("a",{parentName:"li",href:"/docs/codegen/passing-data-to-components/treeprops"},"TreeProps")," - the ways you can pass data to components."),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/codegen/state-for-specs"},"State in Specs")," - data that is encapsulated and managed within the component and is transparent to its parent."),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/codegen/events-for-specs"},"Events for Specs")," - a general-purpose API to connect components with one another."),(0,a.mdx)("li",{parentName:"ul"},(0,a.mdx)("a",{parentName:"li",href:"/docs/codegen/trigger-events"},"Triggering Events with Handles")," - how to use a ",(0,a.mdx)("inlineCode",{parentName:"li"},"Handle")," to trigger events on components.")))}u.isMDXComponent=!0}}]);