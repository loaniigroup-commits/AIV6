package com.loanii.dailytaskmanager;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int NAVY = Color.rgb(3,20,40);
    private final int NAVY2 = Color.rgb(8,38,70);
    private final int CARD = Color.rgb(10,45,78);
    private final int GOLD = Color.rgb(224,184,78);
    private final int WHITE = Color.WHITE;
    private final int MUTED = Color.rgb(181,197,214);
    private final int GREEN = Color.rgb(53,180,111);
    private final int RED = Color.rgb(221,79,79);
    private final int PENDING = Color.rgb(236,184,68);

    private static final int REQ_FILE = 1201;
    private static final int REQ_CAMERA = 1202;
    private static final int REQ_NOTIFY = 1203;
    private static final int REQ_AI = 1204;

    private LinearLayout taskList;
    private TextView dateText, statText, percentText, rangeText;
    private Calendar selectedDate = Calendar.getInstance();
    private String mode = "DAILY";
    private final ArrayList<Task> tasks = new ArrayList<>();
    private SharedPreferences prefs;
    private Task attachmentTarget;
    private Uri currentCameraUri;

    static class Attachment {
        String uri, name, kind;
        boolean owned;
        Attachment(String uri, String name, String kind, boolean owned){this.uri=uri;this.name=name;this.kind=kind;this.owned=owned;}
    }

    static class Task {
        String id, title, description, startDate, status, resolvedDate, reminderDate, reminderTime;
        ArrayList<Attachment> attachments = new ArrayList<>();
        Task(String id, String title, String description, String startDate, String status, String resolvedDate,
             String reminderDate, String reminderTime){
            this.id=id; this.title=title; this.description=description; this.startDate=startDate; this.status=status;
            this.resolvedDate=resolvedDate; this.reminderDate=reminderDate; this.reminderTime=reminderTime;
        }
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        prefs = getSharedPreferences("daily_manager", MODE_PRIVATE);
        loadTasks();
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        setContentView(buildUi());
        refresh();
        String openTask = getIntent().getStringExtra("taskId");
        if(openTask != null){
            Task t=findTask(openTask);
            if(t!=null) taskList.postDelayed(() -> showTaskDetails(t), 350);
        }
    }

    @Override protected void onResume(){
        super.onResume();
        if(prefs!=null){tasks.clear();loadTasks();if(taskList!=null)refresh();}
    }

    private View buildUi(){
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(1,14,29), NAVY2, NAVY});
        scroll.setBackground(bg);

        LinearLayout root = vbox();
        root.setPadding(dp(18), dp(18), dp(18), dp(36));
        scroll.addView(root);

        LinearLayout brand = hbox();
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.ec_logo); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        brand.addView(logo, new LinearLayout.LayoutParams(dp(56),dp(56)));
        LinearLayout brandText=vbox();
        TextView title = txt("DAILY TASK MANAGER", 23, WHITE, true); title.setLetterSpacing(.045f);
        TextView sub = txt("Plan • Focus • Achieve", 12, GOLD, false);
        brandText.addView(title); brandText.addView(sub);
        LinearLayout.LayoutParams bp=weight(); bp.setMargins(dp(10),0,0,0); brand.addView(brandText,bp);
        root.addView(brand, marginBottom(16));
        root.addView(goldLine(), marginBottom(16));

        LinearLayout tabs = hbox();
        Button d = tab("Daily"), w = tab("Weekly"), m = tab("Monthly");
        d.setOnClickListener(v->{mode="DAILY"; refresh();});
        w.setOnClickListener(v->{mode="WEEKLY"; refresh();});
        m.setOnClickListener(v->{mode="MONTHLY"; refresh();});
        tabs.addView(d, weightWithRight(5)); tabs.addView(w, weightWithRight(5)); tabs.addView(m, weight());
        root.addView(tabs, marginBottom(14));

        LinearLayout dateCard = card();
        TextView choose = txt("SELECTED DATE", 11, GOLD, true);
        dateText = txt("", 19, WHITE, true);
        rangeText = txt("", 11, MUTED, false);
        Button pick = smallButton("CHANGE DATE", NAVY2, GOLD);
        pick.setOnClickListener(v->pickDate());
        dateCard.addView(choose); dateCard.addView(dateText); dateCard.addView(rangeText, marginBottom(8)); dateCard.addView(pick);
        root.addView(dateCard, marginBottom(12));

        Button add = button("＋  ADD TASK", GOLD, NAVY);
        add.setTextSize(14); add.setMinHeight(dp(46)); add.setPadding(dp(10),dp(8),dp(10),dp(8));
        add.setOnClickListener(v->showTaskEditor(null));
        root.addView(add, marginBottom(8));

        Button ai = button("🎙  AI VOICE ASSISTANT", CARD, GOLD);
        ai.setTextSize(13); ai.setMinHeight(dp(44)); ai.setPadding(dp(10),dp(7),dp(10),dp(7));
        GradientDrawable aig = round(CARD,12); aig.setStroke(dp(1),Color.argb(150,224,184,78)); ai.setBackground(aig);
        ai.setOnClickListener(v->startActivity(new Intent(this,AiAssistantActivity.class)));
        root.addView(ai, marginBottom(14));

        LinearLayout stats = card();
        TextView st = txt("COMPLETION", 11, MUTED, true);
        percentText = txt("0%", 38, GOLD, true);
        statText = txt("0 done • 0 pending • 0 canceled", 13, WHITE, false);
        stats.addView(st); stats.addView(percentText); stats.addView(statText);
        root.addView(stats, marginBottom(15));

        TextView heading = txt("TASKS", 13, GOLD, true);
        root.addView(heading, marginBottom(8));
        taskList = vbox(); root.addView(taskList);

        TextView footer = txt("Daily Task Manager • Local data on this device", 10, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp = lp(-1,-2); fp.setMargins(0,dp(24),0,0); root.addView(footer, fp);
        return scroll;
    }

    private View goldLine(){
        View v=new View(this); GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.TRANSPARENT,GOLD,Color.TRANSPARENT}); v.setBackground(g);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1))); return v;
    }

    private void pickDate(){
        new DatePickerDialog(this,(v,y,m,d)->{selectedDate.set(y,m,d); refresh();},
                selectedDate.get(Calendar.YEAR),selectedDate.get(Calendar.MONTH),selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTaskEditor(Task existing){
        final boolean editing = existing != null;
        Calendar chosen = Calendar.getInstance();
        if(editing) chosen = calendarFromKey(existing.startDate);
        else chosen = (Calendar)selectedDate.clone();
        final Calendar taskDate = chosen;
        final int[] hour = {9}, minute = {0};
        final boolean[] hasTime = {false};
        if(editing && existing.reminderTime != null && !existing.reminderTime.isEmpty()){
            String[] p=existing.reminderTime.split(":");
            try { hour[0]=Integer.parseInt(p[0]); minute[0]=Integer.parseInt(p[1]); hasTime[0]=true; } catch(Exception ignored){}
        }

        LinearLayout box=vbox(); box.setPadding(dp(18),dp(8),dp(18),0);
        EditText title=new EditText(this); title.setHint("Task title"); title.setText(editing?existing.title:""); title.setSingleLine(true);
        EditText desc=new EditText(this); desc.setHint("Description / phone / license / reference notes"); desc.setMinLines(4); desc.setGravity(Gravity.TOP); desc.setText(editing?existing.description:"");
        TextView dateLabel=txt("Date: "+prettyDate(taskDate),13,NAVY,true);
        Button dateBtn=smallButton("Choose date", GOLD, NAVY);
        dateBtn.setOnClickListener(v->new DatePickerDialog(this,(dv,y,m,d)->{taskDate.set(y,m,d); dateLabel.setText("Date: "+prettyDate(taskDate));},
                taskDate.get(Calendar.YEAR),taskDate.get(Calendar.MONTH),taskDate.get(Calendar.DAY_OF_MONTH)).show());
        TextView timeLabel=txt(hasTime[0]?"Reminder: "+formatTime(hour[0],minute[0]):"Reminder: none",13,NAVY,true);
        Button timeBtn=smallButton("Set reminder time", GOLD, NAVY);
        timeBtn.setOnClickListener(v->new TimePickerDialog(this,(tv,h,min)->{hour[0]=h;minute[0]=min;hasTime[0]=true;timeLabel.setText("Reminder: "+formatTime(h,min));},hour[0],minute[0],false).show());
        Button clearTime=smallButton("Remove reminder", Color.LTGRAY, NAVY);
        clearTime.setOnClickListener(v->{hasTime[0]=false;timeLabel.setText("Reminder: none");});

        box.addView(title); box.addView(desc); box.addView(dateLabel, marginTop(8)); box.addView(dateBtn, marginTop(6));
        box.addView(timeLabel, marginTop(10)); box.addView(timeBtn, marginTop(6)); box.addView(clearTime, marginTop(6));

        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(editing?"Edit task":"Add task").setView(box)
                .setPositiveButton(editing?"Save":"Add",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String tt=title.getText().toString().trim();
            if(tt.isEmpty()){title.setError("Enter task title");return;}
            if(editing){
                cancelReminder(existing);
                existing.title=tt; existing.description=desc.getText().toString().trim(); existing.startDate=dateKey(taskDate);
                existing.reminderDate=hasTime[0]?dateKey(taskDate):"";
                existing.reminderTime=hasTime[0]?String.format(Locale.US,"%02d:%02d",hour[0],minute[0]):"";
                if(!existing.status.equals("PENDING") && existing.resolvedDate.compareTo(existing.startDate)<0) existing.resolvedDate=existing.startDate;
                if(existing.status.equals("PENDING")) scheduleReminder(existing);
            }else{
                Task t=new Task(UUID.randomUUID().toString(),tt,desc.getText().toString().trim(),dateKey(taskDate),"PENDING","",
                        hasTime[0]?dateKey(taskDate):"", hasTime[0]?String.format(Locale.US,"%02d:%02d",hour[0],minute[0]):"");
                tasks.add(t); scheduleReminder(t);
            }
            saveTasks(); refresh(); dialog.dismiss();
        }));
        dialog.show();
    }

    private void refresh(){
        dateText.setText(new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.ENGLISH).format(selectedDate.getTime()));
        rangeText.setText(rangeDescription());
        taskList.removeAllViews();
        ArrayList<Task> shown = filtered();
        int done=0,pending=0,cancel=0;
        for(Task t:shown){
            String displayStatus=statusForSelectedPeriod(t);
            if(displayStatus.equals("DONE")) done++; else if(displayStatus.equals("CANCEL")) cancel++; else pending++;
            taskList.addView(taskCard(t,displayStatus), marginBottom(9));
        }
        int active=done+pending; int pct=active==0?0:Math.round(done*100f/active);
        percentText.setText(pct+"%"); statText.setText(done+" done  •  "+pending+" pending  •  "+cancel+" canceled");
        if(shown.isEmpty()){
            TextView empty=txt("No tasks for this period.\nTap ADD TASK to create one.",14,MUTED,false);
            empty.setGravity(Gravity.CENTER); empty.setPadding(0,dp(34),0,dp(34)); taskList.addView(empty);
        }
    }

    private View taskCard(Task t,String displayStatus){
        LinearLayout c=card(); c.setClickable(true);
        LinearLayout top=hbox();
        TextView icon=txt(displayStatus.equals("DONE")?"✓":displayStatus.equals("CANCEL")?"✕":"•",18,
                displayStatus.equals("DONE")?GREEN:displayStatus.equals("CANCEL")?RED:PENDING,true);
        icon.setGravity(Gravity.CENTER); top.addView(icon,new LinearLayout.LayoutParams(dp(30),dp(30)));
        LinearLayout tx=vbox();
        TextView name=txt(t.title,15,WHITE,true);
        String meta=(t.startDate)+(t.reminderTime!=null&&!t.reminderTime.isEmpty()?"  •  "+friendlyTime(t.reminderTime):"")+"  •  "+displayStatus;
        TextView m=txt(meta,10,displayStatus.equals("DONE")?GREEN:displayStatus.equals("CANCEL")?RED:PENDING,true);
        tx.addView(name); tx.addView(m);
        if(t.description!=null&&!t.description.trim().isEmpty()){
            String shortDesc=t.description.replace("\n"," "); if(shortDesc.length()>78) shortDesc=shortDesc.substring(0,78)+"…";
            tx.addView(txt(shortDesc,11,MUTED,false), marginTop(3));
        }
        if(!t.attachments.isEmpty()) tx.addView(txt("📎 "+t.attachments.size()+" attachment"+(t.attachments.size()==1?"":"s"),10,GOLD,false),marginTop(3));
        LinearLayout.LayoutParams tp=weight(); tp.setMargins(dp(8),0,0,0); top.addView(tx,tp); c.addView(top);

        if(t.status.equals("PENDING")){
            LinearLayout actions=hbox();
            Button done=actionButton("✓ Done",GREEN,WHITE), cancel=actionButton("✕ Cancel",RED,WHITE);
            done.setOnClickListener(v->{markResolved(t,"DONE");});
            cancel.setOnClickListener(v->{markResolved(t,"CANCEL");});
            LinearLayout.LayoutParams a=weight(); a.setMargins(dp(10),dp(12),dp(7),dp(10)); actions.addView(done,a);
            LinearLayout.LayoutParams b=weight(); b.setMargins(dp(7),dp(12),dp(10),dp(10)); actions.addView(cancel,b); c.addView(actions);
        }
        c.setOnClickListener(v->showTaskDetails(t));
        return c;
    }

    private void markResolved(Task t,String status){
        t.status=status; t.resolvedDate=dateKey(selectedDate); if(t.resolvedDate.compareTo(t.startDate)<0)t.resolvedDate=t.startDate;
        cancelReminder(t); saveTasks(); refresh();
    }

    private void showTaskDetails(Task t){
        cleanMissingAttachments(t);
        LinearLayout box=vbox(); box.setPadding(dp(18),dp(4),dp(18),dp(8));
        TextView title=txt(t.title,20,NAVY,true); box.addView(title);
        box.addView(txt("Started: "+t.startDate+"   •   Status: "+t.status,11,Color.DKGRAY,true),marginTop(4));
        if(t.reminderTime!=null&&!t.reminderTime.isEmpty()) box.addView(txt("Reminder: "+t.reminderDate+" at "+friendlyTime(t.reminderTime),12,Color.DKGRAY,false),marginTop(5));
        TextView dh=txt("DESCRIPTION",11,NAVY,true); box.addView(dh,marginTop(14));
        TextView desc=txt((t.description==null||t.description.isEmpty())?"No description":t.description,14,Color.DKGRAY,false); desc.setTextIsSelectable(true); box.addView(desc,marginTop(4));
        TextView ah=txt("ATTACHMENTS",11,NAVY,true); box.addView(ah,marginTop(16));
        LinearLayout attachmentList=vbox(); box.addView(attachmentList,marginTop(5));
        populateAttachments(attachmentList,t);
        Button addAtt=smallButton("＋  Add photo / file",GOLD,NAVY); addAtt.setOnClickListener(v->chooseAttachmentSource(t)); box.addView(addAtt,marginTop(9));

        ScrollView sv=new ScrollView(this); sv.addView(box);
        AlertDialog dlg=new AlertDialog.Builder(this).setView(sv)
                .setPositiveButton("Edit",(d,w)->showTaskEditor(t))
                .setNeutralButton(t.status.equals("PENDING")?"Done":"Set pending",(d,w)->{
                    if(t.status.equals("PENDING")) markResolved(t,"DONE"); else {t.status="PENDING";t.resolvedDate="";scheduleReminder(t);saveTasks();refresh();}
                })
                .setNegativeButton("Close",null).create();
        dlg.setOnShowListener(x->{
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(NAVY);
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(GREEN);
        });
        dlg.show();
    }

    private void populateAttachments(LinearLayout list, Task t){
        list.removeAllViews();
        if(t.attachments.isEmpty()){list.addView(txt("No attachments",12,Color.GRAY,false));return;}
        for(Attachment a:new ArrayList<>(t.attachments)){
            LinearLayout row=hbox(); row.setPadding(dp(8),dp(6),dp(8),dp(6)); row.setBackground(round(Color.rgb(236,239,243),10));
            TextView name=txt("📎  "+a.name,12,NAVY,true); name.setOnClickListener(v->openAttachment(a)); row.addView(name,weight());
            Button del=smallButton("✕",Color.TRANSPARENT,RED); del.setTextSize(12); del.setMinWidth(dp(36)); del.setOnClickListener(v->confirmRemoveAttachment(t,a)); row.addView(del,new LinearLayout.LayoutParams(dp(44),dp(38)));
            list.addView(row,marginBottom(5));
        }
    }

    private void chooseAttachmentSource(Task t){
        attachmentTarget=t;
        new AlertDialog.Builder(this).setTitle("Add attachment").setItems(new String[]{"Take photo","Choose photo or file"},(d,which)->{
            if(which==0) launchCamera(); else launchFilePicker();
        }).show();
    }

    private void launchFilePicker(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf","text/plain","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
        startActivityForResult(i,REQ_FILE);
    }

    private void launchCamera(){
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,"DTM_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        if(Build.VERSION.SDK_INT>=29) values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/DailyTaskManager");
        currentCameraUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        if(currentCameraUri==null){toast("Could not create photo");return;}
        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE); i.putExtra(MediaStore.EXTRA_OUTPUT,currentCameraUri); i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i,REQ_CAMERA);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || attachmentTarget==null) return;
        if(requestCode==REQ_FILE && data!=null && data.getData()!=null){
            Uri u=data.getData();
            try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            attachmentTarget.attachments.add(new Attachment(u.toString(),displayName(u),"file",false));
            saveTasks(); toast("Attachment added");
        }else if(requestCode==REQ_CAMERA && currentCameraUri!=null){
            attachmentTarget.attachments.add(new Attachment(currentCameraUri.toString(),displayName(currentCameraUri),"photo",true));
            saveTasks(); toast("Photo attached and saved in Pictures/DailyTaskManager"); currentCameraUri=null;
        }
        refresh();
    }

    private void openAttachment(Attachment a){
        try{
            Uri u=Uri.parse(a.uri); Intent i=new Intent(Intent.ACTION_VIEW); String type=getContentResolver().getType(u); if(type==null)type="*/*";
            i.setDataAndType(u,type); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(i);
        }catch(Exception e){toast("File is no longer available on this phone");}
    }

    private void confirmRemoveAttachment(Task t,Attachment a){
        String msg=a.owned?"Remove this attachment and delete the photo created by the app from your phone?":"Remove this attachment from the task? The original file will stay on your phone.";
        new AlertDialog.Builder(this).setTitle("Remove attachment?").setMessage(msg).setPositiveButton("Remove",(d,w)->{
            if(a.owned){try{getContentResolver().delete(Uri.parse(a.uri),null,null);}catch(Exception ignored){}}
            t.attachments.remove(a); saveTasks(); toast("Attachment removed");
        }).setNegativeButton("Cancel",null).show();
    }

    private void cleanMissingAttachments(Task t){
        ArrayList<Attachment> missing=new ArrayList<>();
        for(Attachment a:t.attachments){try{Cursor c=getContentResolver().query(Uri.parse(a.uri),new String[]{OpenableColumns.DISPLAY_NAME},null,null,null); if(c==null){missing.add(a);} else c.close();}catch(Exception e){missing.add(a);}}
        if(!missing.isEmpty()){t.attachments.removeAll(missing);saveTasks();}
    }

    private String displayName(Uri uri){
        String result="attachment";
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(c!=null&&c.moveToFirst()) result=c.getString(0);
        }catch(Exception ignored){}
        return result==null?"attachment":result;
    }

    private ArrayList<Task> filtered(){
        ArrayList<Task> out=new ArrayList<>();
        Calendar start=periodStart(), end=periodEnd(); String s=dateKey(start), e=dateKey(end);
        for(Task t:tasks){
            String last=t.status.equals("PENDING")?"9999-12-31":(t.resolvedDate==null||t.resolvedDate.isEmpty()?t.startDate:t.resolvedDate);
            if(t.startDate.compareTo(e)<=0 && last.compareTo(s)>=0) out.add(t);
        }
        Collections.sort(out,(a,b)->{int x=a.startDate.compareTo(b.startDate);return x!=0?x:a.title.compareToIgnoreCase(b.title);});
        return out;
    }

    private String statusForSelectedPeriod(Task t){
        if(mode.equals("DAILY")){
            String d=dateKey(selectedDate);
            if(t.status.equals("PENDING")) return "PENDING";
            if(t.resolvedDate!=null&&!t.resolvedDate.isEmpty()&&d.compareTo(t.resolvedDate)<0) return "PENDING";
            return t.status;
        }
        return t.status;
    }

    private Calendar periodStart(){
        Calendar s=(Calendar)selectedDate.clone();
        if(mode.equals("WEEKLY")){int dow=s.get(Calendar.DAY_OF_WEEK);int diff=(dow-Calendar.MONDAY+7)%7;s.add(Calendar.DAY_OF_MONTH,-diff);}
        else if(mode.equals("MONTHLY"))s.set(Calendar.DAY_OF_MONTH,1);
        return s;
    }
    private Calendar periodEnd(){
        Calendar e=periodStart(); if(mode.equals("WEEKLY"))e.add(Calendar.DAY_OF_MONTH,6); else if(mode.equals("MONTHLY"))e.set(Calendar.DAY_OF_MONTH,e.getActualMaximum(Calendar.DAY_OF_MONTH)); return e;
    }
    private String rangeDescription(){
        SimpleDateFormat f=new SimpleDateFormat("dd MMM yyyy",Locale.ENGLISH);
        if(mode.equals("DAILY"))return "Daily view • pending tasks carry forward automatically";
        Calendar s=periodStart(),e=periodEnd();
        if(mode.equals("WEEKLY"))return "Week: "+f.format(s.getTime())+" – "+f.format(e.getTime());
        return "Month: "+new SimpleDateFormat("MMMM yyyy",Locale.ENGLISH).format(s.getTime());
    }

    private void scheduleReminder(Task t){
        if(!t.status.equals("PENDING")||t.reminderDate==null||t.reminderDate.isEmpty()||t.reminderTime==null||t.reminderTime.isEmpty())return;
        try{
            Calendar c=calendarFromKey(t.reminderDate); String[] p=t.reminderTime.split(":"); c.set(Calendar.HOUR_OF_DAY,Integer.parseInt(p[0]));c.set(Calendar.MINUTE,Integer.parseInt(p[1]));c.set(Calendar.SECOND,0);
            if(c.getTimeInMillis()<=System.currentTimeMillis())return;
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            PendingIntent pi=reminderPendingIntent(t);
            if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
        }catch(Exception ignored){}
    }
    private void cancelReminder(Task t){
        try{((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(reminderPendingIntent(t));}catch(Exception ignored){}
    }
    private PendingIntent reminderPendingIntent(Task t){
        Intent i=new Intent(this,ReminderReceiver.class);i.putExtra("title",t.title);i.putExtra("taskId",t.id);
        return PendingIntent.getBroadcast(this,t.id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);NotificationChannel c=new NotificationChannel(ReminderReceiver.CHANNEL_ID,"Task reminders",NotificationManager.IMPORTANCE_HIGH);c.enableVibration(true);nm.createNotificationChannel(c);}
    }
    private void requestNotificationPermissionIfNeeded(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFY);
    }

    private Task findTask(String id){for(Task t:tasks)if(t.id.equals(id))return t;return null;}
    private String dateKey(Calendar c){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());}
    private Calendar calendarFromKey(String k){Calendar c=Calendar.getInstance();try{Date d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(k);if(d!=null)c.setTime(d);}catch(Exception ignored){}return c;}
    private String prettyDate(Calendar c){return new SimpleDateFormat("dd MMM yyyy",Locale.ENGLISH).format(c.getTime());}
    private String formatTime(int h,int m){Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);return new SimpleDateFormat("h:mm a",Locale.ENGLISH).format(c.getTime());}
    private String friendlyTime(String hm){try{String[]p=hm.split(":");return formatTime(Integer.parseInt(p[0]),Integer.parseInt(p[1]));}catch(Exception e){return hm;}}

    private void loadTasks(){
        try{
            JSONArray arr=new JSONArray(prefs.getString("tasks","[]"));
            for(int i=0;i<arr.length();i++){
                JSONObject o=arr.getJSONObject(i);
                String oldDate=o.optString("date",""); String start=o.optString("startDate",oldDate);
                Task t=new Task(o.optString("id",UUID.randomUUID().toString()),o.optString("title","Task"),o.optString("description",""),start,
                        o.optString("status","PENDING"),o.optString("resolvedDate",""),o.optString("reminderDate",""),o.optString("reminderTime",""));
                JSONArray aa=o.optJSONArray("attachments"); if(aa!=null)for(int j=0;j<aa.length();j++){JSONObject x=aa.getJSONObject(j);t.attachments.add(new Attachment(x.optString("uri"),x.optString("name","attachment"),x.optString("kind","file"),x.optBoolean("owned",false)));}
                tasks.add(t);
            }
        }catch(Exception ignored){}
    }
    private void saveTasks(){
        JSONArray arr=new JSONArray();
        try{
            for(Task t:tasks){JSONObject o=new JSONObject();o.put("id",t.id);o.put("title",t.title);o.put("description",t.description);o.put("startDate",t.startDate);o.put("status",t.status);o.put("resolvedDate",t.resolvedDate);o.put("reminderDate",t.reminderDate);o.put("reminderTime",t.reminderTime);
                JSONArray aa=new JSONArray();for(Attachment a:t.attachments){JSONObject x=new JSONObject();x.put("uri",a.uri);x.put("name",a.name);x.put("kind",a.kind);x.put("owned",a.owned);aa.put(x);}o.put("attachments",aa);arr.put(o);}
        }catch(Exception ignored){}
        prefs.edit().putString("tasks",arr.toString()).apply();
    }

    private LinearLayout vbox(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout hbox(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(fg);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(round(bg,12));return b;}
    private Button smallButton(String s,int bg,int fg){Button b=button(s,bg,fg);b.setTextSize(11);b.setMinHeight(dp(38));b.setPadding(dp(8),dp(4),dp(8),dp(4));return b;}
    private Button actionButton(String s,int bg,int fg){
        Button b=new Button(this); b.setAllCaps(false); b.setText(s); b.setTextColor(fg); b.setTextSize(10);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setMinHeight(dp(36)); b.setMinimumHeight(0);
        b.setPadding(dp(8),dp(3),dp(8),dp(3));
        GradientDrawable g=round(bg,12); g.setStroke(dp(1),Color.argb(235,255,255,255)); b.setBackground(g);
        b.setElevation(dp(4)); return b;
    }
    private Button tab(String s){Button b=smallButton(s,CARD,WHITE);return b;}
    private LinearLayout card(){LinearLayout l=vbox();l.setPadding(dp(14),dp(13),dp(14),dp(13));GradientDrawable g=round(CARD,16);g.setStroke(dp(1),Color.argb(80,224,184,78));l.setBackground(g);return l;}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-2,1f);}
    private LinearLayout.LayoutParams weightWithRight(int r){LinearLayout.LayoutParams p=weight();p.setMargins(0,0,dp(r),0);return p;}
    private LinearLayout.LayoutParams marginBottom(int b){LinearLayout.LayoutParams p=lp(-1,-2);p.setMargins(0,0,0,dp(b));return p;}
    private LinearLayout.LayoutParams marginTop(int t){LinearLayout.LayoutParams p=lp(-1,-2);p.setMargins(0,dp(t),0,0);return p;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
