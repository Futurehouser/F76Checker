package com.futurehouser.f76checker

/** In-app UI strings, switchable at runtime independently of the phone's language. */
class Strings(val lang: Lang) {
    private fun t(en: String, es: String) = if (lang == Lang.ES) es else en

    val search get() = t("Search…", "Buscar…")
    val all get() = t("All", "Todas")
    val owned get() = t("Owned", "Tengo")
    val missing get() = t("Missing", "Me faltan")
    val noResults get() = t("No items match", "No hay resultados")
    val checkUpdates get() = t("Check for updates", "Buscar actualizaciones")
    val openWiki get() = t("Open in wiki", "Abrir en la wiki")
    val resetAll get() = t("Clear all checks", "Desmarcar todo")
    val resetConfirm get() = t("Uncheck every item in this category?", "¿Desmarcar todos los elementos de esta categoría?")
    val cancel get() = t("Cancel", "Cancelar")
    val ok get() = t("OK", "Aceptar")
    val updateTitle get() = t("Update available", "Actualización disponible")
    fun updateBody(v: String, cur: String) =
        t("Version $v is available (you have $cur). Download and install it now?",
          "La versión $v está disponible (tienes la $cur). ¿Descargarla e instalarla ahora?")
    val update get() = t("Update", "Actualizar")
    val later get() = t("Later", "Más tarde")
    val skip get() = t("Skip this version", "Omitir esta versión")
    val downloading get() = t("Downloading update…", "Descargando actualización…")
    val upToDate get() = t("You have the latest version", "Tienes la última versión")
    val updateError get() = t("Could not check for updates", "No se pudo comprobar si hay actualizaciones")
    val downloadError get() = t("Download failed", "Error en la descarga")
    val allowInstall get() = t(
        "Allow this app to install updates, then press Update again.",
        "Permite que esta app instale actualizaciones y pulsa Actualizar de nuevo."
    )
    val releaseNotes get() = t("What's new", "Novedades")

    val sections: Map<String, String> get() = if (lang == Lang.ES) mapOf(
        "pistols_rifles" to "Pistolas / Rifles",
        "pistols" to "Pistolas",
        "rifles" to "Rifles",
        "shotguns" to "Escopetas",
        "bows" to "Arcos",
        "heavy" to "Armas pesadas",
        "explosive" to "Armas pesadas explosivas",
        "unique_ranged" to "Únicas (a distancia)",
        "one_handed" to "Cuerpo a cuerpo: una mano",
        "two_handed" to "Cuerpo a cuerpo: dos manos",
        "unarmed" to "Desarmado",
        "unique_melee" to "Únicas (cuerpo a cuerpo)",
        "grenades" to "Granadas",
        "mines" to "Minas",
        "throwing" to "Cuchillos arrojadizos",
        "other" to "Otras",
    ) else mapOf(
        "pistols_rifles" to "Pistols / Rifles",
        "pistols" to "Pistols",
        "rifles" to "Rifles",
        "shotguns" to "Shotguns",
        "bows" to "Bows",
        "heavy" to "Heavy guns",
        "explosive" to "Explosive heavy guns",
        "unique_ranged" to "Unique (ranged)",
        "one_handed" to "Melee: one-handed",
        "two_handed" to "Melee: two-handed",
        "unarmed" to "Unarmed",
        "unique_melee" to "Unique (melee)",
        "grenades" to "Grenades",
        "mines" to "Mines",
        "throwing" to "Throwing knives",
        "other" to "Other",
    )
}
