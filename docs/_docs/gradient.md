---
docid: gradient
title: Gradients
layout: gradient
permalink: /docs/gradients
---

<!-- Workaround for https://github.com/jekyll/jekyll/issues/7629 -->
<p>&nbsp;</p>

{% capture gradient-java %}{% include_relative gradient/gradient-java.md %}{% endcapture %}
{% capture gradient-kt %}{% include_relative gradient/gradient-kt.md %}{% endcapture %}

<article class="code-block active" id="doc-gradient-java">
    {{ gradient-java | markdownify }}
</article>
<article class="code-block" id="doc-gradient-kt">
    {{ gradient-kt | markdownify }}
</article>
