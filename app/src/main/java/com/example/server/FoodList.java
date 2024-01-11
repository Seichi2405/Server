package com.example.server;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.server.Common.Common;
import com.example.server.Interface.ItemClickListener;
import com.example.server.Model.Category;
import com.example.server.Model.Food;
import com.example.server.ViewHolder.FoodViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.UUID;

public class FoodList extends AppCompatActivity {

    RecyclerView recyclerView;
    RelativeLayout rootLayout;
    RecyclerView.LayoutManager layoutManager;
    FloatingActionButton fab;
    //firebase
    FirebaseDatabase database;
    DatabaseReference foodList;
    FirebaseStorage storage;
    StorageReference storageReference;
    String categoryId="";
    FirebaseRecyclerAdapter<Food, FoodViewHolder>adapter;
    EditText edtName, edtDescription, edtPrice, edtDiscount;
    Button btnSelect, btnUpload;
    Food newFood;
    Uri saveUri;
    private final  int PICK_IMAGE_RESQUEST = 71;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        //firebase
        database = FirebaseDatabase.getInstance("https://fastfooddb-12713-default-rtdb.asia-southeast1.firebasedatabase.app");
        foodList = database.getReference("Food");
        storage = FirebaseStorage.getInstance("gs://fastfooddb-12713.appspot.com");
        storageReference = storage.getReference("image/");

        recyclerView = findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);



        if (getIntent() != null && getIntent().hasExtra("CategoryId")) {
            categoryId = getIntent().getStringExtra("CategoryId");
            if (categoryId != null && !categoryId.isEmpty()) {
                loadListFood(categoryId);
            } else {
                Toast.makeText(this, "CategoryId is empty or null", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Intent does not have CategoryId", Toast.LENGTH_SHORT).show();
        }
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddFoodDialog();
            }
        });
    }
    //Hien list food
    private void loadListFood(String categoryId) {
        FirebaseRecyclerOptions<Food> options =
                new FirebaseRecyclerOptions.Builder<Food>()
                        .setQuery(foodList.orderByChild("menuId").equalTo(categoryId), Food.class)
                        .build();
        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder holder, int position, @NonNull Food model) {
                Picasso.get().load(model.getImage()).into(holder.food_image);
                holder.food_name.setText(model.getName());
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.food_item, parent, false);
                return new FoodViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }
    //them food
    private void showAddFoodDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodList.this);
        alertDialog.setTitle("Thêm Món ăn");
        alertDialog.setMessage("Hãy điền đủ thông tin");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_food_layout, null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
        edtDescription = add_menu_layout.findViewById(R.id.edtDescription);
        edtPrice = add_menu_layout.findViewById(R.id.edtPrice);
        edtDiscount = add_menu_layout.findViewById(R.id.edtDiscount);

        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        //tao su kien cho nut Chon
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.baseline_shopping_cart_24);

        alertDialog.setPositiveButton("", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                if(newFood != null){
                    foodList.push().setValue(newFood);
                    Snackbar.make(rootLayout, "Món ăn mới" +newFood.getName()+ "đã thêm", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        alertDialog.setNegativeButton("Đóng", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }
    //action chon anh
    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Chọn hình ảnh"),PICK_IMAGE_RESQUEST);
    }
    //action up anh
    private void uploadImage() {
        if (saveUri != null) {
            ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Vui lòng chờ...");
            mDialog.show();
            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("image/" + imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(FoodList.this, "Đã tải lên", Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // Tạo một đối tượng Category mới
                                    newFood = new Food();
                                    newFood.setName(edtName.getText().toString());
                                    newFood.setDescription(edtDescription.getText().toString());
                                    newFood.setPrice(edtPrice.getText().toString());
                                    newFood.setDiscount(edtDiscount.getText().toString());
                                    newFood.setMenuId(categoryId);
                                    newFood.setImage(uri.toString());
                                    foodList.push().setValue(newFood);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                            Toast.makeText(FoodList.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            double process = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                            mDialog.setMessage("Đã tải lên " + process + "%");
                        }
                    });
        }
    }
    //aciton ấn giữ
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_RESQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            saveUri = data.getData();
            btnSelect.setText("Đã chọn");
        }
    }
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if(item.getTitle().equals(Common.UPDATE)){
            showUpdateDialog(adapter.getRef(item.getOrder()).getKey(), adapter.getItem(item.getOrder()));
        } else if (item.getTitle().equals(Common.DELETE)) {
            deleteCategory(adapter.getRef(item.getOrder()).getKey());
        }
        return super.onContextItemSelected(item);
    }
    //xoa, sua
    private void deleteCategory(String key) {
        foodList.child(key).removeValue();
        Toast.makeText(this, "Đã xóa món ăn này", Toast.LENGTH_SHORT).show();
    }
    //them
    private void showUpdateDialog(String key, Food item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FoodList.this);
        alertDialog.setTitle("Sửa món ăn");
        alertDialog.setMessage("Hãy điền đủ thông tin");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_menu_layout, null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        edtName.setText(item.getName());

        // Set event listener for the "Choose Image" button
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeImage(item);
            }
        });


        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.baseline_shopping_cart_24);

        alertDialog.setPositiveButton("Xác Nhận", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                item.setName(edtName.getText().toString());
                foodList.child(key).setValue(item);
            }
        });

        alertDialog.setNegativeButton("Đóng", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialog.show();
    }

    private void changeImage(Food item) {
        if (saveUri != null) {
            final ProgressDialog mDialog = new ProgressDialog(this);
            mDialog.setMessage("Vui lòng chờ...");
            mDialog.show();

            String imageName = UUID.randomUUID().toString();
            final StorageReference imageFolder = storageReference.child("image/" + imageName);

            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(FoodList.this, "Đã tải lên", Toast.LENGTH_SHORT).show();

                            // Lấy đường dẫn download URL của ảnh đã tải lên
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // Tạo một đối tượng Category mới
                                    item.setImage(uri.toString());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                            Toast.makeText(FoodList.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            double process = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                            mDialog.setMessage("Đã tải lên " + process + "%");
                        }
                    });
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
