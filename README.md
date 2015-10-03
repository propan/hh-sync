# hh-sync

a command-line utility for syncing workouts from Endomondo to HeiaHeia

## Important!

The unitility does ask you to provide Endomondo and HeiaHeia credentials. They are sent only to Endomodo and HeiaHeia using HTTPS connection ans stored locally. But, when given, they are stored in plain text in `.hh-sync` file within the working directory. **Make sure you keep that file safe.**

## Good to know

 - You can run syncing in interactive mode using `--interactive` flag. When provided, you will be asked to confirm syncing of every workout detected to be new.

- The untility searches new workouts only within a window of last 20 workouts.

## Installation

1. Checkout sources
2. Build with ``lein uberjar``
    
## Usage

1. Configure the CLI tool

```lang=bash
java -jar hh-sync-0.0.1-standalone.jar --configure
```

2. Sync workouts

```lang=bash
java -jar hh-sync-0.0.1-standalone.jar --sync
```

## License

Copyright © 2015 Pavel Prokopenko

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
