package com.tomer.chitchat.adap


enum class ClickEvents {
    DOWNLOAD, UPLOAD, REPLY, ROOT, FILE, IMAGE
}

interface ChatViewEvents {
    fun onChatItemClicked(pos: Int, type: ClickEvents)
    fun onChatItemLongClicked(pos: Int)
    fun onOpenLinkInBrowser(link: String)
}