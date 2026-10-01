package com.tomer.chitchat

import com.google.firebase.installations.time.SystemClock
import com.tomer.chitchat.crypto.CipherUtils
import com.tomer.chitchat.crypto.CryptoCipher
import com.tomer.chitchat.crypto.CryptoKey
import com.tomer.chitchat.crypto.RepoCipher
import com.tomer.chitchat.crypto.RoomCipherRepo
import kotlinx.coroutines.delay
import org.junit.Test

import org.junit.Assert.*
import java.util.Arrays
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
//            val pocof3 = "98f26ca3e22bf852fbf2ef2b7f65dbc7e89bffeec122a918c82f57985be7b08a66b525cb3a3eb5d20df7b3b1809d978498afee47c8f5231d";
//            val pocof1 = "3154985f9510d5f945f5dd6b0979de51e2ec2bec56e497f156bf7f8f1b8b3fda84fae2b104a747aef1c6f0c455255339305803fa2e395d45";
//            val serf1 = CryptoCipher(
//                object : RepoCipher{
//                    override fun getKey(phone: String): CryptoKey {
//                        val key = CryptoKey(
//                            "8218549340",false,
//                            pocof1
//                            ,ByteArray(1)
//                        )
//                        println(key)
//                        return key;
//                    }
//
//                    override fun saveKey(key: CryptoKey) {
//                        println("saving $key")
//                    }
//
//                }
//            )
//
//        println(serf1.updateKeyAndGenerateFullKey(pocof3,"8218549340"))
//
//
//        val serf2 = CryptoCipher(
//                object : RepoCipher{
//                    override fun getKey(phone: String): CryptoKey {
//                        val key = CryptoKey(
//                            "9997628974",false,
//                            pocof3
//                            ,ByteArray(1)
//                        )
//                        println(key)
//                        return key;
//                    }
//
//                    override fun saveKey(key: CryptoKey) {
//                        println("saving $key")
//                    }
//
//                }
//            )
//
//        println(serf2.updateKeyAndGenerateFullKey(pocof1,"9997628974"))

        var list = CopyOnWriteArrayList<String>(listOf("tomer","Himu","Bahi","NHI","degi","tujhe"))

        val thre = thread {
            Thread.sleep(200)
            list.add("dekjo")
            println(list)
        }
        val itr = list.iterator()
        while (itr.hasNext()){
            val ele = itr.next()
            if (ele.startsWith('t')) itr.remove()
        }


        println(list)
        thre.join()

    }
}