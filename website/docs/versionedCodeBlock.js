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
import {site} from '../lithoVersions.js';

// VersionedCodeBlock is a wrapper component for the normal codeblock. It replaces
// placeholders e.g. {{site.lithoVersion}} with current versions stated in ../lithoVersion.js
// When updating versions in documentation, it suffices to update the versions in ../lithoVersion.js 
export const VersionedCodeBlock = ({language, code}) => {
  // replace versions
  const modifiedCode = code.replaceAll('{{site.lithoVersion}}', site.lithoVersion)
    .replaceAll('{{site.soloaderVersion}}', site.soloaderVersion)
    .replaceAll('{{site.lithoSnapshotVersion}}', site.lithoSnapshotVersion)
    .replaceAll('{{site.flipperVersion}}', site.flipperVersion);
  // render as a codeblock
  return(
  <Highlight {...defaultProps} code={modifiedCode} language={language} theme={github}>
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