package voice.core.extension.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One search result returned by a source script (standard fields + extras). */
public data class SearchItem(
  public val id: String,
  public val title: String,
  public val author: String,
  public val cover: String,
  public val intro: String,
  public val trackCount: Int,
  public val extra: JsonObject,
)

/** One chapter returned by a source script (standard fields + extras). */
public data class ChapterItem(
  public val id: String,
  public val title: String,
  public val order: Int,
  public val durationSeconds: Long,
  public val extra: JsonObject,
)

/** Thrown when a source script violates the field contract. */
public class SourceContractException public constructor(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Validates and normalizes the JSON the three source stages return. Field
 * rules mirror the reference contract: search items need `id` + `bookTitle`,
 * chapter items need `chapter_id` + `title`, audio needs an http(s) url.
 */
public object SourceContract {

  private val json = Json { ignoreUnknownKeys = true }

  public fun parseSearchResults(json: String): List<SearchItem> {
    return parseItems(json, "search") { item, index ->
      val id = item.stringField(listOf("id"), index)
      val title = item.stringField(listOf("bookTitle", "title"), index)
      SearchItem(
        id = id,
        title = title,
        author = item.stringField(listOf("bookAnchor", "author", "teller"), index, optional = true),
        cover = item.stringField(listOf("bookImage", "cover"), index, optional = true),
        intro = item.stringField(listOf("bookDesc", "intro", "description"), index, optional = true),
        trackCount = item.intField(listOf("count", "trackCount", "total"), index, optional = true) ?: 0,
        extra = item,
      )
    }
  }

  public fun parseChapters(json: String): List<ChapterItem> {
    return parseItems(json, "chapters") { item, index ->
      val id = item.stringField(listOf("chapter_id", "id", "trackId"), index)
      val title = item.stringField(listOf("title", "name"), index)
      ChapterItem(
        id = id,
        title = title,
        order = item.intField(listOf("order", "index", "episode_num"), index, optional = true) ?: (index + 1),
        durationSeconds =
          item.longField(listOf("duration", "durationSeconds"), index, optional = true) ?: 0L,
        extra = item,
      )
    }
  }

  /** Returns the resolved audio url. */
  public fun parseAudioUrl(json: String): String {
    val element = try {
      json.decodeJsonLenient()
    } catch (e: Exception) {
      throw SourceContractException("audio 返回的不是合法 JSON: ${e.message}", e)
    }
    val url = when (element) {
      is JsonPrimitive -> element.contentOrNull
      is JsonObject -> element.stringField(listOf("url", "audio_url", "playUrl", "play_url"), 0, optional = true)
      else -> null
    }
    if (url.isNullOrBlank()) {
      throw SourceContractException("audio 未返回音频 URL")
    }
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      throw SourceContractException("audio 返回的 URL 不是 http(s) 链接")
    }
    return url
  }

  private inline fun <T> parseItems(
    json: String,
    stage: String,
    mapItem: (JsonObject, Int) -> T,
  ): List<T> {
    val element = try {
      json.decodeJsonLenient()
    } catch (e: Exception) {
      throw SourceContractException("$stage 返回的不是合法 JSON: ${e.message}", e)
    }
    if (element !is JsonArray) {
      throw SourceContractException("$stage 必须返回数组")
    }
    return element.mapIndexed { index, item ->
      if (item !is JsonObject) {
        throw SourceContractException("$stage[$index] 不是对象")
      }
      mapItem(item, index)
    }
  }

  private fun String.decodeJsonLenient() = json.parseToJsonElement(this)

  private fun JsonObject.stringField(
    names: List<String>,
    index: Int,
    optional: Boolean = false,
  ): String {
    for (name in names) {
      val value = this[name]
      if (value is JsonPrimitive) {
        val content = value.contentOrNull ?: continue
        if (content.isNotBlank()) return content
      }
    }
    if (optional) return ""
    throw SourceContractException("第${index + 1}项缺少必填字段 $names")
  }

  private fun JsonObject.intField(
    names: List<String>,
    index: Int,
    optional: Boolean,
  ): Int? = longField(names, index, optional)?.toInt()

  private fun JsonObject.longField(
    names: List<String>,
    index: Int,
    optional: Boolean,
  ): Long? {
    for (name in names) {
      val value = this[name]
      if (value is JsonPrimitive) {
        val asLong = value.contentOrNull?.toLongOrNull()
        if (asLong != null) return asLong
      }
    }
    return if (optional) null else throw SourceContractException("第${index + 1}项缺少必填字段 $names")
  }
}
