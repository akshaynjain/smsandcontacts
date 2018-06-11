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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.lifeofcoding.cacheutlislibrary.CacheUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by akshaya on 6/08/2018.
 */

public class MainActivity extends AppCompatActivity implements ItemFragment.OnListFragmentInteractionListener,ContactsFragment.OnListFragmentInteractionListener {


    static final int REQUEST_PERMISSION_KEY = 1;
    ProgressBar loader;
    FloatingActionButton fab;
    ArrayList<HashMap<String, String>> smsList = new ArrayList<HashMap<String, String>>();
    ArrayList<HashMap<String, String>> tmpList = new ArrayList<HashMap<String, String>>();
    FrameLayout frameLayout;
    LoadSms loadsmsTask;
    FragmentTransaction transaction;;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CacheUtils.configureCache(this);
        frameLayout=(FrameLayout)findViewById(R.id.container);
        loader = (ProgressBar) findViewById(R.id.loader);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        transaction = getSupportFragmentManager().beginTransaction();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                frameLayout.removeAllViews();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.container, ContactsFragment.newInstance(1),"contacts").addToBackStack("CalendarFragment");
                transaction.commit();
            }
        });
    }

    @Override
    public void onListFragmentInteraction(HashMap < String, String > sms) {
        if (!loadsmsTask.isCancelled()){
            loadsmsTask.cancel(true);
        }
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra("name",sms.get(Function.KEY_NAME));
        intent.putExtra("address", sms.get(Function.KEY_PHONE));
        intent.putExtra("thread_id", sms.get(Function.KEY_THREAD_ID));
        startActivity(intent);
    }

    @Override
    public void onListFragmentInteraction(Contact contact) {
        if (!loadsmsTask.isCancelled()){
            loadsmsTask.cancel(true);
        }
        if (!contact.getNumbers().isEmpty()) {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("name", contact.getName());
            intent.putExtra("address", contact.getNumbers().get(0).getNumber());
            intent.putExtra("thread_id", 0);
            startActivity(intent);
        }else {
            Toast.makeText(MainActivity.this,"No Number Found",Toast.LENGTH_SHORT).show();
            return;
        }
    }

    class LoadSms extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loader.setVisibility(View.VISIBLE);
            smsList.clear();
        }

        protected String doInBackground(String... args) {
            String xml = "";
            try {
                Uri uriInbox = Uri.parse("content://sms/inbox");
                Cursor inbox = getContentResolver().query(uriInbox, null, "address IS NOT NULL) GROUP BY (thread_id", null, null); // 2nd null = "address IS NOT NULL) GROUP BY (address"
                Uri uriSent = Uri.parse("content://sms/sent");
                Cursor sent = getContentResolver().query(uriSent, null, "address IS NOT NULL) GROUP BY (thread_id", null, null); // 2nd null = "address IS NOT NULL) GROUP BY (address"
                Cursor c = new MergeCursor(new Cursor[]{inbox,sent}); // Attaching inbox and sent sms
                if (c.moveToFirst()) {
                    for (int i = 0; i < c.getCount(); i++) {
                        String name = null;
                        String phone = "";
                        String _id = c.getString(c.getColumnIndexOrThrow("_id"));
                        String thread_id = c.getString(c.getColumnIndexOrThrow("thread_id"));
                        String msg = c.getString(c.getColumnIndexOrThrow("body"));
                        String type = c.getString(c.getColumnIndexOrThrow("type"));
                        String timestamp = c.getString(c.getColumnIndexOrThrow("date"));
                        phone = c.getString(c.getColumnIndexOrThrow("address"));
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
            Collections.sort(smsList, new MapComparator(Function.KEY_TIMESTAMP, "dsc")); // Arranging sms by timestamp decending
            ArrayList<HashMap<String, String>> purified = Function.removeDuplicates(smsList); // Removing duplicates from inbox & sent
            smsList.clear();
            smsList.addAll(purified);
            // Updating cache data
            try{
                Function.createCachedFile (MainActivity.this,"smsapp", smsList);
            }catch (Exception e) {}
            // Updating cache data
            return xml;
        }

        @Override
        protected void onPostExecute(String xml) {
            loader.setVisibility(View.GONE);
            if(!tmpList.equals(smsList))
            {
                ItemFragment fragment=ItemFragment.newInstance(1,smsList);
                frameLayout.removeAllViews();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.container, fragment,"sms");
                transaction.commit();
            }else {

            }
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
                    loadsmsTask = new LoadSms();
                    loadsmsTask.execute();
                } else
                {
                    Toast.makeText(MainActivity.this, "You must accept permissions.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void init(){
        try {
                tmpList = (ArrayList<HashMap<String, String>>) Function.readCachedFile(MainActivity.this, "smsapp");
                ItemFragment fragment = ItemFragment.newInstance(1, tmpList);
                frameLayout.removeAllViews();
                // Add the fragment to the 'fragment_container' FrameLayout
                getSupportFragmentManager().beginTransaction().add(R.id.container, fragment,"sms").commit();
        }catch (Exception e){

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String[] PERMISSIONS = {Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS};
        if(!Function.hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
        }else{
            init();
            loadsmsTask = new LoadSms();
            loadsmsTask.execute();
        }
    }

    @Override
    public void onBackPressed() {
            if(getFragmentManager().getBackStackEntryCount()>=1){
                String fragmentTag=getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName();
                getSupportFragmentManager().findFragmentByTag(fragmentTag);
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
                transaction.replace(R.id.container, currentFragment,currentFragment.getTag());
                transaction.commit();
            }else {
                super.onBackPressed();
            }
    }


}
