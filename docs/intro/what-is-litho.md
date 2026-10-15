---
id: what-is-litho
title: "What is Litho?"
keywords:
    - Litho
    - declarative UI
    - Android UI framework
    - KComponent
    - immutable components
    - async layout
    - background thread layout
    - performance
    - Meta UI framework
api_category: introduction
related_docs: [components-basics, mainconcepts, hooks-intro]
api_names: [KComponent, Component, LithoView, ComponentTree]
---

import {FbInternalOnly} from 'docusaurus-plugin-internaldocs-fb/internal';
import FbBunnylolTip from './fb/_bunnylol-tip.mdx';
import FbSupportGroups from './fb/_support-groups.mdx';

<FbInternalOnly>
<FbBunnylolTip />
</FbInternalOnly>

Litho is a declarative UI framework for Android, specializing in performant and maintainable UIs. It allows developers to define UI using immutable Components which can be composed together, allowing code to be more easily re-used. Litho also automatically moves complex business logic and layout off the main thread to enable faster and smoother user experiences.

Litho is used extensively at Meta in its Android apps. To get started learning how to use Litho, start with [Components Basics](/docs/components-basics)!

<FbInternalOnly>
<FbSupportGroups />
</FbInternalOnly>
