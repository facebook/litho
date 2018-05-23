---
docid: mount-specs
title: Mount Specs
layout: mount-specs
permalink: /docs/mount-specs
---

{% capture mount-spec-java %}{% include_relative mount-specs/mount-spec-java.md %}{% endcapture %}
{% capture mount-spec-kt %}{% include_relative mount-specs/mount-spec-kt.md %}{% endcapture %}

<article class="code-block active" id="doc-mount-spec-java">
    {{ mount-spec-java | markdownify }}
</article>
<article class="code-block" id="doc-mount-spec-kt">
    {{ mount-spec-kt | markdownify }}
</article>
