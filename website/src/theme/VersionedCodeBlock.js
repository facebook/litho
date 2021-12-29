/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
 *
 * @format
 */

import React from 'react';
import Highlight, {defaultProps} from 'prism-react-renderer';
import github from 'prism-react-renderer/themes/github';
import dracula from 'prism-react-renderer/themes/dracula';
import {site} from '../../versionConfig.js';
import useThemeContext from '@theme/hooks/useThemeContext';

/*
VersionedCodeBlock is a wrapper component for the normal codeblock. It replaces
placeholders e.g. {{site.lithoVersion}} with current versions stated in ../versionConfig.js
When updating versions in documentation, it suffices to update the versions in ../versionConfig.js

Use in *.mdx like:

 import VersionedCodeBlock from '@theme/VersionedCodeBlock'

 <VersionedCodeBlock language="groovy" code={`
 dependencies {
   debugImplementation 'com.facebook.flipper:flipper:{{site.flipperVersion}}'
   debugImplementation 'com.facebook.flipper:flipper-litho-plugin:{{site.flipperVersion}}'
 }
 `}/>
*/
const VersionedCodeBlock = ({language, code}) => {
  // replace all placeholders with actual versions
  const modifiedCode = code
    .replace(/{{site.lithoVersion}}/g, site.lithoVersion)
    .replace(/{{site.soloaderVersion}}/g, site.soloaderVersion)
    .replace(/{{site.lithoSnapshotVersion}}/g, site.lithoSnapshotVersion)
    .replace(/{{site.flipperVersion}}/g, site.flipperVersion)
    .trim();
  const theme = getCodeBlockTheme();
  // render as a codeblock
  return (
    <Highlight
      {...defaultProps}
      code={modifiedCode}
      language={language}
      theme={theme}>
      {({className, style, tokens, getLineProps, getTokenProps}) => (
        <pre className={className} style={style}>
          {tokens.map((line, i) => (
            <div {...getLineProps({line, key: i})}>
              {line.map((token, key) => (
                <span {...getTokenProps({token, key})} />
              ))}
            </div>
          ))}
        </pre>
      )}
    </Highlight>
  );
};

// returns theme for codeblock depending on whether dark mode is activated
function getCodeBlockTheme() {
  const {isDarkTheme} = useThemeContext();
  if (isDarkTheme) {
    return dracula;
  } else {
    return github;
  }
}

export default VersionedCodeBlock;
