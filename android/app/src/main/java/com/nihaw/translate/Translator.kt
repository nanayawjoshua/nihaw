package com.nihaw.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.android.gms.tasks.Tasks

class Translator {

    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    private val cache = mutableMapOf<String, String>()

    fun translate(text: String, onResult: (String) -> Unit, onError: ((Exception) -> Unit)? = null) {
        cache[text]?.let {
            onResult(it)
            return
        }

        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { result ->
                        cache[text] = result
                        onResult(result)
                    }
                    .addOnFailureListener { e ->
                        onError?.invoke(e)
                    }
            }
            .addOnFailureListener { e ->
                onError?.invoke(e)
            }
    }

    suspend fun translateAsync(text: String): String {
        cache[text]?.let { return it }

        val conditions = DownloadConditions.Builder().build()
        Tasks.await(translator.downloadModelIfNeeded(conditions))

        val result = Tasks.await(translator.translate(text))
        cache[text] = result
        return result
    }

    fun downloadModel() {
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
    }

    fun close() {
        translator.close()
    }
}
