# Kölyök Játékok (Kopp-kopp)

> **Nem kereskedelmi projekt.** A forráskód, a képek és a hangok jelenleg csak megtekintésre,
> személyes tesztelésre és a tulajdonossal egyeztetett közös fejlesztésre használhatók.
> Kereskedelmi forgalomba (pl. Play Áruházban árusításra) **nem** szabad tenni: a hangok és a
> képek AI-szolgáltatással készültek, a licencfeltételeiket a nyilvános megjelenés előtt még át
> kell nézni. Lásd: [LICENSE](LICENSE).

Android-játékgyűjtemény 4 éves gyerekeknek (PIN-zárolt kezdőlap, kioszk módú képernyőrögzítés).

## Telepítés a Play Áruház nélkül

A projekt jelenleg belső tesztelésben van a Play-en, de **nincs rá szükség**: az aláírt telepítő
közvetlenül feltölthető a készülékre.

1. Töltsd le a legfrissebb APK-t: [`telepito/kolyok-v26-1.25.apk`](telepito/kolyok-v26-1.25.apk)
   (a GitHubon a fájl oldalán a *Download raw file* gombbal; kb. 80 MB).
2. Nyisd meg a tableten/telefonon, és engedélyezd az ismeretlen forrásból való telepítést
   (Android 8.0+ / API 26+; a rendszer rákérdez, melyik alkalmazásnak engedélyezed).
3. Első indításkor az alkalmazás egy 4 jegyű PIN kódot kér a szülői zároláshoz.

Megjegyzés: ez az APK az app feltöltési kulcsával van aláírva. Ha a készüléken a Play-ről
telepített változat is van, előbb azt el kell távolítani (a két aláírás eltér), különben az
Android nem engedi a frissítést.

---

## Első lépések

1. Nyisd meg ezt a mappát (`D:\ApK`) **Android Studio**-val ("Open" -> válaszd ki a mappát).
2. Szinkronizálás (Gradle Sync) után válassz egy emulátort vagy csatlakoztatott
   Android tabletet/telefont (Android 8.0 / API 26+), és nyomj Run-t.

A build parancssorból is le van ellenőrizve (`./gradlew assembleDebug` sikeresen lefut, az APK
elkészül), szóval Android Studio-ban is simán szinkronizálódnia és futnia kell.

**Eszközlánc:** Gradle 9.8.0, Android Gradle Plugin 9.4.1, Kotlin 2.4.20 (AGP beépített Kotlin
támogatásával, külön `org.jetbrains.kotlin.android` plugin nélkül), Compose BOM 2026.09.00,
compileSdk/targetSdk 37. Ezekre azért volt szükség, mert az Android Studio által telepített JDK
(25-ös) csak a legújabb Gradle/AGP/Kotlin verziókkal kompatibilis.

## Jelenlegi állapot

- Kezdőlap 6 csempével: Rajzolj magadról, Színöntögető, Kockaépítő, Kígyó, Tortakészítő, Mozaik.
  A csempék megpöccennek (rugós kicsinyedés), amikor megérinted őket.
  (A YouTube Kids csempe a kérésre kikerült az appból.)
- PIN-kód beállítás első indításkor, majd PIN-védett Beállítások képernyő (PIN módosítás,
  automatikus zárolás be/ki kapcsoló).
- Zárolás/feloldás gomb a kezdőlapon: **Android beépített "Screen Pinning" (app pinning)**
  funkcióját használja — nem kell semmilyen külön telepítés vagy jogosultság hozzá. Feloldani
  csak a helyes PIN megadásával lehet.
  - Ha az eszközön még nincs bekapcsolva az "Alkalmazás rögzítése" (App pinning) rendszerbeállítás,
    a zárolás gomb nem fog látványosan tenni semmit. Ezt egyszer, kézzel kell bekapcsolni:
    **Beállítások -> Biztonság -> Továbbiak -> Alkalmazás rögzítése** (gyártótól függően eltérő
    helyen lehet).
- **1. játék – Rajzolj magadról! (kész):**
  - "Új fénykép": előlapi kamerával (nincs, ha nem elérhető, hátlapira esik vissza) fotó készítése.
  - **Arcfelismerés (ML Kit, a készüléken fut, nincs internethez kötve):** a kijelzőn egy fehér
    kör jelzi a céltartományt, ami **zöldre vált**, amint az egész arc (nem csak a homlok) belefér
    — kb. fél másodperc stabil, teljes arc után az app **magától lefotóz**. A kézi gomb is végig
    ott van tartalék megoldásként, ha valamiért nem indulna el magától a felismerés.
  - Rajzoló vászon: a fotó a háttér, bal oldali színpaletta (8 szín) + vékony/vastag ecset +
    visszavonás + törlés, jobb fent nagy pipa gomb a mentéshez.
  - **Egy fotózásból egy mentés:** minden fotózáskor egy közös azonosítót kap az eredeti kép és a
    kész (rárajzolt) verzió; a "Kész" gomb mindig ugyanazt a párost írja felül, nem hoz létre új
    fájlt minden koppintásnál. Ha egy meglévő rajzra rajzolsz rá újra és újra elmented, az is a
    saját (nem egy új) fájlját írja felül.
  - "Galéria": a mentett (kész) rajzok rácsos nézete; megnyitva egy rajzot lehet rá újra rajzolni
    vagy törölni (törléskor az eredeti fotó is törlődik).
  - A fotók és rajzok az app privát tárhelyén vannak (`filesDir/drawings/originals` és
    `filesDir/drawings/finished`), nincs szükség tárhely-jogosultságra. Kamera jogosultságot
    futásidőben kéri az app.
