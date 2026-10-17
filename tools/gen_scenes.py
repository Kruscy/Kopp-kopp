import os
from scenes import STICKERS, SCENES

DRAW = r"D:\ApK\app\src\main\res\drawable-nodpi"
KT = r"D:\ApK\app\src\main\java\hu\apk\kidsapp\ui\screens\scene\SceneData.kt"
os.makedirs(os.path.dirname(KT), exist_ok=True)

st_by_id = {s[0]: s for s in STICKERS}
emoji = {"town": "🏙️", "shop": "🥐", "house": "🏠", "dino": "🦖", "space": "🚀", "forest": "🌲"}
kinds = {"driver": "DRIVER", "walker": "WALKER", "flyer": "FLYER", "static": "STATIC", "lamp": "LAMP", "egg": "EGG", "rocket": "ROCKET"}

lines = ["package hu.apk.kidsapp.ui.screens.scene", "", "import hu.apk.kidsapp.R", "",
         "// Generált fájl (lásd matrica/ mappa): a matricás világ jelenetei és matricái.",
         "enum class Kind { DRIVER, WALKER, FLYER, STATIC, LAMP, EGG, ROCKET }", "",
         "class StickerDef(val id: String, val image: Int, val name: String, val kind: Kind, val h: Float)", "",
         "class SceneDef(val id: String, val title: String, val emoji: String, val bg: Int, val stickers: List<StickerDef>)", "",
         "val sceneWorlds: List<SceneDef> = listOf("]
used = 0
for sid, title, bgdesc, ids in SCENES:
    if not os.path.exists(os.path.join(DRAW, f"bg_{sid}.webp")):
        print("kihagyva (nincs háttér):", sid)
        continue
    have = [i for i in ids if os.path.exists(os.path.join(DRAW, f"st_{i}.webp"))]
    if len(have) < 8:
        print("kihagyva (kevés matrica):", sid, len(have))
        continue
    lines.append(f'    SceneDef("{sid}", "{title}", "{emoji[sid]}", R.drawable.bg_{sid}, listOf(')
    for i in have:
        _, name, _, kind, h = st_by_id[i]
        lines.append(f'        StickerDef("{i}", R.drawable.st_{i}, "{name}", Kind.{kinds[kind]}, {h}f),')
    lines.append("    )),")
    used += 1
    miss = [i for i in ids if i not in have]
    if miss:
        print(sid, "hiányzik:", miss)
lines.append(")")
open(KT, "w", encoding="utf-8").write("\n".join(lines) + "\n")
print("jelenetek:", used)
