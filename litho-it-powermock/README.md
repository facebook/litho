# What's This?

Powermock needs to cast some dark magic onto the ClassLoaders to do its job.
This renders them non-reusable and we have to discard them after every test
suite. The same is true for dynamic Robolectric config changes (indicated by
`@Config` uses).

This is problematic as we need to load `libyoga` and the JVM has a limitation of
only ever loading a shared library once per process without the ability to
unload it.

This module runs tests in a way that prevents processes from ever using a
ClassLoader more than once which comes at a steep performance cost. So please be
mindful about adding new tests here.

Here be dragons.
