package com.example.vmac.WatBot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final String DINNER = "dinner";
    public static final String BREAKFAST = "breakfast";
    public static final String LUNCH = "lunch";
    public static final String HOW_OLD_ARE_YOU = "How old are you";
    public static final String WHAT_HEIGHT = "What height";
    public static final String GIVE_ME_BEST = "Give me best";
    public static final String WHAT_YOU_EAT_TODAY = "What you eat today";
    public static final String ASK_ME_SOMETHING = "Ask me something";
    public static int MAX_CALORIES = 2500;

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
    List<String> listOfMenus = new ArrayList<>();
    double age, height, weight;
    List<String> preferableMenus = new ArrayList<>();
    String eatToday;
    private static DecimalFormat df2 = new DecimalFormat("#.##");

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
        //---------------------------------------------
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInternetConnection()) {
                    if (userAskQuestion) {
                        final String inputmessage = MainActivity.inputMessage.getText().toString().trim();
                        if (inputmessage.contains("body bmi")) {
                            chatBootAskQuestion(inputmessage, "1");
                            chatBootAskQuestion("Your bmi is " + bmi(height, weight) + " %", "2");
                        } else if (inputmessage.contains("body fat")) {
                            chatBootAskQuestion(inputmessage, "1");
                            chatBootAskQuestion("Your body fat is " + bf(height, weight, age) + "%", "2");
                        } else if (inputmessage.contains("hungry")) {
                            chatBootAskQuestion(inputmessage, "1");
                            String finalDestination = "";
                            for (String menu : preferableMenus) {
                                String finalMenuPreferaneResponse = "https://www.bbcgoodfood.com/recipes/collection/";
                                finalMenuPreferaneResponse = finalMenuPreferaneResponse + menu;
                                finalDestination = finalDestination + finalMenuPreferaneResponse + "\n";

                            }
                            chatBootAskQuestion("I suggest " + finalDestination, "2");
                        } else if (inputmessage.contains("calories")) {
                            chatBootAskQuestion(inputmessage, "1");
                            jsonObject.addProperty("question", eatToday);
                            Log.i("calories", eatToday);
                            Log.i("da", jsonObject.toString());
                            postCall(jsonObject.toString());
                        } else {
                            chatBootAskQuestion(inputmessage, "1");
                            jsonObject.addProperty("question", inputmessage);
                            Log.i("GGG", jsonObject.toString());
                            postCall(jsonObject.toString());
                        }
                    } else {
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
        //.............................................................

    /*      Calendar dinnerCalendar = setAlarmTime(20, 42);
        createDinnerReminder(dinnerCalendar.getTimeInMillis());
        LocalBroadcastManager.getInstance(this).registerReceiver(mSampleReceiver, new IntentFilter(DINNER));

        //..........
      Calendar breakfastCalendar = setAlarmTime(20, 56);
        createBreakFestReminder(breakfastCalendar.getTimeInMillis());
        LocalBroadcastManager.getInstance(this).registerReceiver(breakFastJobService, new IntentFilter(BREAKFAST));

        // .............
    /*    Calendar lunchCalendar = setAlarmTime(13, 44);
        createLunchReminder(lunchCalendar.getTimeInMillis());
        LocalBroadcastManager.getInstance(this).registerReceiver(lunchJobService, new IntentFilter(LUNCH));
*/
    }

    @NonNull
    private Calendar setAlarmTime(int hour, int minute) {
        LocalTime timePicker = LocalTime.of(hour, minute);
        Calendar calendar = Calendar.getInstance();
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                    timePicker.getHour(), timePicker.getMinute(), 0);
        } else {
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                    timePicker.getHour(), timePicker.getMinute(), 0);
        }
        return calendar;
    }

    private final MenuJobService mSampleReceiver = new MenuJobService() {
        @Override
        public void onReceive(Context context, Intent intent) {
            chatBootAskQuestion("Time for dinner, what do you prefer to eat?", "2");

        }
    };

    long delay = 2000; // 1 seconds after user stops typing
    long last_text_edit = 0;
    Handler handler = new Handler();

    private Runnable input_finish_checker = new Runnable() {
        public void run() {
            if (System.currentTimeMillis() > (last_text_edit + delay - 500)) {
                final String inputmessage = inputMessage.getText().toString().trim();
                Message inputMessage = new Message();
                inputMessage.setMessage(inputmessage);
                inputMessage.setId("1");
                messageArrayList.add(inputMessage);
                if (inputmessage.toLowerCase().equals("yes")) {
                    chatBootAskQuestion("Good choice!", "2");
                    Thread.currentThread().interrupt(); //this is a MUST

                } else {
                    chatBootAskQuestion("Ok, i will notice this to not ask again.", "2");
                    Thread.currentThread().interrupt(); //this is a MUST
                }
            }
        }
    };

    private final BreakFastJobService breakFastJobService = new BreakFastJobService() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            chatBootAskQuestion("Time for breakfast, what do you prefer to eat?", "2");


            chatBootAskQuestion(listOfMenus.get(0), "2");
            inputMessage.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count,
                                              int after) {
                }

                @Override
                public void onTextChanged(final CharSequence s, int start, int before,
                                          int count) {
                    //You need to remove this to run only once
                    handler.removeCallbacks(input_finish_checker);
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    //avoid triggering event when text is empty
                    if (s.length() > 0) {
                        last_text_edit = System.currentTimeMillis();
                        handler.postDelayed(input_finish_checker, delay);
                    } else {
                        // handler.removeCallbacks(input_finish_checker);
                    }
                }
            });
            mAdapter.notifyDataSetChanged();
        }
    };


    private final LunchJobService lunchJobService = new LunchJobService() {
        @Override
        public void onReceive(Context context, Intent intent) {
            chatBootAskQuestion("Time for lunch, what do you preffer to eat?", "2");

        }
    };

    private void createDinnerReminder(long time) {
        //getting the alarm manager
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //creating a new intent specifying the broadcast receiver
        Intent i = new Intent(this, MenuJobService.class);
        //creating a pending intent using the intent
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

        //setting the repeating alarm that will be fired every day
        am.setRepeating(AlarmManager.RTC, time, AlarmManager.INTERVAL_DAY, pi);
        Toast.makeText(this, "Alarm is set", Toast.LENGTH_SHORT).show();
    }

    private void createBreakFestReminder(long time) {
        //getting the alarm manager
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //creating a new intent specifying the broadcast receiver
        Intent i = new Intent(this, BreakFastJobService.class);
        //creating a pending intent using the intent
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

        //setting the repeating alarm that will be fired every day
        am.setRepeating(AlarmManager.RTC, time, AlarmManager.INTERVAL_DAY, pi);
        Toast.makeText(this, "Alarm is set", Toast.LENGTH_SHORT).show();
    }

    private void createLunchReminder(long time) {
        //getting the alarm manager
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //creating a new intent specifying the broadcast receiver
        Intent i = new Intent(this, LunchJobService.class);
        //creating a pending intent using the intent
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);

        //setting the repeating alarm that will be fired every day
        am.setRepeating(AlarmManager.RTC, time, AlarmManager.INTERVAL_DAY, pi);
        Toast.makeText(this, "Alarm is set", Toast.LENGTH_SHORT).show();
    }


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

                if (jsonAnswer.contains("calories")) {
                    Log.i("jsonAnsw", jsonAnswer);
                    jsonAnswer = jsonAnswer.replace("\"", "").trim();

                    int numCalories = Integer.parseInt(jsonAnswer.split(" ")[0]);
                    chatBootAskQuestion("You eat " + numCalories + " calories. You should eat up to " + (MAX_CALORIES - numCalories), "2");
                    MAX_CALORIES = MAX_CALORIES - numCalories;

                } else {
                    chatBootAskQuestion(jsonAnswer, "2");
                }
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
    
    public void chatBootAskQuestion(String s, String s2) {
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

        String questionString;
        if (String.valueOf(jsonObject.get("question")).length() < 5) {
            userAskQuestion = true;
            String defaultQ = "Ask me something :D";
            getInformation(defaultQ);
            return defaultQ;
        } else {

            questionString = String.valueOf(jsonObject.get("question"));
            getInformation(questionString);
            if (questionString.contains("What body weight")) {
                return "Hello " + inputMessage.getText().toString().trim() + " " + questionString;
            }
        }
        return questionString;
    }

    private void getInformation(String question) {
        if (question.contains(HOW_OLD_ARE_YOU)) {
            weight = Double.parseDouble(inputMessage.getText().toString().trim());
        }
        if (question.contains(WHAT_HEIGHT)) {
            age = Double.parseDouble(inputMessage.getText().toString().trim());
        }
        if (question.contains(GIVE_ME_BEST)
        ) {
            height = Double.parseDouble(inputMessage.getText().toString().trim());
        }
        if (question.contains(WHAT_YOU_EAT_TODAY)) {
            preferableMenus = Arrays.asList((inputMessage.getText().toString().split(", ")));
        }
        if (question.contains(ASK_ME_SOMETHING)) {
            eatToday = (inputMessage.getText().toString());
        }
    }

    private double bmi(double height, double weight) {
        return  Math.abs(Double.parseDouble(df2.format((weight / (height * height)) * 703)));

    }

    private double bf(double height, double weight, double age) {
        return Math.abs(Double.parseDouble(df2.format((1.2 * bmi(height, weight) + 0.23 * age - 16.2))));
    }
}