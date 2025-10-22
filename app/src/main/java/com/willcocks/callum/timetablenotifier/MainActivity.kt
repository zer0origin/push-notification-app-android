package com.willcocks.callum.timetablenotifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.callum.timetablenotifier.R
import com.willcocks.callum.timetablenotifier.models.RegisterModel
import com.willcocks.callum.timetablenotifier.network.PushNotificationService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.InternalSerializationApi
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val p = PushNotificationService()
    private val TAG = "MainActivity"
    private val serverApiHostname = "http://10.0.2.2:8082"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @OptIn(InternalSerializationApi::class, DelicateCoroutinesApi::class)
    override fun onStart() {
        Log.d(TAG, "Beginning Startup")
        super.onStart()

        Log.d(TAG, "Checking application permissions")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        val filename = "settings"
        val settingsFile = File(filesDir, filename)
        val config = Config(settingsFile)

        Log.d(TAG, "Loading config")
        config.loadOrCreateConfig(contentToWriteToFile = {
            config.setContent("uuid", "")
            config.setContent("username", "")
        })

        var uuidStr = config.getContent("uuid")
        var username = config.getContent("username") ?: ""
        if (username.isEmpty()){
            Log.e(TAG, "No username present!")
            setContentView(R.layout.setup)
            return
        }

        var reqModel: RegisterModel
        if (uuidStr?.isEmpty() == true || uuidStr == null) {
            reqModel = RegisterModel("${p.getCurrentToken()}", username, null)
        } else {
            Log.d(TAG, "Reusing value of uuid: $uuidStr")
            reqModel = try {
                RegisterModel("${p.getCurrentToken()}", username, UUID.fromString(uuidStr))
            } catch (e: IllegalArgumentException) {
                RegisterModel("${p.getCurrentToken()}", username, null)
            }
        }

        Log.d(TAG, "Attempting to register")
        p.registerToken(serverApiHostname, reqModel, callback = { uuid ->
            Log.d(TAG, "Received UUID of ${uuid.toString()} from server!")
            config.setContent("uuid", uuid.toString())
            config.writeContentsToDisk()
        })

        Log.d(TAG, "Started")
    }

    fun getToken(view: View) {
        val token = p.getCurrentToken()
        val tokenInfo = findViewById<TextView>(R.id.tokenInfo)
        tokenInfo.text = token
        Log.d(TAG, "Token: $token")
    }
}