package com.gymbuddy.wear

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

// Shared with the phone app (src/hooks/useWatchConnection.ts).
const val MESSAGE_PATH = "/gymbuddy/msg"

// Advertised by the phone app (modules/wear-bridge/android/src/main/res/values/wear.xml).
private const val PHONE_CAPABILITY = "gymbuddy_phone"

data class Payload(val type: String, val text: String? = null, val ts: Long = System.currentTimeMillis()) {
    fun toJson(): String = JSONObject().apply {
        put("type", type)
        text?.let { put("text", it) }
        put("ts", ts)
    }.toString()

    companion object {
        fun fromJson(json: String): Payload = try {
            val obj = JSONObject(json)
            Payload(
                type = obj.optString("type", "text"),
                text = if (obj.has("text")) obj.getString("text") else null,
                ts = obj.optLong("ts", System.currentTimeMillis()),
            )
        } catch (e: Exception) {
            Payload(type = "text", text = json)
        }
    }
}

/** Thin wrapper around the Wear OS Data Layer for talking to the GymBuddy phone app. */
class PhoneConnection(context: Context) {
    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    /** Reachable phones that have the GymBuddy app installed. */
    suspend fun phoneNodes(): Set<Node> =
        capabilityClient.getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE).await().nodes

    /** Sends to every reachable phone and returns how many were reached. */
    suspend fun send(payload: Payload): Int {
        val bytes = payload.toJson().toByteArray(Charsets.UTF_8)
        val nodes = phoneNodes()
        nodes.forEach { messageClient.sendMessage(it.id, MESSAGE_PATH, bytes).await() }
        return nodes.size
    }

    fun addMessageListener(onMessage: (Payload) -> Unit): MessageClient.OnMessageReceivedListener {
        val listener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
            if (event.path == MESSAGE_PATH) {
                onMessage(Payload.fromJson(String(event.data, Charsets.UTF_8)))
            }
        }
        messageClient.addListener(listener)
        return listener
    }

    fun removeMessageListener(listener: MessageClient.OnMessageReceivedListener) {
        messageClient.removeListener(listener)
    }
}
