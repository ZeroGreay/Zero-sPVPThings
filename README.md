# Zero's PVP Things

A client-side Fabric mod for **Minecraft 26.2** with PvP quality-of-life features. Everything can be toggled
from **Mod Menu**, or press **Right Shift** in game to open the settings.

Requires Fabric Loader and Fabric API. Mod Menu is optional (it only adds the config button).

## Features

| Feature | What it does | Default keys |
|---|---|---|
| **Item highlighting** | Colors inventory and hotbar slots by item. Rules match an item id, an item tag (`#minecraft:swords`) or a glob (`*_axe`). Styles: fill, outline, or both, with adjustable opacity. | `H` while hovering an item cycles its color |
| **Inventory scaler** | Makes inventory and container screens bigger or smaller without changing GUI Scale. The size changes in steps (default 10%) and never grows past what fits in the window. | In an inventory: `=` bigger, `-` smaller, `0` reset |
| **Momentum bar** | Replaces the XP bar with your horizontal speed, as a gradient bar or the XP bar texture. A white tick marks your recent peak, and the speed in blocks/sec shows where the XP level normally is. | - |
| **Gamma** | Overrides brightness with a slider that goes up to **1500%**. | `G` toggles it |
| **Gap health tracker** | While you eat a golden apple, shows the total health (including absorption) you lost during the eat, then keeps it on screen for a few seconds. | - |
| **Custom crosshair** | 8 shapes (cross, cross+dot, T, X, dot, circle, circle+dot, circle+cross) with adjustable length, thickness, gap, dot size, circle radius, outline, color, a 25%–400% scale, and an optional gap that widens while your attack recharges. Includes a live-preview editor, presets, **Marvel Rivals crosshair code import**, and its own `ZPVP1:` share codes. | Bind "Toggle Custom Crosshair" yourself |
| **CustomHUD** | A rebuilt version of the [CustomHUD](https://modrinth.com/mod/customhud) profile format: text HUD files with variables, conditionals and colors. Has an in-game editor, 3 profiles, and reloads automatically when you save a profile file. | Bind "Toggle CustomHUD" / "Cycle Profile" yourself |

All keys can be rebound in *Options → Controls → Key Binds → Zero's PVP Things*.

### Marvel Rivals crosshair codes

Paste a code like
`3;1;10.0,10.0,10.0,10.0;100.0,100.0,100.0,100.0;50.0,50.0,50.0,50.0;100.0,100.0,100.0,100.0`
into the editor's code box and press **Import Code**.

Marvel Rivals doesn't document its code format, so the importer's field mapping is a best guess based on
public codes:

* field 1: reticle type (1 cross, 2 circle, 3 dot, 4 cross+dot)
* field 2: outline on/off
* then groups of 4 percentages: shape (length, thickness, gap, dot/circle size), main color RGBA, outline
  RGBA, circle settings

If you don't provide a group, it keeps its default. Imported crosshairs usually need some tweaking in the
editor. **Export** copies a `ZPVP1:` code, which re-imports exactly.

## CustomHUD profiles

Profiles are saved in `config/zeropvpthings/customhud/profile_1.txt` … `profile_3.txt`. Edit them in game
(*settings → Edit Profiles…*) or in any text editor; the HUD reloads them automatically.

```
// comment
==TextShadow: true==              global settings go before the first section
==BackgroundColor: #60000000==    (also: ForegroundColor, Scale, LineSpacing, Padding)

==Section:TopLeft==               TopLeft TopCenter TopRight CenterLeft Center CenterRight BottomLeft ...
==Section:BottomRight,0,-2==      optional x,y offset; settings after a section header only affect it
&aFPS: &f{fps}                    &0-&f colors, &l &o &n &m &k &r, &#RRGGBB or &{#RRGGBB} hex colors
{x -p1}                           -pN decimals, -zfN zero fill, -uc / -lc upper/lower case
{{fps >= 60, "&a", fps >= 30, "&e", "&c"}}{fps}    inline conditional: cond, value, ..., else
=if: target & combo > 2=          block conditional: =elseif: ...=  =else=  =endif=
&cTarget: {target_name}
=endif=
{icon:totem_of_undying}{count:totem_of_undying}     item icons
```

Conditions support `| & !`, `= != > < >= <=`, `+ - * / %`, parentheses, numbers and `"strings"`.

**Variables** (the in-game *Variables…* button lists them all):
`fps ping players server server_name singleplayer multiplayer memory_used memory_max memory_percent version
java_version username entities x y z bx by bz cx cz nether_x nether_z yaw pitch facing facing_short
facing_axis biome dimension light speed speed_kmh time time12 date day hour24 minute health max_health hearts
absorption total_health armor food saturation air xp_level sprinting sneaking on_ground in_water burning hurt
attack_cooldown selected_slot cps lcps rcps reach combo target target_name target_health target_max_health
target_distance gap_lost eating_gap gamma`

Variables that take an argument:
* `{item:<main|off|head|chest|legs|feet>}` plus `:count :dur :max_dur :dur_percent :id :icon`
* `{count:<item>}`: how many of an item you carry, e.g. `{count:golden_apple}`
* `{icon:<item>}`: draws an item icon
* `{effect:<effect>}`: the effect's level (0 if you don't have it); `{effect:<effect>:duration}` gives the
  time left as m:ss

## Building

```
./gradlew build
```

Needs JDK 25. The jar is written to `build/libs/`. Each push also runs the GitHub Actions `build` workflow,
which uploads the jar as an artifact.