- **2. játék – Színöntögető (kész):**
  - Csövekre kell koppintani: első koppintás kiválasztja a forrást (kiemelkedik), második
    koppintás megpróbál belőle önteni a másik csőbe; ha nem lehet, csak átváltja a kiválasztást.
  - 12 fokozatosan nehezedő pálya (`game/pour/PourLevels.kt`): az 1. pálya 2 színnel, 1 mozdulattal
    megoldható, onnantól lassan nő a színek száma, a cső mérete és az üres csövek száma.
  - A pályák véletlenszerűen generálódnak, de a generátor egy beépített megoldó-kereséssel
    (`isSolvable` BFS a `game/pour/PourEngine.kt`-ban) ellenőrzi, hogy tényleg kirakható-e, mielőtt
    megmutatná — így garantáltan nincs megoldhatatlan pálya.
  - "Új keverés" gomb: ha egy pálya elakadna, újrakeveri ugyanazt a nehézségi szintet.
  - Megoldás után felugró "Következő pálya" ablak; a haladás (hányadik pályánál tart) elmentődik,
    legközelebb onnan folytatja.
  - **Üveg-hatás:** a csövek gradiens hátteret és egy üveges fénycsíkot kaptak, a folyadék-szintek
    is gradiens-árnyalásúak (mintha fény törne meg bennük), a legfelső réteg lekerekített
    "domború" felülettel. A folyadék most már mindig a pohár lekerekített formáján **belül**
    marad (korábban a szögletes sávok kilógtak a kerekített sarkoknál — ez javítva).
  - **"Lebegő öntés" animáció** (kb. 1,4 mp): koppintásra a forrás-pohár **ténylegesen felemelkedik,
    megdől és átlebeg** a cél-pohár fölé, ott "kiönti" a tartalmát (a forrás szintje csökken, a
    célé nő), majd visszalebeg a helyére — nem csak egyszerűen átugrik a szín. Közben mindkét
    pohár sárgán kiemelkedik, amíg tart az öntés (közben nem lehet újat koppintani).
    **Javítva:** a lebegés/dőlés mértékét csökkentettem és plusz helyet hagytam felül, mert
    korábban a pohár teteje kilóghatott a látható képből.
- **3. játék – Kockaépítő (kész, átdolgozva):**
  - **Átalakítva a visszajelzésed alapján**: a korábbi izometrikus rácsos elrendezés helyett most
    egy **egysoros, inkább oldalnézetes** polc van — a kockákat csak egymás mellé (egy sorba)
    lehet lerakni, oszloponként felfelé egymásra pakolva. Minden kocka egy egyszerű, letisztult
    "extrudált" dobozként jelenik meg (nagy, jól olvasható előlap + vékony felső/oldalsó csík a
    mélységhez) — ettől még megvan a 3D hatása, de nem isometrikus/zavaró, hanem side-view-szerű.
  - Bal oldalon egy oszlopban 6 színes kocka-minta: húzd ki onnan egy oszlopra, hogy lerakj egy
    kockát rá.
  - Egy már lerakott kockát megfogva vissza lehet húzni a bal oldali paletta fölé — ez leveszi az
    oszlop tetejéről; vagy át lehet húzni egy másik oszlopra, hogy odarakja.
  - "Kuka" gomb jobb fent: egy koppintással törli az egész építményt, ha újra akarnak kezdeni.
  - Fizika/gravitáció nincs (egy 4 évesnek ez csak félreértést okozna) — a kockák egyszerűen
    egymásra épülnek, ahogy rárakod őket.
  - Gradiens-árnyalt, fényfoltos kockák, lágy (nem kemény fekete) élekkel; kontakt-árnyék a
    torony alatt; lerakáskor rugósan pattanva esik a helyére.
- **4. játék – Kígyó (kész):**
  - Irányítás: nincs D-pad, az **egész képernyőn** lehet húzni az ujjal — a kígyó a húzás
    irányába fordul (folyamatosan figyeli, amíg húzod, tehát menet közben is lehet irányt
    váltani).
  - Szándékosan **megbocsátó**, stresszmentes verzió egy 4 évesnek: nincs "game over" —
    a kígyó a szélen körbeér a másik oldalra, és önmagával is simán átfedhet, nem hal meg.
  - Lassú tempó (700ms/lépés), gyümölcs (piros pötty) felevésekor nő a kígyó és nő a
    "Pontszám" a tetején; jobb fent "Új játék" gomb az újrakezdéshez.
