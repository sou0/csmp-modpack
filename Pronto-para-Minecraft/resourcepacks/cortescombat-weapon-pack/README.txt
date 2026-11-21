WEAPON PACK OF ASSETS - Cortes Combat
=======================================

This folder is the home of your weapons' assets. Activate it as a resource pack
(Options > Resource Packs), or the server offers it on join in multiplayer.

Where everything goes (replace 'cortescombat' with your item's namespace):

  assets/<ns>/models/item/<id>.json     the item's icon (the model)
  assets/<ns>/textures/item/<id>.png     its texture
  assets/<ns>/textures/particle/slash/   the slash sprite (32x160, 5 frames)
                                        referenced by texture.slash in the YAML
  assets/<ns>/poses/<id>.json            the animation, referenced by animation.pose
  assets/<ns>/sounds.json + sounds/      the sounds, referenced by sounds.swing, etc.

The STATS (damage, cone, charge...) go in config/cortescombat/weapons/*.yaml, NOT
here: that folder is read by both sides and this one is only loaded as a resource
pack.

On the first run the mod also writes an annotated example at
config/cortescombat/weapons/ejemplo.yaml.example, with every field commented in
Spanish and English. Copy it to mi_pack.yaml and edit what you like.

If the game complains about the pack_format when you activate it, the number it asks
for is the right one: put it in pack.mcmeta.

Full guide: docs/custom-weapons.md (English) and docs/custom-weapons.es.md (Spanish).
