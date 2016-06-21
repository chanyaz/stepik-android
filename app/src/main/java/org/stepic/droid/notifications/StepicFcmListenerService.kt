package org.stepic.droid.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.yandex.metrica.YandexMetrica
import org.stepic.droid.base.MainApplication
import org.stepic.droid.core.IShell
import org.stepic.droid.notifications.model.Notification
import javax.inject.Inject

class StepicFcmListenerService : FirebaseMessagingService() {

    val hacker = HackFcmListener()

    override fun onMessageReceived(message : RemoteMessage) {
         val data = message.data
        val notificationRawString: String? = data?.get("object")
        try {
            val userId = hacker.mShell.getSharedPreferenceHelper().profile.id
            val userIdServerString = data?.get("user_id")?:""
            val userIdServer = Integer.parseInt(userIdServerString)
            if (userIdServer.toLong() != userId){
                return;
            }
            val stepicNotification = Gson().fromJson(notificationRawString, Notification::class.java)
            stepicNotification?.let {
                hacker.notificationManager.showNotification(it)
            }
        } catch(e: Exception) {
            YandexMetrica.reportError("notification error parse", e);
        }
    }

}

class HackFcmListener() {
        @Inject
    lateinit var notificationManager: INotificationManager

        @Inject
    lateinit var mShell : IShell

    init {
        MainApplication.component().inject(this)
    }
}