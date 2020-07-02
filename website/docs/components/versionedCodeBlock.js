/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 */

import React from "react";
import Highlight, { defaultProps } from "prism-react-renderer";
import github from 'prism-react-renderer/themes/github';
import dracula from 'prism-react-renderer/themes/dracula';
import {site} from '../../versionConfig.js';
import useThemeContext from '@theme/hooks/useThemeContext';

// VersionedCodeBlock is a wrapper component for the normal codeblock. It replaces
// placeholders e.g. {{site.lithoVersion}} with current versions stated in ../versionConfig.js
// When updating versions in documentation, it suffices to update the versions in ../versionConfig.js 
export const VersionedCodeBlock = ({language, code}) => {
  // replace versions
  const modifiedCode = code.replaceAll('{{site.lithoVersion}}', site.lithoVersion)
    .replaceAll('{{site.soloaderVersion}}', site.soloaderVersion)
    .replaceAll('{{site.lithoSnapshotVersion}}', site.lithoSnapshotVersion)
    .replaceAll('{{site.flipperVersion}}', site.flipperVersion);
  const theme = getCodeBlockTheme();
    // render as a codeblock
  return(
  <Highlight {...defaultProps} code={modifiedCode} language={language} theme={theme}>
    {({ className, style, tokens, getLineProps, getTokenProps }) => (
      <pre className={className} style={style}>
        {tokens.map((line, i) => (
          <div {...getLineProps({ line, key: i })}>
            {line.map((token, key) => (
              <span {...getTokenProps({ token, key })}/>
            ))}
          </div>
        ))}
      </pre>
    )}
  </Highlight>
  );
}

// returns theme for codeblock depending on whether dark mode is activated
function getCodeBlockTheme() {
  const {isDarkTheme} = useThemeContext();
  if (isDarkTheme) {
    return dracula;
  } else {
    return github;
  }
}