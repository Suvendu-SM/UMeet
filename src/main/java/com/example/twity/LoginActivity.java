package com.example.twity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;


public class LoginActivity extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    private EditText mNumber;
    private EditText mPassword;
    private Button mLogin;
    private FirebaseAuth firebaseAuth;
    private TextView mSignup;
    private FrameLayout frameLayout;
    private SharedPreferences sharedPreferences;
    static String USER_NUMBER;

    public LoginActivity() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login_activity, container, false);
        mNumber = view.findViewById(R.id.user_number);
//        mPassword = view.findViewById(R.id.user_Password);
        mLogin = view.findViewById(R.id.Login);
        mSignup = view.findViewById(R.id.signup);
        firebaseAuth = FirebaseAuth.getInstance();
        frameLayout = getActivity().findViewById(R.id.register_FrameLayout);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkInput();
            }
        });

    }

    private void checkInput() {
        String userNumber = mNumber.getText().toString();
        if (!TextUtils.isEmpty(userNumber)) {
            if (userNumber.length() == 10) {
                USER_NUMBER = userNumber;
                setFragment(new OtpActivity(userNumber));
            } else {
                mNumber.setError("Invalid number");
            }
        } else {
            mNumber.setError("Field is empty");
        }

    }

    public void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(frameLayout.getId(), fragment);
        fragmentTransaction.commit();
    }
}