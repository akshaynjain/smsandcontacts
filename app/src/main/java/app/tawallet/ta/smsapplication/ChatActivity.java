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

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    RecyclerView mRecyclerView;
    ChatAdapter mChatAdapter;
    LoadSms mLoadSmsTask;
    String name;
    String address;
    EditText mEditTextNewMessage;
    ImageButton mImageButtonSendMessage;
    int thread_id_main;
    private Handler handler = new Handler();
    ArrayList<HashMap<String, String>> smsList = new ArrayList<>();
    ArrayList<HashMap<String, String>> customList = new ArrayList<>();
    ArrayList<HashMap<String, String>> tmpList = new ArrayList<>();

    LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setUpToolBar();
        findViews();
        Intent intent = getIntent();
        try {
            name = intent.getStringExtra("name");
            address = intent.getStringExtra("address");
            thread_id_main = Integer.parseInt(intent.getStringExtra("thread_id"));
        }catch (Exception e){}
        setTitle(name);
        if (thread_id_main!=0) {
            startLoadingSms();
        }
        mImageButtonSendMessage.setOnClickListener(this);
    }

    public void setTitle(String name){
        getSupportActionBar().setTitle(name);
    }

    public void setUpToolBar(){
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void findViews(){
        mRecyclerView =findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mEditTextNewMessage =  findViewById(R.id.new_message);
        mImageButtonSendMessage =  findViewById(R.id.send_message);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.send_message:
                Send_message();
                break;
            default:

                break;
        }
    }

    public void Send_message(){
        String text = mEditTextNewMessage.getText().toString();
        if(text.length()>0) {
            String tmp_msg = text;
            mEditTextNewMessage.setText("Sending....");
            mEditTextNewMessage.setEnabled(false);
            if(Function.sendSMS(address, tmp_msg))
            {
                mEditTextNewMessage.setText("");
                mEditTextNewMessage.setEnabled(true);
                // Creating a custom list for newly added sms
                customList.clear();
                customList.addAll(smsList);
                customList.add(Function.mappingInbox(null, null, null, null, tmp_msg, "2", null, "Sending..."));
                mChatAdapter = new ChatAdapter(customList,ChatActivity.this);
                mRecyclerView.setAdapter(mChatAdapter);
            }else{
                mEditTextNewMessage.setText(tmp_msg);
                mEditTextNewMessage.setEnabled(true);
            }
        }
    }


    public void startLoadingSms() {
        final Runnable r = new Runnable() {
            public void run() {
                mLoadSmsTask = new LoadSms();
                mLoadSmsTask.execute();
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
                mChatAdapter = new ChatAdapter(smsList,ChatActivity.this);
                mRecyclerView.setAdapter(mChatAdapter);
            }
        }
    }

}
