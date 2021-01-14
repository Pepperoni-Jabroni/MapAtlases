# Map Atlases (In-Dev)

<a href="https://www.curseforge.com/minecraft/mc-mods/fabric-api"><img src="https://i.imgur.com/Ol1Tcf8.png" width="149" height="50" title="Fabric API" alt="Fabric API"></a>

A vanilla-friendly mini-map/world-view mod using vanilla Maps, introducing the "Atlas".

The "Atlas" features and details:
- Crafted with a **Filled** Map (the source map), a Book, and something sticky (Slimeball or Honey Bottle). The source map determines the Atlas' scale. *Who knew this whole time, all you had to do to get a mini-map was slap a map onto a book?*
- When the Atlas is on your hot-bar, it will render your current-position Map on the HUD if a such Map exists inside the Atlas.
- You can put both Filled Maps and Empty Maps with an Atlas in a crafting inventory to insert either Map type into the Atlas.
- Filled Maps which are added **must** be of same Scale.
- There's a maximum of 128 Maps in each Atlas.
- Filled Maps inside the Atlas will continue updating your location & world-state.
- Empty Maps are consumed when you enter an un-mapped region to generate a new Map of the corresponding Scale.
- The Atlas has a world map view as well. To access this, right-click the Atlas or press "m" while the Atlas is on your hot-bar.
- The Atlas world view has 4 scales: 1x1, 3x3, 5x5, & 7x7. Use your mouse scroll to change zoom levels.
- The Atlas world view has full pan support as well, using your mouse to drag the world map.
- Atlases also support right-clicking Banners to add "waypoints", which display in both the mini-map & world-view map.
- Atlases have 3 sounds: An opening world-view sound, a new map drawing sound, & a page flipping (new mini-map displaying) sound.
- **Note**: Atlases are limited to the Overworld due to some buggy code out of my control.
- *Todo:* Config control for `activation_location` (inv, hot-bar, hands, main-hand, off-hand), `max_map_count`, `crafting_materials`, `is_multidimensional`, `force_scale`

Future ideas:
- Fix issues with Atlases in other dimensions.
- Putting an Atlas in a Lectern.

## Crafting an Atlas
![](https://i.imgur.com/yjKU4nO.png)

![](https://i.imgur.com/EwwBQ6d.png)

## Maps inside the Atlas will render if the Atlas is on your hot-bar
![](https://i.imgur.com/sPCpk0u.png)

## Adding more Maps to an Atlas
![](https://i.imgur.com/rIQxD2U.png)

## Current Map is rendered when you move locations
![](https://i.imgur.com/MwxT6uf.png)

## Custom Tooltip
![](https://i.imgur.com/XZqmjJT.png)

## World Map View 1x1
![](https://i.imgur.com/UUDgvnO.png)

## World Map View 3x3
![](https://i.imgur.com/yTu35Vz.png)

## World Map View 5x5
![](https://i.imgur.com/9PBGB4E.png)

## World Map View 7x7
![](https://i.imgur.com/FE4tiSn.png)

## World Map View 1x1 (& Mini-map) with a Banner
![](https://i.imgur.com/aWmjdNK.png)

Sound Sources
- [Atlas open sound](https://freesound.org/people/InspectorJ/sounds/416179/)
- [Atlas page flip sound](https://freesound.org/people/flag2/sounds/63318/)
- [Atlas new map creation sound](https://freesound.org/people/Tomoyo%20Ichijouji/sounds/211247/)
