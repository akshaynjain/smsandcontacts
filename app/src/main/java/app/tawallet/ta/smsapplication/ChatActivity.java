package app.tawallet.ta.smsapplication;

import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ChatAdapter adapter;
    LoadSms loadsmsTask;
    String name;
    String address;
    EditText new_message;
    ImageButton send_message;
    int thread_id_main;
    private Handler handler = new Handler();
    Thread t;
    ArrayList<HashMap<String, String>> smsList = new ArrayList<HashMap<String, String>>();
    ArrayList<HashMap<String, String>> customList = new ArrayList<HashMap<String, String>>();
    ArrayList<HashMap<String, String>> tmpList = new ArrayList<HashMap<String, String>>();

    LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        recyclerView=findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(mLayoutManager);
        new_message = (EditText) findViewById(R.id.new_message);
        send_message = (ImageButton) findViewById(R.id.send_message);
        Intent intent = getIntent();
        try {
            name = intent.getStringExtra("name");
            address = intent.getStringExtra("address");
            thread_id_main = Integer.parseInt(intent.getStringExtra("thread_id"));
        }catch (Exception e){}
        getSupportActionBar().setTitle(name);
        if (thread_id_main!=0) {
            startLoadingSms();
        }
        send_message.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String text = new_message.getText().toString();
                if(text.length()>0) {
                    String tmp_msg = text;
                    new_message.setText("Sending....");
                    new_message.setEnabled(false);
                    if(Function.sendSMS(address, tmp_msg))
                    {
                        new_message.setText("");
                        new_message.setEnabled(true);
                        // Creating a custom list for newly added sms
                        customList.clear();
                        customList.addAll(smsList);
                        customList.add(Function.mappingInbox(null, null, null, null, tmp_msg, "2", null, "Sending..."));
                        adapter = new ChatAdapter(customList,ChatActivity.this);
                        recyclerView.setAdapter(adapter);
                    }else{
                        new_message.setText(tmp_msg);
                        new_message.setEnabled(true);
                    }
                }
            }
        });
    }

    public void startLoadingSms()
    {
        final Runnable r = new Runnable() {
            public void run() {
                loadsmsTask = new LoadSms();
                loadsmsTask.execute();
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(r, 0);
    }

    class LoadSms extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tmpList.clear();
        }

        protected String doInBackground(String... args) {
            String xml = "";
            try {
                Uri uriInbox = Uri.parse("content://sms/inbox");
                Cursor inbox = getContentResolver().query(uriInbox, null, "thread_id=" + thread_id_main, null, null);
                Uri uriSent = Uri.parse("content://sms/sent");
                Cursor sent = getContentResolver().query(uriSent, null, "thread_id=" + thread_id_main, null, null);
                Cursor c = new MergeCursor(new Cursor[]{inbox,sent}); // Attaching inbox and sent sms
                if (c.moveToFirst()) {
                    for (int i = 0; i < c.getCount(); i++) {
                        String phone = "";
                        String _id = c.getString(c.getColumnIndexOrThrow("_id"));
                        String thread_id = c.getString(c.getColumnIndexOrThrow("thread_id"));
                        String msg = c.getString(c.getColumnIndexOrThrow("body"));
                        String type = c.getString(c.getColumnIndexOrThrow("type"));
                        String timestamp = c.getString(c.getColumnIndexOrThrow("date"));
                        phone = c.getString(c.getColumnIndexOrThrow("address"));
                        tmpList.add(Function.mappingInbox(_id, thread_id, name, phone, msg, type, timestamp, Function.converToTime(timestamp)));
                        c.moveToNext();
                    }
                }
                c.close();
            }catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Collections.sort(tmpList, new MapComparator(Function.KEY_TIMESTAMP, "asc"));
            return xml;
        }

        @Override
        protected void onPostExecute(String xml) {
            if(!tmpList.equals(smsList))
            {
                smsList.clear();
                smsList.addAll(tmpList);
                adapter = new ChatAdapter(smsList,ChatActivity.this);
                recyclerView.setAdapter(adapter);
            }
        }
    }



}
