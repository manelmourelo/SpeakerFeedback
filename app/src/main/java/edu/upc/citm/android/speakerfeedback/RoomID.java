package edu.upc.citm.android.speakerfeedback;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoomID extends AppCompatActivity {

    TextView enter_room;
    private String password_text = "";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_id);
        enter_room = findViewById(R.id.room_id);
    }
    public void onClickEnterRoom(View view)
    {
        final String room_id = enter_room.getText().toString();

        if(room_id.equals(""))
        {
            Toast.makeText(this,"Enter a room id :/", Toast.LENGTH_SHORT).show();
        }
        else
        {
            db.collection("rooms").document(room_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    Log.i("SpeakerFeedback", documentSnapshot.toString());
                    if (documentSnapshot.exists() && documentSnapshot.contains("open")) {
                        if (documentSnapshot.contains("password") && !documentSnapshot.getString("password").isEmpty()) { // Contains password
                            comparePasswordPopUp(documentSnapshot.get("password").toString());
                        } else {
                            Intent data = new Intent();
                            data.putExtra("room_id", enter_room.getText().toString());
                            setResult(RESULT_OK, data);
                            finish();
                        }
                    } else {
                        if (!documentSnapshot.exists()) {
                            Toast.makeText(RoomID.this,
                                    "Room with ID " + "'" + room_id + ":" + " NOT EXIST", Toast.LENGTH_SHORT).show();
                        } else if (!documentSnapshot.contains("open")) {
                            Toast.makeText(RoomID.this,
                                    "Room with ID " + "'" + room_id + "'" + " NOT OPEN", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(RoomID.this,
                            e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("SpeakerFeedback", e.getMessage());
                }
            });
        }
    }
    protected  void comparePasswordPopUp(final String password)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter password:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                password_text = input.getText().toString();
                if (password_text.equals(password))
                {
                    Toast.makeText(RoomID.this, "Password correct", Toast.LENGTH_SHORT).show();

                    Intent data = new Intent();
                    data.putExtra("room_id", enter_room.getText().toString());
                    setResult(RESULT_OK, data);
                    finish();
                }
                else
                    Toast.makeText(RoomID.this, "Password incorrect!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
