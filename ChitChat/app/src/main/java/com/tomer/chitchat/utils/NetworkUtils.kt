package com.tomer.chitchat.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object NetworkUtils {

    suspend fun downloadBytes(urlString: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var outputStream: ByteArrayOutputStream? = null
            var connection: HttpURLConnection? = null

            return@withContext try {
                val url = URL(urlString)
                connection = withContext(Dispatchers.IO) {
                    url.openConnection()
                } as HttpURLConnection
                connection.requestMethod = "GET"

                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK)
                    throw Exception("HTTP error code: ${connection.responseCode}")

                inputStream = connection.inputStream
                outputStream = ByteArrayOutputStream()

                val buffer = ByteArray(1024)
                var bytesRead: Int

                while (withContext(Dispatchers.IO) {
                        inputStream.read(buffer)
                    }.also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                outputStream.toByteArray()
            } catch (e: Exception) {
                null
            } finally {
                withContext(Dispatchers.IO) {
                    inputStream?.close()
                    outputStream?.close()
                }
                connection?.disconnect()
            }
        }
    }

    suspend fun downloadBytesToFile(urlString: String, outFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            var inputStream: InputStream? = null
            val outputStream = outFile.outputStream()
            var connection: HttpURLConnection? = null

            return@withContext try {
                val url = URL(urlString)
                connection = withContext(Dispatchers.IO) {
                    url.openConnection()
                } as HttpURLConnection
                connection.requestMethod = "GET"

                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK)
                    throw Exception("HTTP error code: ${connection.responseCode}")

                inputStream = connection.inputStream

                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (withContext(Dispatchers.IO) {
                        inputStream.read(buffer)
                    }.also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
                true
            } catch (e: Exception) {
                false
            } finally {
                inputStream?.close()
                outputStream.close()
                connection?.disconnect()
            }
        }
    }

}