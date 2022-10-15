package com.puccontent.org.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class LastOpenData(
    val map: HashMap<String, Int>
) {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}