---
docid: layout-specs
title: Layout Specs
layout: layout-specs
permalink: /docs/layout-specs
---

{% capture layout-spec-java %}{% include_relative layout-specs/layout-spec-java.md %}{% endcapture %}
{% capture layout-spec-kt %}{% include_relative layout-specs/layout-spec-kt.md %}{% endcapture %}

{{ layout-spec-java | markdownify }}
{{ layout-spec-kt | markdownify }}