package app.tawallet.ta.smsapplication;

import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

import static java.util.Objects.*;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    
    private RecyclerView mRecyclerView;
    private ChatAdapter mChatAdapter;
    private LoadSms mLoadSmsTask;
    private String mName;
    private String mAddress;
    private AppCompatEditText mEditTextNewMessage;
    private AppCompatImageButton mImageButtonSendMessage;
    private int mThreadIdMain;
    private Handler mHandler = new Handler();
    private ArrayList<HashMap<String, String>> smsList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> customList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> tmpList = new ArrayList<>();
    private LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setUpToolBar();
        setTitle();
        findViews();
        if (mThreadIdMain!=0) {
            startLoadingSms();
        }
        mImageButtonSendMessage.setOnClickListener(this);
    }

    public void setTitle(){
        Intent intent = getIntent();
        try {
            mName = intent.getStringExtra("name");
            mAddress = intent.getStringExtra("address");
            mThreadIdMain = Integer.parseInt(intent.getStringExtra("thread_id"));
            Objects.requireNonNull(getSupportActionBar()).setTitle(mName);
        }catch (Exception ignored){}
    }

    public void setUpToolBar(){
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
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
                //do nothing here
                break;
        }
    }

    public void Send_message(){
        String tmp_msg = mEditTextNewMessage.getText().toString();
        if(tmp_msg.length()>0) {
            mEditTextNewMessage.setText(R.string.msg_sending);
            mEditTextNewMessage.setEnabled(false);
            if(Function.sendSMS(mAddress, tmp_msg))
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
                mHandler.postDelayed(this, 5000);
            }
        };
        mHandler.postDelayed(r, 0);
    }


    class LoadSms extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tmpList.clear();
        }

        protected String doInBackground(String... args) {
            String xml = "";
            getChats();
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

    public void getChats(){
        try {
            Uri uriInbox = Uri.parse("content://sms/inbox");
            Cursor inbox = getContentResolver().query(uriInbox, null, "thread_id=" + mThreadIdMain, null, null);
            Uri uriSent = Uri.parse("content://sms/sent");
            Cursor sent = getContentResolver().query(uriSent, null, "thread_id=" + mThreadIdMain, null, null);
            Cursor c = new MergeCursor(new Cursor[]{inbox,sent}); // Attaching inbox and sent sms
            if (c.moveToFirst()) {
                for (int i = 0; i < c.getCount(); i++) {
                    String _id = c.getString(c.getColumnIndexOrThrow("_id"));
                    String thread_id = c.getString(c.getColumnIndexOrThrow("thread_id"));
                    String msg = c.getString(c.getColumnIndexOrThrow("body"));
                    String type = c.getString(c.getColumnIndexOrThrow("type"));
                    String timestamp = c.getString(c.getColumnIndexOrThrow("date"));
                    String phone = c.getString(c.getColumnIndexOrThrow("address"));
                    tmpList.add(Function.mappingInbox(_id, thread_id, mName, phone, msg, type, timestamp, Function.converToTime(timestamp)));
                    c.moveToNext();
                }
            }
            if (sent!=null)
            sent.close();
            if (inbox!=null)
            inbox.close();
            c.close();
        }catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        Collections.sort(tmpList, new MapComparator(Function.KEY_TIMESTAMP, "asc"));
    }
}
