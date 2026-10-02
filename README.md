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
- The pug is a full pet: it can sit, follows its owner, keeps the collar color inherited from its parents, and can wear wolf armor. It is 30 % smaller than a wolf.
- Includes a **pug spawn egg** (in the Spawn Eggs tab of the creative inventory, or `/give @s minepug:pug_spawn_egg`). Using the egg on an adult pug produces a baby pug.

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
