---
docid: state
title: State
layout: states
permalink: /docs/state
---

{% capture state-java %}{% include_relative state/state-java.md %}{% endcapture %}
{% capture state-kt %}{% include_relative state/state-kt.md %}{% endcapture %}

<article class="code-block active" id="doc-state-java">
    {{ state-java | markdownify }}
</article>
<article class="code-block" id="doc-state-kt">
    {{ state-kt | markdownify }}
</article>
