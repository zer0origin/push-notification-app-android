package com.willcocks.callum.timetablenotifier.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.callum.timetablenotifier.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.willcocks.callum.timetablenotifier.models.RegisterModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.net.ConnectException
import java.util.UUID
import java.util.function.Consumer
import java.util.function.Supplier

class PushNotificationService : FirebaseMessagingService() {
    val pushNotificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val TAG = "PushNotificationService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body
        showNotification(title, body)
    }

    private fun showNotification(title: String?, message: String?) {
        val channelId = "default_channel_id"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId).setContentTitle(title)
            .setContentText(message).setSmallIcon(R.drawable.ic_calendar_icon_foreground).build()

        notificationManager.notify(1, notification)
    }

    fun getCurrentToken(): String? {
        return runBlocking {
            getCurrentTokenSync()
        }
    }

    suspend fun getCurrentTokenSync(): String? {
        return FirebaseMessaging.getInstance().token.await()
    }

    private val client = OkHttpClient()

    @OptIn(InternalSerializationApi::class)
    fun registerToken(hostname: String, request: RegisterModel, callback: (UUID?) -> Unit) {
        val reqJson = Json.Default.encodeToString(request)
        val JSON: MediaType? = MediaType.parse("application/json; charset=utf-8")
        val body: RequestBody = RequestBody.create(JSON, reqJson)
        Log.d(ContentValues.TAG, "registerToken Body: $reqJson")

        val request = Request.Builder().url("$hostname/register").post(body).build()

        pushNotificationScope.launch {
            try {
                val response: Response = client.newCall(request).execute()
                response.body().use { responseBody ->
                    val data = responseBody?.string()

                    if (data == null) {
                        callback(null)
                        return@launch
                    }

                    val jsonObject = JSONObject(data)
                    val uidStr: String = (jsonObject.get("userUID") ?: "") as String

                    val uuid = UUID.fromString(uidStr)
                    callback(uuid)
                }
            } catch (e: ConnectException) {
                Log.d(TAG, "Failed to connect to API service!")
//                showNotification("TableNotifier", "Failed to connect to API service!")
                e.printStackTrace()
                return@launch
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                callback(null)
                return@launch
            }
        }
    }
}