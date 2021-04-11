package com.example.twity;

import android.app.ActivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ImageView sentMessageButton;
    private EditText messageContainer;
    private String navBarImage, navBarText;
    private TextView dispatched_user_name;
    private CircleImageView dispatched_user_Image;
    private TextView isActive;
    private String receiverId, senderId;
    private DatabaseReference rootRef, messageRoot;
    private List<Messages> messagesList;
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;
    private String navBarNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        sentMessageButton = findViewById(R.id.sent_message_button);
        messageContainer = findViewById(R.id.message_box_text);
        userMessagesList = findViewById(R.id.message_container);

        rootRef = FirebaseDatabase.getInstance().getReference();
        messageRoot = FirebaseDatabase.getInstance().getReference();
        senderId = FirebaseAuth.getInstance().getUid();
        navBarText = getIntent().getExtras().get("dispatched_user_name").toString();
        navBarImage = getIntent().getExtras().get("dispatched_user_image").toString();
        navBarNumber = getIntent().getExtras().get("dispatched_user_number").toString();
        receiverId = getIntent().getExtras().get("dispatched_user_id").toString();
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);


        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater layoutInflater = getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.custom_nav_bar, null);
        dispatched_user_Image = view.findViewById(R.id.custom_nav_bar_image);
        dispatched_user_name = view.findViewById(R.id.custom_nav_bar_text);
        dispatched_user_name.setText(navBarText);

        isActive = view.findViewById(R.id.custom_nav_bar_lastSeen);
        getLastSeenStatus();
        isActive.setMovementMethod(new ScrollingMovementMethod());
        isActive.setFocusable(true);
        isActive.setSelected(true);

        Picasso.get().load(navBarImage).placeholder(R.drawable.ic_userprofile).into(dispatched_user_Image);
        actionBar.setCustomView(view);

        messagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messagesList);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        linearLayoutManager.setStackFromEnd(true);
        userMessagesList.setAdapter(messageAdapter);
        requestLoadMessages();

        sentMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
                messageContainer.setText("");
            }
        });

    }

    private void getLastSeenStatus() {

        final String lastSeen[] = new String[1];
        rootRef.child("User").child(receiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("re", receiverId);
                if (snapshot.hasChild("lastSeenStatus")) {
                    lastSeen[0] = snapshot.child("lastSeenStatus").getValue().toString();
                }
                isActive.setText(lastSeen[0]);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void sendMessage() {


        String message = messageContainer.getText().toString().trim();
        if (!TextUtils.isEmpty(message)) {
            String messageReceiverRef = "Message/" + receiverId + "/" + senderId;
            String messageSenderRef = "Message/" + senderId + "/" + receiverId;

            DatabaseReference userMessageRef = rootRef
                    .child(messageSenderRef).child(messageReceiverRef).push();
            String messagePushId = userMessageRef.getKey();

            Map<String, String> messageTextBody = new TreeMap<>();
            messageTextBody.put("message", message);
            messageTextBody.put("from", senderId);

            HashMap<String, Object> messageBodyDetail = new HashMap<>();
            messageBodyDetail.put(messageSenderRef + "/" + messagePushId, messageTextBody);
            messageBodyDetail.put(messageReceiverRef + "/" + messagePushId, messageTextBody);

            rootRef.updateChildren(messageBodyDetail).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });

            messageRoot.child("Message").child(senderId).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    HashMap<String, Object> newOnSenderChat = new HashMap<>();
                    newOnSenderChat.put(navBarNumber, receiverId);

                    final String[] senderNumber = new String[1];

                    rootRef.child("User").child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {

                            senderNumber[0] = snapshot.child("userNumber").getValue().toString();

                            if (senderNumber[0].length() > 0) {

                                HashMap<String, Object> newOnReceiverChat = new HashMap<>();
                                newOnReceiverChat.put(senderNumber[0], senderId);

                                rootRef.child("User").child(senderId).child("chatList").updateChildren(newOnSenderChat).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                    }
                                });

                                rootRef.child("User").child(receiverId).child("chatList").updateChildren(newOnReceiverChat).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void requestLoadMessages() {
        rootRef.child("Message").child(senderId).child(receiverId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Messages messages = snapshot.getValue(Messages.class);
                messagesList.add(messages);
                messageAdapter.notifyDataSetChanged();
                userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onStart() {
        HashMap<String, Object> lastSeenStatus = new HashMap<>();
        lastSeenStatus.put("lastSeenStatus", "Online");
        rootRef.child("User").child(senderId).updateChildren(lastSeenStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

            }
        });

        super.onStart();
    }

    @Override
    protected void onStop() {
        ActivityManager.RunningAppProcessInfo myProcess = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(myProcess);
        boolean isInBackground = myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        if (isInBackground) {
            HashMap<String, Object> lastSeenStatus = new HashMap<>();
            String lastSeen = new SimpleDateFormat("yy/MM/dd HH:MM a").format(new Date());
            lastSeenStatus.put("lastSeenStatus", lastSeen);
            rootRef.child("User").child(senderId).updateChildren(lastSeenStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
        }
        super.onStop();
    }

}