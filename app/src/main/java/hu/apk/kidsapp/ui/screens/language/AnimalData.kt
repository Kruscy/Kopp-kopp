package hu.apk.kidsapp.ui.screens.language

import hu.apk.kidsapp.R

// Generált fájl (lásd nyelv/ mappa): az állatok, angol és német névvel.
class Animal(val id: String, val image: Int, val en: String, val article: String, val de: String, val hu: String)

val animals: List<Animal> = listOf(
    Animal("dog", R.drawable.animal_dog, "Dog", "der", "Hund", "kutya"),
    Animal("cat", R.drawable.animal_cat, "Cat", "die", "Katze", "cica"),
    Animal("cow", R.drawable.animal_cow, "Cow", "die", "Kuh", "tehén"),
    Animal("horse", R.drawable.animal_horse, "Horse", "das", "Pferd", "ló"),
    Animal("pig", R.drawable.animal_pig, "Pig", "das", "Schwein", "malac"),
    Animal("sheep", R.drawable.animal_sheep, "Sheep", "das", "Schaf", "bárány"),
    Animal("chicken", R.drawable.animal_chicken, "Chicken", "das", "Huhn", "tyúk"),
    Animal("duck", R.drawable.animal_duck, "Duck", "die", "Ente", "kacsa"),
    Animal("rabbit", R.drawable.animal_rabbit, "Rabbit", "das", "Kaninchen", "nyuszi"),
    Animal("mouse", R.drawable.animal_mouse, "Mouse", "die", "Maus", "egér"),
    Animal("bird", R.drawable.animal_bird, "Bird", "der", "Vogel", "madár"),
    Animal("fish", R.drawable.animal_fish, "Fish", "der", "Fisch", "hal"),
    Animal("frog", R.drawable.animal_frog, "Frog", "der", "Frosch", "béka"),
    Animal("elephant", R.drawable.animal_elephant, "Elephant", "der", "Elefant", "elefánt"),
    Animal("lion", R.drawable.animal_lion, "Lion", "der", "Löwe", "oroszlán"),
    Animal("monkey", R.drawable.animal_monkey, "Monkey", "der", "Affe", "majom"),
    Animal("bear", R.drawable.animal_bear, "Bear", "der", "Bär", "medve"),
    Animal("giraffe", R.drawable.animal_giraffe, "Giraffe", "die", "Giraffe", "zsiráf"),
    Animal("butterfly", R.drawable.animal_butterfly, "Butterfly", "der", "Schmetterling", "pillangó"),
    Animal("squirrel", R.drawable.animal_squirrel, "Squirrel", "das", "Eichhörnchen", "mókus"),
)
