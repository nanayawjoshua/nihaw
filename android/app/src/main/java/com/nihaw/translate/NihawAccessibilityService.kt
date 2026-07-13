package com.nihaw.translate

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NihawAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // TODO: extract text nodes from event source
        // Filter Chinese Unicode range: \u4E00-\u9FFF, \u3400-\u4DBF
        // Deduplicate by text hash
        // Send to FloatingBubbleService for translation
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo?): List<String> {
        val texts = mutableListOf<String>()
        node ?: return texts
        if (node.text != null) {
            texts.add(node.text.toString())
        }
        for (i in 0 until node.childCount) {
            texts.addAll(extractTextFromNode(node.getChild(i)))
        }
        return texts
    }

    private fun isChinese(text: String): Boolean {
        return text.any { c ->
            val code = c.code
            (code in 0x4E00..0x9FFF) || (code in 0x3400..0x4DBF) || (code in 0xF900..0xFAFF)
        }
    }
}
