# Minepug 🐶

A **Fabric** mod for **Minecraft Java Edition 26.3 (Wilderness Bound)**. Breed two tamed wolves and, with a bit of luck, the puppy will be a **pug (carlino)**.

> **Important:** Minepug is a third-party, unofficial mod. It is built **for** Minecraft Java Edition 26.3 *Wilderness Bound*, but it is **not** part of Minecraft, not part of the *Wilderness Bound* drop, and not a Mojang Studios product. It is not affiliated with or endorsed by Mojang or Microsoft.

## How it works

- Every wolf and pug carries a persistent **generation** counter in its data: a freshly tamed wild wolf is generation 0.
- When two tamed wolves breed, the chance of the puppy being a pug depends on the generations behind its parents:

| Parents' generation | Pug chance |
|---------------------|------------|
| 0 (freshly tamed)   | 10 %       |
| 1                   | 20 %       |
| 2                   | 30 %       |
| 3                   | 40 %       |
| 4                   | 50 %       |
| 5                   | 60 %       |
| 6                   | 70 %       |
| 7                   | 80 %       |
| 8 or more           | 90 % (max) |

- The puppy, whether wolf or pug, inherits its parents' generation + 1, so the lineage keeps counting on every breeding.
- The pug is a full pet: it can sit and follows its owner. It is 30 % smaller than a wolf.
- Includes a **pug spawn egg** (in the Spawn Eggs tab of the creative inventory, or `/give @s minepug:pug_spawn_egg`). Using the egg on an adult pug produces a baby pug.

## Pug behavior

The pug has its own personality on top of the vanilla wolf AI:

- **Less damage** than wolves and **cannot wear wolf armor**.
- **Alert bark**: when a hostile mob comes close (12 blocks), the pug barks with a sharper pitch than a wolf's growl — even while sitting or away from the player's spawn. If it can reach the mob, it chases it down (without biting) to scare it off; creepers make it run to hide with its owner instead, and ghasts and wardens are only watched from a distance.
- **Sniffing**: when items are dropped nearby it walks over and sniffs them, head down. If the item is food (bones, meat, anything edible) it eats it after a short pause.
- **Furnace watch**: if there is food cooking in a nearby furnace (or smoker/blast furnace) it sits down in front of it and watches.
- **Curiosity**: it occasionally approaches players, passive animals, hostile mobs and dropped items, planting itself and visibly turning and tilting its head to stare at whatever caught its eye.
- **Zoomies**: it bursts into running in smooth circles (with dust puffs at its paws) for a few seconds, then sits for a few seconds, when it meets a passive mob, when it runs into a dog it hasn't seen in a while (not always), and occasionally at random.
- **Pug + pug breeding always produces a pug**; mixing a pug with a wolf uses the generation-based chance.

## Hidden personality

Every pug rolls two hidden traits on creation (persisted with it and **inherited from its pug parents**: the average of both parents plus a little variation):

- **Clinginess** (0 = independent, 1 = total velcro dog): higher clinginess keeps it closer to its owner and makes it pay less attention to ambient things (fewer zoomies and curiosities). When it is cold or wet it sticks even closer.
- **Obedience** (0 = disobedient, 1 = obedient): with low obedience it will more easily get up to do its own thing even after being told to sit. If it is also clingy and can reach you, it comes trotting after you; otherwise it wanders a while within a radius that grows as its obedience drops.

The distance it will travel for ambient activities (sniffing, curiosity, zoomies, furnace watching, chasing, cooling off) scales with both traits: independent, disobedient pugs roam far; clingy, obedient pugs stay close to you.

Use **`/minepug info`** to reveal the traits of the pug nearest to you.

## Colour variant and litters

- **Black pugs**: a rarer variant (10 % of pug births, 30 % with one black parent, 75 % with two). Black parents pass it on; it can also show up from plain pugs. Newborns from spawn eggs have a 15 % chance.
- **Litters**: pugs sometimes have **twins** — the chance starts at 10 % and grows with the parents' generation (up to 35 %).

## Other quirks

- **Owner reunion**: if you've been away for a couple of minutes it greets you with happy barks and zoomies.
- **Tail chasing**: some random zoomies are a quick little spin chasing its own tail.
- **Play with dogs**: when it re-meets a dog it hasn't seen in a while, it runs play circles around it.
- **Creeper fear**: it never chases creepers; instead it runs to hide with its owner (or flees the other way if it has none).
- **Heat and cold**: in hot biomes during sunny days it looks for water and lies down to cool off; when it's cold or raining it stays extra close to you.
- **Sploot**: from time to time it flops belly-down to rest (with little snores), preferring a cushion if there is one nearby.
- **Begging**: if you hold food it comes over, sits on its own and hops for a treat.
- **Pug noises**: higher-pitched voice, snorts when sniffing, snores when resting and the occasional pug puff 💨.

Despite all of this... it's still a pug.

## Chances

Values live in `src/main/java/com/minepug/MinepugConfig.java`:

```java
BASE_CHANCE = 0.10           // chance with generation-0 parents
CHANCE_PER_GENERATION = 0.10 // +10 % per parents' generation
MAX_CHANCE = 0.90            // cap
```

## Building

Requirements: **JDK 25 or newer** (tested with JDK 25 and 27).

```
gradlew.bat build
```

The jar is written to `build/libs/minepug-1.0.0.jar`. Copy it into the `mods` folder of your Fabric 26.3 installation, **along with Fabric API** (on 26.3 the built-in registries are frozen after bootstrap, and Fabric API is required for mods to register content).

## Project structure

```
src/main/java/com/minepug/
├── Minepug.java                  # mod entrypoint (+ creative tab hook)
├── MinepugEntityTypes.java       # "minepug:pug" entity type
├── MinepugItems.java             # spawn egg
├── PugEntity.java                # the pug (extends Wolf)
├── MinepugWolf.java              # mixin access bridge
├── MinepugConfig.java            # probabilities
├── mixin/
│   ├── WolfMixin.java            # generation counter (persisted)
│   ├── AnimalMixin.java          # swaps the offspring for a pug
│   ├── DefaultAttributesMixin.java # pug attributes
│   └── client/
│       └── EntityRenderersMixin.java    # renderer (PugRenderer)
└── client/
    ├── MinepugClient.java
    ├── model/PugModel.java      # pug model (ported from MorePugMod)
    └── render/PugRenderer.java  # renderer using the wolf render state
src/main/resources/assets/minepug/
├── textures/entity/pug.png       # pug texture (64x64, from MorePugMod)
├── textures/item/pug_spawn_egg.png
├── items/pug_spawn_egg.json      # item model definition
├── models/item/pug_spawn_egg.json
├── lang/en_us.json
├── lang/es_es.json
└── icon.png
```

The pug model is a dedicated **pug-shaped model** (big head, chunky body, curled tail) ported from MorePugMod, with walk/sit/water-shake animations based on the vanilla 26.3 wolf. Babies reuse the same model with automatic age scaling.

## Promotional material

The `promo/` folder contains a banner (21:9) and a promotional image (16:9) generated from the pug texture.

## License

MIT — see `LICENSE`.

### Pug model & texture

The pug model geometry and texture come from **MorePugMod** (pugpoggg, MIT) and were ported to 26.3. Full details and the complete license text are in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
