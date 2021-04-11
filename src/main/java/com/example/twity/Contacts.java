package com.example.twity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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


public class Contacts extends AppCompatActivity {

    private ArrayList<String> contactsName;
    private ArrayList<String> contactsNumber;
    private ArrayList<String> contactsImages;
    private ArrayList<String> contactsStatus;
    private ArrayAdapter<String> loadContacts;
    private DatabaseReference reference;
    private FirebaseAuth firebaseAuth;
    private ListView listView;
    private String userId;
    private ArrayList<Boolean> isAdapterViewStateChanged;
    private ProgressBar contentLoadingProgressBar;
    private boolean isAvailable;
    private ArrayList<String> storeUserId;

    public Contacts() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_contacts);
        contentLoadingProgressBar = findViewById(R.id.contact_progressbar);
        firebaseAuth = FirebaseAuth.getInstance();
        userId = firebaseAuth.getCurrentUser().getUid();
        getSupportActionBar().setTitle("Contacts");
        reference = FirebaseDatabase.getInstance().getReference();
        isAdapterViewStateChanged = new ArrayList<>();
        listView = findViewById(R.id.contact_list);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        isAvailable = true;

        retrieveContactNumbers();


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent chatActivity = new Intent(Contacts.this, ChatActivity.class);
                chatActivity.putExtra("dispatched_user_name", contactsName.get(position));
                chatActivity.putExtra("dispatched_user_image", contactsImages.get(position));
                chatActivity.putExtra("dispatched_user_number", contactsNumber.get(position));
                chatActivity.putExtra("dispatched_user_id", storeUserId.get(position));
                startActivity(chatActivity);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contact, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.refresh_contacts) {
            if (isAvailable)
                retrieveContactNumbers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void retrieveContactNumbers() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 100);
        } else {
            readContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        readContacts();
    }

    private void readContacts() {
        isAvailable = false;
        contentLoadingProgressBar.setVisibility(View.VISIBLE);
        reference = FirebaseDatabase.getInstance().getReference("phone");
        HashSet<String> uniqueContactFile = new HashSet<>();
        final HashMap<String, Object> userContact = new HashMap<>();
        final ArrayList<String> nameList = new ArrayList<>();
        final ArrayList<String> numberList = new ArrayList<>();
        String user_number;
        String SELECTION = ContactsContract.Contacts.HAS_PHONE_NUMBER;
        String SORTODER = ContactsContract.Contacts.DISPLAY_NAME;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, SELECTION, null, SORTODER);
        if (cursor.moveToFirst()) {
            String contactName;
            String contactNumber;
            do {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                int indexOfNormalizedNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
                String normalizedNumber = cursor.getString(indexOfNormalizedNumber);
                if ((!uniqueContactFile.contains(normalizedNumber))) {
                    user_number = contactNumber.replaceAll("\\+91|[^0-9]", "");
                    nameList.add(contactName);
                    numberList.add(user_number);
                    userContact.put(user_number, contactName);
                    uniqueContactFile.add(normalizedNumber);
                }
            } while (cursor.moveToNext());
            cursor.close();
            retrieveOnServiceContactNumbers(nameList, numberList);

        }
    }

    private void retrieveOnServiceContactNumbers(final ArrayList<String> nameList, final ArrayList<String> numberList) {

        final int maxSize = numberList.size() - 1;
        contactsName = new ArrayList<>();
        contactsNumber = new ArrayList<>();
        storeUserId = new ArrayList<>();
        for (int start = 0; start < numberList.size(); start++) {
            final int isEndOfList = start;
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String number = numberList.get(isEndOfList);
                    String name = nameList.get(isEndOfList);
                    if (snapshot.hasChild(number) && number.length() == 10) {
                        contactsName.add(name);
                        contactsNumber.add(number);
                        isAdapterViewStateChanged.add(false);
                        storeUserId.add(snapshot.child(number).getValue().toString());
                    }

                    if (isEndOfList==maxSize)
                        getUserStatus(storeUserId);


                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }


    }

    private void getUserStatus(ArrayList<String> storeUserId) {
        contactsImages = new ArrayList<>();
        contactsStatus = new ArrayList<>();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("User");
        final int storeSize = storeUserId.size();
        final int isEndOfFile[] = new int[1];
        for (int i = 0; i < storeSize; i++) {
            isEndOfFile[0] = i;
            rootRef.child(storeUserId.get(isEndOfFile[0])).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.hasChild("userStatus")) {
                        contactsStatus.add(snapshot.child("userStatus").getValue().toString());
                    } else {
                        contactsStatus.add("");
                    }
                    if (snapshot.hasChild("image")) {
                        contactsImages.add(snapshot.child("image").getValue().toString());
                    } else {
                        contactsImages.add("null");
                    }
                    requestUpdateContactList();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void requestUpdateContactList() {
        loadContacts = new customAdapter(this);
        listView.setAdapter(loadContacts);
        loadContacts.notifyDataSetChanged();
        contentLoadingProgressBar.setVisibility(View.INVISIBLE);
        isAvailable = true;

    }


    private class customAdapter extends ArrayAdapter<String> {
        Activity context;
        int imageListSize = contactsImages.size() - 1;

        customAdapter(Activity context) {
            super(context, R.layout.contact_view, contactsName);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = context.getLayoutInflater();
            View view = layoutInflater.inflate(R.layout.contact_view, null, true);

            TextView retrieved_user_name = view.findViewById(R.id.contact_list_user_name);
            TextView retrieved_user_status = view.findViewById(R.id.contact_list_user_subtitle);
            ImageView userProfileImage = view.findViewById(R.id.contact_list_user_image_view);
            try {
                retrieved_user_name.setText(contactsName.get(position));
                retrieved_user_status.setText(contactsStatus.get(position));
                Picasso.get().load(contactsImages.get(position)).placeholder(R.drawable.ic_userprofile).into(userProfileImage);
            } catch (Exception e) {
            }
            return view;
        }
    }


    @Override
    protected void onStart() {
        HashMap<String, Object> lastSeenStatus = new HashMap<>();
        lastSeenStatus.put("lastSeenStatus", "Online");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.child("User").child(userId).updateChildren(lastSeenStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
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
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        boolean isInBackground = myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        if (isInBackground) {
            HashMap<String, Object> lastSeenStatus = new HashMap<>();
            String lastSeen = new SimpleDateFormat("yy/MM/dd HH:MM a").format(new Date());
            lastSeenStatus.put("lastSeenStatus", lastSeen);
            ref.child("User").child(userId).updateChildren(lastSeenStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}