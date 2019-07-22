package com.rcpl.notifier;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import de.hdodenhof.circleimageview.CircleImageView;


public class AccountSettingsActivity extends AppCompatActivity {
        private Toolbar mToolbar;
        private DatabaseReference mUserDatabase;
        private FirebaseUser mCurrentUser;

        //Android Layout

        private CircleImageView mDisplayImage;
        private TextView mName;
        private TextView mStatus;

        private Button mStatusBtn;
        private Button mImageBtn;


        private static final int GALLERY_PICK = 1;

        // Storage Firebase
        private StorageReference mImageStorage;

        private ProgressDialog mProgressDialog;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_account_settings);

            mToolbar = (Toolbar) findViewById(R.id.profile_toolbar);
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Your Profile");


            mDisplayImage = (CircleImageView) findViewById(R.id.settings_image);
            mName = (TextView) findViewById(R.id.settings_name);
            mStatus = (TextView) findViewById(R.id.settings_status);

            mStatusBtn = (Button) findViewById(R.id.settings_status_btn);
            mImageBtn = (Button) findViewById(R.id.settings_image_btn);

            mImageStorage = FirebaseStorage.getInstance().getReference();

            mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

            String current_uid = mCurrentUser.getUid();



            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
            mUserDatabase.keepSynced(true);

            mUserDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    String name = dataSnapshot.child("Name").getValue().toString();
                    final String image = dataSnapshot.child("Image").getValue().toString();
//                    Log.d("Garvit log : ",image);
                    String status = dataSnapshot.child("Status").getValue().toString();
                 //   String thumb_image = dataSnapshot.child("Thumb_Image").getValue().toString();

                    mName.setText(name);
                    mStatus.setText(status);

                    Picasso.with(com.rcpl.notifier.AccountSettingsActivity.this).load(image).into(mDisplayImage);


                    if(!image.equals("default")) {

//                        Picasso.with(AllUsersActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mDisplayImage);

                        Picasso.with(getApplicationContext())
                                .load(image)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(mDisplayImage);

                        Picasso.with(com.rcpl.notifier.AccountSettingsActivity.this).load(image).networkPolicy(NetworkPolicy.OFFLINE)
                                .placeholder(R.drawable.default_avatar).into(mDisplayImage, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError() {

                                Picasso.with(com.rcpl.notifier.AccountSettingsActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mDisplayImage);

                            }
                        });

                    }


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });




            mImageBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    Intent galleryIntent = new Intent();
                    galleryIntent.setType("image/*");
                    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);


                /*
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(SettingsActivity.this);
                        */

                }
            });


        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
//            Bundle bundle = data.getExtras();


            if(requestCode == GALLERY_PICK && resultCode == RESULT_OK) {

                if (data != null) {
                    Uri imageUri = data.getData();

                    CropImage.activity(imageUri)
                            .setAspectRatio(1, 1)
                            .setMinCropWindowSize(500, 500)
                            .start(this);

                    //Toast.makeText(SettingsActivity.this, imageUri, Toast.LENGTH_LONG).show();
                } else if (data == null) {
                    Toast.makeText(this, "No picture selected", Toast.LENGTH_SHORT).show();
                }
            }

            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                CropImage.ActivityResult result = CropImage.getActivityResult(data);

                if (resultCode == RESULT_OK) {


                    mProgressDialog = new ProgressDialog(com.rcpl.notifier.AccountSettingsActivity.this);
                    mProgressDialog.setTitle("Uploading Image...");
                    mProgressDialog.setMessage("Please wait while we upload and process the image.");
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.show();


                    Uri resultUri = result.getUri();

//                    File thumb_filePath = new File(resultUri.getPath());

                    String current_user_id = mCurrentUser.getUid();

                    StorageReference filepath = mImageStorage.child("profile_images").child(current_user_id + ".jpg");


                    filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if(task.isSuccessful()){

                                 String download_url = task.getResult().getDownloadUrl().toString();

                                            mUserDatabase.child("Image").setValue(download_url).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    if(task.isSuccessful()){

                                                        mProgressDialog.dismiss();
                                                        Toast.makeText(com.rcpl.notifier.AccountSettingsActivity.this, "Success in Uploading.", Toast.LENGTH_LONG).show();

                                                    }

                                                }
                                            });


                                        }  else {

                                Toast.makeText(com.rcpl.notifier.AccountSettingsActivity.this, "Error in uploading.", Toast.LENGTH_LONG).show();
                                mProgressDialog.dismiss();

                            }

                        }
                    });



                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                    Exception error = result.getError();

                }
            }


        }



    }
