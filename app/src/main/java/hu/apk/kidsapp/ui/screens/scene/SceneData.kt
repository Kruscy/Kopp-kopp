package hu.apk.kidsapp.ui.screens.scene

import hu.apk.kidsapp.R

// Generált fájl (lásd matrica/ mappa): a matricás világ jelenetei és matricái.
enum class Kind { DRIVER, WALKER, FLYER, STATIC, LAMP, EGG, ROCKET }

class StickerDef(val id: String, val image: Int, val name: String, val kind: Kind, val h: Float)

class SceneDef(val id: String, val title: String, val emoji: String, val bg: Int, val stickers: List<StickerDef>)

val sceneWorlds: List<SceneDef> = listOf(
    SceneDef("town", "Város", "🏙️", R.drawable.bg_town, listOf(
        StickerDef("car", R.drawable.st_car, "Autó", Kind.DRIVER, 0.15f),
        StickerDef("bus", R.drawable.st_bus, "Busz", Kind.DRIVER, 0.18f),
        StickerDef("bike", R.drawable.st_bike, "Bicikli", Kind.DRIVER, 0.15f),
        StickerDef("firetruck", R.drawable.st_firetruck, "Tűzoltóautó", Kind.DRIVER, 0.18f),
        StickerDef("train", R.drawable.st_train, "Vonat", Kind.DRIVER, 0.18f),
        StickerDef("house", R.drawable.st_house, "Ház", Kind.STATIC, 0.3f),
        StickerDef("shop", R.drawable.st_shop, "Bolt", Kind.STATIC, 0.3f),
        StickerDef("tree", R.drawable.st_tree, "Fa", Kind.STATIC, 0.31f),
        StickerDef("flower", R.drawable.st_flower, "Virág", Kind.STATIC, 0.11f),
        StickerDef("trafficlight", R.drawable.st_trafficlight, "Közlekedési lámpa", Kind.LAMP, 0.28f),
        StickerDef("boy", R.drawable.st_boy, "Kisfiú", Kind.WALKER, 0.25f),
        StickerDef("girl", R.drawable.st_girl, "Kislány", Kind.WALKER, 0.25f),
        StickerDef("dog", R.drawable.st_dog, "Kutya", Kind.WALKER, 0.14f),
        StickerDef("bird", R.drawable.st_bird, "Madár", Kind.FLYER, 0.09f),
    )),
    SceneDef("shop", "Pékség", "🥐", R.drawable.bg_shop, listOf(
        StickerDef("bread", R.drawable.st_bread, "Kenyér", Kind.STATIC, 0.12f),
        StickerDef("croissant", R.drawable.st_croissant, "Kifli", Kind.STATIC, 0.1f),
        StickerDef("cake", R.drawable.st_cake, "Torta", Kind.STATIC, 0.14f),
        StickerDef("cookie", R.drawable.st_cookie, "Keksz", Kind.STATIC, 0.09f),
        StickerDef("milk", R.drawable.st_milk, "Tej", Kind.STATIC, 0.14f),
        StickerDef("apple", R.drawable.st_apple, "Alma", Kind.STATIC, 0.1f),
        StickerDef("banana", R.drawable.st_banana, "Banán", Kind.STATIC, 0.1f),
        StickerDef("orange", R.drawable.st_orange, "Narancs", Kind.STATIC, 0.1f),
        StickerDef("cheese", R.drawable.st_cheese, "Sajt", Kind.STATIC, 0.1f),
        StickerDef("basket", R.drawable.st_basket, "Kosár", Kind.STATIC, 0.14f),
        StickerDef("baker", R.drawable.st_baker, "Pék", Kind.WALKER, 0.27f),
        StickerDef("cashregister", R.drawable.st_cashregister, "Pénztárgép", Kind.STATIC, 0.14f),
        StickerDef("boy", R.drawable.st_boy, "Kisfiú", Kind.WALKER, 0.25f),
        StickerDef("girl", R.drawable.st_girl, "Kislány", Kind.WALKER, 0.25f),
    )),
    SceneDef("house", "Otthon", "🏠", R.drawable.bg_house, listOf(
        StickerDef("sofa", R.drawable.st_sofa, "Kanapé", Kind.STATIC, 0.22f),
        StickerDef("bed", R.drawable.st_bed, "Ágy", Kind.STATIC, 0.22f),
        StickerDef("table", R.drawable.st_table, "Asztal", Kind.STATIC, 0.2f),
        StickerDef("chair", R.drawable.st_chair, "Szék", Kind.STATIC, 0.18f),
        StickerDef("lamp", R.drawable.st_lamp, "Lámpa", Kind.STATIC, 0.28f),
        StickerDef("shelf", R.drawable.st_shelf, "Polc", Kind.STATIC, 0.3f),
        StickerDef("plant", R.drawable.st_plant, "Cserepes növény", Kind.STATIC, 0.16f),
        StickerDef("cat", R.drawable.st_cat, "Cica", Kind.WALKER, 0.13f),
        StickerDef("dog", R.drawable.st_dog, "Kutya", Kind.WALKER, 0.14f),
        StickerDef("doll", R.drawable.st_doll, "Baba", Kind.STATIC, 0.14f),
        StickerDef("ball", R.drawable.st_ball, "Labda", Kind.STATIC, 0.09f),
        StickerDef("teddy", R.drawable.st_teddy, "Mackó", Kind.STATIC, 0.14f),
        StickerDef("clock", R.drawable.st_clock, "Óra", Kind.STATIC, 0.12f),
        StickerDef("tv", R.drawable.st_tv, "Tévé", Kind.STATIC, 0.16f),
    )),
    SceneDef("dino", "Dínók világa", "🦖", R.drawable.bg_dino, listOf(
        StickerDef("trex", R.drawable.st_trex, "Tirannoszaurusz", Kind.WALKER, 0.24f),
        StickerDef("triceratops", R.drawable.st_triceratops, "Triceratopsz", Kind.WALKER, 0.19f),
        StickerDef("brontosaurus", R.drawable.st_brontosaurus, "Brontoszaurusz", Kind.WALKER, 0.28f),
        StickerDef("stegosaurus", R.drawable.st_stegosaurus, "Sztegoszaurusz", Kind.WALKER, 0.19f),
        StickerDef("pteranodon", R.drawable.st_pteranodon, "Pteranodon", Kind.FLYER, 0.15f),
        StickerDef("dinoegg", R.drawable.st_dinoegg, "Dínótojás", Kind.EGG, 0.14f),
        StickerDef("babydino", R.drawable.st_babydino, "Kisdínó", Kind.WALKER, 0.12f),
        StickerDef("volcano", R.drawable.st_volcano, "Vulkán", Kind.STATIC, 0.34f),
        StickerDef("fern", R.drawable.st_fern, "Páfrány", Kind.STATIC, 0.22f),
        StickerDef("rock", R.drawable.st_rock, "Szikla", Kind.STATIC, 0.14f),
    )),
)
