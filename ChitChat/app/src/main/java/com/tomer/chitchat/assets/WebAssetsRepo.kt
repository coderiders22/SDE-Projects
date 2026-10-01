package com.tomer.chitchat.assets

import android.content.Context
import android.os.Build
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.modals.states.UiMsgModalBuilder
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
import com.tomer.chitchat.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import javax.inject.Inject
import kotlin.random.Random

class WebAssetsRepo @Inject constructor(
    context: Context
) : RepoAssets {

    private val assetsFolder =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) File(
            File(context.getExternalFilesDir("ChitChat"), "assets")
                .absolutePath.replace("Android/data", "Android/media").replace(
                    ".chitchat/files",
                    ".chitchat"
                )
        )
        else File(context.getExternalFilesDir("ChitChat"), "assets")
    private val gifAssets = File(assetsFolder, "gifs")
    private val jsonAssets = File(assetsFolder, "jsons")

    private val jsonFilesBinLink = "https://chitchat.devhimu.in/assets/assets/jsons/"
    private val gifFilesBinLink = "https://chitchat.devhimu.in/assets/assets/gifs/"
    private val telemojiFilesBinLink = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Telegram-Animated-Emojis/main/"
    private val googleJsonFilesBinLink = "https://fonts.gstatic.com/s/e/notoemoji/latest/"

    init {
        if (gifAssets.mkdirs())
            File(gifAssets, ".nomedia").createNewFile()
        jsonAssets.mkdirs()
    }

    override suspend fun getLottieJson(name: String, sync: Boolean): String? {
        val f = File(jsonAssets, name)
        if (f.exists()) return readJson(f)
        if (sync) return null

        val bytes = NetworkUtils.downloadBytes("$jsonFilesBinLink$name") ?: return null
        withContext(Dispatchers.IO) {
            FileOutputStream(f).use {
                it.write(bytes)
                it.flush()
            }
        }
        return String(bytes, Charset.defaultCharset())
    }

    override suspend fun getGoogleLottieJson(nameJson: String, sync: Boolean): String? {
        val f = File(jsonAssets, nameJson)
        if (f.exists()) return readJson(f)
        if (sync) return null

        val bytes = NetworkUtils.downloadBytes("$googleJsonFilesBinLink$nameJson/lottie.json") ?: return null
        withContext(Dispatchers.IO) {
            FileOutputStream(f).use {
                it.write(bytes)
                it.flush()
            }
        }
        return String(bytes, Charset.defaultCharset())
    }

    override suspend fun getGifFile(name: String, sync: Boolean): File? {
        val f = File(gifAssets, name)
        if (f.exists()) return f
        if (sync) return null

        return if (NetworkUtils.downloadBytesToFile("$gifFilesBinLink$name.gif", f))
            f else null
    }

    override suspend fun getGifTelemoji(name: String, sync: Boolean): File? {
        val f = File(gifAssets, name)
        if (f.exists()) return f
        if (sync) return null

        return if (NetworkUtils.downloadBytesToFile("$telemojiFilesBinLink${ConversionUtils.decode(name)}.webp", f))
            f else null
    }

    override suspend fun getRandomJson(): UiMsgModal {
        val b = UiMsgModalBuilder()
        val google = Random.nextBoolean()
        val entry = (if (google)
            getRandomEntryEfficiently(EmojisHashingUtils.googleJHash)
        else getRandomEntryEfficiently(EmojisHashingUtils.jHash)) ?: return b.build()

        val data = (if (google) getGoogleLottieJson(entry.value)
        else getLottieJson(entry.value)) ?: return b.build()

        b.setMsg(data)
        b.mediaFileName(entry.value)
        b.mediaSize(entry.key)

        return b.build()
    }

    private fun <K, V> getRandomEntryEfficiently(map: Map<K, V>): Map.Entry<K, V>? {
        if (map.isEmpty()) return null
        var selectedEntry: Map.Entry<K, V>? = null
        for ((count, entry) in map.entries.withIndex()) {
            if (Random.nextInt(count + 1) == 0) {
                selectedEntry = entry
            }
        }
        return selectedEntry
    }

    private fun readJson(file: File): String {
        FileInputStream(file).use {
            return String(it.readBytes(), Charset.defaultCharset())
        }
    }

}