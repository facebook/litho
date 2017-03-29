---
layout: home
title: Litho
id: home
---

{% capture landing-page-sample %}{% include_relative landing-page-sample.md %}{% endcapture %}

<div class="gridBlock alternateColor">
  <div class="blockElement twobyGridBlock imageAlignSide">
    <div class="blockContent">
      <div class="blockText">
        <p>
          Litho takes inspiration from proven technologies at Facebook, such as <a href="https://facebook.github.io/react">React</a> and <a href="http://componentkit.org">ComponentKit</a>, to provide a powerful framework that enables Android developers to write efficient UI code by default through a simple annotation-based Java API.
        </p>
      </div>
    </div>
    <div class="blockContent">
      <div class="blockCode">
        {{ landing-page-sample | markdownify }}
      </div>
    </div>
  </div>
</div>
