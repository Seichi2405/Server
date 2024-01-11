package com.example.server;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.server.Common.Common;
import com.example.server.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DangNhap extends AppCompatActivity {

    EditText edtPhone, edtPassword;
    Button btnDangNhap;

    FirebaseDatabase db;
    DatabaseReference users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dang_nhap);

        edtPassword = findViewById(R.id.edtPassword);
        edtPhone = findViewById(R.id.edtPhone);
        btnDangNhap = findViewById(R.id.btnDangNhap);

        db = FirebaseDatabase.getInstance("https://fastfooddb-12713-default-rtdb.asia-southeast1.firebasedatabase.app");
        users = db.getReference("User");

        btnDangNhap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dangNhapUser(edtPhone.getText().toString(),edtPassword.getText().toString());
            }
        });
    }

    private void dangNhapUser(String phone, String password) {
        final ProgressDialog mDialog = new ProgressDialog(DangNhap.this);
        mDialog.setMessage("Vui lòng chờ...");
        mDialog.show();

        final String localPhone = phone;
        final String localPassword = password;
        users.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(localPhone).exists()){
                    mDialog.dismiss();
                    User user = snapshot.child(localPhone).getValue(User.class);
                    user.setPhone(localPhone);
                    if(Boolean.parseBoolean(user.getIsStaff())){
                        if(user.getPassword().equals(localPassword)){
                            Intent login = new Intent(DangNhap.this, Home.class);
                            Common.currentUser = user;
                            startActivity(login);
                            finish();
                        }else {
                            Toast.makeText(DangNhap.this, "Sai Mật Khẩu", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(DangNhap.this, "Hãy đăng nhập tài khoản Staff", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    mDialog.dismiss();
                    Toast.makeText(DangNhap.this, "Tài khoản không tồn tại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}