- **5. játék – Tortakészítő (kész):**
  - **Kör alapú, 3D hengeres torta:** minden réteg egy hengerként jelenik meg (gradiens-árnyalt
    oldalfal + fénylő, kerek tetőlap), felfelé haladva egyre keskenyedik, kerek tányéron áll.
    Lerakáskor a réteg fentről pattanva esik a helyére, ugyanúgy mint a kockaépítőben.
  - Javítva: a henger most a **teljes, kerek testével** látszik (korábban egy forgásirány-hiba
    miatt "megharapva" nézett ki elölről), és lágyabb, lekerekített élekkel rajzolódik.
  - **Helyes rétegsorrend:** minden réteg a saját mázával együtt rajzolódik, mielőtt a felette
    lévő réteg megjelenne — így egy alsóbb réteg (és annak máza) mindig helyesen a felette lévő
    réteg *alatt* marad, sosem lóg ki elé.
  - **Csak a legfelső réteg mázolható** (mindig az aktuális tetejét öntöd le) — az alsóbb
    rétegeken lévő máz nem változik, akkor sem, ha később egy újabb réteget teszel a tetejére.
  - **Egy húzás = teljes bevonat**: egyetlen máz-húzásra a teljes tető **egészben** befedődik
    (kb. 1,3 mp alatt animálva terjed szét körbe), és körben hullámosan lecsurog az oldalára —
    ahogy a képen küldött tortákon. Ha újra ráhúzod (akár másik színt), az egész felület
    újra befestődik az új színnel.
  - Bal oldali paletta: 1 réteg-minta (kis hengerként jelenik meg) + 1 máz-minta (kinyomózsák
    formájú, cseppel a hegyén) + 8 dísz (cseresznye, csillag, szórópor, gyertya, szív, virág,
    csokidarab, eper).
  - **Színválasztó menü egy koppintásra** (a korábbi hosszú nyomás helyett): koppints a réteg-
    mintára vagy a máz-mintára, és mellette kinyílik egy menü a választható színekkel, a
    mintákkal együtt (így látszik is, melyik szín milyen). Réteg: csokis, vaníliás, epres,
    áfonyás, matcha zöld; máz: fehér, eper, citrom, csokis. A kiválasztott színnel **azonnal
    animáció teszi a tortára** a formát (fentről pattanva esik a helyére) vagy a mázat
    (ráfolyik, lecsurog). Ha a menün kívülre koppintasz, bezárul. A paletta-mintát továbbra is
    ki lehet húzni a tortára az épp kiválasztott színnel.
  - A díszek pontosan oda kerülnek, ahova leejtetted őket; húzd ki bármelyik palettaelemet a
    képernyő bármely részére, nem kell pontosan célozni.
  - **Konyha háttér**: csempés fal + fa munkapult, amin a tányér áll.
  - Jobb fent két gomb: "Visszavonás" (mindig a legutóbb hozzáadott elemet veszi le, akár
    réteg, máz-lépés vagy dísz volt — máz visszavonásnál a korábbi mázszínt állítja vissza, ha
    volt) és "Kuka" (teljes törlés/újrakezdés).
- **6. játék – Mozaik (kész, új):**
  - Egy nagy négyzetrács, amit a képernyő méretéhez igazodva tölt ki a program annyi cellával,
    amennyi elfér.
  - Bal oldalt **15 különböző szín** (két oszlopban) — koppints egyre, hogy kiválaszd, utána
    koppints vagy **húzd végig az ujjad** a rácson, és útközben minden cella, amin áthaladsz,
    felveszi a kiválasztott színt — mint egy digitális mozaik-/pixel-kifestő.
  - Jobb fent "Kuka" gomb: egy koppintással üresre törli az egész rácsot.

## Minden játék elkészült

1. ~~Fényképezés + rajzolás a fotóra + mentett rajzok galériája~~ — **kész**
2. ~~Szín-öntögetős puzzle (water sort), fokozatosan nehezedő pályákkal~~ — **kész**
3. ~~3D hatású kockaépítő~~ — **kész** (később egysoros, oldalnézetesebb formára átdolgozva)
4. ~~Lassú, egyszerűsített kígyó játék~~ — **kész**
5. ~~Tortakészítő, bal oldali hozzávaló-választóval~~ — **kész**
6. ~~Mozaik (nagy négyzetrács, sok színnel)~~ — **kész**

## Kiadás a Play Store-on (belső tesztelés)

A tablet automatikus frissítéséhez a Google Play belső teszt sávját használjuk:

- **Aláíró kulcs**: `app/kolyok-release.jks`, jelszavak: `keystore.properties` (a projekt
  gyökerében, **nincs verziókövetésbe téve** — ha git-et használnál, a `.gitignore` már kizárja).
  **Ezt a két fájlt örökre meg kell őrizni** — nélkülük nem lehet többé frissítést kiadni
  ugyanahhoz az app-hoz. Érdemes biztonságos helyre (pl. jelszókezelő, felhő) is kimenteni.
- **Csomagnév (applicationId):** `hu.apk.kidsapp`
- Aláírt AAB build: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- Minden új feltöltés előtt az `app/build.gradle.kts`-ben a `versionCode`-ot eggyel emelni kell
  (a `versionName` is frissíthető, de az csak megjelenítési szöveg).
- Play Console → Testing → Internal testing → Create new release → az AAB feltöltése →
  Tesztelők hozzáadása (a tableten használt Google-fiók) → a tesztelő az "opt-in" linken
  csatlakozik és telepíti a Play Store-ból. Onnantól minden új belső teszt kiadás automatikusan
  frissül a tableten.

## Gyerekbiztosítás (összenyomkodás elleni javítások, v3 / 1.2)

Ha a gyerek gyorsan, sokszor rányom gombokra, korábban fehér képernyő maradhatott. Okok és javítások:

- **Navigáció (fő ok):** minden Vissza/Kész gomb nyers `popBackStack()`-et hívott, így gyors
  többszöri nyomásra a kezdőlap is kipattant, és az üres verem fehér képernyőt adott. Mostantól
  minden navigáció csak akkor fut le, ha az aktuális képernyő nincs átmenetben, a kezdőlap nem
  pattintható ki, a dupla koppintás nem nyit meg két képernyőt (`launchSingleTop`), és ha
  valamiért mégis üres lenne a verem, az app automatikusan visszatér a kezdőlapra.
- **Rajzolós "Kész" gomb:** korábban minden koppintás újabb, többmásodperces mentést indított a
  főszálon (lefagyás), és többször hívta a visszalépést. Most csak egyszer menti, háttérszálon, és a
  fotót kisebb felbontásban tölti be (nincs OutOfMemory a nagy kamerakép miatt).
