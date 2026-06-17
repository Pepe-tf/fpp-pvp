# fpp-pvp

bots fighting bots fighting players. thats basically it.

pepe owns the fpp side of things, this is for our server (sdiybt / cybr)

## whats in here rn

- bot vs player
- bot vs bot (yes they can hit each other)
- /fpp pvp commands
- target picking (closest, low hp, random)
- fight back when u hit a bot
- they walk at u if ur far (uses fpp navigate, pathfinder ext would help)

perm: `fpp.pvp`

## install

1. build jar (or wait for releases idk)
2. drop in `plugins/FakePlayerPlugin/extensions/`
3. restart or `/fpp reload`
4. config: `plugins/FakePlayerPlugin/extensions/FPP-PvP/config.yml`

## commands i think

```
/fpp pvp Steve
/fpp pvp Steve Notch
/fpp pvp Steve --once
/fpp pvp Steve --stop
/fpp pvp all
/fpp pvp --stop
```

## build

java 21, clone fake-player-plugin next to this

```bash
cd ../fake-player-plugin && ./gradlew compileJava
cd ../fpp-pvp && ./gradlew build
```

jar in `build/libs/`

## refs

- https://github.com/Pepe-tf/fake-player-plugin
- https://github.com/Pepe-tf/fpp-register-extension

wip. kb feels weird sometimes. see TODO.md
