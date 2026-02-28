package com.codesrahul.streamplus.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MapTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): Map<String, String> {
        if (value == null) return emptyMap()
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMap(map: Map<String, String>?): String {
        return gson.toJson(map ?: emptyMap<String, String>())
    }
}