- **Színöntögető:** "Új keverés"/"Következő pálya" öntés közben megszakítja a folyamatban lévő
  öntést (előtte az az új táblára írt volna, akár összeomlást is okozva). A pályagenerátor
  megoldás-keresője szélességi helyett mélységi keresésre váltott, így magasabb pályákon sem fagyaszt.
- **Kígyó:** a gyümölcs-elhelyezés nem akadhat végtelen ciklusba, ha a kígyó betöltené az egész rácsot.
- **Torta:** gyors egymás utáni mázolásnál a máz állapota már nem veszhet el.

## A Vissza gomb nem takarja a színeket (v4 / 1.3)

A rajzolós és a Mozaik játékban a Vissza gomb a bal felső sarokban úszik a tartalom fölött, és
korábban eltakarhatta az első színeket. Mostantól a színpaletta a gomb alatt kezdődik (kb. 100dp),
a rajzoló képernyő bal sávja pedig görgethető is, ha a kijelző alacsony; a Mozaik palettájának
sorköze a kijelző magasságához igazodik.

## Amit még érdemes végiggondolni

- **Arcfelismerés valós tesztje**: ezt a funkciót itt nem tudtam élesben kipróbálni (nincs
  kamerás eszköz ehhez a gépen) — a build lefordul és a natív ML Kit könyvtár be is épül az
  APK-ba, de érdemes lesz a tableten kipróbálni, hogy a zöld keret időzítése/érzékenysége
  megfelelő-e (jelenleg kb. 0,55 mp stabil, teljes arc kell az automata kioldáshoz).
- **APK/AAB méret**: az arcfelismerés natív könyvtárai miatt a debug APK kb. 21 MB-ról ~60 MB-ra
  nőtt (minden processzor-architektúrához csomagolva). A Play Store-on keresztüli telepítésnél
  (AAB) ez nem probléma — a Play automatikusan csak a tablet saját architektúrájához való
  részeket küldi le, így a tényleges letöltés jóval kisebb lesz.
- **Első valódi teszt a gyerekkel**: érdemes minden játékot kipróbálni vele, és visszajelzés
  alapján finomhangolni (pl. a színöntögető nehézségi görbéjét, a kígyó sebességét, a
  kockaépítő/mozaik/tortakészítő húzás-érzékenységét).
- **Eszközlánc-frissítések**: a Gradle 9.8.0 / AGP 9.4.1 / Kotlin 2.4.20 kombináció ma (a
  build idején) a legfrissebb stabil verziók voltak — idővel érdemes lesz frissíteni ezeket is.

## Hangok és segítő mondatok (v5 / 1.4)

Ha a gyerek próbál valamit, de nem sikerül, egy kedves női hang (ElevenLabs, `eleven_v4`, "Ana Rita" – hang-ID: `wJqPPQ618aTW29mptyoc`) megmondja, mit kell tenni. Dicséret is van. A 12 mondat az `app/src/main/res/raw/voice_*.mp3` fájlokban van, lejátszó: `util/VoicePlayer.kt`.

| Hol | Mikor | Mondat |
|---|---|---|
| Színöntögető | nem lehet oda önteni | „Oda nem tudod önteni. Próbálj egy másik poharat!" |
| Színöntögető | üres poharat választ elsőnek | „Ez a pohár üres. Válassz egy olyat, amiben van szín!" |
| Színöntögető | kész a pálya | „Ügyes vagy, kész a pálya!" |
| Tortakészítő | máz/dísz torta nélkül | „Először tegyél le egy tortalapot!" |
| Tortakészítő | kihúzta, de visszaejtette a palettára | „Húzd ki a tortára!" |
| Tortakészítő | máz kész (legalább 2 réteg) | „Ügyes vagy, elkészült a tortád!" |
| Kockaépítő | tele az oszlop | „Ez az oszlop tele van. Próbálj egy másikat!" |
| Kockaépítő | oszlopokon kívülre ejtette | „Húzd a kockát az egyik oszlop fölé!" |
| Kígyó | almát eszik | „Nyam-nyam!" |
| Rajzolj magadról | ~6 mp-ig nincs teljes arc | „Told úgy, hogy az egész arcod látsszon!" |
| Rajzolj magadról | teljes arc látszik | „Szuper, tarts így!" |
| Rajzolj magadról | mentés után | „Nagyon szép rajz lett!" |

Egyszerre csak egy mondat szól, és minden mondatnak van várakozási ideje, így a nyomkodás nem csinál belőle zajt. Új mondat hozzáadása: mp3 a `res/raw`-ba, új elem a `Voice` enumba, hívás a játékban (`voice.play(Voice.…)`).

## Öntögetős: a lebegő pohár teteje nem vágódik le (v6 / 1.5)

A poharakat tartalmazó sor `horizontalScroll`-t használ, ami a saját keretére vágja a tartalmát. Ezért a felemelt vagy megdőlt pohár teteje eltűnt, mintha egy kis ablakban lenne az egész. Most a sorra belülről kapott felső/oldalsó padding (`PourGameScreen.kt`), így a vágókeret messze a poharak fölött van.

## Tortakészítő: tányér a pulton, arányos torta (v7 / 1.6)

A tányér most ugyanazzal a perspektívával készül, mint a torta (`ELLIPSE_RATIO`), a tortánál 1,3-szer szélesebb, vastagsága, mélyedése és árnyéka van (`drawPlate`). A pult éle a tányér mögött van, így a tányér a pult lapján áll, és a torta alsó íve végig a tányéron belül marad.

