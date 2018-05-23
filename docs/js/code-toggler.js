'use strict';

(function () {
  window.addEventListener('hashchange', e => {
    updateCodeBlock(e.newURL);
  });

  const updateCodeBlock = (url) => {
    const parsedUrl = new URL(url);
    const hash = parsedUrl.hash.substring(1);
    const newBlock = document.getElementById(hash);

    if (newBlock == null || !newBlock.classList.contains("code-block")) {
      console.warn('URL hash is not a code block: ', hash);
      return;
    }

    document.querySelectorAll('.code-block.active,.toggler-button.active').forEach(e => e.classList.remove('active'));
    document.querySelector(`.toggler-button[href="#${hash}"]`).classList.add('active');
    newBlock.classList.add('active');
  };

  document.addEventListener('DOMContentLoaded', _ => {
    updateCodeBlock(window.location.href);
  });
}());