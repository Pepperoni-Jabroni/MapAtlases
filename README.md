# Map Atlases

<a href="https://www.curseforge.com/minecraft/mc-mods/fabric-api"><img src="https://i.imgur.com/Ol1Tcf8.png" width="149" height="50" title="Fabric API" alt="Fabric API"></a>

<a href="https://www.curseforge.com/minecraft/mc-mods/map-atlases"><img alt="Curseforge" src="https://cf.way2muchnoise.eu/full_436298_downloads.svg"></a> <a href="https://modrinth.com/mod/map-atlases"><img alt="Modrinth" src="https://img.shields.io/modrinth/dt/map-atlases?label=Modrinth%20Downloads"></a> <a href="https://github.com/Pepperoni-Jabroni/MapAtlases"><img alt="GitHub" src="https://img.shields.io/github/downloads/Pepperoni-Jabroni/MapAtlases/total?label=Downloads&logo=github"></a>


A vanilla-friendly mini-map/world-view mod using vanilla Maps, introducing the "Atlas".

The "Atlas" features and details:
- Crafted with a **Filled** Map (the source map), a Book, and something sticky (Slimeball or Honey Bottle). The source map determines the Atlas' scale. *Who knew this whole time, all you had to do to get a mini-map was slap a map onto a book?*
- When the Atlas is on your hot-bar or off-hand, it will render your current-position Map on the HUD if a such Map exists inside the Atlas.
- You can put both Filled Maps and Empty Maps with an Atlas in a crafting inventory to insert either Map type into the Atlas.
- Filled Maps which are added **must** be of same Scale.
- There's a maximum of 512 Maps in each Atlas (configurable/togglable).
- **Atlases do work inter-dimensionally**. They create new Maps in new Dimensions as you'd expect.
- Filled Maps inside the Atlas will continue updating your location & world-state.
- Empty Maps are consumed when you enter an un-mapped region to generate a new Map of the corresponding Scale.
- If the player is in Creative, un-mapped regions will generate new Maps even if there's no Empty Maps in the Atlas.
- You may also cut out Maps when putting it in a Crafting Grid with Shears. 
   - This will remove Filled Maps from the Atlas until only the Source Map is left, which cannot be cut out. 
   - It will then start removing Empty Maps from the Atlas if there are no Filled Maps left inside (besides Source Map).
   - This does consume Shear's durability (Unbreaking still functions though).
- The Atlas has a world map view as well. To access this, right-click the Atlas or press "m" while the Atlas is on your hot-bar.
- The Atlas world view has zoom support & scales by odd numbers 1x1, 3x3, 5x5, 7x7, etc using your mouse scroll wheel.
- The Atlas world view has full pan support as well, using your mouse to drag the world map.
- Atlases also support right-clicking Banners to add "waypoints", which display in both the mini-map & world-view map.
- Atlases can be **merged** with a Cartography Table - simply put an Atlas in the top & bottom slots and the Atlases will combine their mapped regions with one-another!
- You can mass-add entire stacks of Empty Maps to the Atlas using a Cartography Table as well.
- **Optional Trinkets support**: 
   - When Trinkets is installed, there will be a new Slot for an Atlas
   - The Atlas will continue to function as normal inside the Trinkets slot
