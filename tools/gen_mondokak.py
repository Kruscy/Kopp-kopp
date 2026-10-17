import re

MD = r"D:\ApK\mondokak\mondokak_szoveg.md"
KT = r"D:\ApK\app\src\main\java\hu\apk\kidsapp\ui\screens\stories\MondokaData.kt"

emoji = ["🍈", "🐦", "🌧️", "🐦", "🍞", "❄️", "🖐️", "🍎", "🐌", "🎠", "☀️", "🚶", "🕯️", "🪶", "🎻", "🐷", "🐐", "👁️", "🧔", "🌹", "👜", "🐴", "🥚", "🔥", "🚢", "🌾", "🔨", "🐰", "🧂", "🎩", "🐖", "🐻"]

md = open(MD, encoding="utf-8").read()
items = []
for sec in re.split(r"\n(?=## \d+\. )", md)[1:]:
    lines = sec.strip().splitlines()
    title = re.match(r"## \d+\. (.*)", lines[0]).group(1).strip()
    body = [l for l in lines[1:] if not l.startswith("*(")]
    text = "\n".join(l.strip() for l in body).strip()
    text = re.sub(r"\n\s*\n", "\n", text)
    items.append((title, text))
assert len(items) == 32, len(items)


def esc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$").replace("\n", "\\n")


out = ["package hu.apk.kidsapp.ui.screens.stories", "", "import hu.apk.kidsapp.R", "",
       "// Generált fájl (tools/gen_mondokak.py): a mondókák szövege a mondokak/mondokak_szoveg.md-ből.",
       "// Minden mondóka egyoldalas Story, hogy ugyanaz a polc és olvasó használható legyen.",
       "val mondokak: List<Story> = listOf("]
for i, (title, text) in enumerate(items, 1):
    out.append(f'    Story("{esc(title)}", "{emoji[i-1]}", listOf(StoryPage(R.drawable.mondoka_{i:02d}, "mondoka_{i:02d}", "{esc(text)}"))),')
out.append(")")
open(KT, "w", encoding="utf-8").write("\n".join(out) + "\n")
print("kész:", len(items))
