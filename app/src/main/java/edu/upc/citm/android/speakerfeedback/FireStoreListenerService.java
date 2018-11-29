package edu.upc.citm.android.speakerfeedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class FireStoreListenerService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("SpeakerFeecback", "FireStoreListenerService.onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
      Log.i("SpeakerFeecback", "FireStoreListenerService.onStartCommand");

      //TODO: Crear una notificació i cridar startForeground (perquè el servei segeueix funcionant)
        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(String.format("Connectat a testroom"))
                .setSmallIcon(R.drawable.ic_message)
                .build();

        startForeground(1,notification);
      return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("SpeakerFeecback", "FireStoreListenerService.onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
