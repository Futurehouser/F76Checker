package com.futurehouser.f76checker

/** In-app UI strings, switchable at runtime independently of the phone's language. */
class Strings(val lang: Lang) {
    private fun t(en: String, es: String) = if (lang == Lang.ES) es else en

    val home get() = t("Home", "Inicio")
    val categories get() = t("Categories", "Categorías")
    val homeSubtitle get() = t("Pick a category to track", "Elige una categoría")
    val totalProgress get() = t("Total collection", "Colección total")
    fun setsCount(n: Int) = t("$n sets", "$n conjuntos")
    fun itemsCount(n: Int) = t("$n items", "$n objetos")
    val search get() = t("Search…", "Buscar…")
    val all get() = t("All", "Todo")
    val owned get() = t("Owned", "Tengo")
    val missing get() = t("Missing", "Me faltan")
    val noResults get() = t("No items match", "No hay resultados")
    val checkUpdates get() = t("Check for updates", "Buscar actualizaciones")
    val resetAll get() = t("Clear all checks", "Desmarcar todo")
    val resetConfirm get() = t("Uncheck every item in this category?", "¿Desmarcar todos los elementos de esta categoría?")
    val soundOn get() = t("Sounds: on", "Sonidos: activados")
    val soundOff get() = t("Sounds: off", "Sonidos: desactivados")
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
    val openMenu get() = t("Open menu", "Abrir menú")
    val wholeSet get() = t("Whole set", "Conjunto completo")
}
