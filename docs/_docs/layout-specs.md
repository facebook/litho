---
docid: layout-specs
title: Layout Specs
layout: layout-specs
permalink: /docs/layout-specs
---

<!-- Workaround for https://github.com/jekyll/jekyll/issues/7629 -->
<p>&nbsp;</p>

{% capture layout-spec-java %}{% include_relative layout-specs/layout-spec-java.md %}{% endcapture %}
{% capture layout-spec-kt %}{% include_relative layout-specs/layout-spec-kt.md %}{% endcapture %}

<article class="code-block active" id="doc-layout-spec-java">
    {{ layout-spec-java | markdownify }}
</article>
<article class="code-block" id="doc-layout-spec-kt">
    {{ layout-spec-kt | markdownify }}
</article>
