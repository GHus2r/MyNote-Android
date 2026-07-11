package com.mynote.android.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

/**
 * 从 assets 加载 JSON 静态数据的工具
 * 用于替代硬编码在 Kotlin 中的大型数据字典
 */
object JsonAssetLoader {

    @PublishedApi internal val gson = Gson()

    /**
     * 从 assets 加载 JSON 数组并反序列化为指定类型列表
     * 失败返回 null（调用方可用硬编码兜底）
     */
    inline fun <reified T> loadList(context: Context, assetPath: String): List<T>? {
        return try {
            val stream = context.assets.open(assetPath)
            val reader = InputStreamReader(stream, Charsets.UTF_8)
            val type = object : TypeToken<List<T>>() {}.type
            val result: List<T> = gson.fromJson(reader, type)
            reader.close()
            result
        } catch (e: Exception) {
            android.util.Log.e("JsonAsset", "Failed to load $assetPath", e)
            null
        }
    }
}