## Tortakészítő: képkeretes cica a falon (v8 / 1.7)

A konyhai fal csempéjén egy fa keretben, fonálon lógó rajzolt cica-kép van (`drawCatFrame` a `CakeGameScreen.kt`-ban, a háttér része, nem kattintható). Mérete a kijelzőhöz igazodik.

## Két gyerek egyszerre rajzolhat + Cica-összerakó (v9 / 1.8)

**Többujjas rajzolás.** A *Rajzolj magadról* rajzvásznán és a *Mozaik* játékban minden ujj külön vonalat húz (`util/MultiTouch.kt`, `Modifier.multiTouch`). A szín abban a pillanatban rögzül, amikor az ujj leér, így ha az egyik gyerek közben másik színt választ, a másik vonala nem vált át. A Mozaikban a gyors ujjmozgásnál sem maradnak ki cellák (a két pont között végigfestünk). Egyetlen koppintás is pöttyöt hagy a rajzon.

**Cica-összerakó** (`ui/screens/cat/`): egy animált (lélegzik, pislog, farkát csóválja, füle rebben) ülő cicát lehet összeállítani, jobb oldalt kategóriákkal. Koppintásra a cica ugrik, fejét megdönti, szívek szállnak. A 🎲 gomb véletlenszerűen állítja össze.

| Kategória | Lehetőségek |
|---|---|
| 🎨 Szín | narancs, szürke, fekete, fehér, krém, barna, kékes, rózsaszín |
| ✨ Szőr | selymes (fényes, folyó szálak), bolyhos (pihés körvonal és farok), csíkos, foltos (háromszínű), pöttyös |
| 🐾 Mancs | ugyanolyan, fehér zokni, rózsaszín, fekete, kék |
| 👀 Szem | zöld, kék, borostyán, barna, lila |
| 🎀 Extra | semmi, masni, csengős nyakörv, virág, korona, szemüveg |

A cica kódból, vektorosan van rajzolva (`CatDrawing.kt`), nincs hozzá képfájl. Új szín vagy minta felvétele a `furColors` / `FurType` listákban elég.


## Autó-összerakó, Számok, ABC (v10 / 1.9)

**Autó-összerakó** (`ui/screens/car/`): négy jármű (sportkocsi, terepjáró, kamion, busz) valódi 3D-ben. Ujjal húzva forgatható (vízszintesen forgás, függőlegesen dőlés), magától is lassan forog egy forgótányéron. Koppintásra be-/kikapcsol a lámpa, a kerekek megpörögnek, az autó megrugózik. Állítható: jármű, karosszéria színe, lámpa színe (a fénykéve és a ragyogás is ezt a színt kapja), gumi színe, felni stílusa (csillag, küllős, tárcsa, turbina, terepes, virág), felni színe, matrica (csillag, villám, láng, szív, virág, mosoly, csíkok, kockás) és a matrica színe, valamint 🎲 véletlen összeállítás.

A 3D saját, kis szoftveres megjelenítő (`CarModel.kt`, `CarRender.kt`): az alkatrészek kupa alakú testek, a rajzolási sorrendet az alkatrészpárokat elválasztó síkok adják (nem a középpont mélysége), ezért ami mögött van, az nem látszik át. A kerekek a karosszérián kívül ülnek, és semmi nem lóg bele semmibe, mert az átlógás hibás takarást okozna. A járműveket 12 szögből is ellenőrizzük, ha új alkatrészt veszünk fel (emulátoron, `scratchpad/sheet.py`-szerű képsorral).

