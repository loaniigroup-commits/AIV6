package com.loanii.dailytaskmanager;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class AiAssistantActivity extends Activity implements TextToSpeech.OnInitListener {
    private final int NAVY = Color.rgb(3,20,40), NAVY2 = Color.rgb(8,38,70), CARD = Color.rgb(10,45,78);
    private final int GOLD = Color.rgb(224,184,78), WHITE = Color.WHITE, MUTED = Color.rgb(181,197,214);
    private final int GREEN = Color.rgb(53,180,111);
    private static final int REQ_AUDIO = 8801;
    private LinearLayout chatBox;
    private EditText input;
    private TextView status;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY); getWindow().setNavigationBarColor(NAVY);
        tts = new TextToSpeech(this,this);
        setContentView(buildUi());
        addAssistantMessage("AI Assistant ready. Ask me anything, or say: Add a task tomorrow at 10 AM to renew the license.");
    }

    private View buildUi(){
        LinearLayout root=vbox(); root.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(1,14,29),NAVY2,NAVY}); root.setBackground(bg);

        LinearLayout head=hbox();
        Button back=smallButton("‹ Back",CARD,WHITE); back.setOnClickListener(v->finish()); head.addView(back,new LinearLayout.LayoutParams(dp(82),dp(40)));
        TextView title=txt("AI VOICE ASSISTANT",20,WHITE,true); LinearLayout.LayoutParams tw=weight(); tw.setMargins(dp(10),0,0,0); head.addView(title,tw);
        root.addView(head,marginBottom(10));
        TextView sub=txt("Talk naturally • Ask questions • Create tasks by voice",11,GOLD,false); root.addView(sub,marginBottom(12));

        ScrollView sv=new ScrollView(this); chatBox=vbox(); sv.addView(chatBox);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1f); root.addView(sv,sp);

        status=txt("Online AI requires internet",10,MUTED,false); root.addView(status,marginTop(8));
        input=new EditText(this); input.setHint("Type or use the microphone…"); input.setTextColor(WHITE); input.setHintTextColor(MUTED); input.setSingleLine(false); input.setMaxLines(4);
        input.setBackground(round(CARD,12)); input.setPadding(dp(12),dp(10),dp(12),dp(10)); root.addView(input,marginTop(8));

        LinearLayout actions=hbox();
        Button mic=aiActionButton("🎙  Use Voice",GOLD,NAVY); mic.setOnClickListener(v->startVoice());
        Button send=aiActionButton("Send",GREEN,WHITE); send.setOnClickListener(v->sendCurrent());
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(42),1f); a.setMargins(0,dp(10),dp(6),dp(10)); actions.addView(mic,a);
        LinearLayout.LayoutParams c=new LinearLayout.LayoutParams(0,dp(42),1f); c.setMargins(dp(6),dp(10),0,dp(10)); actions.addView(send,c); root.addView(actions);
        return root;
    }

    private void startVoice(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO); return;
        }
        if(!SpeechRecognizer.isRecognitionAvailable(this)){toast("Voice recognition is not available on this phone");return;}
        if(recognizer!=null) recognizer.destroy();
        recognizer=SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle p){status.setText("Listening…");}
            public void onBeginningOfSpeech(){}
            public void onRmsChanged(float v){}
            public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){status.setText("Processing speech…");}
            public void onError(int e){status.setText("Voice error. Try again or type your request.");}
            public void onResults(Bundle r){
                ArrayList<String> x=r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(x!=null&&!x.isEmpty()){input.setText(x.get(0)); input.setSelection(input.length()); sendCurrent();}
            }
            public void onPartialResults(Bundle b){}
            public void onEvent(int t,Bundle b){}
        });
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault().toLanguageTag());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak to your task assistant"); recognizer.startListening(i);
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ_AUDIO&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startVoice();}

    private void sendCurrent(){
        String m=input.getText().toString().trim(); if(m.isEmpty())return;
        if(AiConfig.BACKEND_URL.contains("YOUR-WORKER")){new AlertDialog.Builder(this).setTitle("AI backend not connected").setMessage("Deploy the included Cloudflare Worker, then put its /api/chat URL in AiConfig.java and rebuild the APK.").setPositiveButton("OK",null).show();return;}
        input.setText(""); addUserMessage(m); status.setText("Thinking…");
        new Thread(()->callBackend(m)).start();
    }

    private void callBackend(String message){
        try{
            URL u=new URL(AiConfig.BACKEND_URL); HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(60000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json");
            JSONObject req=new JSONObject(); req.put("message",message); req.put("current_date",new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())); req.put("timezone",TimeZone.getDefault().getID());
            try(OutputStream os=c.getOutputStream()){os.write(req.toString().getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode(); InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream(); String body=readAll(is);
            if(code<200||code>=300) throw new IOException("Server error "+code+": "+body);
            JSONObject out=new JSONObject(body); String reply=out.optString("reply",""); String action=out.optString("action","chat");
            // Never show a fake "Done" when the backend returned an unexpected/raw payload.
            if(reply.isEmpty()){
                String raw = extractOpenAiText(out);
                reply = raw.isEmpty() ? "I received an unexpected response from the AI service. Please check the Worker endpoint." : raw;
            }
            runOnUiThread(()->{status.setText("Ready");addAssistantMessage(reply);speak(reply);});
            if("add_task".equals(action) && out.has("task")){
                JSONObject task=out.getJSONObject("task"); runOnUiThread(()->confirmTask(task));
            }
        }catch(Exception e){runOnUiThread(()->{status.setText("Connection failed");addAssistantMessage("I couldn't reach the AI service. Check your internet or backend setup.");});}
    }

    private void confirmTask(JSONObject t){
        String title=t.optString("title","Task"), desc=t.optString("description",""), date=t.optString("date",new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())), time=t.optString("time","");
        String msg=title+"\n\nDate: "+date+(time.isEmpty()?"":"\nTime: "+time)+(desc.isEmpty()?"":"\n\n"+desc);
        new AlertDialog.Builder(this).setTitle("Add this task?").setMessage(msg).setPositiveButton("Add task",(d,w)->{saveTask(title,desc,date,time);addAssistantMessage("✓ Task added to your manager.");speak("Task added.");}).setNegativeButton("Cancel",null).show();
    }

    private void saveTask(String title,String desc,String date,String time){
        try{
            SharedPreferences prefs=getSharedPreferences("daily_manager",MODE_PRIVATE); JSONArray arr=new JSONArray(prefs.getString("tasks","[]")); JSONObject o=new JSONObject(); String id=UUID.randomUUID().toString();
            o.put("id",id);o.put("title",title);o.put("description",desc);o.put("startDate",date);o.put("status","PENDING");o.put("resolvedDate","");o.put("reminderDate",time.isEmpty()?"":date);o.put("reminderTime",time);o.put("attachments",new JSONArray());arr.put(o);prefs.edit().putString("tasks",arr.toString()).apply();
            if(!time.isEmpty()) scheduleReminder(id,title,date,time); sendBroadcast(new Intent("com.loanii.dailytaskmanager.TASKS_CHANGED").setPackage(getPackageName()));
        }catch(Exception e){toast("Task could not be saved");}
    }

    private void scheduleReminder(String id,String title,String date,String time){
        try{
            SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US); Date when=f.parse(date+" "+time); if(when==null||when.getTime()<=System.currentTimeMillis())return;
            Intent i=new Intent(this,ReminderReceiver.class);i.putExtra("title",title);i.putExtra("taskId",id); PendingIntent pi=PendingIntent.getBroadcast(this,id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTime(),pi); else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTime(),pi);
        }catch(Exception ignored){}
    }

    private void addUserMessage(String s){addBubble("You",s,GOLD,NAVY);}
    private void addAssistantMessage(String s){addBubble("AI",s,CARD,WHITE);}
    private void addBubble(String who,String s,int bg,int fg){LinearLayout card=vbox();card.setPadding(dp(12),dp(9),dp(12),dp(9));card.setBackground(round(bg,13));TextView h=txt(who,10,who.equals("AI")?GOLD:NAVY,true);TextView m=txt(s,14,fg,false);card.addView(h);card.addView(m,marginTop(3));chatBox.addView(card,marginBottom(8));}

    @Override public void onInit(int statusCode){ttsReady=statusCode==TextToSpeech.SUCCESS;if(ttsReady){tts.setLanguage(Locale.getDefault());tts.setSpeechRate(1.0f);}}
    private void speak(String s){
        if(ttsReady&&s!=null&&!s.isEmpty()){
            boolean arabic=s.matches(".*[\u0600-\u06FF].*");
            tts.setLanguage(arabic ? new Locale("ar") : Locale.getDefault());
            tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"ai-reply");
        }
    }
    @Override protected void onDestroy(){super.onDestroy();if(recognizer!=null)recognizer.destroy();if(tts!=null){tts.stop();tts.shutdown();}}

    private String extractOpenAiText(JSONObject data){
        try{
            String direct=data.optString("output_text","");
            if(!direct.isEmpty()) return direct;
            JSONArray output=data.optJSONArray("output");
            if(output==null) return "";
            StringBuilder sb=new StringBuilder();
            for(int i=0;i<output.length();i++){
                JSONObject item=output.optJSONObject(i); if(item==null) continue;
                JSONArray content=item.optJSONArray("content"); if(content==null) continue;
                for(int j=0;j<content.length();j++){
                    JSONObject c=content.optJSONObject(j); if(c==null) continue;
                    String t=c.optString("text",""); if(!t.isEmpty()){
                        if(sb.length()>0) sb.append("\n"); sb.append(t);
                    }
                }
            }
            return sb.toString();
        }catch(Exception ignored){ return ""; }
    }

    private String readAll(InputStream is)throws IOException{if(is==null)return"";BufferedReader br=new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);return sb.toString();}
    private LinearLayout vbox(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout hbox(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button smallButton(String s,int bg,int fg){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(fg);b.setTextSize(11);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setMinHeight(dp(38));b.setPadding(dp(8),dp(4),dp(8),dp(4));b.setBackground(round(bg,12));return b;}
    private Button aiActionButton(String s,int bg,int fg){
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(s);
        b.setTextColor(fg);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(10),0,dp(10),0);
        GradientDrawable g=new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(13));
        g.setStroke(dp(1), Color.argb(185,255,255,255));
        b.setBackground(g);
        b.setElevation(dp(4));
        return b;
    }
    private GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1f);}
    private LinearLayout.LayoutParams marginBottom(int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(b));return p;}
    private LinearLayout.LayoutParams marginTop(int t){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(t),0,0);return p;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
