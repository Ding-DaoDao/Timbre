package voice.core.online

import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnlineChapterStoreTest {

  private val baseDir = createTempDirectory("online-chapters").toFile()
  private val store = OnlineChapterStore(baseDir)

  private val chapters = listOf(
    OnlineChapter(id = "c1", title = "第1集", durationSeconds = 1800),
    OnlineChapter(id = "c2", title = "第2集", durationSeconds = 0),
  )

  @Test
  fun `a stored chapter list survives a new store instance`() = runTest {
    store.put("A::b1", chapters)

    val reloaded = OnlineChapterStore(baseDir)
    assertEquals(chapters, reloaded.chapters("A::b1"))
  }

  @Test
  fun `loadAll restores every stored list into memory`() = runTest {
    val other = listOf(OnlineChapter(id = "c1", title = "第1集"))
    store.put("A::b1", chapters)
    store.put("B::b2", other)

    val reloaded = OnlineChapterStore(baseDir)
    reloaded.loadAll()

    assertEquals(chapters, reloaded.loadedChapters.value["A::b1"])
    assertEquals(other, reloaded.loadedChapters.value["B::b2"])
  }

  @Test
  fun `an unknown book reads as empty`() = runTest {
    assertTrue(store.chapters("A::unknown").isEmpty())
    assertTrue(store.loadedChapters.value.isEmpty())
  }

  @Test
  fun `remove deletes the stored list`() = runTest {
    store.put("A::b1", chapters)
    assertTrue(store.chapters("A::b1").isNotEmpty())

    store.remove("A::b1")

    assertTrue(store.chapters("A::b1").isEmpty())
    assertTrue(store.loadedChapters.value["A::b1"] == null)
  }

  @Test
  fun `updateChapterDuration touches only the matching chapter and persists`() = runTest {
    store.put("A::b1", chapters)

    assertTrue(store.updateChapterDuration("A::b1", "c2", 1750))

    val updated = store.chapters("A::b1")
    assertEquals(1800, updated.single { it.id == "c1" }.durationSeconds)
    assertEquals(1750, updated.single { it.id == "c2" }.durationSeconds)

    // a book without a stored list has nothing to update
    assertFalse(store.updateChapterDuration("A::unknown", "c2", 1750))
    // writing the same duration again reports no change
    assertFalse(store.updateChapterDuration("A::b1", "c2", 1750))
  }

  @Test
  fun `a corrupt file reads as empty without crashing`() = runTest {
    store.put("A::b1", chapters)
    // find the persisted file and trash it: the file name is a hash of the key
    val files = baseDir.listFiles().orEmpty().filter { it.extension == "json" }
    assertEquals(1, files.size)
    files.single().writeText("{ not json")

    assertTrue(OnlineChapterStore(baseDir).chapters("A::b1").isEmpty())
  }
}
