package edu.upc.citm.android.speakerfeedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class FireStoreListenerService extends Service {

    private boolean connected = false;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("SpeakerFeecback", "FireStoreListenerService.onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
      Log.i("SpeakerFeecback", "FireStoreListenerService.onStartCommand");
      //TODO: Crear una notificació i cridar startForeground (perquè el servei segeueix funcionant)
        if(!connected){
           String roomID = intent.getStringExtra("room");
           if(!roomID.isEmpty())
           {
               db.collection("rooms").document(roomID).collection("polls").whereEqualTo("open",true).addSnapshotListener(polls_listener);
               createForegroundNotification(roomID);
               connected = true;
           }
        }
      return START_NOT_STICKY;
    }

    private void createForegroundNotification(String room_id) {
        Intent intent =new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(String.format("Connectat a" + room_id))
                .setSmallIcon(R.drawable.ic_message)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1,notification);
        connected = true;
    }

    private void createForegroundNotificationNewQuestion(String room_id) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(String.format("Connectat a" + room_id))
                .setSmallIcon(R.drawable.ic_message)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[] { 250, 250, 250, 250, 250 })
                .setAutoCancel(true)
                .build();

        startForeground(1, notification);
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

    private EventListener<QuerySnapshot> polls_listener = new EventListener<QuerySnapshot>(){
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error al rebre polls", e);
                return;
            }

            for (DocumentSnapshot doc : documentSnapshots)
            {
                Poll poll = doc.toObject(Poll.class);
                if(poll.isOpen())
                {
                    Log.d("SpeakerFeedback", poll.getQuestion());

                    Intent intent = new Intent(FireStoreListenerService.this, MainActivity.class);
                    PendingIntent pending_intent = PendingIntent.getActivity(FireStoreListenerService.this, 0, intent, 0);

                    Notification notification = new NotificationCompat.Builder(FireStoreListenerService.this, App.CHANNEL_ID)
                            .setContentTitle("New poll: " +String.format(poll.getQuestion()))
                            .setSmallIcon(R.drawable.ic_message)
                            .setContentIntent(pending_intent)
                            .setVibrate(new long[] { 250, 250, 250, 250, 250 })
                            .setAutoCancel(true)
                            .build();

                    startForeground(1, notification);
                }
            }
        }
    };
}
