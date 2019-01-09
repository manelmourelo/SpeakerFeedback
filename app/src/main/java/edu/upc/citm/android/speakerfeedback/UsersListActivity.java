package edu.upc.citm.android.speakerfeedback;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersListActivity extends AppCompatActivity {

    private RecyclerView users_list_view;
    private Adapter adapter;
    public List<User> users;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        users = new ArrayList<>();

        adapter = new Adapter(this, users);

        users_list_view = findViewById(R.id.users_list_view);
        users_list_view.setLayoutManager(new LinearLayoutManager(this));
        users_list_view.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        );
        users_list_view.setAdapter(adapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.go_back, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.go_back:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView user_name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            user_name = itemView.findViewById(R.id.user_name_view2);
        }
        public void bind(User item) {
            user_name.setText(item.getName());
        }
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {
        Context context;
        List<User> users;

        public Adapter(Context context, List<User> users) {
            this.context = context;
            this.users = users;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(context).inflate(R.layout.user_view, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
            holder.bind(users.get(pos));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        db.collection("users").whereEqualTo("room", "testroom").
                addSnapshotListener(this,usersListener);
    }

    private EventListener<QuerySnapshot> usersListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error on recieve users inside a room", e);
                return;
            }

            users.clear();
            for (DocumentSnapshot doc : documentSnapshots)
            {
                User new_user = new User(doc.getString("name"));
                users.add(new_user);
            }
            adapter.notifyDataSetChanged();
        }
    };

}
