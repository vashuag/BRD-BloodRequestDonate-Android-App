package com.example.brd;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brd.adapters.UserAdapter;
import com.example.brd.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class DispalyRequestsActivity extends AppCompatActivity {

    RecyclerView list;
    ArrayList<User> requests,temp;
    UserAdapter adapter;
    EditText villageFilterRequest, Requestfilter;
    User self;
    String uid = FirebaseAuth.getInstance().getUid();
    PopupMenu popupMenu;
    ProgressDialog progressDialog;
    private long backPressedTime;
    private Toast back;

    MaterialButton requestCancelBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispaly_requests);


        initializeComponents();
        getRequests();


    }

    private void updateList(String toString) {
        temp.clear();
        int count = 0;
        for( User v : requests){
            if (v.getBloodGroup().contains(toString)){
                System.out.println(v.getBloodGroup());
                count = count + 1 ;
            }
            else if (v.getVillage().toUpperCase().contains(toString)) {
                System.out.println(v.getVillage());
                count = count + 1;
            }
            if (count >= 1) {
                temp.add(v);
            }
            count = 0;
        }
        adapter.updateList(temp);
    }


    private void initializeComponents() {
        requestCancelBtn = findViewById(R.id.btnAddRequest);
        popupMenu = new PopupMenu(this,findViewById(R.id.more));
        popupMenu.getMenuInflater().inflate(R.menu.context_menu,popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if(item.getItemId() == R.id.changePass){
                FirebaseAuth.getInstance().sendPasswordResetEmail(self.getEmail());
                Snackbar snack = Snackbar.make(findViewById(android.R.id.content),"Password Reset Link Sent On Registered Email.", Snackbar.LENGTH_LONG);
                View view1 = snack.getView();
                FrameLayout.LayoutParams params =(FrameLayout.LayoutParams)view1.getLayoutParams();
                params.gravity = Gravity.CENTER_VERTICAL;
                view1.setLayoutParams(params);
                snack.show();
            }else if(item.getItemId() == R.id.logout){
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, SplashScreen.class));
                DispalyRequestsActivity.this.finish();
            }else if(item.getItemId()==R.id.deleteAcc){
                doDeleteCurrentUser();
            }
            return true;
        });

        temp = new ArrayList<>();
        requests = new ArrayList<>();
        villageFilterRequest = findViewById(R.id.villageFilterRequest);
        Requestfilter = findViewById(R.id.Requestfilter);
        list = findViewById(R.id.requestList);
        requests = new ArrayList<>();
        adapter = new UserAdapter(this, requests, position -> {
            //call button handle
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:"+temp.get(position).getMobile()));
            startActivity(intent);
        }, position -> {
            //share button handle
            User sent = temp.get(position);
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TITLE,"Be Hero, Donate Blood.");
            sendIntent.putExtra(Intent.EXTRA_TEXT, "*Its Urgent.*\n"+"Here is the Information about blood Request:\nName: "+sent.getFName()+" "+sent.getLName()+"\nBlood Group : "+sent.getBloodGroup()+"\nAddress: "+sent.getVillage()+" "+sent.getState()+" "+sent.getTehsil()+"\nMobile Number: "+sent.getMobile());
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, "Be a Hero, Donate Blood.");
            startActivity(shareIntent);
        });

        list.setLayoutManager(new LinearLayoutManager(DispalyRequestsActivity.this));
        list.setAdapter(adapter);

        villageFilterRequest.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateList(s.toString());
            }
        });
        Requestfilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateList(s.toString());
            }
        });
    }

    public void viewDonorsList(View view) {
        startActivity(new Intent(DispalyRequestsActivity.this,DisplayDonorsActivity.class));
        this.finish();
    }


    public void popUp(View view) {
        popupMenu.show();
    }



    private void getRequests() {
        FirebaseDatabase.getInstance().getReference("Donors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requests.clear();
                temp.clear();
                for(DataSnapshot ds:snapshot.getChildren()){
                    User user = ds.getValue(User.class);
                    if(user.getStep().equals("Done")) {
                        if (user.getRequestBlood().equals("True")) {
                            requests.add(user);
                            temp.add(user);
                        }
                        if (user.getUID().equals(uid)) {
                            self = user;
                            if (self.getRequestBlood().equals("True")) {
                                requestCancelBtn.setText("Cancel Blood Request");
                            } else {
                                requestCancelBtn.setText("Request Blood");
                            }
                        }
                    }
                }
                updateList(villageFilterRequest.getText().toString());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    public void requestBlood(View view) {
        updateBloodRequest(!self.getRequestBlood().equals("True"));
    }

    private void updateBloodRequest(boolean b) {
        HashMap<String,Object> hashMap = new HashMap<>();
        if(b){
            hashMap.put("RequestBlood","True");
        }else {
            hashMap.put("RequestBlood","False");
        }
        FirebaseDatabase.getInstance().getReference("Donors").child(uid).updateChildren(hashMap);
    }

    private void doDeleteCurrentUser() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait..");
        progressDialog.setMessage("We are deleting your Account");
        progressDialog.setCancelable(false);
        progressDialog.show();
        FirebaseDatabase
                .getInstance()
                .getReference()
                .child("Donors")
                .child(uid)
                .setValue(null)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        FirebaseAuth.getInstance().getCurrentUser().delete()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            progressDialog.dismiss();
                                            Intent intent = new Intent(DispalyRequestsActivity.this, LoginScreenActivity.class);
                                            startActivity(intent);
                                        }
                                    }
                                });
                    }
                });

    }
    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            back.cancel();
            super.onBackPressed();
            return;
        } else {
            back = Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT);
            back.show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}