# hh-sync

a command-line utility for syncing workouts from Endomondo to HeiaHeia

## Important!

The unitility does ask you to provide Endomondo and HeiaHeia (or Facebook) credentials. They are sent only to Endomodo and HeiaHeia (or Facebook) using HTTPS connection and stored locally. But, when given, they are stored in plain text in `~/.hh-sync` folder. **Make sure you keep these files safe.**

## Good to know

 - You can run syncing in interactive mode using `--interactive` flag. When provided, you will be asked to confirm syncing of every workout detected to be new.

 - By default, the untility searches new workouts only within a window of last 20 workouts. You can change that behaviour by giving `--depth` option.

## Installation

 - Download the [hh-sync](https://github.com/propan/hh-sync/blob/stable/hh-sync) script
 - Place it on your $PATH where your shell can find it (eg. ~/bin)
 - Set it to be executable (chmod a+x ~/bin/lein)
 - Run it (hh-sync) and it will download the self-install package

## Building

1. Checkout sources
2. Build with ``lein uberjar``
    
## Usage

1. Configure the CLI tool

```lang=bash
hh-sync --configure
```

2. Sync workouts

```lang=bash
hh-sync --sync
```

## Upgrading hh-sync

To upgrade to the latest version run:

```lang=bash
hh-sync upgrade
```

## License

Copyright © 2015 Pavel Prokopenko

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
