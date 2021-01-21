package com.example.vmac.WatBot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;

import java.io.UnsupportedEncodingException;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter mAdapter;
    private ArrayList messageArrayList;
    private static EditText inputMessage;
    private ImageButton btnSend;
    private ImageButton btnRecord;
    private boolean initialRequest;
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String TAG = "MainActivity";
    private static final int RECORD_REQUEST_CODE = 101;
    private boolean userAskQuestion = false;
    JsonParser jsonParser = new JsonParser();
    JsonObject jsonObject = new JsonObject();
    RequestQueue ExampleRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ExampleRequestQueue = Volley.newRequestQueue(this);

        inputMessage = findViewById(R.id.message);
        btnSend = findViewById(R.id.btn_send);
        btnRecord = findViewById(R.id.btn_record);
        String customFont = "Montserrat-Regular.ttf";
        Typeface typeface = Typeface.createFromAsset(getAssets(), customFont);
        inputMessage.setTypeface(typeface);
        recyclerView = findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();
        mAdapter = new ChatAdapter(messageArrayList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        this.inputMessage.setText("");
        this.initialRequest = true;
        getCall();

        createNotification();
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInternetConnection()) {

                    if (userAskQuestion) {
                        final String inputmessage = MainActivity.inputMessage.getText().toString().trim();
                        chatBootAskQuestion(inputmessage, "1");
                        jsonObject.addProperty("question",inputmessage);
                        Log.i("GGG", jsonObject.toString());
                        postCall(jsonObject.toString());

                    }else{
                        sendMessage();
                    }
                }
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordMessage();
            }
        });
    }

    public void createNotification () {
        Intent myIntent = new Intent(getApplicationContext() , NotifyService. class ) ;
        AlarmManager alarmManager = (AlarmManager) getSystemService( ALARM_SERVICE ) ;
        PendingIntent pendingIntent = PendingIntent. getService ( this, 0 , myIntent , 0 ) ;
        Calendar calendar = Calendar. getInstance () ;
        calendar.set(Calendar. SECOND , 0 ) ;
        calendar.set(Calendar. MINUTE , 8 ) ;
        calendar.set(Calendar. HOUR , 12 ) ;
        calendar.set(Calendar. AM_PM , Calendar. PM ) ;
        calendar.add(Calendar. DAY_OF_MONTH , 10 ) ;
        alarmManager.setRepeating(AlarmManager. RTC_WAKEUP , calendar.getTimeInMillis() , 1000 * 60 * 60 * 24 , pendingIntent) ;
    }

    // Speech to Text Record Audio permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case RECORD_REQUEST_CODE: {

                if (grantResults.length == 0
                        || grantResults[0] !=
                        PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                }
                return;
            }
            case MicrophoneHelper.REQUEST_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
        // if (!permissionToRecordAccepted ) finish();

    }


    // Sending a message to Watson Conversation Service
    private void sendMessage() {
        final String inputmessage = this.inputMessage.getText().toString().trim();
        Message inputMessage = new Message();
        inputMessage.setMessage(inputmessage);
        inputMessage.setId("1");
        messageArrayList.add(inputMessage);
        getCall();
        mAdapter.notifyDataSetChanged();
    }

    private void getCall() {

        String url = "http://10.0.2.2:5000/profile";
        StringRequest ExampleStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                jsonObject = (JsonObject) jsonParser.parse(response);
                if (!userAskQuestion) {
                    chatBootAskQuestion(checkIfLastMessageFromProfile(response, jsonObject), "2");
                } else {

                }

            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
            }
        });

        ExampleRequestQueue.add(ExampleStringRequest);
    }

    private void postCall(final String body) {


        String url = "http://10.0.2.2:5000/chat/question/Ciprian";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("REs", response);
                JsonObject jsonObject = (JsonObject) jsonParser.parse(response);
                String jsonAnswer = String.valueOf(jsonObject.get("answer"));
                Log.i("jsonAnsw", jsonAnswer);
                chatBootAskQuestion(jsonAnswer, "2");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VOLLEY", error.toString());
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    Log.i("body", String.valueOf(body.getBytes("utf-8")));
                    // chatBootAskQuestion(String.valueOf(body.getBytes("utf-8")),"2");
                    return body == null ? null : body.getBytes("utf-8");

                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", body, "utf-8");
                    return null;
                }
            }


        };
        ExampleRequestQueue.add(stringRequest);

    }

    private void chatBootAskQuestion(String s, String s2) {
        Message outMessage = new Message();
        outMessage.setMessage(s);
        outMessage.setId(s2);
        messageArrayList.add(outMessage);
        mAdapter.notifyDataSetChanged();
    }

    //Record a message via Watson Speech to Text
    private void recordMessage() {
        getCall();
    }

    /**
     * Check Internet Connection
     *
     * @return
     */
    private boolean checkInternetConnection() {
        // get Connectivity Manager object to check connection
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // Check for network connections
        if (isConnected) {
            return true;
        } else {
            Toast.makeText(this, " No Internet Connection available ", Toast.LENGTH_LONG).show();
            return false;
        }

    }

    private String checkIfLastMessageFromProfile(String serverResponse, JsonObject jsonObject) {

        if (String.valueOf(jsonObject.get("question")).length() < 5) {
            userAskQuestion = true;
            return "Ask me something :D";
        } else
            return String.valueOf(jsonObject.get("question"));
    }
}