- Atlases have 3 sounds: An opening world-view sound, a new map drawing sound, & a page flipping (new mini-map displaying) sound.
- The mod is extremely highly configurable and offers in-game config editing with ModMenu. See the current config [options here](https://github.com/Pepperoni-Jabroni/MapAtlases/blob/main/src/main/java/pepjebs/mapatlases/config/MapAtlasesConfig.java)!

## Crafting an Atlas
![2022-06-24_19 46 45](https://user-images.githubusercontent.com/17690401/175755582-aecd94b1-ac3a-4686-a3d5-82cea1e3583d.png)
![2022-06-24_19 47 16](https://user-images.githubusercontent.com/17690401/175755583-83e57650-ce2b-49e3-93e6-a0cf67ff1d0d.png)

## Maps inside the Atlas will render if the Atlas is on your hot-bar
![2022-06-24_19 45 51](https://user-images.githubusercontent.com/17690401/175755590-dedbaaf0-f970-4755-a42f-484264609811.png)

## Adding more Maps to an Atlas
![2022-06-24_19 48 05](https://user-images.githubusercontent.com/17690401/175755596-5895ebab-b1a2-4c58-bc70-dcb03083762f.png)

## Current Map is rendered when you move locations
![java_VKNugiTAlO (online-video-cutter](https://user-images.githubusercontent.com/17690401/182008727-dd3a0d38-b493-4367-8b9e-cf873442373a.gif)

## Custom Tooltip
![2022-06-24_19 48 21](https://user-images.githubusercontent.com/17690401/175755670-3819eca7-cbc4-4be5-a7c8-3d4286dacd19.png)

## World Map View
![untitled](https://user-images.githubusercontent.com/17690401/182009108-f87ca806-a698-4c4f-a62b-ab10ca328ead.GIF)

## Cutting a Map out of an Atlas
![2022-06-24_19 48 45](https://user-images.githubusercontent.com/17690401/175755627-bf5ff6b5-752d-4bfd-85d2-82c863bc1257.png)

## Merging 2 Atlases
![2022-06-24_19 46 20](https://user-images.githubusercontent.com/17690401/175755632-2c6d953d-2ce2-4020-b2ff-ee5cd85aa6f6.png)

## Mass adding Empty Maps to Atlas
![2022-06-24_19 46 08](https://user-images.githubusercontent.com/17690401/175755635-751ed66c-11f2-448e-96e4-7cf20d2ddc07.png)

## Trinkets support
![2022-07-09_11 57 22](https://user-images.githubusercontent.com/17690401/178119933-adba64dc-1ba6-425d-8608-40d98b722cb8.png)

## End Atlas Mini-Map
![2022-07-09_11 55 46](https://user-images.githubusercontent.com/17690401/178119945-5a5bde0c-48de-4ab2-92a2-1607fd2c7387.png)

## End Atlas World-Map
![2022-07-09_11 56 18](https://user-images.githubusercontent.com/17690401/178119955-d1a90fc7-114c-483e-903b-456f0bd74066.png)

## Newly textured world-map background
![2022-08-26_18 06 28](https://user-images.githubusercontent.com/17690401/187008291-33800e98-04c9-4340-a433-91a094fe56b6.png)

## New as of 2.2.0
**Atlases can now be displayed in Lecterns!**
And each dimension has its own texture - collect 'em all!
![atlas_lectern_promo](https://user-images.githubusercontent.com/17690401/196099897-b22d161b-c357-46dd-9087-84a642b1ab53.png)

Sound Sources
- [Atlas open sound](https://freesound.org/people/InspectorJ/sounds/416179/)
- [Atlas page flip sound](https://freesound.org/people/flag2/sounds/63318/)
- [Atlas new map creation sound](https://freesound.org/people/Tomoyo%20Ichijouji/sounds/211247/)

Required Dependencies:
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config)

Optional Dependencies:
- [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu)
- [Trinkets](https://www.curseforge.com/minecraft/mc-mods/trinkets)

Known Incompatiblities:
- [Accurate Maps](https://www.curseforge.com/minecraft/mc-mods/accurate-maps) (*todo: fix here or submit PR to their project*)
- [Immersive Portals](https://www.curseforge.com/minecraft/mc-mods/immersive-portals-mod)

Recommended Additions:
- [Better Nether Map](https://modrinth.com/mod/better-nether-map) - Makes Nether (& other ceiling dimension) Maps create at y-level of player
- Cheaper Maps Recipe Data Pack [[1.19]](https://github.com/Pepperoni-Jabroni/MapAtlases/releases/download/2.0.1/cheaper-map-crafting+1.19.zip) - Why would a Map require a Compass? This Data Pack adds a recipe for Empty Maps that's simply 1 Paper + 1 Ink Sac shapeless. 
- Optional "Modern" Resource Pack [[1.19]](https://github.com/Pepperoni-Jabroni/MapAtlases/releases/download/2.1.0/map_atlases_modern_resource_pack+1.19.zip) - This Resource Pack gives the Mini-map and World-map a "modern" look, abandoning the vanilla Map style textures.

## Photo of "Modern" Resource Pack & Better Nether Map mod
![2022-09-15_19 51 51](https://user-images.githubusercontent.com/17690401/190546424-894fc024-884f-4cea-a8e4-0315643fb7f9.png)

![2022-09-15_19 50 29](https://user-images.githubusercontent.com/17690401/190546249-388c58b5-99de-463c-b0d8-d3c40ebb4c33.png)