**Számok** (`ui/screens/numbers/`): 1–10. A magyar óvodai és kisiskolás módszertan ajánlása szerint a számfogalmat a számlálás (egy az egyhez megfeleltetés), majd a mennyiség – számkép – számnév összekapcsolása alapozza meg. *Tanulom* mód: nagy számjegy és a számneve, a tárgyakra koppintva a gyerek megszámolja őket (minden koppintásra elhangzik a következő szám, sorszám-jelvény jelenik meg, a végén „Szuper, megszámoltad!"), mellette tízes keret (számkép). *Keresd!* mód: a hang megkérdezi „Melyik az öt?", három számkártyából kell választani; rossz válasz után „Próbáld újra!", jó után dicséret.

**ABC** (`ui/screens/abc/`): a bővített, 44 betűs magyar ábécé (a q, w, x, y is benne van). Betűre koppintva nagy betű (nagy és kicsi) és egy kép jelenik meg, a hang kimondja például „A mint alma", megkéri a gyereket: „Most te mondd vissza!", újra bemondja a mondatot, kb. 4,5 másodpercet vár, majd megdicséri. Az ábécé a jobb oldali panelen mindig látszik; a ◀ ▶ gombokkal lépegethet. A dz-nél nincs közismert dz-vel kezdődő szó, ezért ott a „madzag" szerepel (a dz a szó belsejében van kiemelve). A q, w, x, y példaszavai idegen eredetűek (quiz, waffle, xerox, yacht).

**Hangok**: 74 új mondat (ElevenLabs `eleven_v4`, „Ana Rita", hang-ID `wJqPPQ618aTW29mptyoc`) a `res/raw/voice_*.mp3` fájlokban: `voice_num_1..10`, `voice_find_1..10`, `voice_abc_<betű>` (a betűazonosítók: a, a2=á, e2=é, i2=í, o2=ó, oe=ö, oe2=ő, u2=ú, ue=ü, ue2=ű, a többi a betű maga), valamint `voice_praise_1..5`, `voice_try_again`, `voice_counted`, `voice_pick_number`, `voice_pick_letter`, `voice_say_back`. A lejátszó a `util/ClipPlayer.kt` (hangok és szünetek sorozata, új sorozat indításakor a régi leáll).

**Két gyerek egyszerre rajzolhat**: a rajzvásznon és a mozaikban minden ujj külön vonalat húz (`util/MultiTouch.kt`), a szín az ujj leérésekor rögzül.

## Léghoki, szélesebb kockaépítő, új hang (v11 / 1.10)

**Léghoki** (`ui/screens/hockey/`): két játékos egy táblán, a tablet két hosszú végén ülve. A tábla a képernyő hosszában fekszik, a kapuk a két végén vannak, mindenki a saját oldalán húzza az ujját; az ütő a saját feléről egészen a középvonalig előretolható. A két ujj egymástól függetlenül követi a saját ütőjét (`util/MultiTouch.kt`). A korong pattog a falakon és a kapufákon, az ütő visszaüti, és a saját mozgásából is ad neki egy kis lendületet. Gyerekbarát sebességek: a korong nagy, lassan indul, súrlódik, sosem áll meg, és a legnagyobb sebessége korlátozott (a tábla szélességének 36%-a másodpercenként); az ütőn keresztül nem megy át. Az első, aki 5 gólt szerez, nyer: a hang bemondja a színét („A piros nyert!" / „A kék nyert!"), gratulál, tűzijáték szóródik a nyertes színében, majd a játék magától újraindul (a tűzijáték nyomai ilyenkor törlődnek). A pattogás, ütés és gól hangeffektjeit a program generálja (`util/SoundFx.kt`), nincs hozzá fájl.

**Kockaépítő** (`ui/screens/blocks/`, `game/blocks/`): a pálya 30 oszlop széles (kb. két és fél képernyő). A háttérre húzva a kamera mozog (lendülettel), a ◀ ▶ nyilak nyomva tartva görgetnek, az alsó minitérkép koppintásra odaugrik, és ha elemet húzol a pálya szélére, a kamera magától megy utána. Nyolc forma van (kocka, lap, oszlop, henger, tető, gömb, boltív, csillag), hat színben; a formák 3D-s hatással rajzolódnak. Az oszlopok 8 egység magasak lehetnek. A legfelső elem áthúzható másik oszlopra, vagy visszahúzva a palettára törölhető.

**Új hang**: minden mondat (91 db) az ElevenLabs `xjlfQQ3ynqiEyRpArrT8` hanggal készült (`eleven_v4`); az új mondatoknál ugyanezt a hangot használd. A `res/raw/keep.xml` megakadályozza, hogy erőforrás-zsugorítás törölje a névvel betöltött hangokat. Új hang hozzáadása: mp3 a `res/raw`-ba (kisbetű, számjegy, aláhúzás a névben), majd `Step.Clip("név")` a `ClipPlayer`-ben vagy egy új elem a `Voice` enumban.

### Hogyan ellenőrizzük (a kiadás előtt)
- `./gradlew lintDebug`: 0 hiba.
- Emulátoron minden képernyő megnyitása, és játékonként a valódi használat (öntés, kockák, kígyó, torta, számlálás, betűk, léghoki gól → győzelem → újraindulás).
- Véletlen nyomkodás: `adb shell monkey -p hu.apk.kidsapp --throttle 60 --pct-motion 40 --pct-touch 50 -s <mag> -v 2500`, összeomlás- és fehér képernyő-keresés.
- Két ujj egyszerre: `adb emu event send EV_ABS:ABS_MT_SLOT:0 EV_ABS:ABS_MT_TRACKING_ID:100 … EV_SYN:0:0` (az emulátor konzolján át, a sima `input` csak egy ujjat tud). Ellenőrizve: rajzvászon és mozaik (minden ujj saját színnel), léghoki (két ütő).
- 3D autók: 12 szögből készült képsor járművenként, takarási hibák keresésére.

## Rendszersávok, léghoki-javítás, nagyobb tortadíszek (v12 / 1.11)

- **Android navigációs sáv**: az app edge-to-edge fut, ezért a tartalom (pl. a számok) becsúszott az alsó rendszersáv alá. Mostantól az `AppRoot` (`MainActivity.kt`) minden képernyőt a rendszersávok (állapotsor, navigációs sáv) közé szorít (`windowInsetsPadding(WindowInsets.systemBars)`); a sáv mögötti rész színe a képernyőé (a léghokié sötét). Mind a három gombos, mind a gesztus-navigációval ellenőrizve minden képernyőn.
- **Léghoki**: az az oldal fagyott le, ahol egy elakadt érintés (pl. tenyér) „lefoglalta” az ütőt. Most az új érintés mindig átveszi a saját oldal ütőjét. Az ütő a középvonalig tolható (a két ütő nem lóghat át a másik térfelére).
- **Tortakészítő**: két oszlopos, nagyobb paletta; a díszek 1,35×-ösek a palettán és 1,6×-osak a tortán, a találati felület (50 dp) is nagyobb, így könnyen felvehetők. A máz a torta hátulján már nem látszik (se a perem, se a csorgás).

## Tortamáz: teljesen lefolyik, hullámos szegéllyel (v13 / 1.12)

A máz a torta oldalán végigfolyik (a felső lap elülső ívétől lefelé), és hullámos alsó széllel ér véget; a hátulsó félen nincs. A hullámzás hossza változó (a réteg magasságának ~50–93%-a). Rajzolás közben a máz a söprés mögött fokozatosan „folyik le”. A korábbi különálló cseppeket és szegélypöttyöket ez váltja fel (`drawIcing`, `CakeGameScreen.kt`).

## Számok és ABC: írásgyakorlás (v14 / 1.13)

A Számok és az ABC képernyőn is van egy **✏️ Írom** gomb. Az írás képernyő (`ui/screens/writing/WritingPractice.kt`) balra mutatja a mintát, jobbra nagyban a halvány alakot, amin végig kell húzni az ujját. A menet: 1) nagyban egyszer, 2) nagyban még egyszer, 3) kicsiben ötször egymás után (5 doboz, mindig a következő aktív); ha ez is megvan, jön a következő szám/betű. Minden lépés után a hang megdicsér („Nagyon ügyes vagy!” stb.), és kéri a következőt („Most írd le még egyszer!”, „Most írd le ötször!”, „Jöhet a következő!”); az 5 dobozos résznél egy csengő hang jelzi a kész dobozt. A felső sor pontjai mutatják, hol tart (2 nagy + 5 kis pötty). Az ABC-nél az **A / a** gombbal lehet nagy- vagy kisbetűt gyakorolni; a ◀ ▶ gombokkal lehet lépni a jelek között.

