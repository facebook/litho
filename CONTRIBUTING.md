# Contributing to Litho

We want to make contributing to this project as easy and transparent as possible.

## Building from source

1. Clone the repo.
2. Ensure your Android SDK has the right dependencies.  To build Litho you need:
   - [The Android NDK and build tools](https://developer.android.com/studio/projects/add-native-code.html) (NDK, CMake, LLDB)
   - The Android 8.0 (API 26) SDK
   - Version 27.0.3 of the The Android SDK Build tools.
3. Import the project by selecting the repo's root directory. You should be able to successfully sync the gradle project now!

## Our Development Process

We develop on a private branch internally at Facebook. We regularly update this github project with the changes from the internal repo. External pull requests are cherry-picked into our repo and then pushed back out.

## Testing

When making changes to the code base, make sure that the existing tests pass and you cover
new features appropriately. You can run the test suite

1. with buck: `buck test ...`, or
2. with gradle: `./gradlew test`.

## Pull Requests

We actively welcome your pull requests!

1. Fork the repo and create your branch from master.
2. If you've added code that should be tested, add tests.
3. If you've changed APIs, update the documentation (under `/docs`)
4. Ensure the test suite passes.
5. If you haven't already, complete the Contributor License Agreement ("CLA").
6. We will review your code and merge it.

## Contributor License Agreement ("CLA")

In order to accept your pull request, we need you to submit a CLA. You only need to do this once to work on any of Facebook's open source projects.

Complete your CLA here: [https://code.facebook.com/cla](https://code.facebook.com/cla).

## Issues

We use GitHub issues to track public bugs.  When you report an issue the more information the better. Here are some things that will help you get an answer faster:

 * A title as well as a body for the issue
 * A screenshot or video of the problem
 * Logcat output, if your app is crashing
 * A snippet of the code in question
 * Place code in blocks so that it reads like code:

    ```
    ```java (or xml)
    your code here
    ```(terminating backticks)
    ```

### Security bugs

Facebook has a [bounty program](https://www.facebook.com/whitehat/) for the safe disclosure of security bugs. In those cases, please go through the process outlined on that page and do not file a public issue.

## Coding Style

Please use 2 spaces for indentation rather than tabs. We follow the [Google Java
Style](https://google.github.io/styleguide/javaguide.html). You can use the
[google-java-format tool](https://github.com/google/google-java-format) to
format your code accordingly.

Most importantly, be consistent with existing code.  Look around the codebase and match the style.

## License

By contributing to Litho, you agree that your contributions will be licensed under its Apache-2 license.
