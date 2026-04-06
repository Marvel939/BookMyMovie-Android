package com.example.bookmymovie.location

import android.content.Context
import com.example.bookmymovie.config.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val country: String,
    val state: String
)

object GoogleGeocoding {
    private const val GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json"

    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): LocationData? = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiKey = ConfigManager.getGoogleMapsApiKey(context)
            if (apiKey.isEmpty() || apiKey == "YOUR_GOOGLE_MAPS_API_KEY_HERE") {
                return@withContext null
            }
            val url = "$GEOCODING_API_URL?latlng=$latitude,$longitude&key=$apiKey"
            
            val response = makeApiCall(url)
            if (response != null) {
                parseGeocodingResponse(response, latitude, longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCoordinatesFromAddress(
        context: Context,
        address: String
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiKey = ConfigManager.getGoogleMapsApiKey(context)
            if (apiKey.isEmpty() || apiKey == "YOUR_GOOGLE_MAPS_API_KEY_HERE") {
                return@withContext null
            }
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val url = "$GEOCODING_API_URL?address=$encodedAddress&key=$apiKey"
            
            val response = makeApiCall(url)
            if (response != null) {
                parseCoordinatesResponse(response)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun makeApiCall(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val statusCode = connection.responseCode
            if (statusCode == 200) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.use { it.readText() }
                inputStream.close()
                response
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseGeocodingResponse(
        jsonResponse: String,
        latitude: Double,
        longitude: Double
    ): LocationData? {
        return try {
            val jsonObject = JSONObject(jsonResponse)
            val results = jsonObject.getJSONArray("results")
            
            if (results.length() > 0) {
                val firstResult = results.getJSONObject(0)
                val formattedAddress = firstResult.getString("formatted_address")
                val addressComponents = firstResult.getJSONArray("address_components")
                
                var city = ""
                var country = ""
                var state = ""
                
                for (i in 0 until addressComponents.length()) {
                    val component = addressComponents.getJSONObject(i)
                    val types = component.getJSONArray("types")
                    val longName = component.getString("long_name")
                    
                    for (j in 0 until types.length()) {
                        val type = types.getString(j)
                        when (type) {
                            "locality" -> city = longName
                            "administrative_area_level_1" -> state = longName
                            "country" -> country = longName
                        }
                    }
                }
                
                LocationData(
                    latitude = latitude,
                    longitude = longitude,
                    address = formattedAddress,
                    city = city,
                    country = country,
                    state = state
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCoordinatesResponse(jsonResponse: String): Pair<Double, Double>? {
        return try {
            val jsonObject = JSONObject(jsonResponse)
            val results = jsonObject.getJSONArray("results")
            
            if (results.length() > 0) {
                val firstResult = results.getJSONObject(0)
                val location = firstResult.getJSONObject("geometry").getJSONObject("location")
                val latitude = location.getDouble("lat")
                val longitude = location.getDouble("lng")
                Pair(latitude, longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
