package com.example.twity;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileSection extends AppCompatActivity {
    private ImageView user_profile;
    private static TextView user_name;
    private static TextView user_status;
    private static TextView user_email;
    private Button user_name_edit;
    private Button user_status_edit;
    private FirebaseAuth firebaseAuth;
    private StorageReference profileStorageRef;
    private ImageView profile_selector;
    private DatabaseReference reference;
    private String userId;
    private static final int GALLERY_VALUE = 1;

    public ProfileSection() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_section);
         user_name = findViewById(R.id.user_name);
        user_status = findViewById(R.id.user_status);
        user_profile = findViewById(R.id.profile_image);
        user_status_edit = findViewById(R.id.edit_user_name);
        user_name_edit = findViewById(R.id.edit_user_status);
        profile_selector = findViewById(R.id.profile_selector);
        firebaseAuth = FirebaseAuth.getInstance();
        userId = firebaseAuth.getCurrentUser().getUid();
        profileStorageRef = FirebaseStorage.getInstance().getReference().child("profile Images");
        reference = FirebaseDatabase.getInstance().getReference();
        retrieveUserInfo();

        user_name_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserProfile(user_name);
            }
        });
        user_status_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserProfile(user_status);
            }
        });
        profile_selector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getGallery = new Intent(Intent.ACTION_GET_CONTENT);
                getGallery.setType("image/*");
                startActivityForResult(getGallery, GALLERY_VALUE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_VALUE && resultCode == RESULT_OK && data != null) {
            Uri ImageUri = data.getData();
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                final StorageReference storageRef = profileStorageRef.child(userId + ".jpeg");
                storageRef.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> downloadedUri = taskSnapshot.getStorage().getDownloadUrl();

                        downloadedUri.addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                reference.child("User").child(userId).child("image").setValue(uri.toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        retrieveUserInfo();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        }
    }

    private void retrieveUserInfo() {
        reference.child("User").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String user_name;
                String user_status;
                String user_profile_image;
                String user_Email;

                if (snapshot.exists()) {
                    if (snapshot.hasChild("userName")) {
                        user_name = snapshot.child("userName").getValue().toString();
                        ProfileSection.user_name.setText(user_name);
                    }
                    if (snapshot.hasChild("userStatus")) {
                        user_status = snapshot.child("userStatus").getValue().toString();
                        ProfileSection.user_status.setText(user_status);
                    }
                    if (snapshot.hasChild("userEmail")) {
                        user_Email = snapshot.child("userEmail").getValue().toString();
                        ProfileSection.user_email.setText(user_Email);
                    }
                    if (snapshot.hasChild("image")) {
                        user_profile_image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(user_profile_image).into(user_profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void updateUserProfile(final TextView textView) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ProfileSection.this, R.style.AlertDialog);
        alertDialog.setTitle("Enter your name");
        final EditText input = new EditText(ProfileSection.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setHint("Enter name");
        alertDialog.setView(input);
        alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String user_info = input.getText().toString().trim();
                if (!user_info.equals("")) {
                    reference.child(userId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Map<String, Object> userData = new HashMap<>();
                            if (textView.getId() == user_name.getId())
                                userData.put("userName", user_info);
                            else
                                userData.put("userStatus", user_info);
                            reference.child("User").child(userId).updateChildren(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        textView.setText(user_info);
                                    } else {
                                        Toast.makeText(ProfileSection.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                } else {
                    input.setError("field is empty");
                }
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }


    @Override
    protected void onStart() {
        HashMap<String, Object> lastSeenStatus = new HashMap<>();
        lastSeenStatus.put("lastSeenStatus", "Online");
        reference.child("User").child(userId).updateChildren(lastSeenStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
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
            reference.child("User").child(userId).updateChildren(lastSeenStatus).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                }
            });
        }
        super.onStop();
    }

}
