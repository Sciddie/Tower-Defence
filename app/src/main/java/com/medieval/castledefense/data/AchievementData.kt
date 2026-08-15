package com.medieval.castledefense.data

enum class Achievement(val title: String, val description: String) {
    FIRST_VICTORY("Erster Sieg", "Schließe dein erstes Level ab."),
    GOLD_RUSH("Goldrausch", "Sammle insgesamt 10.000 Gold."),
    CASTLE_LORD("Burgherr", "Schließe 25 Level ab."),
    INVINCIBLE("Unbesiegbar", "Schließe ein Level ab, ohne Leben zu verlieren."),
    BOSS_SLAYER("Bossbezwinger", "Besiege deinen ersten Boss."),
    MASTER_BUILDER("Baumeister", "Baue alle sechs Turmarten."),
    MAX_POWER("Volle Macht", "Bringe einen Turm auf Stufe 5."),
    SLAYER_500("Schlächter", "Besiege insgesamt 500 Gegner.")
}
