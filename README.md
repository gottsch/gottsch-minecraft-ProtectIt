# Protect It

**Protect It** is built for Forge 1.16.5+. It is a tiny, bare-bones "land claim" mod to protect specified areas. I developed it to use in my PiPack series modpacks (for hosting on Raspberry Pi), however it can be used on any world.  The protection is meant to be set by a server admin, however future versions (2.0+) will allow the player to claim land areas.

**Commands:**

/protect [block | pvp] [add | remove | list | clear] [pos | uuid] args: commands are consolidated into one command.

ex. /protect block add 0 63 0 10 63 10 : area is protected against all players.

ex. /protect block add 0 63 0 10 63 10 @gottsch : area is owned by gottsch, protected against all other players.

ex. /protect block remove [uuid] : remove all protections owned by the uuid

ex. /protect block remove 0 63 0 10 63 10 : remove all protections that intersects with the area.

**Protects against:**

block break

block place

block tool interaction

living destroy block

explosion

and piston movement


