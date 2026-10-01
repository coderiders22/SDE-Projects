package com.tomer.chitchat.modals.msgs

import com.tomer.chitchat.utils.Utils

abstract class Message(private val msg: String) {
    override fun toString() = msg
}

class Typing : Message("*-TYP-*")
class NoTyping : Message("*N-TYP*")

class NewConnection(publicSecretHEX: String) : Message("*-NEW-*$publicSecretHEX,-,${Utils.myName}")
class AcceptConnection(publicSecretHEX: String) :
    Message("*F-ACC*$publicSecretHEX,-,${Utils.myName}")

class RejectConnection() : Message("*F-REJ*")
class BulkReceived(toPhone: String, list: String) : Message("$toPhone*MSG-BR$list")
class OfflineStatus() : Message("*P-STA*")

// <(10)toPhone>  <(7)MSG_TYPE>
// <DATA>  { <(12) TEMPID><ENC DATA> }
class ChatMessage(msgId: String, msg: String) : Message("*-MSG-*$msgId$msg")