RandomCoords2
=============

A Minecraft Spigot server plugin: randomly offsets the position of players.
Offsetting the coordinates is done by modifying game packets. Therefore, the coordinates will be offset only on the client side.
The amount of offset is randomly chosen per user login and also per world.

Dependencies
============

This plugin depends on ProtocolLib, so should be installed with ProtocolLib.

Compiling
=========

```sh
git clone https://github.com/kbinani/RandomCoords2.git
cd RandomCoords2
./gradlew assemble
```

jar file will be found at `build/libs`

License
=======

MIT

Author
======

kbinani
