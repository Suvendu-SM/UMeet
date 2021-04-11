package com.example.twity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


public class OtpActivity extends Fragment {

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth firebaseAuth;
    private String mVerificationId;
    private String mNumber;
    private String mPassword;
    private String mEmail;
    private Button mVerify;
    private EditText token1;
    private EditText token2;
    private EditText token3;
    private EditText token4;
    private EditText token5;
    private EditText token6;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;

    private PhoneAuthProvider.ForceResendingToken mResendToken;

    public OtpActivity(String number) {
        this.mNumber = number;
    }

    public OtpActivity(String mEmail, String mNumber, String mPassword) {
        // Required empty public constructor

        this.mEmail = mEmail;
        this.mNumber = mNumber;
        this.mPassword = mPassword;
    }


    // TODO: Rename and change types and number of parameters


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_otp_activity, container, false);
        firebaseAuth = FirebaseAuth.getInstance();
        mVerify = view.findViewById(R.id.VerifyBtn);
        progressBar = view.findViewById(R.id.otp_ProgressBar);
        token1 = view.findViewById(R.id.token1);
        token2 = view.findViewById(R.id.token2);
        token3 = view.findViewById(R.id.token3);
        token4 = view.findViewById(R.id.token4);
        token5 = view.findViewById(R.id.token5);
        token6 = view.findViewById(R.id.token6);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sentOtp();
        EditText editText[] = {token1, token2, token3, token4, token5, token6};
        int i = 0;
        while (i < editText.length - 1) {
            final int index = i;
            editText[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1)
                        editText[index + 1].requestFocus();

                }
            });
            i++;
        }
        mVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                PhoneAuthCredential credential=PhoneAuthProvider.getCredential();
                progressBar.setVisibility(View.VISIBLE);
                String code = token1.getText().toString() + token2.getText().toString() + token3.getText().toString() + token4.getText().toString() + token5.getText().toString() + token6.getText().toString();
                if (code.length() == 6) {
                    try {
                        mVerify.setEnabled(false);
                        PhoneAuthCredential Credential = PhoneAuthProvider.getCredential(mVerificationId, code);
                        signInWithPhoneAuthCredential(Credential);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getActivity(), "Wrong OTP", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    public void sentOtp() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
//                signInWithPhoneAuthCredential(credential);
//                Log.d("onVerfication", credential.toString());
            }


            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // ...
                    Toast.makeText(getActivity(), "Wrong Otp", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                    Toast.makeText(getActivity(), "Too Many Requests", Toast.LENGTH_SHORT).show();

                }

                // Show a message and update the UI
                // ...
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                // ...
            }
        };
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber("+91" + mNumber)       // Phone number to verify
                        .setTimeout(10L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(getActivity())                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            sharedPreferences = getActivity().getSharedPreferences("login", Context.MODE_PRIVATE);
                            sharedPreferences.edit().putBoolean("logged", true).apply();
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                            String userID = FirebaseAuth.getInstance().getUid();
                            HashMap<String, Object> userContactInfo = new HashMap<>();
                            userContactInfo.put(mNumber, userID);

                            HashMap<String, Object> newUser = new HashMap<>();
                            newUser.put("userId", userID);
                            newUser.put("userNumber", mNumber);
                            newUser.put("userName", "");
                            newUser.put("userStatus", "Hey there i am using UMeet");


                            reference.child("User").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.hasChild(userID)) {

                                        reference.child("User").child(userID).updateChildren(newUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                            }
                                        });

                                        reference.child("phone").updateChildren(userContactInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Intent intent = new Intent(getContext(), MainActivity.class);
                                                startActivity(intent);

                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });


                            // Sign in success, update UI with the signed-in user's information

//                            FirebaseUser user = task.getResult().getUser();
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
//                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(getActivity(), "Invalid Credential", Toast.LENGTH_SHORT).show();
                            }
                            progressBar.setVisibility(View.INVISIBLE);
                            mVerify.setEnabled(true);
                        }
                    }
                });
    }
}