**Megbocsátó felismerés:** nincs vonalvezetés- vagy sorrendellenőrzés. A betű alakját a program egy pontrácsra bontja (a betű rajzolatából), az ujj útvonalát sűrűn mintavételezi, és az alak 85%-át kell betakarni a ~10%-os ecsetsugáron belül; a húzott pontok legalább 40%-a legyen az alak közelében (hogy a firka ne számítson). Az ékezetek (pl. Á) az alak részei. A kisebb eltévedés nem baj, az ujj nem kell, hogy felemelés nélkül menjen. Egy 🔄 gombbal törölhető a doboz tartalma.

Új hangok (`voice_write_it`, `voice_write_again`, `voice_write_five`, `voice_write_next`) és egy generált „ding” hangeffekt (`util/SoundFx.kt`).

## Play-kompatibilitás: 16 KB oldalméret (v15 / 1.14)

A Google Play a 15-ös Android-célú appoktól megköveteli a 16 KB-os memóriaoldal támogatását. A CameraX 1.3.4 `libimage_processing_util_jni.so` könyvtára még 4 KB-ra volt igazítva, ezért a CameraX 1.5.1-re frissült. Ellenőrzés: az AAB minden 64 bites `.so` fájlja 16384-es ELF-igazítású (arm64-v8a, x86_64).

## Mesekönyv (v16 / 1.15)

Új tile: **Mesék** (`ui/screens/stories/`). A polcon három mese van: **A három kismalac**, **A pogácsa**, **Az égig érő fa** (mind 8 oldal). Egy oldal = egy akvarell illusztráció + a szöveg alul + egy külön felvett hangfájl (`res/raw/mese_<mese>_<oldal>.mp3`, ugyanaz a hang, mint a játékokban). A felolvasás végén kis szünet után az oldal magától lapoz (`ClipPlayer` sorozat: szünet → hang → szünet → lapozás); a végén „Vége! – Újra / Másik mese”. Kézi lapozás: nyilak a két szélen vagy balra/jobbra húzás; a szünet gomb leállítja az automatikus olvasást.

**Licenc:** a szövegek saját újramesélések (`mesek/mesek_szoveg.md`), a cselekmény hagyományos népmesei/szabad (a konkrét szöveg nem átvett, védett kiadványokból), az Égig érő fa egy saját, szelídített változat a mese motívumaival. A képek a `gpt-image-2` modellel készültek (ElevenLabs fiókon át), 1280×720 webp-ben, a `res/drawable-nodpi/` mappában. **A képek kereskedelmi felhasználási feltételét a megjelenés előtt ellenőrizni kell.** A `StoryData.kt` a szövegfájlból generált (`mesek/` mappa + generáló szkript); új mesét ugyanígy, kép + hang + szöveg hozzáadásával lehet felvenni.

## Verzió a beállításokban (v17 / 1.16)

A Beállítások képernyő alján megjelenik a telepített verzió („1.16 (17. kiadás)”), így a tableten látszik, megérkezett-e a Play frissítése (`SettingsScreen.kt`, `PackageManager`-ből olvasva).

## Három új mese a mesekönyvben (v18 / 1.17)

A mesekönyv most 6 mesét tartalmaz. Új: **A kiskakas gyémánt félkrajcárja** (magyar népmese), **A farkas és a hét kecskegida** (Grimm, szelídítve: nem eszi meg senkit, a gidák bezárva maradnak), **A rút kiskacsa** (Andersen). Mindegyik 8 oldalas, saját újramesélés (`mesek/mesek_szoveg.md`), ugyanazzal a magyar hanggal és modellel (`xjlfQQ3ynqiEyRpArrT8`, `eleven_v4`), mint az első három, és ugyanabban az akvarell stílusban. A polc most rácsos (3 oszlop, görgethető).

## Nyelvek játék (v19 / 1.18)

`ui/screens/language/`: először zászlót kell választani (angol / német, kirajzolva, nem emoji), utána állatokat tanul a gyerek a választott nyelven: nagy kép, a szó (németnél a névelő színezve: der = kék, die = piros, das = zöld), kimondja, megkéri, hogy mondja vissza, aztán megdicséri (ugyanúgy, mint az ABC-nél). 20 állat, a képek akvarellek (`drawable-nodpi/animal_*.webp`). A hangok (`res/raw/lang_en_<állat>`, `lang_de_<állat>`): angol „Emma – Bright Kids Educator” (anyanyelvi amerikai gyerek-oktató hang), német „Julia” (anyanyelvi német mesemondó hang), mindkettő `eleven_multilingual_v2` modellel (egyedi szavaknál ez stabilabb, mint az `eleven_v4`); a kísérőmondatok magyarul ugyanazzal a hanggal, mint a többi játékban. A németnél a szó a névelővel hangzik el („Der Hund.”). A csempe elrejthető a `HomeScreen.kt`-ban a `LANGUAGE_GAME_READY` állításával.

