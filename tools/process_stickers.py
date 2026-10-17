import glob, json, os, sys, urllib.request
from PIL import Image
from rembg import new_session, remove
from scenes import STICKERS, SCENES

TR = r"C:\Users\Krusk\.claude\projects\D--Claude\f369c39d-bb8c-4845-a266-f5f6a0af95b2\tool-results"
RAW = r"D:\ApK\matrica\raw"
DRAW = r"D:\ApK\app\src\main\res\drawable-nodpi"
os.makedirs(RAW, exist_ok=True)

BG_KEYS = {
    "shop": "bakery shop interior",
    "house": "living room interior",
    "dino": "prehistoric landscape",
    "space": "outer space background",
    "forest": "sunny forest glade",
}

# generation_id -> (prompt, url)
gens = {}
for f in glob.glob(TR + r"\mcp-c33cc947-947a-4bda-981e-2918531021d3-creative_get_flow_run_status-*.txt"):
    try:
        t = open(f, encoding="utf-8").read()
        d = json.loads(t[t.index("{"):])
    except Exception:
        continue
    for m in d.get("media", []):
        if m.get("kind") == "image" and m.get("prompt"):
            gens[m["generation_id"]] = (m["prompt"], m.get("master_url") or m["url"])

found_st, found_bg = {}, {}
for g, (prompt, url) in gens.items():
    if "sticker illustration" in prompt:
        for sid, name, desc, kind, h in STICKERS:
            if ("one single " + desc) in prompt:
                found_st[sid] = url
                break
    else:
        for sc, key in BG_KEYS.items():
            if key in prompt:
                found_bg[sc] = url

print("matrica:", len(found_st), "/", len(STICKERS), "hiányzik:", [s[0] for s in STICKERS if s[0] not in found_st and s[0] != "car"])
print("háttér:", sorted(found_bg), "hiányzik:", [k for k in BG_KEYS if k not in found_bg])

session = new_session("u2net")


def cut(src_png, out_webp, longest=520):
    im = Image.open(src_png).convert("RGB")
    out = remove(im, session=session)
    bbox = out.getbbox()
    out = out.crop(bbox)
    w, h = out.size
    s = longest / max(w, h)
    out = out.resize((max(1, int(w * s)), max(1, int(h * s))), Image.LANCZOS)
    out.save(out_webp, "WEBP", quality=88, method=6)
    return os.path.getsize(out_webp)


total = 0
for sid, url in found_st.items():
    p = os.path.join(RAW, f"st_{sid}.png")
    if not os.path.exists(p):
        urllib.request.urlretrieve(url, p)
    out = os.path.join(DRAW, f"st_{sid}.webp")
    total += cut(p, out)
# a kocsi és a városháttér a korábbi tesztből
if os.path.exists(os.path.join(RAW, "test_car.png")):
    total += cut(os.path.join(RAW, "test_car.png"), os.path.join(DRAW, "st_car.webp"))
print("matricák webp: %.1f MB" % (total / 1e6))

for sc, url in found_bg.items():
    p = os.path.join(RAW, f"bg_{sc}.png")
    if not os.path.exists(p):
        urllib.request.urlretrieve(url, p)
    Image.open(p).convert("RGB").save(os.path.join(DRAW, f"bg_{sc}.webp"), "WEBP", quality=80, method=6)
Image.open(os.path.join(RAW, "test_town_bg.png")).convert("RGB").save(os.path.join(DRAW, "bg_town.webp"), "WEBP", quality=80, method=6)
print("kész")
