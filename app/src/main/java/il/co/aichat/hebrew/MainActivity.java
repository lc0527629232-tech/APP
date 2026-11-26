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
import javax.net.ssl.*;
import java.security.*;

public class MainActivity extends Activity {
    // Android 4.4 supports TLS 1.2 but does not enable it by default for client sockets.
    static class Tls12SocketFactory extends SSLSocketFactory {
      private final SSLSocketFactory delegate;
      Tls12SocketFactory(SSLSocketFactory factory){delegate=factory;}
      private Socket tune(Socket socket){
        if(socket instanceof SSLSocket){
          SSLSocket ssl=(SSLSocket)socket;
          try {
            Set<String> supported=new HashSet<String>(Arrays.asList(ssl.getSupportedProtocols()));
            if(supported.contains("TLSv1.2")) ssl.setEnabledProtocols(new String[]{"TLSv1.2"});
          } catch(Exception ignored){}
        }
        return socket;
      }
      public String[] getDefaultCipherSuites(){return delegate.getDefaultCipherSuites();}
      public String[] getSupportedCipherSuites(){return delegate.getSupportedCipherSuites();}
      public Socket createSocket(Socket s,String host,int port,boolean autoClose)throws IOException{return tune(delegate.createSocket(s,host,port,autoClose));}
      public Socket createSocket(String host,int port)throws IOException{return tune(delegate.createSocket(host,port));}
      public Socket createSocket(String host,int port,java.net.InetAddress local,int localPort)throws IOException{return tune(delegate.createSocket(host,port,local,localPort));}
      public Socket createSocket(java.net.InetAddress host,int port)throws IOException{return tune(delegate.createSocket(host,port));}
      public Socket createSocket(java.net.InetAddress address,int port,java.net.InetAddress local,int localPort)throws IOException{return tune(delegate.createSocket(address,port,local,localPort));}
    }
    void enableTls12(HttpsURLConnection connection){
      if(Build.VERSION.SDK_INT>=16 && Build.VERSION.SDK_INT<=19){
        try { connection.setSSLSocketFactory(new Tls12SocketFactory((SSLSocketFactory)SSLSocketFactory.getDefault())); }
        catch(Exception ignored){}
      }
    }
    LinearLayout root, messages; EditText input; ScrollView scroll; TextView status;
    android.content.SharedPreferences prefs; JSONArray history=new JSONArray();
    final int ink=Color.rgb(30,38,58), muted=Color.rgb(112,124,145), purple=Color.rgb(99,82,220);
    public void onCreate(Bundle b){
      super.onCreate(b);
      // Keep the window setup compatible with Android 4.4 (API 19).
      prefs=getSharedPreferences("chat",0);
      try { history=new JSONArray(prefs.getString("history","[]")); } catch(Exception e) { history=new JSONArray(); }
      build();
      load();
    }
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

    }
    void welcome(){TextView title=text("שלום! 👋",25,ink,true);title.setGravity(Gravity.RIGHT);title.setPadding(4,22,4,4);messages.addView(title);TextView sub=text("אני כאן כדי לעזור לך לחשוב, ללמוד, ליצור ולפתור בעיות. במה נתחיל?",15,muted,false);sub.setGravity(Gravity.RIGHT);sub.setPadding(4,4,4,20);messages.addView(sub);String[] tips={"הסבר לי נושא מורכב בפשטות","עזור לי לכתוב משהו","תן לי רעיונות לפרויקט"};for(String s:tips){TextView t=text("✧  "+s,14,purple,true);t.setGravity(Gravity.RIGHT);t.setPadding(14,14,14,14);t.setBackground(shape(Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=9;messages.addView(t,p);t.setOnClickListener(v->{input.setText(s);send();});}}
    void bubble(String s,boolean user){TextView t=text(s,15,user?Color.WHITE:ink,false);t.setTextIsSelectable(true);t.setGravity(Gravity.RIGHT);t.setPadding(15,12,15,12);t.setBackground(shape(user?purple:Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.leftMargin=user?42:0;p.rightMargin=user?0:42;p.bottomMargin=10;messages.addView(t,p);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
    void send(){String q=input.getText().toString().trim();if(q.length()==0)return;String provider=prefs.getString("provider","openai");
      String key=prefs.getString("gemini".equals(provider)?"key_gemini":"key_openai","");
      if(key.length()==0){settings();Toast.makeText(this,"יש להגדיר מפתח API לספק שנבחר",Toast.LENGTH_LONG).show();return;}input.setText("");bubble(q,true);try{JSONObject m=new JSONObject();m.put("role","user");m.put("content",q);history.put(m);save();}catch(Exception e){}status.setText("●  חושב...");new Thread(()->ask(key,q)).start();}
    void ask(String key,String q){HttpURLConnection c=null;try{
      String provider=prefs.getString("provider","openai");
      String model=prefs.getString("gemini".equals(provider)?"model_gemini":"model_openai","gemini".equals(provider)?"gemini-2.5-flash":"gpt-4o-mini");
      String endpoint=prefs.getString("gemini".equals(provider)?"endpoint_gemini":"endpoint_openai","gemini".equals(provider)?"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent":"https://api.openai.com/v1/chat/completions");
      c=(HttpURLConnection)new URL(endpoint).openConnection();if(c instanceof HttpsURLConnection)enableTls12((HttpsURLConnection)c);c.setRequestMethod("POST");c.setConnectTimeout(25000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
      JSONObject body=new JSONObject();
      if("gemini".equals(provider)){
        c.setRequestProperty("x-goog-api-key",key);
        JSONArray contents=new JSONArray();
        for(int i=0;i<history.length();i++){
          JSONObject item=history.getJSONObject(i);String role=item.optString("role");
          if(!"user".equals(role)&&!"assistant".equals(role))continue;
          JSONObject part=new JSONObject();part.put("text",item.optString("content"));
          JSONObject content=new JSONObject();content.put("role","assistant".equals(role)?"model":"user");content.put("parts",new JSONArray().put(part));contents.put(content);
        }
        body.put("contents",contents);
        JSONObject generation=new JSONObject();generation.put("temperature",0.7);body.put("generationConfig",generation);
      } else {
        c.setRequestProperty("Authorization","Bearer "+key);
        body.put("model",model);body.put("messages",history);body.put("temperature",0.7);
      }
      OutputStream os=c.getOutputStream();os.write(body.toString().getBytes("UTF-8"));os.close();int code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=is.read(buf))>0)out.write(buf,0,n);is.close();JSONObject response=new JSONObject(new String(out.toByteArray(),"UTF-8"));
      if(code<200||code>=300)throw new Exception(response.optJSONObject("error")!=null?response.getJSONObject("error").optString("message","שגיאת שרת"):("HTTP "+code));
      String answer;
      if("gemini".equals(provider)){
        JSONArray candidates=response.getJSONArray("candidates");JSONObject candidate=candidates.getJSONObject(0);JSONArray parts=candidate.getJSONObject("content").getJSONArray("parts");StringBuilder sb=new StringBuilder();for(int i=0;i<parts.length();i++)sb.append(parts.getJSONObject(i).optString("text",""));answer=sb.toString();
      } else answer=response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
      JSONObject a=new JSONObject();a.put("role","assistant");a.put("content",answer);history.put(a);save();runOnUiThread(()->{bubble(answer,false);status.setText("●  מוכן לשיחה");});
      }catch(Exception e){String err="לא הצלחתי לקבל תשובה: "+e.getMessage()+"\\nבדקו חיבור לאינטרנט ואת הגדרות ה־API.";runOnUiThread(()->{bubble(err,false);status.setText("●  נדרשת בדיקה");});}
      finally{if(c!=null)c.disconnect();}}
    void save(){prefs.edit().putString("history",history.toString()).apply();}
    void load(){try{if(history.length()==0){welcome();return;}for(int i=0;i<history.length();i++){JSONObject m=history.getJSONObject(i);bubble(m.optString("content"),"user".equals(m.optString("role")));}}catch(Exception e){history=new JSONArray();welcome();}}
    void settings(){
      // Preserve an older single key as the OpenAI key during upgrade.
      if(!prefs.contains("key_openai") && prefs.contains("key")) {
        prefs.edit().putString("key_openai",prefs.getString("key","")).apply();
      }
      final String geminiDefaultUrl="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
      final String openaiDefaultUrl="https://api.openai.com/v1/chat/completions";
      ScrollView scroller=new ScrollView(this);
      LinearLayout box=new LinearLayout(this);box.setPadding(18,8,18,8);box.setOrientation(1);scroller.addView(box);
      TextView activeTitle=text("באיזה שירות להשתמש בצ׳אט?",14,ink,true);box.addView(activeTitle);
      Spinner provider=new Spinner(this);
      String[] providers={"GPT – OpenAI","Gemini – Google"};
      provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,providers));
      provider.setSelection("gemini".equals(prefs.getString("provider","openai"))?1:0);
      box.addView(provider);
      TextView note=text("בחרו את השירות הפעיל. לכל שירות יש מפתח API והגדרות נפרדים.",12,muted,false);
      note.setPadding(0,4,0,12);box.addView(note);

      TextView gTitle=text("① Google Gemini",16,Color.rgb(66,133,244),true);gTitle.setPadding(0,10,0,4);box.addView(gTitle);
      TextView gDesc=text("מפתח API של Google AI Studio",12,muted,false);box.addView(gDesc);
      EditText gKey=new EditText(this);gKey.setSingleLine(true);gKey.setInputType(129);gKey.setHint("הדבק כאן מפתח Gemini");gKey.setText(prefs.getString("key_gemini",""));box.addView(gKey);
      EditText gModel=new EditText(this);gModel.setSingleLine(true);gModel.setHint("מודל Gemini");gModel.setText(prefs.getString("model_gemini","gemini-2.5-flash"));box.addView(gModel);
      EditText gUrl=new EditText(this);gUrl.setSingleLine(true);gUrl.setTextSize(12);gUrl.setHint("כתובת API של Gemini");gUrl.setText(prefs.getString("endpoint_gemini",geminiDefaultUrl));box.addView(gUrl);

      View separator=new View(this);separator.setBackgroundColor(Color.rgb(225,228,238));LinearLayout.LayoutParams sep=new LinearLayout.LayoutParams(-1,1);sep.topMargin=14;sep.bottomMargin=8;box.addView(separator,sep);
      TextView oTitle=text("② GPT – OpenAI",16,Color.rgb(16,163,127),true);oTitle.setPadding(0,6,0,4);box.addView(oTitle);
      TextView oDesc=text("מפתח API של OpenAI",12,muted,false);box.addView(oDesc);
      EditText oKey=new EditText(this);oKey.setSingleLine(true);oKey.setInputType(129);oKey.setHint("הדבק כאן מפתח GPT");oKey.setText(prefs.getString("key_openai",""));box.addView(oKey);
      EditText oModel=new EditText(this);oModel.setSingleLine(true);oModel.setHint("מודל GPT");oModel.setText(prefs.getString("model_openai","gpt-4o-mini"));box.addView(oModel);
      EditText oUrl=new EditText(this);oUrl.setSingleLine(true);oUrl.setTextSize(12);oUrl.setHint("כתובת API של OpenAI");oUrl.setText(prefs.getString("endpoint_openai",openaiDefaultUrl));box.addView(oUrl);
      TextView privacy=text("המפתחות נשמרים בנפרד במכשיר שלך ואינם נכתבים למאגר GitHub.",11,muted,false);privacy.setPadding(0,12,0,4);box.addView(privacy);

      new AlertDialog.Builder(this).setTitle("הגדרות שירותי AI").setView(scroller)
        .setNegativeButton("ביטול",null)
        .setPositiveButton("שמירת ההגדרות",(d,w)->{
          String active=provider.getSelectedItemPosition()==1?"gemini":"openai";
          prefs.edit()
            .putString("provider",active)
            .putString("key_gemini",gKey.getText().toString().trim())
            .putString("model_gemini",gModel.getText().toString().trim())
            .putString("endpoint_gemini",gUrl.getText().toString().trim())
            .putString("key_openai",oKey.getText().toString().trim())
            .putString("model_openai",oModel.getText().toString().trim())
            .putString("endpoint_openai",oUrl.getText().toString().trim())
            .apply();
          Toast.makeText(this,"ההגדרות נשמרו",Toast.LENGTH_SHORT).show();
        }).show();
    }
}