## Matricás világ (v20 / 1.19)

Új csempe: **Matricák** (`ui/screens/scene/`). Jelenetek: **Város**, **Pékség**, **Otthon**, **Dínók világa** (a **Világűr** és az **Erdő** háttere már kész, a matricáik még nem: kifogyott a kredit, lásd lent). Mindegyik jelenetben alul egy görgethető tálca van matricákkal: koppintásra a matrica a színtérre kerül, húzással oda lehet vinni, ahová a gyerek akarja, és a tálcára visszahúzva törlődik. Két gyerek egyszerre is húzhat (`util/MultiTouch.kt`).

**A matricák mozognak és reagálnak:**
- gyalogló (gyerekek, állatok, dínók): ide-oda sétálnak, megfordulnak; flyer (madár, pteranodon): lebegnek, hullámzanak; tárgyak: finoman „lélegeznek”;
- koppintásra: a járművek (autó, busz, bicikli, tűzoltóautó, vonat) végigmennek a képernyőn (dudálnak), a dínók ordítanak, a madár csiripel, a tárgyak ugrálnak (boing), a lámpa színt vált (piros, sárga, zöld, kimondja a színt feliratban), a **dínótojás megreped és kikel belőle a kisdínó**;
- ha két matricát egymás mellé tesz le, köszöntik egymást (szívek, ugrálás, csengő);
- koppintáskor felirat jelenik meg a nevével. A kimondott név (`res/raw/st_<azonosító>.mp3`) még nincs legenerálva, a program addig csak kiírja.

A matricák ugyanolyan akvarell stílusúak; a háttér eltávolítása helyben történik (`rembg`), a képek `drawable-nodpi/st_*.webp` és `bg_*.webp`. A tartalom forrása `matrica/` mappa + generáló szkriptek (`SceneData.kt` generált). Hanghatások: `util/SoundFx.kt` (pop, boing, honk, chirp, roar, whoosh, ding).

## Matricák: magasabb nézőpont (v21 / 1.20)

A Város, az Otthon és a Dínók világa hátterei madártávlatból készültek (a horizont a kép felső negyedében van, a föld tölti ki az alsó háromnegyedet), és közelebbről, a matricákhoz méretarányosabban látszanak. A házak, fák és járművek matricái kicsit kisebbek lettek. A Pékség háttere még a régi (szemből nézett), és a Világűr/Erdő is az ElevenLabs-kredit feltöltésére vár.

## Zene: zenebuborékok (v22 / 1.21)

Új játék a főoldalon (Zene). Hat sor színes, mosolygó buborék: öt hangmagasság (pentaton, a C-dúr hangjaiból) és egy dobhang. A bal szélső buborékra koppintva szól a hang; a rácson az üres foltokra koppintva buborék kerül oda, és a lejátszás gombbal 8 lépéses, ismétlődő dallammá áll össze (futó oszlop, ugráló buborékok, lebegő hangjegyek). A kuka törli a rácsot. Hangfájl nélkül, a SoundFx generálja a hangokat (n0-n5).

## Zene: lassabb tempó, automatikus indulás (v23 / 1.22)

A Zene játék megnyitáskor magától elindul, és lassabban (520 ms/lépés) játssza a dallamot.

## Főzés (v24 / 1.23)

Új játék a főoldalon (Főzés). Kilenc hozzávaló (a matricák képeiből: alma, banán, narancs, sajt, kenyér, tej, kifli, torta, keksz) a képernyő alján; koppintással vagy a fazékba húzással kerülnek bele (legfeljebb hat). A fazék nagyjából 38 fokos rálátásból látszik, a leves színe a belerakott hozzávalók színéből keveredik. A kanalat a fazékban körözve lehet keverni (gőz, buborékok, forgó hozzávalók); ha elég a keverés, a szörnyecske tátog, és egy koppintással megeszi a levest (repülő falatok, szívek). Utána új főzés kezdődik. A hangeffektek a SoundFx-ből szólnak.

## Kódgyár (v25 / 1.24)

Új játék a főoldalon (Kódgyár). Egy kedves robotot kell a csillagokhoz vezetni: a négy színes nyílgombbal (jobbra, balra, fel, le) programot lehet összerakni (legfeljebb 10 lépés), a zöld lejátszás gombbal a robot sorban végrehajtja (a futó lépés kiemelve). Hatféle szint, egyre több csillaggal és sziklával. Hibázni nem lehet: ha a robot falnak vagy sziklának megy, megrázkódik; ha nem gyűjtött össze minden csillagot, szelíden visszagurul az elejére, és a program marad, javítható. Siker esetén konfetti, majd a következő szint jön. A hangeffektek a SoundFx-ből szólnak.

## Öt új mese: 11 mese a mesekönyvben (v26 / 1.25)

A répa, A kis gömböc, A teknős és a nyúl, A mézeskalács-emberke, Az egér és az oroszlán (mesenként 8 oldal, kép és magyar hang). A szövegek a `mesek/mesek_szoveg.md`-ben vannak; a további 6 mese szövege (`mesek/mesek_szoveg_2.md`) még kép és hang nélkül vár. A mesekönyv polca 11 mesével már görgethető. A hangokat fizetős (Starter) csomagon generáltuk.
