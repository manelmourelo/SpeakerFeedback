package edu.upc.citm.android.speakerfeedback;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REGISTER_USER = 0;
    private static final int INSERT_ROOM_ID =1;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String userId;
    private List<Poll> polls = new ArrayList<>();

    private ListenerRegistration roomRegistration;
    private ListenerRegistration usersRegistration;

    private RecyclerView polls_view;
    private Adapter adapter;
    private TextView users_conected;
    private String room_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        users_conected = findViewById(R.id.users_conected);
        adapter = new Adapter();
        polls_view = findViewById(R.id.recycler_view);
        polls_view.setLayoutManager(new LinearLayoutManager(this));
        polls_view.setAdapter(adapter);

        // Busquem a les preferències de l'app l'ID de l'usuari per saber si ja s'havia registrat
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null) {
            // Hem de registrar l'usuari, demanem el nom
            Intent intent = new Intent(this, RegisterUserActivity.class);
            startActivityForResult(intent, REGISTER_USER);
            Toast.makeText(this, "Encara t'has de registrar", Toast.LENGTH_SHORT).show();
        } else {
            // Ja està registrat, mostrem el id al Log
            Log.i("SpeakerFeedback", "userId = " + userId);
        }
    }

    private void SelectRoom() {
        Intent intent = new Intent(this,RoomID.class);
        startActivityForResult(intent, INSERT_ROOM_ID);
    }

    private void startFireStoreListenerService(){
        Intent intent = new Intent(this,FireStoreListenerService.class);
        intent.putExtra("room","room_id");
        startService(intent);
    }

    private void stopFireStoreListenerService(){
        Intent intent = new Intent(this,FireStoreListenerService.class);
        stopService(intent);
    }

    private EventListener<DocumentSnapshot> roomListener = new EventListener<DocumentSnapshot>() {
        @Override
        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
            if (e != null)
            {
                Log.e("SpeakerFeedback", "error al rebre rooms", e);
                return;
            }
            if(documentSnapshot.getBoolean("open") == false) {
                stopFireStoreListenerService();
                db.collection("users").document(userId).update(
                        "room", FieldValue.delete());
                SelectRoom();
                finish();
            }
            else {
                String name = documentSnapshot.getString("name");
                setTitle(name);
            }
        }
    };

    private EventListener<QuerySnapshot> usersListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null)
            {
                Log.e("SpeakerFeedback", "error al rebre usuaris dins un room", e);
                return;
            }
            users_conected.setText(String.format("Num. Users : %d", documentSnapshots.size()));
        }
    };

    private EventListener<QuerySnapshot> pollsListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null){
                Log.e("SpeakerFeedBack", "Error al rebre la llista de 'polls'");
                return;
            }
            polls = new ArrayList<>();
            for (DocumentSnapshot doc : documentSnapshots){
                Poll poll = doc.toObject(Poll.class);
                poll.setId(doc.getId());
                polls.add(poll);
            }
            Log.i("SpeakerFeedback", String.format("He carregat %d polls.", polls.size()));
            adapter.notifyDataSetChanged();
        }
    };

    protected void onStart() {


        if(room_id != null){
               db.collection("rooms").document(room_id)
                    .addSnapshotListener(this, roomListener);

            db.collection("users").whereEqualTo("room", room_id)
                    .addSnapshotListener(this, usersListener);

            db.collection("rooms").document(room_id).collection("polls")
                    .orderBy("start", Query.Direction.DESCENDING)
                    .addSnapshotListener(this, pollsListener);
        }
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REGISTER_USER:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    registerUser(name);
                    SelectRoom();
                } else {
                    Toast.makeText(this, "Has de registrar un nom", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case INSERT_ROOM_ID:
                room_id = data.getStringExtra("room_id");
                startFireStoreListenerService();
                enterRoom();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void registerUser(String name) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        db.collection("users").add(fields).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                userId = documentReference.getId();
                SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
                prefs.edit()
                        .putString("userId", userId)
                        .commit();
                Log.i("SpeakerFeedback", "New user: userId = " + userId);
                enterRoom();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("SpeakerFeedback", "Error creant objecte", e);
                Toast.makeText(MainActivity.this,
                        "No s'ha pogut registrar l'usuari, intenta-ho més tard", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void enterRoom() {
        db.collection("users").document(userId)
                .update(
                        "room", "room_id",
                        "last_active", new Date()
                );
        startFireStoreListenerService();
    }

    private void exitRoom() {
        stopFireStoreListenerService();
        db.collection("users").document(userId).update("room", FieldValue.delete());
        SelectRoom();
    }


    public void onPollClicked(int pos){

        Poll poll = polls.get(pos);
        if(!poll.isOpen()){
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(poll.getQuestion());
        String[] options = new String[poll.getOptions().size()];

        for(int i = 0; i < poll.getOptions().size(); i++){
            options[i] = poll.getOptions().get(i);
        }

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Map<String, Object> map = new HashMap<String, Object>();
                map.put("pollid", polls.get(0).getId());
                map.put("option", which);
                db.collection("rooms").document("room_id").collection("votes").document(userId).set(map);

            }
        });

        builder.create().show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.users_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.go_to_users:
                Intent intent = new Intent(this, UsersListActivity.class);
                startActivity(intent);
                break;
            case R.id.close:
                exitRoom();
                db.collection("users").document(userId).delete();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    deleteSharedPreferences("config");
                }
                Intent intent2 = new Intent(this, RegisterUserActivity.class);
                startActivityForResult(intent2, REGISTER_USER);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class ViewHolder extends  RecyclerView.ViewHolder{
        private CardView card_view;
        private TextView label_view;
        private TextView questions_view;
        private TextView options_view;

        public ViewHolder(final View itemView) {
            super(itemView);
            card_view = itemView.findViewById(R.id.card_view);
            label_view = itemView.findViewById(R.id.label_view);
            questions_view = itemView.findViewById(R.id.questions_view);
            options_view = itemView.findViewById(R.id.options_view);
            card_view.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    int pos = getAdapterPosition();
                    onPollClicked(pos);
                }
            });
        }
    }


    class Adapter extends  RecyclerView.Adapter<ViewHolder>{
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.poll_view, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Poll poll = polls.get(position);
            if (position == 0)
            {
                holder.label_view.setVisibility(View.VISIBLE);
                if (poll.isOpen())
                {
                    holder.label_view.setText("Active");
                }
                else
                {
                    holder.label_view.setText("Previous");
                }
            }
            else
            {
                if (!poll.isOpen() && polls.get(position - 1).isOpen())
                {
                    holder.label_view.setVisibility(View.VISIBLE);
                    holder.label_view.setText("Previous");
                }
                else
                {
                    holder.label_view.setVisibility(View.GONE);
                }
            }
            holder.card_view.setCardElevation(poll.isOpen() ? 10.0f : 0.0f);
            if (!poll.isOpen()){
                holder.card_view.setBackgroundColor(0xFFF0F0F0);
            }
            holder.questions_view.setText(poll.getQuestion());
            holder.options_view.setText(poll.getOptionsAsString());
        }
        @Override
        public int getItemCount() {
            return polls.size();
        }
    }
}
