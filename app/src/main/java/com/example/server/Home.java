package com.example.server;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.example.server.Common.Common;
import com.example.server.Interface.ItemClickListener;
import com.example.server.Model.Category;
import com.example.server.ViewHolder.MenuViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.server.databinding.ActivityHomeBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.UUID;

public class Home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    TextView txtFullName;
    FirebaseDatabase database;
    DatabaseReference category;
    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseRecyclerAdapter<Category, MenuViewHolder> adapter;
    EditText edtName;
    Button btnSelect, btnUpload;
    Category newCategory;
    Uri saveUri;
    DrawerLayout drawer;
    private final  int PICK_IMAGE_RESQUEST = 71;

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarHome.toolbar);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Quản lý Menu");
        setSupportActionBar(toolbar);

        //firebase
        database = FirebaseDatabase.getInstance("https://fastfooddb-12713-default-rtdb.asia-southeast1.firebasedatabase.app");
        category = database.getReference("Category");
        storage = FirebaseStorage.getInstance("gs://fastfooddb-12713.appspot.com");
        storageReference = storage.getReference("image/");

        //view
        recycler_menu = findViewById(R.id.recycler_menu);
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_menu.setLayoutManager(layoutManager);
        loadMenu();

        binding.appBarHome.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               showDialog();
            }
        });
        drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        View headerView = navigationView.getHeaderView(0);
        txtFullName = headerView.findViewById(R.id.txtFullName);
        if (Common.currentUser != null) {
            txtFullName.setText(Common.currentUser.getName());
        } else {
            txtFullName.setText("Người dùng chưa đăng nhập");
            Toast.makeText(this, "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
        }


    }
    //Them
    private void showDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("Thêm danh mục");
        alertDialog.setMessage("Hãy điền đủ thông tin");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_menu_layout, null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
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

                if(newCategory != null){
                    category.push().setValue(newCategory);
                    Snackbar.make(drawer, "Danh mục mới" +newCategory.getName()+ "đã thêm", Snackbar.LENGTH_SHORT).show();
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
                            Toast.makeText(Home.this, "Đã tải lên", Toast.LENGTH_SHORT).show();

                            // Lấy đường dẫn download URL của ảnh đã tải lên
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // Tạo một đối tượng Category mới
                                    newCategory = new Category(edtName.getText().toString(), uri.toString());

                                    // Thêm đối tượng Category vào cơ sở dữ liệu
                                    category.push().setValue(newCategory);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mDialog.dismiss();
                            Toast.makeText(Home.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_RESQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            saveUri = data.getData();
            btnSelect.setText("Đã chọn");
        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Chọn hình ảnh"),PICK_IMAGE_RESQUEST);
    }

    private void loadMenu() {
        FirebaseRecyclerOptions<Category> options =
                new FirebaseRecyclerOptions.Builder<Category>()
                        .setQuery(category, Category.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MenuViewHolder viewHolder, int position, @NonNull Category model) {
                viewHolder.txtMenuName.setText(model.getName());
                Picasso.get().load(model.getImage()).into(viewHolder.imageView);
                Category clickItem = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                            Intent foodList = new Intent(Home.this, FoodList.class);
                            foodList.putExtra("CategoryId", adapter.getRef(position).getKey());
                            startActivity(foodList);
                    }
                });
            }
            @NonNull
            @Override
            public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item, parent, false);
                return new MenuViewHolder(view);
            }
        };
        recycler_menu.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            // Handle the "Home" action
        } else if (id == R.id.nav_logout) {
            Intent signIn= new Intent(Home.this, DangNhap.class);
            signIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(signIn);
        }
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    //Xoa,Sua
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if(item.getTitle().equals(Common.UPDATE)){
            showUpdateDialog(adapter.getRef(item.getOrder()).getKey(), adapter.getItem(item.getOrder()));
        } else if (item.getTitle().equals(Common.DELETE)) {
            deleteCategory(adapter.getRef(item.getOrder()).getKey());
        }
        return super.onContextItemSelected(item);
    }
    private void deleteCategory(String key) {
        category.child(key).removeValue();
        Toast.makeText(this, "Đã xóa danh mục này", Toast.LENGTH_SHORT).show();
    }
    private void showUpdateDialog(String key, Category item) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("Sửa danh mục");
        alertDialog.setMessage("Hãy điền đủ thông tin");

        LayoutInflater inflater = this.getLayoutInflater();
        View add_menu_layout = inflater.inflate(R.layout.add_new_menu_layout, null);

        edtName = add_menu_layout.findViewById(R.id.edtName);
        btnSelect = add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload = add_menu_layout.findViewById(R.id.btnUpload);

        edtName.setText(item.getName());

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
                category.child(key).setValue(item);
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
    private void changeImage(Category item) {
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
                            Toast.makeText(Home.this, "Đã tải lên", Toast.LENGTH_SHORT).show();

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
                            Toast.makeText(Home.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
}