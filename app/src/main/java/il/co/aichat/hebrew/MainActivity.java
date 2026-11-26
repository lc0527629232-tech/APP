package il.co.aichat.hebrew;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.content.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, messages; EditText input; ScrollView scroll; TextView status;
    android.content.SharedPreferences prefs; JSONArray history=new JSONArray();
    final int ink=Color.rgb(30,38,58), muted=Color.rgb(112,124,145), purple=Color.rgb(99,82,220);
    public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(246,247,252));getWindow().setNavigationBarColor(Color.rgb(246,247,252));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);prefs=getSharedPreferences("chat",0);build();load();}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    void build(){
      root=new LinearLayout(this);root.setOrientation(1);root.setPadding(18,12,18,12);root.setBackgroundColor(Color.rgb(246,247,252));setContentView(root);
      LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(4,8,4,14);
      TextView logo=text("✦",25,Color.WHITE,true);logo.setGravity(Gravity.CENTER);logo.setBackground(shape(purple,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(48,48);top.addView(logo,lp);
      LinearLayout brand=new LinearLayout(this);brand.setOrientation(1);brand.setPadding(12,0,0,0);brand.addView(text("צ׳אט AI",21,ink,true));brand.addView(text("העוזר החכם שלך",12,muted,false));top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
      TextView settings=text("⚙",24,ink,true);settings.setGravity(Gravity.CENTER);settings.setOnClickListener(v->settings());top.addView(settings,new LinearLayout.LayoutParams(44,48));
      TextView fresh=text("＋ שיחה חדשה",13,purple,true);fresh.setGravity(Gravity.CENTER);fresh.setPadding(12,10,12,10);fresh.setBackground(shape(Color.WHITE,22));fresh.setOnClickListener(v->{history=new JSONArray();messages.removeAllViews();welcome();save();});top.addView(fresh);
      root.addView(top);
      status=text("●  מוכן לשיחה",12,Color.rgb(36,151,111),false);status.setPadding(4,0,0,10);root.addView(status);
      scroll=new ScrollView(this);scroll.setFillViewport(true);messages=new LinearLayout(this);messages.setOrientation(1);messages.setPadding(0,8,0,12);scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
      LinearLayout composer=new LinearLayout(this);composer.setGravity(Gravity.CENTER_VERTICAL);composer.setPadding(8,8,8,8);composer.setBackground(shape(Color.WHITE,25));
      input=new EditText(this);input.setTextSize(15);input.setTextColor(ink);input.setHintTextColor(muted);input.setHint("כתבו הודעה...");input.setSingleLine(false);input.setMinLines(1);input.setMaxLines(4);input.setPadding(10,8,10,8);input.setBackgroundColor(Color.TRANSPARENT);composer.addView(input,new LinearLayout.LayoutParams(0,-2,1));
      TextView send=text("➤",21,Color.WHITE,true);send.setGravity(Gravity.CENTER);send.setBackground(shape(purple,22));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(46,46);sp.leftMargin=6;composer.addView(send,sp);send.setOnClickListener(v->send());
      root.addView(composer);TextView foot=text("התשובות נוצרות באמצעות שירות AI חיצוני",10,muted,false);foot.setGravity(Gravity.CENTER);foot.setPadding(0,7,0,0);root.addView(foot);
      if(history.length()==0)welcome();
    }
    void welcome(){TextView title=text("שלום! 👋",25,ink,true);title.setGravity(Gravity.RIGHT);title.setPadding(4,22,4,4);messages.addView(title);TextView sub=text("אני כאן כדי לעזור לך לחשוב, ללמוד, ליצור ולפתור בעיות. במה נתחיל?",15,muted,false);sub.setGravity(Gravity.RIGHT);sub.setPadding(4,4,4,20);messages.addView(sub);String[] tips={"הסבר לי נושא מורכב בפשטות","עזור לי לכתוב משהו","תן לי רעיונות לפרויקט"};for(String s:tips){TextView t=text("✧  "+s,14,purple,true);t.setGravity(Gravity.RIGHT);t.setPadding(14,14,14,14);t.setBackground(shape(Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=9;messages.addView(t,p);t.setOnClickListener(v->{input.setText(s);send();});}}
    void bubble(String s,boolean user){TextView t=text(s,15,user?Color.WHITE:ink,false);t.setTextIsSelectable(true);t.setGravity(Gravity.RIGHT);t.setPadding(15,12,15,12);t.setBackground(shape(user?purple:Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.leftMargin=user?42:0;p.rightMargin=user?0:42;p.bottomMargin=10;messages.addView(t,p);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
    void send(){String q=input.getText().toString().trim();if(q.length()==0)return;String key=prefs.getString("key","");if(key.length()==0){settings();Toast.makeText(this,"יש להגדיר מפתח API תחילה",Toast.LENGTH_LONG).show();return;}input.setText("");bubble(q,true);try{JSONObject m=new JSONObject();m.put("role","user");m.put("content",q);history.put(m);save();}catch(Exception e){}status.setText("●  חושב...");new Thread(()->ask(key,q)).start();}
    void ask(String key,String q){HttpURLConnection c=null;try{String endpoint=prefs.getString("endpoint","https://api.openai.com/v1/chat/completions");String model=prefs.getString("model","gpt-4o-mini");c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(25000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+key);
      JSONObject body=new JSONObject();body.put("model",model);body.put("messages",history);body.put("temperature",0.7);OutputStream os=c.getOutputStream();os.write(body.toString().getBytes("UTF-8"));os.close();int code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=is.read(buf))>0)out.write(buf,0,n);is.close();JSONObject response=new JSONObject(new String(out.toByteArray(),"UTF-8"));if(code<200||code>=300)throw new Exception(response.optJSONObject("error")!=null?response.getJSONObject("error").optString("message","שגיאת שרת"):("HTTP "+code));String answer=response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");JSONObject a=new JSONObject();a.put("role","assistant");a.put("content",answer);history.put(a);save();runOnUiThread(()->{bubble(answer,false);status.setText("●  מוכן לשיחה");});}
      catch(Exception e){String err="לא הצלחתי לקבל תשובה: "+e.getMessage()+"\nבדקו חיבור לאינטרנט ואת הגדרות ה־API.";runOnUiThread(()->{bubble(err,false);status.setText("●  נדרשת בדיקה");});}
      finally{if(c!=null)c.disconnect();}}
    void save(){prefs.edit().putString("history",history.toString()).apply();}
    void load(){try{history=new JSONArray(prefs.getString("history","[]"));for(int i=0;i<history.length();i++){JSONObject m=history.getJSONObject(i);bubble(m.optString("content"),"user".equals(m.optString("role")));}}catch(Exception e){history=new JSONArray();}}
    void settings(){LinearLayout box=new LinearLayout(this);box.setPadding(20,8,20,4);box.setOrientation(1);EditText k=new EditText(this);k.setHint("מפתח API");k.setSingleLine(true);k.setInputType(129);k.setText(prefs.getString("key",""));box.addView(k);EditText ep=new EditText(this);ep.setHint("כתובת API");ep.setSingleLine(true);ep.setText(prefs.getString("endpoint","https://api.openai.com/v1/chat/completions"));box.addView(ep);EditText mo=new EditText(this);mo.setHint("שם המודל");mo.setSingleLine(true);mo.setText(prefs.getString("model","gpt-4o-mini"));box.addView(mo);new AlertDialog.Builder(this).setTitle("הגדרות חיבור AI").setMessage("נדרש מפתח API מספק שירות AI. המפתח נשמר במכשיר בלבד.").setView(box).setNegativeButton("ביטול",null).setPositiveButton("שמירה",(d,w)->prefs.edit().putString("key",k.getText().toString().trim()).putString("endpoint",ep.getText().toString().trim()).putString("model",mo.getText().toString().trim()).apply()).show();}
}
