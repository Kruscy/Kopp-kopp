package hu.apk.kidsapp.ui.screens.stories

import hu.apk.kidsapp.R

// Ezt a fájlt a mesek/ mappa szövegéből generáltuk (saját újramesélés, szabad felhasználású).
class StoryPage(val image: Int, val clip: String, val text: String)

class Story(val title: String, val emoji: String, val pages: List<StoryPage>) {
    val cover: Int get() = pages.first().image
}

val stories: List<Story> = listOf(
    Story("A három kismalac", "🐷", listOf(
        StoryPage(R.drawable.mese_kismalac_1, "mese_kismalac_1", "Egyszer volt, hol nem volt, élt egyszer három kismalac. Egy szép napon az anyukájuk azt mondta: „Kis malacaim, ideje, hogy mindegyikőtök építsen magának egy saját házikót!”"),
        StoryPage(R.drawable.mese_kismalac_2, "mese_kismalac_2", "Az első kismalac nagyon lusta volt. Összeszedett egy kis szalmát, gyorsan összetákolt belőle egy házat, aztán lefeküdt aludni."),
        StoryPage(R.drawable.mese_kismalac_3, "mese_kismalac_3", "A második kismalac vékony fadarabokból épített házat. Az sem lett túl erős, de hamar elkészült vele, és már ment is játszani."),
        StoryPage(R.drawable.mese_kismalac_4, "mese_kismalac_4", "A harmadik kismalac szorgalmas volt. Nehéz téglákat cipelt, egyiket a másik tetejére rakta. Sokáig tartott, de nagyon erős, szép házat épített."),
        StoryPage(R.drawable.mese_kismalac_5, "mese_kismalac_5", "Egyszer arra járt a nagy szürke farkas. Bekopogott a szalmaházhoz: „Kismalac, kismalac, engedj be!” „Nem, nem, a szőröm szálán sem!” Erre a farkas nagyot fújt, és a szalmaház szétrepült. Az első kismalac futott a testvéréhez."),
        StoryPage(R.drawable.mese_kismalac_6, "mese_kismalac_6", "A két kismalac a faházba bújt. A farkas ott is kopogott, ott is fújt, és a faház is összedőlt. Szaladtak a harmadik kismalac téglaházához."),
        StoryPage(R.drawable.mese_kismalac_7, "mese_kismalac_7", "A farkas fújt, fújt, de a téglaház meg se moccant. Akkor fel akart mászni a kéményen, de a harmadik kismalac nagy fazék vizet tett a tűzre. A farkas megijedt a gőztől, és elszaladt az erdőbe. Soha többé nem jött vissza."),
        StoryPage(R.drawable.mese_kismalac_8, "mese_kismalac_8", "A három kismalac együtt maradt az erős téglaházban. Vacsorára almás pitét ettek, és mindannyian megtanulták: aki szorgalmas, annak erős háza lesz. Jó éjszakát, kismalacok!"),
    )),
    Story("A pogácsa", "🥯", listOf(
        StoryPage(R.drawable.mese_pogacsa_1, "mese_pogacsa_1", "Egyszer volt, hol nem volt, élt egy nagymama meg egy nagypapa. Egy reggel a nagymama pogácsát sütött. Az egyik pogácsa különlegesen kerek és aranysárga lett, és a nagymama kitette az ablakba hűlni."),
        StoryPage(R.drawable.mese_pogacsa_2, "mese_pogacsa_2", "A pogácsa nem akart ott ülni. Ugrott egyet, gurult egyet, le a földre, és elgurult az úton. „Gurulok, gurulok, senkise fog el!”"),
        StoryPage(R.drawable.mese_pogacsa_3, "mese_pogacsa_3", "Az úton nyuszi ugrált. „Pogácsa, pogácsa, megeszlek!” „Nem eszel meg, nyuszi, elgurulok előled, ahogy a nagymamától is elgurultam!” És gurult tovább."),
        StoryPage(R.drawable.mese_pogacsa_4, "mese_pogacsa_4", "Következett egy nagy szürke farkas. „Pogácsa, pogácsa, megeszlek!” „Nem eszel meg, farkas, elgurulok előled is!” És gurult tovább."),
        StoryPage(R.drawable.mese_pogacsa_5, "mese_pogacsa_5", "Aztán jött egy nagy barna medve. „Pogácsa, pogácsa, megeszlek!” „Nem eszel meg, medve, elgurulok előled is!” És a pogácsa gurult tovább."),
        StoryPage(R.drawable.mese_pogacsa_6, "mese_pogacsa_6", "Végül egy ravasz vörös róka állta az útját. „Jaj, de szépen gurulsz, pogácsa! Gyere közelebb, az orromra, hadd halljam jobban a dalodat!” De a pogácsa megsejtette a ravaszságot: „Nem megyek, te ravasz róka!”"),
        StoryPage(R.drawable.mese_pogacsa_7, "mese_pogacsa_7", "És úgy elgurult a domboldalon, hogy a róka sehogy sem érte utol. Gurult, gurult, egészen haza, a nagymama konyhájáig."),
        StoryPage(R.drawable.mese_pogacsa_8, "mese_pogacsa_8", "A nagymama kitárta az ajtót: „Hát te, kis csavargó!” A nagymama és a nagypapa nevettek, és sütöttek még egy egész tepsi pogácsát. Mindenki kapott belőle: a nyuszi, a medve, a farkas és még a róka is. Jó étvágyat!"),
    )),
    Story("Az égig érő fa", "🌳", listOf(
        StoryPage(R.drawable.mese_fa_1, "mese_fa_1", "Élt egyszer egy szegény asszony a kisfiával, Jankóval. A kertjükben állt egy varázsfa, amelyik minden nap nőtt, egyre magasabbra, a ház fölé, a templom fölé, egészen az égig."),
        StoryPage(R.drawable.mese_fa_2, "mese_fa_2", "„Anyám, megnézem, mi van a fa tetején!” Az anyja kenyeret és kulacs vizet csomagolt, megölelte, és Jankó elindult fölfelé."),
        StoryPage(R.drawable.mese_fa_3, "mese_fa_3", "Mászott, mászott, és az egyik ágon egy kismadár ült, és sírt. „Eltört a szárnyam!” Jankó megsimogatta, kenyérmorzsát adott neki. „Ha bajban leszel, hívj!” mondta a kismadár."),
        StoryPage(R.drawable.mese_fa_4, "mese_fa_4", "Följebb hangyák cipekedtek. „Elszórtuk a magjainkat, és nem tudjuk fölvinni!” Jankó segített összeszedni őket. „Ha szükséged lesz ránk, szólj!” mondták a hangyák."),
        StoryPage(R.drawable.mese_fa_5, "mese_fa_5", "Az ég tetején felhőország volt, és abban egy nagy, jóságos óriás ült. „Jaj, elvesztettem a Hold ezüstkulcsát, és nélküle nem ragyoghat az éjszaka! Segíts megtalálni!”"),
        StoryPage(R.drawable.mese_fa_6, "mese_fa_6", "Jankó hívta a hangyákat. Ezer kicsi hangya kutatta át a felhőket, és megtalálták a kulcsot. De a Hold magasan volt. Jankó hívta a kismadarat, és az fölrepült vele a Holdig."),
        StoryPage(R.drawable.mese_fa_7, "mese_fa_7", "Kattant a zár, és a Hold ezüst fénnyel kigyúlt. Az óriás nagyon boldog volt. „Mit kívánsz?” Jankó azt mondta: „Azt szeretném, hogy anyámnak mindig legyen kenyere.” Az óriás adott neki egy zsák aranyszínű búzamagot."),
        StoryPage(R.drawable.mese_fa_8, "mese_fa_8", "Jankó leereszkedett a fán, és hazaért. Anyja átölelte. Elvetették a magokat, és attól kezdve mindig volt kenyerük. Az égig érő fa pedig még ma is ott áll, aki jól néz, látja a csúcsát a felhők között. Jó éjszakát!"),
    )),
    Story("A kiskakas gyémánt félkrajcárja", "🐓", listOf(
        StoryPage(R.drawable.mese_kakas_1, "mese_kakas_1", "Egyszer volt, hol nem volt, élt egy kiskakas. Egy reggel a szemétdombon kapirgált, és egyszer csak megcsillant valami a földön. Egy gyémánt félkrajcár volt!"),
        StoryPage(R.drawable.mese_kakas_2, "mese_kakas_2", "A kiskakas nagyon boldog volt. Fényesre törölgette a szárnyával, és büszkén vitte magával. „Ez az én kincsem!” kukorékolta."),
        StoryPage(R.drawable.mese_kakas_3, "mese_kakas_3", "Arra járt a király, meglátta a csillogó pénzt, és elvette a kiskakastól. „Ez mostantól az enyém!” mondta, és bement a palotába."),
        StoryPage(R.drawable.mese_kakas_4, "mese_kakas_4", "A kiskakas nem hagyta magát. Elindult a palotába, és az ajtó előtt nagyot kukorékolt: „Kukurikú! Add vissza a gyémánt félkrajcáromat!”"),
        StoryPage(R.drawable.mese_kakas_5, "mese_kakas_5", "A király mérges lett. „Vigyétek a kakast a kúthoz!” De a kiskakas azt mondta: „Hasam, hasam, igyad a vizet!” És a kiskakas megitta az egész kút vizét."),
        StoryPage(R.drawable.mese_kakas_6, "mese_kakas_6", "Most a király a nagy kemencéhez vitette. „Ott majd megsül a kakas!” De a kiskakas kiöntötte a vizet a tűzre, és a láng azonnal kialudt."),
        StoryPage(R.drawable.mese_kakas_7, "mese_kakas_7", "A király csak nézett. „Ilyen bátor és okos kiskakasról még nem hallottam!” Visszaadta a gyémánt félkrajcárt, és még egy zacskó aranyat is adott mellé."),
        StoryPage(R.drawable.mese_kakas_8, "mese_kakas_8", "A kiskakas boldogan hazaszaladt a gyémánt félkrajcárral és az arany zacskóval. Aznap este nagyot kukorékolt a szemétdombon. Jó éjszakát!"),
    )),
    Story("A farkas és a hét kecskegida", "🐐", listOf(
        StoryPage(R.drawable.mese_kecske_1, "mese_kecske_1", "Élt egyszer egy kecskeanyó a hét kicsi gidájával. Egy reggel a kecskeanyó piacra készült. „Gidáim, zárjátok be az ajtót, és ne engedjetek be senkit!” mondta."),
        StoryPage(R.drawable.mese_kecske_2, "mese_kecske_2", "„Különösen a farkast ne engedjétek be! Rekedt a hangja, és fekete a lába.” „Értjük, anyuka!” mondták a gidák, és a kecskeanyó elindult."),
        StoryPage(R.drawable.mese_kecske_3, "mese_kecske_3", "Nemsokára kopogtak az ajtón. „Nyissátok ki, én vagyok az anyátok!” De a hang nagyon rekedt volt. „Nem, nem! Te a farkas vagy!” kiáltották a gidák."),
        StoryPage(R.drawable.mese_kecske_4, "mese_kecske_4", "A farkas elment, krétát evett, hogy vékony legyen a hangja, és a lábát belemártotta a lisztbe, hogy fehér legyen. Aztán visszament a kecskék házához."),
        StoryPage(R.drawable.mese_kecske_5, "mese_kecske_5", "Újra kopogott, vékony hangon: „Nyissátok ki, itt az anyátok!” De a gidák kinéztek az ablakon, és meglátták a szürke farkát. „Nem engedünk be, te farkas!”"),
        StoryPage(R.drawable.mese_kecske_6, "mese_kecske_6", "A farkas hiába dörömbölt, az ajtó be volt zárva. A hét kicsi gida csendben bebújt: az egyik az ágy alá, a másik a szekrénybe, a harmadik az óra mögé."),
        StoryPage(R.drawable.mese_kecske_7, "mese_kecske_7", "Hazajött a kecskeanyó. Meglátta a farkast az ajtó előtt, és rákiáltott: „Takarodj innen, te hamis farkas!” A farkas megijedt, és elszaladt az erdőbe."),
        StoryPage(R.drawable.mese_kecske_8, "mese_kecske_8", "A gidák előbújtak, és a nyakába ugrottak az anyukájuknak. „Nem engedtünk be senkit!” mondták. A kecskeanyó megölelte mind a hetet. Jó éjszakát!"),
    )),
    Story("A rút kiskacsa", "🦢", listOf(
        StoryPage(R.drawable.mese_kacsa_1, "mese_kacsa_1", "Egy szép nyári napon a kacsamama tojásaiból kikeltek a kiskacsák. Hat kicsi, sárga kiskacsa pipiskedett, de a legnagyobb tojásból egy nagy, szürke kiskacsa bújt ki."),
        StoryPage(R.drawable.mese_kacsa_2, "mese_kacsa_2", "A kacsamama szerette mindet, de a többi állat kinevette a szürke kiskacsát. „Milyen nagy és szürke! Milyen rút!” mondták. A kiskacsa nagyon elszomorodott."),
        StoryPage(R.drawable.mese_kacsa_3, "mese_kacsa_3", "Egy este elhatározta, hogy elmegy. Átbújt a kerítés alatt, és a nádasban talált egy csendes tavat. Ott egyedül aludt el."),
        StoryPage(R.drawable.mese_kacsa_4, "mese_kacsa_4", "Jött az ősz, hullottak a falevelek. A szürke kiskacsa egyedül úszkált, és nézte, ahogy a vadlibák elrepülnek a meleg földek felé."),
        StoryPage(R.drawable.mese_kacsa_5, "mese_kacsa_5", "Aztán megérkezett a tél. A tó befagyott, a kiskacsa fázott és éhes volt. Egy jószívű halász a kabátjába rejtette, és hazavitte, hogy átmelegedjen."),
        StoryPage(R.drawable.mese_kacsa_6, "mese_kacsa_6", "Végre eljött a tavasz! A nap kisütött, a jég elolvadt. A kiskacsa kitárta a szárnyait, és meglepetésére szépen repült!"),
        StoryPage(R.drawable.mese_kacsa_7, "mese_kacsa_7", "A tónál három gyönyörű fehér hattyú úszott. Félve odaúszott hozzájuk, és lenézett a víz tükrébe. Egy szép, fehér hattyút látott magával szemben."),
        StoryPage(R.drawable.mese_kacsa_8, "mese_kacsa_8", "A kiskacsa nem csúnya volt, hanem hattyú! A hattyúk szívesen fogadták, és együtt úsztak a napsütötte tavon. Jó éjszakát!"),
    )),
)
