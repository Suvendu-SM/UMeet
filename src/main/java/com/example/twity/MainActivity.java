package com.example.twity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference rootRef;
    private String userId;
    private static Context appContext;
    private static boolean wasInBackground;

    private ListView listView;
    private DatabaseReference refRoot;
    private ArrayAdapter<String> loadUserOnChat;
    private ArrayList<String> nameList;
    private ArrayList<String> numberList;
    private ArrayList<String> userIdList;
    private ArrayList<String> imageList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        FloatingActionButton fab = findViewById(R.id.fab);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        firebaseAuth = FirebaseAuth.getInstance();
        getSupportActionBar().setTitle("Chat");
        listView = findViewById(R.id.chat_view_listView);
        refRoot = FirebaseDatabase.getInstance().getReference();
        listView.setDividerHeight(0);

        retrieveContactNumbers();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loadContact = new Intent(MainActivity.this, Contacts.class);
                startActivity(loadContact);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent chatActivity = new Intent(MainActivity.this, ChatActivity.class);
                chatActivity.putExtra("dispatched_user_name", nameList.get(position));
                chatActivity.putExtra("dispatched_user_number", numberList.get(position));
                chatActivity.putExtra("dispatched_user_image", imageList.get(position));
                chatActivity.putExtra("dispatched_user_id", userIdList.get(position));
                startActivity(chatActivity);
            }
        });

    }


    private void retrieveContactNumbers() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 100);
        } else {
            requestLoadChatView(userId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        requestLoadChatView(userId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_section, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.user_profile) {
            Intent user_profile_intent = new Intent(MainActivity.this, ProfileSection.class);
            startActivity(user_profile_intent);
            return true;
        } else if (item.getItemId() == R.id.Log_out) {
            firebaseAuth.signOut();
            deleteSharedPreferences("login");
            Intent intent = new Intent(MainActivity.this, Registration.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onMoveToForeground() {
        // app moved to foreground
        wasInBackground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onMoveToBackground() {
        // app moved to background
        wasInBackground = false;
    }


    private void requestLoadChatView(String userId) {

        refRoot.child("User").child(userId).child("chatList").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, String> userOnChatList = new HashMap<>();
                for (DataSnapshot items : snapshot.getChildren()) {
                    userOnChatList.put(items.getKey(), items.getValue().toString());
                }
                if (userOnChatList.size() == snapshot.getChildrenCount()) {
                    readContact(userOnChatList);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void readContact(final HashMap<String, String> userOnChatList) {

        nameList = new ArrayList<>();
        numberList = new ArrayList<>();
        imageList = new ArrayList<>();
        userIdList = new ArrayList<>();
        HashSet<String> uniqueContactFile = new HashSet<>();

        String SELECTION = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        String SORTODER = ContactsContract.Contacts.DISPLAY_NAME;
        ContentResolver contentResolver = this.getContentResolver();

        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, SELECTION, null, SORTODER);

        if (cursor.moveToFirst()) {

            String contactName;
            String contactNumber;
            String user_number;

            do {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                int indexOfNormalizedNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                String normalizedNumber = cursor.getString(indexOfNormalizedNumber);
                if ((!uniqueContactFile.contains(normalizedNumber))) {
                    user_number = contactNumber.replaceAll("\\+91|[^0-9]", "");
                    if (userOnChatList.containsKey(user_number)) {
                        nameList.add(contactName);
                        numberList.add(user_number);
                        userIdList.add(userOnChatList.get(user_number));
                        uniqueContactFile.add(normalizedNumber);
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        requestRetrieveImages();

    }

    private void requestRetrieveImages() {
        for (int index = 0; index < userIdList.size(); index++) {
            refRoot.child("User").child(userIdList.get(index)).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.hasChild("image")) {
                        imageList.add(snapshot.child("image").getValue().toString());
                    } else {
                        imageList.add("null");
                    }
                    loadUserOnChat = new customAdapter(MainActivity.this, nameList, numberList, imageList);
                    listView.setAdapter(loadUserOnChat);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

    }


    private class customAdapter extends ArrayAdapter<String> {

        Activity context;
        ArrayList<String> contactName;
        ArrayList<String> contactNumber;
        ArrayList<String> imageList;

        public customAdapter(Activity context, ArrayList<String> contactName, ArrayList<String> contactNumber, ArrayList<String> imageList) {
            super(context, R.layout.contact_view, contactName);
            this.context = context;
            this.contactName = contactName;
            this.contactNumber = contactNumber;
            this.imageList = imageList;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = context.getLayoutInflater();
            View view = layoutInflater.inflate(R.layout.contact_view, null, false);

            TextView userName = view.findViewById(R.id.contact_list_user_name);
            TextView userLastMessage = view.findViewById(R.id.contact_list_user_subtitle);
            ImageView userImageView = view.findViewById(R.id.contact_list_user_image_view);

            try {
                refRoot.child("Message").child(userId).child(userIdList.get(position)).limitToLast(1).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren()) {
                            for (DataSnapshot item : snapshot.getChildren())
                                userLastMessage.setText(item.child("message").getValue().toString());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                userName.setText(contactName.get(position));
                Picasso.get().load(imageList.get(position)).placeholder(R.drawable.ic_userprofile).into(userImageView);
            } catch (Exception e) {

            }

            return view;
        }
    }

    @Override
    protected void onStart() {
        HashMap<String, Object> lastSeenStatus = new HashMap<>();
        lastSeenStatus.put("lastSeenStatus", "Online");
        rootRef.child("User").child(userId).updateChildren(lastSeenStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
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
            rootRef.child("User").child(userId).updateChildren(lastSeenStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
        }
        super.onStop();
    }


}

