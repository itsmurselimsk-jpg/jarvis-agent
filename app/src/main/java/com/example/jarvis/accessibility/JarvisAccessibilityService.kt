package com.example.jarvis.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiElementNode(
    val text: String,
    val contentDescription: String,
    val viewId: String,
    val className: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val bounds: Rect
)

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: JarvisAccessibilityService? = null

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _lastInspectedNodes = MutableStateFlow<List<UiElementNode>>(emptyList())
        val lastInspectedNodes: StateFlow<List<UiElementNode>> = _lastInspectedNodes.asStateFlow()

        fun isServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${JarvisAccessibilityService::class.java.name}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(":").any { it.equals(expectedServiceName, ignoreCase = true) }
        }

        fun getInstance(): JarvisAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isRunning.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive observation; only inspect when explicitly commanded
    }

    override fun onInterrupt() {
        _isRunning.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isRunning.value = false
    }

    // Inspect visible UI nodes
    fun inspectVisibleUi(): List<UiElementNode> {
        val root = rootInActiveWindow ?: return emptyList()
        val results = mutableListOf<UiElementNode>()
        traverseNode(root, results)
        _lastInspectedNodes.value = results
        return results
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, list: MutableList<UiElementNode>) {
        if (node == null) return

        // Safety gate: never inspect or extract password or pin fields
        if (node.isPassword) return

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (text.isNotBlank() || desc.isNotBlank() || node.isClickable) {
            list.add(
                UiElementNode(
                    text = text,
                    contentDescription = desc,
                    viewId = viewId,
                    className = className,
                    isClickable = node.isClickable,
                    isEditable = node.isEditable,
                    bounds = bounds
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, list)
        }
    }

    // Tap on a node matching visible text
    fun tapOnText(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchedNodes = root.findAccessibilityNodeInfosByText(query)
        for (node in matchedNodes) {
            var target: AccessibilityNodeInfo? = node
            while (target != null) {
                if (target.isClickable) {
                    val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                target = target.parent
            }
        }
        return false
    }

    // Dispatch a gesture tap at screen coordinates
    fun tapAt(x: Float, y: Float, onComplete: (Boolean) -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 100)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    onComplete(true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    onComplete(false)
                }
            }, null)
        } else {
            onComplete(false)
        }
    }

    // Scroll active window forward or backward
    fun performScroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return root.performAction(action)
    }

    // Type text into active editable focus
    fun typeTextIntoFocused(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        if (focused.isPassword) return false // Safety rule: refuse to type into password fields
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    // Global navigation actions
    fun navigateBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun navigateHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun showRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
}
