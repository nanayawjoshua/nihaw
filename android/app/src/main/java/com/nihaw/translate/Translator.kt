package com.nihaw.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class Translator {

    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    private val cache = mutableMapOf<String, String>()

    suspend fun translate(text: String): String {
        cache[text]?.let { return it }

        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)

        val result = translator.translate(text)
        cache[text] = result
        return result
    }

    fun downloadModel() {
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
    }
}
