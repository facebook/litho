## User Documentation for Litho

This directory will contain the user and feature documentation for Litho. The documentation will be hosted on GitHub pages.

### Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details on how to add or modify content.

### Run the Site Locally

You can run the site either using our custom Docker container or by setting up
the necessary Ruby gems on your machine. The Docker setup is way faster and
doesn't require littering your machine with Ruby artifacts, but the choice is
yours.

### Using Docker

Install Docker via your system's package manager or, if your operating system
comes out of Cupertino, use [Docker for Mac](https://docs.docker.com/docker-for-mac/).

In the `docs/` folder, run this:

```
docker run -t --rm -v "$PWD":/usr/src/app -p "4000:4000" passy/github-pages:193
```

This will launch a web server on http://localhost:4000/ that monitors the docs
directory for changes and regenerates on the fly.

NOTE: If you see a error from bundler like `Could not find i18n-0.9.1 in any of
the sources`, try removing your local `Gemfile.lock` and rerun the command.
But please don't commit that result.

### Using Local Ruby

The requirements for running a GitHub pages site locally is described in [GitHub help](https://help.github.com/articles/setting-up-your-github-pages-site-locally-with-jekyll/#requirements). The steps below summarize these steps.

> If you have run the site before, you can start with step 1 and then move on to step 5.

1. Ensure that you are in the same directory where this `README.md` and the `Gemfile` file exists (e.g., it could be in `Litho/docs` on `master`, in the root of a `gh-pages` branch, etc). The below RubyGems commands, etc. must be run from there.

1. Make sure you have Ruby and [RubyGems](https://rubygems.org/) installed.

   > Ruby >= 2.2 is required for the gems. On the latest versions of Mac OS X, Ruby 2.0 is the
   > default. Use [Homebrew](http://brew.sh) and the `brew install ruby` command (or your
   > preferred upgrade mechanism) to install a newer version of Ruby for your Mac OS X system.

1. Make sure you have [Bundler](http://bundler.io/) installed.

    ```shell
    # may require sudo
    gem install bundler
    ```
1. Install the project's dependencies

    ```shell
    # run this in the directory containing the "Gemfile" file
    bundle install
    ```

    > If you get an error when installing `nokogiri`, you may be running into the problem described
    > in [this nokogiri issue](https://github.com/sparklemotion/nokogiri/issues/1483). You can
    > either `brew uninstall xz` (and then `brew install xz` after the bundle is installed) or
    > `xcode-select --install` (although this may not work if you have already installed command
    > line tools).

1. Run Jekyll's server.

    - On first runs or for structural changes to the documentation (e.g., new sidebar menu item), do a full build.

    ```shell
    # run this in the directory containing the "Gemfile" file
    bundle exec jekyll serve
    ```

    - For content changes only, you can use `--incremental` for faster builds.

    ```shell
    # run this in the directory containing the "Gemfile" file
    bundle exec jekyll serve --incremental
    ```

    > We use `bundle exec` instead of running straight `jekyll` because `bundle exec` will always use the version of Jekyll from our `Gemfile`. Just running `jekyll` will use the system version and may not necessarily be compatible.

    - To run using an actual IP address, you can use `--host=0.0.0.0`

    ```shell
    # run this in the directory containing the "Gemfile" file
    bundle exec jekyll serve --host=0.0.0.0
    ```

    This will allow you to use the IP address associated with your machine in the URL. That way you could share it with other people.

    e.g., on a Mac, you can your IP address with something like `ifconfig | grep "inet " | grep -v 127.0.0.1`.

1. Either of commands in the previous step will serve up the site on your local device at http://127.0.0.1:4000/ or http://localhost:4000.

### Updating the Bundle

The site depends on Github Pages and the installed bundle is based on the `github-pages` gem.
Occasionally that gem might get updated with new or changed functionality. If that is the case,
you can run:

```shell
# run this in the directory containing the "Gemfile" file
bundle update
```

to get the latest packages for the installation.
