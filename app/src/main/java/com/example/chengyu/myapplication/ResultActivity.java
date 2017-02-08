package com.example.chengyu.myapplication;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.String;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class ResultActivity extends AppCompatActivity {
    private ActionBar actionBar;
    private stockDetailInfo stock;
    private boolean favor;
    private WebView webImage;
    private String image_url;
    private CallbackManager callbackManager;
    private ShareDialog shareDialog;


    public static final String PREFS_NAME = "hello";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.show();

        //receive data from MainActivity
        Bundle bundle = this.getIntent().getExtras();
        String result = bundle.getString("result");
        if (result.charAt(result.length() - 1) == '0') {
            favor = false;
        } else {
            favor = true;
        }
        result = result.substring(0, result.length() - 1);
        Gson gson = new Gson();
        stock = gson.fromJson(result, stockDetailInfo.class);
        stock.favor = favor;
        actionBar.setTitle(stock.Name);
        //build first page
        try {
            initTabHost();
            buildStockDetail(stock);
            new loadInStockActivity().execute(stock.Symbol);
            new loadNews().execute(stock.Symbol);
            new createHistoricalPage().execute(stock.Symbol);
        } catch (Exception e) {
            e.printStackTrace();
        }
////////////////////////////////////shareFB()///////////////////////////////////////////////////////
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);

        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Toast toast = Toast.makeText(getApplicationContext(),"Facebook post successfully",Toast.LENGTH_LONG);
                toast.show();
            }

            @Override
            public void onCancel() {
                Toast toast = Toast.makeText(getApplicationContext(),"Facebook post cancelled",Toast.LENGTH_LONG);
                toast.show();

            }

            @Override
            public void onError(FacebookException error) {
                Toast toast = Toast.makeText(getApplicationContext(), "Facebook post in error", Toast.LENGTH_LONG);
                toast.show();
            }
        });
