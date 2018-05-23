---
docid: props
title: Props
layout: props
permalink: /docs/props
---

{% capture props-java %}{% include_relative props/props-java.md %}{% endcapture %}
{% capture props-kt %}{% include_relative props/props-kt.md %}{% endcapture %}

<article class="code-block active" id="doc-props-java">
    {{ props-java | markdownify }}
</article>
<article class="code-block" id="doc-props-kt">
    {{ props-kt | markdownify }}
</article>