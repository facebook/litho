import React from "react";
import Highlight, { defaultProps } from "prism-react-renderer";
import github from 'prism-react-renderer/themes/github';
import {site} from '../lithoVersions.js';

const exampleCode = `dependencies {
  // ...
  // Litho
  implementation 'com.facebook.litho:litho-core:{{site.lithoVersion}}'
  implementation 'com.facebook.litho:litho-widget:{{site.lithoVersion}}'

  annotationProcessor 'com.facebook.litho:litho-processor:{{site.lithoVersion}}'

  // SoLoader
  implementation 'com.facebook.soloader:soloader:{{site.soloaderVersion}}'

  // For integration with Fresco
  implementation 'com.facebook.litho:litho-fresco:{{site.lithoVersion}}'

  // For testing
  testImplementation 'com.facebook.litho:litho-testing:{{site.lithoVersion}}'
}
`;

export const VersionedCodeBlock = ({language, code}) => {
  const modifiedCode = code.replaceAll('{{site.lithoVersion}}', site.lithoVersion)
    .replaceAll('{{site.soloaderVersion}}', site.soloaderVersion)
    .replaceAll('{{site.lithoSnapshotVersion}}', site.lithoSnapshotVersion)
    .replaceAll('{{site.flipperVersion}}', site.flipperVersion);

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