/////////////////////////////////////shareFB()///////////////////////////////////////////////////////
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void initTabHost() {
        TabHost host = (TabHost) findViewById(R.id.tabHost);
        host.setup();
        LayoutInflater i = LayoutInflater.from(this);
        i.inflate(R.layout.layout_current, host.getTabContentView());
        i.inflate(R.layout.layout_historical, host.getTabContentView());
        i.inflate(R.layout.layout_news, host.getTabContentView());
        if (host != null) {
            host.addTab(host.newTabSpec("tab1").setIndicator("CURRENT").setContent(R.id.tabSpec1));
            host.addTab(host.newTabSpec("tab2").setIndicator("HISTORICAL").setContent(R.id.tabSpec2));
            host.addTab(host.newTabSpec("tab3").setIndicator("NEWS").setContent(R.id.tabSpec3));
        }
    }

    private void buildStockDetail(stockDetailInfo stock) throws IllegalAccessException, NoSuchFieldException {
        ListView list = (ListView) findViewById(R.id.stockDetail);
        ArrayList<HashMap<String, String>> mylist = new ArrayList<>();
        Field field1 = R.drawable.class.getField("down");
        String down = "" + field1.getInt(R.drawable.class);
        Field field2 = R.drawable.class.getField("up");
        String up = "" + field2.getInt(R.drawable.class);
        HashMap<String, String> map;
        //process data
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        String key = "NAME";
        String value = stock.Name;
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "SYMBOL";
        value = stock.Symbol;
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "LASTPRICE";
        value = stock.LastPrice;
        double d = Double.parseDouble(value);
        DecimalFormat df = new DecimalFormat("#0.00");
        value = df.format(d);
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "CHANGE";
        value = stock.Change;
        d = Double.parseDouble(value);
        df = new DecimalFormat("#0.00");
        value = df.format(d);
        String percent = stock.ChangePercent;
        d = Double.parseDouble(percent);
        df = new DecimalFormat("#0.00");
        percent = df.format(d);
        if (Double.parseDouble(percent) > 0) {
            value = value + "(+" + percent + "%)";
            map.put("Image", up);
        } else if (Double.parseDouble(percent) < 0) {
            value = value + "(" + percent + "%)";
            map.put("Image", down);
        } else {
            value = value + "(" + percent + "%)";
        }
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "TIMESTAMP";
        value = stock.Timestamp;
        SimpleDateFormat df1=new SimpleDateFormat("E MMM d HH:mm:ss 'UTC'Z yyyy");
        SimpleDateFormat df2=new SimpleDateFormat("d MMM yyyy HH:mm:ss a");
        df2.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        try {
            Date dd=df1.parse(value);
            value=df2.format(dd);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "MARKETCAP";
        value = stock.MarketCap;
        d = Double.parseDouble(value);
        df = new DecimalFormat("#0.00");
        value = df.format(d);
        double tmp = Double.parseDouble(value);
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
        value = marketCap;
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "VOLUME";
        value = stock.Volume;
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "CHANGE YTD";
        value = stock.ChangeYTD;
        d = Double.parseDouble(value);
        df = new DecimalFormat("#0.00");
        value = df.format(d);
        percent = stock.ChangePercentYTD;
        d = Double.parseDouble(percent);
        df = new DecimalFormat("#0.00");
        percent = df.format(d);
        if (Double.parseDouble(percent) > 0) {
            value = value + "(" + percent + "%)";
            map.put("Image", up);
        } else if (Double.parseDouble(percent) < 0) {
            value = value + "(" + percent + "%)";
            map.put("Image", down);
        } else {
            value = value + "(" + percent + "%)";
        }
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "HIGH";
        value = stock.High;
        d = Double.parseDouble(value);
        df = new DecimalFormat("#0.00");
        value = df.format(d);
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "LOW";
        value = stock.Low;
        d = Double.parseDouble(value);
        df = new DecimalFormat("#0.00");
        value = df.format(d);
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        map = new HashMap<>();
        key = "OPEN";
        value = stock.Open;
        d = Double.parseDouble(value);
        df = new DecimalFormat("#0.00");
        value = df.format(d);
        map.put("Title", key);
        map.put("Value", value);
        mylist.add(map);
///////////////////////////////////////////////////////////////////////////////////
        SimpleAdapter mSchedule = new SimpleAdapter(this, mylist, R.layout.updownlist,
                new String[]{"Title", "Value", "Image"},
                new int[]{R.id.Title, R.id.Value, R.id.Image});
        if (list != null) {
            list.setAdapter(mSchedule);
            View listHeader = View.inflate(this, R.layout.current_title, null);
            list.addHeaderView(listHeader);
            TextView curView = (TextView) findViewById(R.id.CurrTitle);
            curView.setText("Stock Details");
            View listFooter = View.inflate(this, R.layout.current_footer, null);
            list.addFooterView(listFooter);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu;
        if (stock.favor) {
            getMenuInflater().inflate(R.menu.menu_result_like, menu);
            Log.i("recalled", "recalled");
        } else {
            getMenuInflater().inflate(R.menu.menu_result_dislike, menu);
            Log.i("recalled", "recalled");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //do different operation based on ID
        switch (id) {
            case R.id.action_fb:
                shareFb();
                break;
            case R.id.action_favorite:
                saveFavorite();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
////////////////////////////////////shareFB()///////////////////////////////////////////////////////
    private void shareFb() {
        Log.i("fb", "share");
        String price=stock.LastPrice;
        double d = Double.parseDouble(price);
        DecimalFormat df = new DecimalFormat("#0.00");
        price = df.format(d);
        String change=stock.Change;
        String changePer=stock.ChangePercent;
        d = Double.parseDouble(change);
        df = new DecimalFormat("#0.00");
        change = df.format(d);
        d = Double.parseDouble(changePer);
        df = new DecimalFormat("#0.00");
        changePer = df.format(d);
        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent linkContent = new ShareLinkContent.Builder()
                    .setImageUrl(Uri.parse(image_url))
                    .setContentTitle("Current Stock Price of "+stock.Name+" is "+"$"+price)
                    .setContentDescription(
                            "Stock Information of "+stock.Name+" ("+stock.Symbol+"), "+
                                    "Last Traded Price: "+"$"+price+", Change: "+
                                    change+"("+changePer+"%)")
                    .setContentUrl(Uri.parse("http://dev.markitondemand.com/"))
                    .build();
            shareDialog.show(linkContent);
        }

    }

    ////////////////////////////////////shareFB()///////////////////////////////////////////////////////
    private void saveFavorite() {
        Log.i("favorite", "click");
        if (stock.favor) {
            stock.favor = false;
            //should remove this stock from favoriteListCache
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            Set<String> tmp =settings.getStringSet("favoriteList",new HashSet<String>());
            Set<String> favorSet=new HashSet<>(tmp);
            if(favorSet.contains(stock.Symbol))
            {
                favorSet.remove(stock.Symbol);
                editor.putStringSet("favoriteList",favorSet);
            }
            editor.commit();
            Set<String> test =settings.getStringSet("favoriteList",new HashSet<String>());
            Log.i("DEBUG_OUT",test.toString());
        } else {
            stock.favor = true;
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            Set<String> tmp =settings.getStringSet("favoriteList",new HashSet<String>());
            Set<String> favorSet=new HashSet<>(tmp);
            favorSet.add(stock.Symbol);
            editor.putStringSet("favoriteList",favorSet);
            // Commit the edits!
            editor.commit();
            //should add this stock to favoriteListCache
            Set<String> test =settings.getStringSet("favoriteList",new HashSet<String>());
            Log.i("DEBUG_OUT",test.toString());
        }
        invalidateOptionsMenu();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    class loadInStockActivity extends AsyncTask<String, String, Bitmap> {
        private String stockSymbol;
        HttpURLConnection urlConnection = null;
        private URL myFileUrl = null;
        private Bitmap bitmap = null;

        @Override
        protected Bitmap doInBackground(String... Symbols) {
            stockSymbol = Symbols[0];
            stockSymbol = stockSymbol.trim();
            String link = "http://chart.finance.yahoo.com/t?s=" + stockSymbol + "&lang=en-US&width=1080&height=600";
            image_url=link;
            try {
                URL url = new URL(link);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    InputStream in = urlConnection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(in);
                    in.close();
                }
            } catch (Exception e) {
                Log.w("Error", e.getMessage());
            } finally {
                urlConnection.disconnect();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView image = (ImageView) findViewById(R.id.stockImage);
            image.setImageBitmap(bitmap);
            final Bitmap bm=bitmap;
            image.setOnClickListener(new ImageView.OnClickListener(

            ) {
                @Override
                public void onClick(View v) {
                    // Get screen size
                    Display display = ResultActivity.this.getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int screenWidth = size.x;
                    int screenHeight = size.y;

                    // Get target image size
                    int bitmapHeight = bm.getHeight();
                    int bitmapWidth = bm.getWidth();

                    // Scale the image down to fit perfectly into the screen
                    // The value (250 in this case) must be adjusted for phone/tables displays
                    while(bitmapHeight > (screenHeight - 250) || bitmapWidth > (screenWidth - 250)) {
                        bitmapHeight = bitmapHeight / 2;
                        bitmapWidth = bitmapWidth / 2;
                    }

                    // Create resized bitmap image
                    BitmapDrawable resizedBitmap = new BitmapDrawable(ResultActivity.this.getResources(), Bitmap.createScaledBitmap(bm, bitmapWidth, bitmapHeight, false));

                    // Create dialog
                    Dialog dialog = new Dialog(ResultActivity.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.pic);

                    ImageView image = (ImageView) dialog.findViewById(R.id.imageview);
                    image.setImageBitmap( resizedBitmap.getBitmap());
                    dialog.getWindow().setBackgroundDrawable(null);
                    dialog.show();
                }
            });
        }
    }
//////////////////////////////////////////////////////////////////////////////////////////////
    class loadNews extends AsyncTask<String, String, String>{
        HttpURLConnection urlConnection = null;
        private URL myFileUrl = null;
        String stockSymbol;
        String news;

        @Override
        protected String doInBackground(String... Symbols) {
            stockSymbol = Symbols[0];
            stockSymbol = stockSymbol.trim();
            String link = "http://stockinfo-1268.appspot.com/index.php?news="+ stockSymbol+"&format=json";
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
                    news=result.toString();
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
        protected void onPostExecute(String string) {
            JSONObject jObject = null;
            ListView list = (ListView) findViewById(R.id.newsList);
            HashMap<String, String> map;
            final ArrayList<HashMap<String, String>> mylist = new ArrayList<>();
            try {
                    jObject = new JSONObject(news);
                    JSONArray sour =jObject.getJSONObject("d").getJSONArray("results");
                    for(int i=0;i<sour.length();i++)
                    {
                        String Title_value;
                        String Title_Description;
                        String Title_Source;
                        String Title_Date;
                        String Title_URL;
                        map=new HashMap<>();
                        JSONObject mid=sour.getJSONObject(i);
                        Title_value=mid.getString("Title");
                        Title_Description=mid.getString("Description");
                        Title_Source=mid.getString("Source");
                        Title_Source="Publisher: "+Title_Source;
                        Title_Date=mid.getString("Date");
                        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss'Z'");
                        SimpleDateFormat df2 = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
                        Date d=df1.parse(Title_Date);
                        Title_Date=df2.format(d);
                        Title_Date="Date: "+Title_Date;
                        Log.i("URL",mid.getString("Url"));
                        Title_URL=mid.getString("Url");
                        map.put("URL",Title_URL);
                        map.put("Title",Title_value);
                        map.put("Description",Title_Description);
                        map.put("Publisher",Title_Source);
                        map.put("Date",Title_Date);
                        mylist.add(map);
                    }

                SimpleAdapter mSchedule = new SimpleAdapter(ResultActivity.this, mylist, R.layout.newslist,
                        new String[]{"URL","Title", "Description", "Publisher","Date"},
                        new int[]{R.id.URL,R.id.Title, R.id.Description, R.id.Publisher,R.id.Date}){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View v = convertView;
                        for(int i=0;i<mylist.size();i++){
                            if(v == null){
                                LayoutInflater vi = (LayoutInflater)ResultActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                v=vi.inflate(R.layout.newslist, null);
                            }
                            TextView URL = (TextView) v.findViewById(R.id.URL);
                            TextView Title = (TextView) v.findViewById(R.id.Title);
                            TextView description = (TextView) v.findViewById(R.id.Description);
                            TextView Publisher = (TextView) v.findViewById(R.id.Publisher);
                            TextView date = (TextView) v.findViewById(R.id.Date);
                            String tmp_url=mylist.get(position).get("URL").toString();
                            String tmp=mylist.get(position).get("Title").toString();
                            String formal="<a href='"+tmp_url+"'>"+tmp+"</a>";
                            Title.setText(Html.fromHtml(formal));
                            Title.setMovementMethod(LinkMovementMethod.getInstance());
                            description.setText((CharSequence)mylist.get(position).get("Description"));
                            Publisher.setText((CharSequence)mylist.get(position).get("Publisher"));
                            date.setText((CharSequence)mylist.get(position).get("Date"));
                        }
                        return v;
                    }
                };
                if(list!=null)
                {
                    Log.i("BUG","0");
                    list.setAdapter(mSchedule);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return;
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    class createHistoricalPage extends AsyncTask<String, String, String> {
        private String stockSymbol;

        @Override
        protected String doInBackground(String... Symbols) {
            stockSymbol = Symbols[0];
            stockSymbol = stockSymbol.trim();

            return null;
        }

        @Override
        protected void onPostExecute(String string) {
            webImage = (WebView) findViewById(R.id.webView);
            webImage.getSettings().setJavaScriptEnabled(true);
            webImage.getSettings().setSupportZoom(false);
            webImage.getSettings().setBuiltInZoomControls(false);
            webImage.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
            webImage.getSettings().setDefaultFontSize(18);
            webImage.loadUrl("file:///android_asset/historical.html");
            webImage.setWebViewClient(new MyWebViewClient());
        }
    }
    class MyWebViewClient extends WebViewClient {

            public void onPageFinished(WebView view, String url) {
                webImage.loadUrl("javascript:HistoricalCharts('" + stock.Symbol + "')");
            }
    }
}