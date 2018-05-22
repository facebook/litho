---
docid: getting-started
title: Getting Started
layout: docs-getting-started
permalink: /docs/getting-started
---

{% capture gradle %}{% include_relative getting-started/gradle.md %}{% endcapture %}
{% capture gradle-kt %}{% include_relative getting-started/gradle-kt.md %}{% endcapture %}
{% capture buck %}{% include_relative getting-started/buck.md %}{% endcapture %}
{% capture testing-java %}{% include_relative getting-started/testing-java.md %}{% endcapture %}

{{ gradle | markdownify }}
{{ gradle-kt | markdownify }}
{{ buck | markdownify }}
{{ testing-java | markdownify }}
