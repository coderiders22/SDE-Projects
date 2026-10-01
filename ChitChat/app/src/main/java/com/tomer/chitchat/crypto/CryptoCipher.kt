package com.tomer.chitchat.crypto

import android.util.Base64
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec


class CryptoCipher(
    private val repoCipher: RepoCipher
) : CryptoService {

    private var sectKeySpec: SecretKeySpec? = null

    private fun getKeySpecForPhone(phone: String): SecretKeySpec? {
        return try {
            getSecretKey(repoCipher.getKey(phone)?.keyBytes ?: return null)
        } catch (_: Exception) {
            null
        }
    }

    override fun setCurrentPartner(phone: String): Boolean {
        sectKeySpec = null
        val mod = repoCipher.getKey(phone) ?: return false
        try {
            sectKeySpec = getSecretKey(mod.keyBytes)
            return true
        } catch (_: Exception) {
        }
        return false
    }

    private fun generateAndSaveNewKeyForUser(phone: String) =
        CryptoKey(
            phone, false,
            CipherUtils.generateRandom().toString(16),
            ByteArray(1)
        ).also {
            repoCipher.saveKey(it)
        }


    override fun updateKeyAndGenerateFullKey(secret: String, phone: String): Boolean {
        val isAlreadyKeyPresent = repoCipher.getKey(phone) != null
        val oldKey = checkForKeyAndGenerateIfNot(phone)
        repoCipher.saveKey(
            CryptoKey(
                phone, true,
                oldKey.tempKeyMy,
                BigInteger(secret, 16).modPow(BigInteger(oldKey.tempKeyMy, 16), CipherUtils.P)
                    .toByteArray()
            )
        )
        return isAlreadyKeyPresent
    }

    override fun checkForKeyAndGenerateIfNot(phone: String): CryptoKey =
        repoCipher.getKey(phone) ?: generateAndSaveNewKeyForUser(phone)


    @Throws(Exception::class)
    private fun getSecretKey(secretKey: ByteArray): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(secretKey)
        return SecretKeySpec(bytes.copyOf(256 / 8), "AES")
    }


    override fun encString(data: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = CipherUtils.getEncIv()
        val sb = StringBuilder(Base64.encodeToString(iv.iv, Base64.DEFAULT))
        cipher.init(Cipher.ENCRYPT_MODE, sectKeySpec, iv)
        sb.append(Base64.encodeToString(cipher.doFinal(data.encodeToByteArray()), Base64.DEFAULT))
        return sb.toString()
    }

    override fun decString(phone: String, data: String): String? {
        val localSecKey = getKeySpecForPhone(phone) ?: return null
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, localSecKey, CipherUtils.getDecIv(data))
        return try {
            String(cipher.doFinal(Base64.decode(data.substring(24), Base64.DEFAULT)))
        } catch (_: Exception) {
            null
        }
    }


    override fun encStream(ins: InputStream, ois: OutputStream): Boolean {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, sectKeySpec, CipherUtils.ivSpecProvider)
            val cipherOut = CipherOutputStream(ois, cipher)

            ins.copyTo(cipherOut)
            cipherOut.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun decStream(phone: String, ins: InputStream, ois: OutputStream): Boolean {
        val localSecKey = getKeySpecForPhone(phone) ?: return false
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, localSecKey, CipherUtils.ivSpecProvider)
            val cipherIn = CipherInputStream(ins, cipher)

            cipherIn.copyTo(ois)
            cipherIn.close()
            true
        } catch (e: Exception) {
            false
        }

    }


}