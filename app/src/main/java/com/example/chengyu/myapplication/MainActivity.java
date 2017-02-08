package com.example.chengyu.myapplication;

import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.String;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

import com.daimajia.swipe.SwipeLayout;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.timroes.android.listview.EnhancedListView;

import static com.facebook.FacebookSdk.getApplicationContext;

public class MainActivity extends AppCompatActivity {
    public ArrayAdapter<String> adapter;
    public String data;
    public List<String> suggestItem;
    public AutoCompleteTextView autoComplete;
    public static final String PREFS_NAME = "hello";
    Timer timer =null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.coins_icon);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());
        loadFavorList();
        Switch myswitch=(Switch)findViewById(R.id.switch1);
        if(myswitch!=null) {
            myswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                loadFavorList();
                            }
                        },0, 10000);
                    } else {
                        if (timer != null) {
                            timer.cancel();
                        }
                    }
                }
            });
        }
        //autocomplete
        suggestItem =new ArrayList<String>();
        autoComplete=(AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        autoComplete.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable editable) {

            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newText = s.toString();
                new getJson().execute(newText);
            }
        });
        autoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String string = suggestItem.get(position);
                Scanner in=new Scanner(string);
                autoComplete.setText(in.nextLine());
            }
        });
    }

    private void loadFavorList() {
        new loadTask().execute("");
    }

    private void acquireData(List<Map<String, Object>> data, String Symbol) {
        String link="http://stockinfo-1268.appspot.com/index.php?formalsymbol="+Symbol+"&format=json";
        HttpURLConnection urlConnection = null;
        StringBuilder result=new StringBuilder();
        stockDetailInfo stock;
        double d=0;
        DecimalFormat df = new DecimalFormat("#0.00");
        Log.i("Error", "1");
        try{
            URL url= new URL(link);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-type", "json");
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.connect();
            Log.i("Error", "2");
            if(urlConnection.getResponseCode() == 200)
            {
                Log.i("Error", "3");
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while((line = reader.readLine())!=null){
                    result.append(line);
                }
                Log.i("Error", "4");
                Gson gson = new Gson();
                stock = gson.fromJson(result.toString(), stockDetailInfo.class);
                Log.i("Error", "5");
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("favor_symbol",stock.Symbol);
                map.put("favor_name",stock.Name);
                String stock_price=stock.LastPrice;
                d=Double.parseDouble(stock_price);
                stock_price="$"+df.format(d);
                map.put("favor_price",stock_price);
                map.put("favor_cap","Market Cap: ");
                String stock_changeP=stock.ChangePercent;
                d=Double.parseDouble(stock_changeP);
                stock_changeP=df.format(d)+"%";
                map.put("favor_changP",stock_changeP);
                String stock_market = stock.MarketCap;
                d = Double.parseDouble(stock_market);
                stock_market = df.format(d);
                double tmp = Double.parseDouble(stock_market);
                String marketCap = "";
                if (tmp < 50000) {
                    marketCap += df.format(tmp);
                } else if (tmp < 50000000 && tmp >= 50000) {

                    marketCap += df.format(tmp / 1000000.0);
                    marketCap += " Million";
                } else {
                    marketCap += df.format(tmp / 1000000000.0);
                    marketCap += " Billion";
                }
                stock_market = marketCap;
                map.put("favor_market",stock_market);
                data.add(map);
                Log.i("Error", "6");
            }
        }catch(Exception e){
            Log.i("Error", "sb");
            Log.w("Error", e.getMessage());
        }
        finally {
            urlConnection.disconnect();
        }
    }

    public void refreshHandler(View view) {
        loadFavorList();
    }

    class loadTask extends AsyncTask<String,String,String>{

        HttpURLConnection urlConnection = null;
        StringBuilder result=null;
        SharedPreferences settings = null;

        Set<String> tmp= null;
        Set<String> favorList= null;
        stockDetailInfo stock;
        double d=0;
        DecimalFormat df = new DecimalFormat("#0.00");
        final List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        protected void onPreExecute (){
            result=new StringBuilder();
            settings = getSharedPreferences(PREFS_NAME, 0);
            tmp= settings.getStringSet("favoriteList",new HashSet<String>());
            favorList= new HashSet<>(tmp);
            if(favorList.isEmpty())
            {
                return ;
            }
        }

        @Override
        protected void onPostExecute(String result)  {
            if(favorList.isEmpty())
            {
                ListView list=(ListView) findViewById(R.id.listView_favorite);
                list.setAdapter(null);
                return ;
            }

            ListView list=(ListView) findViewById(R.id.listView_favorite);
            SimpleAdapter mSchedule = new SimpleAdapter(MainActivity.this, data, R.layout.layout_favorite,
                    new String[]{"favor_symbol","favor_name", "favor_price", "favor_cap","favor_changP","favor_market"},
                    new int[]{R.id.favor_symbol,R.id.favor_name, R.id.favor_price, R.id.favor_cap,R.id.favor_changP,R.id.favor_market}){
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = convertView;
                    for(int i=0;i<data.size();i++){
                        if(v == null){
                            LayoutInflater vi = (LayoutInflater)MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            v=vi.inflate(R.layout.layout_favorite, null);
                        }


                        v.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this,v) {

                            public void onSwipeLeft(View view) {
                                final View v=view;
//                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
//                                alertDialogBuilder.setTitle("Important Message");
//                                alertDialogBuilder.setMessage("Do you want to delete this stock?");
//                                alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        TextView view_symbol=(TextView)v.findViewById(R.id.favor_symbol);
//                                        String tmp_symbol=view_symbol.getText().toString();
//                                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
//                                        SharedPreferences.Editor editor = settings.edit();
//                                        Set<String> tmp =settings.getStringSet("favoriteList",new HashSet<String>());
//                                        Set<String> favorSet=new HashSet<>(tmp);
//                                        if(favorSet.contains(tmp_symbol))
//                                        {
//                                            favorSet.remove(tmp_symbol);
//                                            editor.putStringSet("favoriteList",favorSet);
//                                        }
//                                        editor.commit();
//                                        dialog.cancel();
//                                        loadFavorList();
//                                    }
//                                });
//                                alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        // if this button was clicked, close dialog
//                                        dialog.cancel();
//                                    }
//                                });
//                                AlertDialog alertDialog = alertDialogBuilder.create();
//                                alertDialog.show();
                                v.startAnimation(outToLeftAnimation(500));
                                //after 500 miliseconds remove from List tvhe item and update the adapter.
                                new java.util.Timer().schedule(
                                        new java.util.TimerTask() {
                                            @Override
                                            public void run() {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        // Your dialog code.
                                                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                                                        alertDialogBuilder.setTitle("Important Message");
                                                        alertDialogBuilder.setMessage("Do you want to delete this stock?");
                                                        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                TextView view_symbol=(TextView)v.findViewById(R.id.favor_symbol);
                                                                String tmp_symbol=view_symbol.getText().toString();
                                                                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                                                SharedPreferences.Editor editor = settings.edit();
                                                                Set<String> tmp =settings.getStringSet("favoriteList",new HashSet<String>());
                                                                Set<String> favorSet=new HashSet<>(tmp);
                                                                if(favorSet.contains(tmp_symbol))
                                                                {
                                                                    favorSet.remove(tmp_symbol);
                                                                    editor.putStringSet("favoriteList",favorSet);
                                                                }
                                                                editor.commit();
                                                                dialog.cancel();
                                                                loadFavorList();
                                                            }
                                                        });
                                                        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                // if this button was clicked, close dialog
                                                                dialog.cancel();
                                                            }
                                                        });
                                                        AlertDialog alertDialog = alertDialogBuilder.create();
                                                        alertDialog.show();
                                                    }
                                                });
                                            }
                                        },
                                        500
                                );

                            }

                            public void onClick(View view) {
                                searchHandler(view);
                            }
                        });

                        TextView favor_symbol = (TextView) v.findViewById(R.id.favor_symbol);
                        TextView favor_name = (TextView) v.findViewById(R.id.favor_name);
                        TextView favor_price = (TextView) v.findViewById(R.id.favor_price);
                        TextView favor_cap = (TextView) v.findViewById(R.id.favor_cap);
                        TextView favor_changP = (TextView) v.findViewById(R.id.favor_changP);
                        TextView favor_market = (TextView) v.findViewById(R.id.favor_market);

                        favor_symbol.setText((CharSequence) data.get(position).get("favor_symbol"));
                        favor_name.setText((CharSequence)data.get(position).get("favor_name"));
                        favor_price.setText((CharSequence)data.get(position).get("favor_price"));
                        favor_cap.setText((CharSequence)data.get(position).get("favor_cap"));
                        favor_market.setText((CharSequence)data.get(position).get("favor_market"));
                        favor_changP.setText((CharSequence) data.get(position).get("favor_changP"));
                        String tmp=data.get(position).get("favor_changP").toString();
                        tmp=tmp.substring(0,tmp.length()-1);
                        double d=Double.parseDouble(tmp);
                        if(d>0){
                            favor_changP.setBackgroundColor(Color.GREEN);
                        }else if(d==0){
                            favor_changP.setBackgroundColor(Color.WHITE);
                        }else{
                            favor_changP.setBackgroundColor(Color.RED);
                        }
                    }
                    return v;
                }
            };
            list.setAdapter(mSchedule);

        }
        ////////////////////////////////////////////////////////////////////////////////////
        private Animation outToLeftAnimation(int duration) {
            Animation outtoLeft = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f);
            outtoLeft.setDuration(duration);
            outtoLeft.setInterpolator(new AccelerateInterpolator());
            return outtoLeft;
        }
        ////////////////////////////////////////////////////////////////////////////////////
        @Override
        protected String doInBackground(String... string) {
            if(favorList.isEmpty())
            {
                return null;
            }
            Iterator<String> iter= favorList.iterator();
            while(iter.hasNext())
            {
                String stock_symbol=iter.next();
                acquireData(data,stock_symbol);
            }
            return null;
        }
    }

    public void quoteHandler(View view) {
        if(autoComplete.getText()!=null)
        {
            String input=autoComplete.getText().toString();
            if(input.length()==0)
            {
                String err="Input is empty!";
                displayErr(err);
            }
            else
            {
                new getDetail().execute(input);
            }
        }
        else
        {
            String err="Input is empty!";
            displayErr(err);
        }
    }

    public void quoteHandler(String input) {

        new getDetail().execute(input);
    }

    private void displayErr(String err) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle("Error Message");
        alertDialogBuilder.setMessage(err);
        alertDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // if this button is clicked, close dialog
               dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void clearHandler(View view) {
        if(adapter!=null&&!adapter.isEmpty())
        {
            adapter.clear();
        }
        if(suggestItem!=null&&!suggestItem.isEmpty())
        {
            suggestItem.clear();
        }
        autoComplete.setText(null);
    }

    public void searchHandler(View view) {
        TextView v=(TextView)view.findViewById(R.id.favor_symbol);
        String stock_symbol=v.getText().toString();
        Log.i("DEBUG_FAVOR",stock_symbol);
        quoteHandler(stock_symbol);
    }

    class getJson extends AsyncTask<String,String,String> {
        HttpURLConnection urlConnection = null;
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            adapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.dropdown, suggestItem);
            autoComplete.setThreshold(3);
            autoComplete.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
        @Override
        protected String doInBackground(String... key) {
            String newText = key[0];
            newText = newText.trim();
            String link="http://stockinfo-1268.appspot.com/index.php?symbol="+newText+"&limit=12&namespace=0&format=json";
            try{
                URL url= new URL(link);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-type", "json");
                urlConnection.setUseCaches(false);
                urlConnection.setAllowUserInteraction(false);
                urlConnection.connect();
                if(urlConnection.getResponseCode() == 200)
                {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    StringBuilder result = new StringBuilder();
                    while((line = reader.readLine())!=null){
                        result.append(line);
                    }
                    Gson gson = new Gson();
                    List<stockBasicInfo> ac = gson.fromJson(result.toString(), new TypeToken<List<stockBasicInfo>>(){}.getType());
                    suggestItem = new ArrayList<String>();
                    for(int i=0;i<ac.size();i++)
                    {
                        suggestItem.add(ac.get(i).Symbol+'\n'+ac.get(i).Name+"("+ac.get(i).Exchange+")");
                    }
                }
            }catch(Exception e){
                Log.w("Error", e.getMessage());
            }
            finally {
                urlConnection.disconnect();
            }
            return null;
        }

    }

    class getDetail extends AsyncTask<String,String,String>{
        HttpURLConnection urlConnection = null;
        stockDetailInfo stock;
        String err="";
        StringBuilder result = new StringBuilder();
        @Override
        protected String doInBackground(String... key){
            String input=key[0].trim();
            String link="http://stockinfo-1268.appspot.com/index.php?formalsymbol="+input+"&format=json";

            try{
                URL url= new URL(link);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-type", "json");
                urlConnection.setUseCaches(false);
                urlConnection.setAllowUserInteraction(false);
                urlConnection.connect();
                if(urlConnection.getResponseCode() == 200)
                {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while((line = reader.readLine())!=null){
                        result.append(line);
                    }
                    Gson gson = new Gson();
                    stock = gson.fromJson(result.toString(), stockDetailInfo.class);
                    if(stock.Message.equals(""))
                    {
                        String status = stock.Status;
                        if (status.equals("Failure|APP_SPECIFIC_ERROR"))
                        {
                            Log.i("state", "failure");
                            err = "Sorry, the statistics for stock "+stock.Symbol+" are not available.";
                        }
                        else
                        {
                            Log.i("state", "happy" + stock.Symbol);
                        }
                    }
                    else
                    {
                        err = "Please input a valid stock name or symbol.";
                    }
                }
            }catch(Exception e){
                Log.w("Error", e.getMessage());
            }
            finally {
                urlConnection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String msg) {
            if(!err.equals(""))
            {
                displayErr(err);
            }
            else
            {
                Log.i("hahahha","hahahha");
                Bundle bundle=new Bundle();
                //should check the favorite cache to see whether this stock is in favorite list
                //If yes, transmit result+"1";else, transmit result+"0"
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                Set<String> tmpSet =settings.getStringSet("favoriteList",new HashSet<String>());
                Set<String> favorSet=new HashSet<>(tmpSet);
                if(favorSet.contains(stock.Symbol))
                {
                    result.append("1");
                }
                else
                {
                    result.append("0");
                }
                String tmp=result.toString();
                bundle.putString("result",tmp);
                Intent intent = new Intent();
                intent.putExtras(bundle);
                intent.setClassName(MainActivity.this,"com.example.chengyu.myapplication.ResultActivity");
                startActivity(intent);
            }
        }
    }
}


class stockBasicInfo{
    //"Symbol":"A","Name":"Agilent Technologies Inc","Exchange"
    public String Symbol;
    public String Name;
    public String Exchange;
    public stockBasicInfo(){}
}
class stockDetailInfo{
    //{"Status":"Failure|APP_SPECIFIC_ERROR","Name":"DJTSM USConGds","Symbol":"DWCCGS","LastPrice":0,"Change"
    //:0,"ChangePercent":0,"Timestamp":null,"MSDate":0,"MarketCap":0,"Volume":0,"ChangeYTD":0,"ChangePercentYTD"
    //        :0,"High":0,"Low":0,"Open":0}
    public String Status;
    public String Name;
    public String Symbol;
    public String LastPrice;
    public String Change;
    public String ChangePercent;
    public String Timestamp;
    public String MSDate;
    public String MarketCap;
    public String Volume;
    public String ChangeYTD;
    public String ChangePercentYTD;
    public String High;
    public String Low;
    public String Open;
    public String Message="";
    public boolean favor=false;
    public stockDetailInfo(){}
}