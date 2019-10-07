/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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