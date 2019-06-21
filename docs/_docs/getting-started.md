---
docid: getting-started
title: Getting Started
layout: docs-getting-started
permalink: /docs/getting-started
---

<!-- Workaround for https://github.com/jekyll/jekyll/issues/7629 -->
<p>&nbsp;</p>

{% capture gradle %}{% include_relative getting-started/gradle.md %}{% endcapture %}
{% capture gradle-kt %}{% include_relative getting-started/gradle-kt.md %}{% endcapture %}
{% capture buck %}{% include_relative getting-started/buck.md %}{% endcapture %}
{% capture testing-java %}{% include_relative getting-started/testing-java.md %}{% endcapture %}
{% capture testing-kt %}{% include_relative getting-started/testing-kt.md %}{% endcapture %}

<article class="code-block active" id="doc-gradle">
    {{ gradle | markdownify }}
</article>
<article class="code-block" id="doc-gradle-kt">
    {{ gradle-kt | markdownify }}
</article>
<article class="code-block" id="doc-buck">
    {{ buck | markdownify }}
</article>
<article class="code-block" id="doc-testing-java">
    {{ testing-java | markdownify }}
</article>
<article class="code-block" id="doc-testing-kt">
    {{ testing-kt | markdownify }}
</article>
