import re

KT = r"D:\ApK\app\src\main\java\hu\apk\kidsapp\ui\screens\stories\StoryData.kt"
MD = r"D:\ApK\mesek\mesek_szoveg.md"

md = open(MD, encoding="utf-8").read()
tales = {}
cur = None
for line in md.splitlines():
    m = re.match(r"## .*\((\w+)\)\s*$", line)
    if m:
        cur = m.group(1)
        tales[cur] = []
        continue
    m = re.match(r"(\d+)\. (.*)", line)
    if m and cur:
        tales[cur].append(m.group(2).strip())
for k, v in tales.items():
    assert len(v) == 8, (k, len(v))

titles = {
    "kismalac": ("A három kismalac", "🐷"),
    "pogacsa": ("A pogácsa", "🥯"),
    "fa": ("Az égig érő fa", "🌳"),
    "kakas": ("A kiskakas gyémánt félkrajcárja", "🐓"),
    "kecske": ("A farkas és a hét kecskegida", "🐐"),
    "kacsa": ("A rút kiskacsa", "🦢"),
    "repa": ("A répa", "🥕"),
    "gomboc": ("A kis gömböc", "🍞"),
    "teknos": ("A teknős és a nyúl", "🐢"),
    "mezeskalacs": ("A mézeskalács-emberke", "🍪"),
    "eger": ("Az egér és az oroszlán", "🦁"),
    "szabo": ("A vitéz szabólegény", "✂️"),
    "tyuk": ("Az aranytojást tojó tyúk", "🐔"),
    "barany": ("A farkas és a bárány", "🐑"),
    "kandur": ("A Csizmás kandúr", "🐈"),
    "tuecsok": ("A tücsök és a hangya", "🦗"),
}
assert set(titles) == set(tales), (set(titles) ^ set(tales))

def esc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")

lines = []
lines.append("package hu.apk.kidsapp.ui.screens.stories")
lines.append("")
lines.append("import hu.apk.kidsapp.R")
lines.append("")
lines.append("// Ezt a fájlt a mesek/ mappa szövegéből generáltuk (saját újramesélés, szabad felhasználású).")
lines.append("class StoryPage(val image: Int, val clip: String, val text: String)")
lines.append("")
lines.append("class Story(val title: String, val emoji: String, val pages: List<StoryPage>) {")
lines.append("    val cover: Int get() = pages.first().image")
lines.append("}")
lines.append("")
lines.append("val stories: List<Story> = listOf(")
for key, (title, emoji) in titles.items():
    lines.append(f'    Story("{title}", "{emoji}", listOf(')
    for i, text in enumerate(tales[key], 1):
        lines.append(f'        StoryPage(R.drawable.mese_{key}_{i}, "mese_{key}_{i}", "{esc(text)}"),')
    lines.append("    )),")
lines.append(")")
open(KT, "w", encoding="utf-8").write("\n".join(lines) + "\n")
print("kész:", KT, len(tales), "mese")
