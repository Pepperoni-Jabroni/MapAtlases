# Map Atlases

<a href="https://www.curseforge.com/minecraft/mc-mods/fabric-api"><img src="https://i.imgur.com/Ol1Tcf8.png" width="149" height="50" title="Fabric API" alt="Fabric API"></a>

A vanilla-friendly Minimap mod using vanilla Maps. Adds a single new item: the "Atlas"!

The "Atlas" features and details:
- Crafted with a **Filled** Map (the source map), a Cartography Table, and a Book. The source map determines the Atlas' scale and dimension.
- When the Atlas is on your hot-bar, it will render your current-position Map on the HUD if a such Map exists inside the Atlas.
- You can put both Filled Maps and Empty Maps with an Atlas in a crafting inventory to insert either Map type into the Atlas.
- Filled Maps which are added **must** be of same Dimension & Scale.
- *Todo:* Filled Maps inside the Atlas will continue updating your location & world-state.
- *Todo:* Empty Maps are consumed when you enter an un-mapped region to generate a new Map of the corresponding Scale and Dimension.
- *Todo:* There's a maximum of 64 Maps in each Atlas.
- *Todo:* Right click activation for the Item to display all the Maps stitched together as a single GUI interface. Zoom and pan support would be awesome.
- *Todo:* Enable multi-dimensional Atlases, and potentially a new Item with it?
- *Todo:* Atlas in a lectern???
- *Todo:* Config control for `activation_location` (inv, hot-bar, hands, main-hand, off-hand), `max_map_count`, `crafting_materials`, `is_multidimensional`, `force_scale`

## Crafting an Atlas
![](https://i.imgur.com/vAMa0XF.png)

## Maps inside the Atlas will render if the Atlas is on your hot-bar
![](https://i.imgur.com/sPCpk0u.png)

## Adding more Maps to an Atlas
![](https://i.imgur.com/rIQxD2U.png)

## Current Map is rendered when you move locations
![](https://i.imgur.com/MwxT6uf.png)

## Custom Tooltip
![](https://i.imgur.com/XZqmjJT.png)
