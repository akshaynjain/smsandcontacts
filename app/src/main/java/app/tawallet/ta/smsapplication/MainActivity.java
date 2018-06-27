package app.tawallet.ta.smsapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.lifeofcoding.cacheutlislibrary.CacheUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by akshaya on 6/08/2018.
 */

public class MainActivity extends AppCompatActivity implements ItemFragment.OnListFragmentInteractionListener,
                                                               ContactsFragment.OnListFragmentInteractionListener,
                                                               View.OnClickListener {

    private static final int REQUEST_PERMISSION_KEY = 1;
    private ProgressBar mProgressBar;
    public FloatingActionButton mFab;
    private ArrayList<HashMap<String, String>> smsList = new ArrayList<>();
    private ArrayList<HashMap<String, String>> tmpList = new ArrayList<>();
    private LoadSms mLoadSmsTask;
    private FragmentTransaction mFragmentTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CacheUtils.configureCache(this);
        setUpToolBar();
        findViews();
        mFragmentTransaction = getSupportFragmentManager().beginTransaction();
        mFab.setOnClickListener(this);
    }

    public void setUpToolBar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void findViews(){
        mProgressBar = findViewById(R.id.loader);
        mFab = findViewById(R.id.fab);
    }

    @Override
    public void onListFragmentInteraction(HashMap < String, String > sms) {
        canceltask();
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("name",sms.get(Function.KEY_NAME));
        intent.putExtra("address", sms.get(Function.KEY_PHONE));
        intent.putExtra("thread_id", sms.get(Function.KEY_THREAD_ID));
        startActivity(intent);
    }

    @Override
    public void onListFragmentInteraction(Contact contact) {
        canceltask();
        if (!contact.getNumbers().isEmpty()) {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("name", contact.getName());
            intent.putExtra("address", contact.getNumbers().get(0).getNumber());
            intent.putExtra("thread_id", 0);
            startActivity(intent);
        }else {
            Toast.makeText(MainActivity.this,"No Number Found",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                    viewContacts();
                break;
            default:

                break;
        }
    }

    public void viewContacts(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.container, ContactsFragment.newInstance(1));
        transaction.addToBackStack(null);
        transaction.commit();
    }

    class LoadSms extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
            smsList.clear();
        }

        protected String doInBackground(String... args) {
            String xml = "";
            getContactsCursor();
            Collections.sort(smsList, new MapComparator(Function.KEY_TIMESTAMP, "dsc")); // Arranging sms by timestamp decending
            ArrayList<HashMap<String, String>> purified = Function.removeDuplicates(smsList); // Removing duplicates from inbox & sent
            smsList.clear();
            smsList.addAll(purified);
            // Updating cache data
            try {
                Function.createCachedFile(MainActivity.this, "smsapp", smsList);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return xml;
        }

        @Override
        protected void onPostExecute(String xml) {
            mProgressBar.setVisibility(View.GONE);
            if(!tmpList.equals(smsList))
            {
                ItemFragment fragment=ItemFragment.newInstance(1,smsList);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.container, fragment,"sms");
                transaction.commit();
            }
        }
    }

    public void getContactsCursor(){
        try {
            Uri uriInbox = Uri.parse("content://sms/inbox");
            Cursor inbox = getContentResolver().query(uriInbox, null, "address IS NOT NULL) GROUP BY (thread_id", null, null); // 2nd null = "address IS NOT NULL) GROUP BY (address"
            Uri uriSent = Uri.parse("content://sms/sent");
            Cursor sent = getContentResolver().query(uriSent, null, "address IS NOT NULL) GROUP BY (thread_id", null, null); // 2nd null = "address IS NOT NULL) GROUP BY (address"
            Cursor c = new MergeCursor(new Cursor[]{inbox,sent}); // Attaching inbox and sent sms
            if (c.moveToFirst()) {
                for (int i = 0; i < c.getCount(); i++) {
                    String name;
                    String _id = c.getString(c.getColumnIndexOrThrow("_id"));
                    String thread_id = c.getString(c.getColumnIndexOrThrow("thread_id"));
                    String msg = c.getString(c.getColumnIndexOrThrow("body"));
                    String type = c.getString(c.getColumnIndexOrThrow("type"));
                    String timestamp = c.getString(c.getColumnIndexOrThrow("date"));
                    String phone = c.getString(c.getColumnIndexOrThrow("address"));
                    name = CacheUtils.readFile(thread_id);
                    if(name == null)
                    {
                        name = Function.getContactbyPhoneNumber(getApplicationContext(), c.getString(c.getColumnIndexOrThrow("address")));
                        CacheUtils.writeFile(thread_id, name);
                    }
                    smsList.add(Function.mappingInbox(_id, thread_id, name, phone, msg, type, timestamp, Function.converToTime(timestamp)));
                    c.moveToNext();
                }
            }
            c.close();
        }catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_PERMISSION_KEY: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    init();
                    mLoadSmsTask = new LoadSms();
                    mLoadSmsTask.execute();
                } else
                {
                    Toast.makeText(MainActivity.this, "You must accept permissions.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void init(){
        try {
            if(getFragmentManager().getBackStackEntryCount()>=1) {
                FragmentManager fm = getSupportFragmentManager();
                for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                    fm.popBackStack();
                }
            }
            tmpList = (ArrayList<HashMap<String, String>>) Function.readCachedFile(MainActivity.this, "smsapp");
            ItemFragment fragment = ItemFragment.newInstance(1, tmpList);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment,"sms").addToBackStack(null).commit();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String[] PERMISSIONS = {Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.WRITE_CONTACTS};
        if(!Function.hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
        }else{
            init();
            mLoadSmsTask = new LoadSms();
            mLoadSmsTask.execute();
        }
    }

    @Override
    public void onBackPressed() {
        canceltask();
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        canceltask();
        super.onDestroy();
    }

    public void canceltask(){
        if (!mLoadSmsTask.isCancelled()){
            mLoadSmsTask.cancel(true);
        }
    }

}
