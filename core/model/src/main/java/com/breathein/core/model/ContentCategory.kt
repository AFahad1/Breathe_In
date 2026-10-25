package com.breathein.core.model

/** What a reel or short is about. [label] is shown to the user; the enum name is what the AI classifier answers with. */
enum class ContentCategory(val label: String) {
    Fitness("Gym & fitness"),
    Memes("Memes"),
    Comedy("Comedy skits"),
    StandUp("Stand-up comedy"),
    News("News"),
    Cooking("Cooking"),
    Music("Music"),
    Guitar("Guitar"),
    Dance("Dance"),
    Sports("Sports"),
    Skills("Skills & DIY"),
    Learning("Learning"),
    Travel("Travel"),
    Beauty("Fashion & beauty"),
    Gaming("Gaming"),
    Babies("Baby videos"),
    Cats("Cat videos"),
    Animals("Animal videos"),
    Other("Other"),
}
