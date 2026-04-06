package com.example.bookmymovie.config

import android.content.Context
import java.util.*

object ConfigManager {
    private var properties: Properties? = null

    fun initialize(context: Context) {
        properties = Properties()
        try {
            val inputStream = context.assets.open("env_config.properties")
            properties?.load(inputStream)
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProperty(key: String, defaultValue: String = ""): String {
        return properties?.getProperty(key, defaultValue) ?: defaultValue
    }

    fun getGoogleMapsApiKey(context: Context): String {
        if (properties == null) {
            initialize(context)
        }
        return getProperty("GOOGLE_MAPS_API_KEY", "")
    }
}
