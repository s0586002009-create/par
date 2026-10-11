package il.co.aniparve
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
 private val p by lazy { getSharedPreferences("ani_parve", MODE_PRIVATE) }
 private val brown=Color.rgb(117,61,29); private val cream=Color.rgb(255,248,233); private val gold=Color.rgb(197,139,53)
 private var screen="home"; private var tick: CountDownTimer?=null; private var clock:TextView?=null; private var bar:ProgressBar?=null; private var pct:TextView?=null
 override fun onCreate(savedInstanceState:Bundle?) { applyTheme(); super.onCreate(savedInstanceState); channel()
  screen=when { p.getBoolean("finished",false)->"done"; p.getLong("end",0)>System.currentTimeMillis()->"timer"; p.getLong("end",0)>0->{p.edit().putBoolean("finished",true).apply();"done"}; else->"home" }
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
   AlertDialog.Builder(this).setTitle("התראות לטיימר").setMessage("כדי לקבל תזכורת בסיום, מומלץ לאפשר התראות. אפשר לשנות זאת בהגדרות המכשיר.").setPositiveButton("המשך"){_,_->ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),3); draw()}.setNegativeButton("אחר כך"){_,_->draw()}.show()
  else draw()
 }
 private fun applyTheme(){AppCompatDelegate.setDefaultNightMode(when(p.getString("theme","system")){"light"->AppCompatDelegate.MODE_NIGHT_NO;"dark"->AppCompatDelegate.MODE_NIGHT_YES;else->AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM})}
 private fun channel(){if(Build.VERSION.SDK_INT>=26){val id="parve_"+p.getString("sound","default");val c=NotificationChannel(id,"אני פרווה",NotificationManager.IMPORTANCE_HIGH);c.enableVibration(p.getBoolean("vibrate",true));if(p.getString("sound","default")=="silent")c.setSound(null,null);getSystemService(NotificationManager::class.java).createNotificationChannel(c);p.edit().putString("channel",id).apply()}}
 private fun draw(){tick?.cancel();when(screen){"timer"->timerScreen();"done"->doneScreen();"settings"->settings();else->home()}}
 private fun base(title:String):LinearLayout {val root=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(20),dp(14),dp(20),dp(20));setBackgroundColor(cream)}
  val top=LinearLayout(this).apply{gravity=Gravity.END};top.addView(Button(this@MainActivity).apply{text="⚙";textSize=22f;setTextColor(brown);setOnClickListener{screen="settings";draw()}},LinearLayout.LayoutParams(dp(54),dp(48)));root.addView(top,LinearLayout.LayoutParams(-1,dp(52)))
  root.addView(TextView(this).apply{text=title;textSize=29f;setTextColor(brown);gravity=Gravity.CENTER;setPadding(0,dp(8),0,dp(18))},LinearLayout.LayoutParams(-1,-2))
  setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)});return root}
 private fun home(){val r=base("עכשיו אכלתי");r.addView(TextView(this).apply{text="מה אכלת עכשיו?";textSize=18f;setTextColor(brown);gravity=Gravity.CENTER})
  val row=LinearLayout(this).apply{gravity=Gravity.CENTER};row.addView(food("🥩","בשרי","meat"),LinearLayout.LayoutParams(0,dp(180),1f).apply{marginEnd=dp(7)});row.addView(food("🧀","חלבי","dairy"),LinearLayout.LayoutParams(0,dp(180),1f).apply{marginStart=dp(7)});r.addView(row,LinearLayout.LayoutParams(-1,dp(190)))
  r.addView(TextView(this).apply{text="את זמני ההמתנה ניתן לשנות בהגדרות";textSize=14f;setTextColor(brown);gravity=Gravity.CENTER;setPadding(0,dp(20),0,0)})}
 private fun food(emoji:String,label:String,key:String)=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;setBackgroundColor(if(key=="meat")Color.rgb(255,229,216) else Color.rgb(225,241,245));elevation=dp(3).toFloat()
  addView(TextView(this@MainActivity).apply{text=emoji;textSize=46f;gravity=Gravity.CENTER});addView(TextView(this@MainActivity).apply{text=label;textSize=23f;setTextColor(brown);gravity=Gravity.CENTER})
  setOnClickListener{val h=p.getInt(if(key=="meat")"meat_h" else "dairy_h",6);val m=p.getInt(if(key=="meat")"meat_m" else "dairy_m",0);val d=TimeUnit.HOURS.toMillis(h.toLong())+TimeUnit.MINUTES.toMillis(m.toLong());p.edit().putLong("duration",d).putLong("end",System.currentTimeMillis()+d).putBoolean("finished",false).apply();schedule();screen="timer";draw()}}
 private fun timerScreen(){val r=base("אני פרווה בעוד");clock=TextView(this).apply{textSize=39f;setTextColor(brown);gravity=Gravity.CENTER};r.addView(clock,LinearLayout.LayoutParams(-1,dp(100)))
  bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progressTintList=android.content.res.ColorStateList.valueOf(gold)};r.addView(bar,LinearLayout.LayoutParams(-1,dp(12)))
  pct=TextView(this).apply{textSize=16f;setTextColor(brown);gravity=Gravity.CENTER;setPadding(0,dp(12),0,dp(20))};r.addView(pct)
  r.addView(Button(this).apply{text="עצור";setOnClickListener{AlertDialog.Builder(this@MainActivity).setTitle("עצירת הטיימר").setMessage("האם אתם בטוחים שאתם רוצים להפסיק את הטיימר?").setNegativeButton("לא",null).setPositiveButton("כן"){_,_->cancel();screen="home";draw()}.show()}},LinearLayout.LayoutParams(-1,dp(54)));startTick()}
 private fun startTick(){val end=p.getLong("end",0);val duration=p.getLong("duration",1).coerceAtLeast(1)
  fun update(){val left=(end-System.currentTimeMillis()).coerceAtLeast(0);clock?.text=String.format(Locale.US,"%02d:%02d:%02d",TimeUnit.MILLISECONDS.toHours(left),TimeUnit.MILLISECONDS.toMinutes(left)%60,TimeUnit.MILLISECONDS.toSeconds(left)%60);val n=(left*100/duration).toInt().coerceIn(0,100);bar?.progress=n;pct?.text="$n% נותרו";if(left==0L){p.edit().putBoolean("finished",true).apply();notifyDone();screen="done";doneScreen()}}
  update();if(screen=="timer")tick=object:CountDownTimer((end-System.currentTimeMillis()).coerceAtLeast(0),1000){override fun onTick(ms:Long){update()};override fun onFinish(){update()}}.start()}
 private fun doneScreen(){tick?.cancel();val r=base("");r.addView(TextView(this).apply{text="⌛";textSize=70f;gravity=Gravity.CENTER;setTextColor(gold)},LinearLayout.LayoutParams(-1,dp(120)));r.addView(TextView(this).apply{text="אני פרווה";textSize=36f;setTextColor(brown);gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,dp(75)));r.addView(TextView(this).apply{text="זמן ההמתנה הסתיים";textSize=18f;setTextColor(brown);gravity=Gravity.CENTER})
  r.addView(Button(this).apply{text="חזרה";setOnClickListener{p.edit().remove("end").remove("duration").putBoolean("finished",false).apply();getSystemService(NotificationManager::class.java).cancel(2);screen="home";draw()}},LinearLayout.LayoutParams(-1,dp(54)))}
 private fun settings(){val r=base("הגדרות");section(r,"זמן המתנה בשרי ← חלבי","meat");section(r,"זמן המתנה חלבי ← בשרי","dairy")
  sw(r,"התראה קבועה בזמן הספירה","persistent",true);sw(r,"רטט בהתראה","vibrate",true)
  r.addView(Button(this).apply{text="צליל התראה";setOnClickListener{AlertDialog.Builder(this@MainActivity).setTitle("צליל התראה").setItems(arrayOf("שקט","ברירת מחדל של המכשיר")){_,i->p.edit().putString("sound",if(i==0)"silent" else "default").apply();channel();settings()}.show()}},LinearLayout.LayoutParams(-1,dp(50)))
  r.addView(Button(this).apply{text="מראה: "+p.getString("theme","system");setOnClickListener{AlertDialog.Builder(this@MainActivity).setTitle("מראה").setItems(arrayOf("לפי המערכת","בהיר","כהה")){_,i->p.edit().putString("theme",arrayOf("system","light","dark")[i]).apply();applyTheme();screen="settings";recreate()}.show()}},LinearLayout.LayoutParams(-1,dp(50)))
  r.addView(Button(this).apply{text="הגדרות התראות וחיסכון בסוללה";setOnClickListener{try{if(Build.VERSION.SDK_INT>=31)startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName"))) else startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")));if(Build.VERSION.SDK_INT>=23&&!getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(packageName))startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:$packageName")))}catch(_:Exception){}}},LinearLayout.LayoutParams(-1,dp(50)))
  r.addView(Button(this).apply{text="חזרה";setOnClickListener{screen=if(p.getBoolean("finished",false))"done" else if(p.getLong("end",0)>System.currentTimeMillis())"timer" else "home";draw()}},LinearLayout.LayoutParams(-1,dp(50)))}
 private fun section(root:LinearLayout,title:String,key:String){root.addView(TextView(this).apply{text=title;textSize=17f;setTextColor(brown);setPadding(0,dp(10),0,0)})
  val row=LinearLayout(this).apply{gravity=Gravity.CENTER};val h=NumberPicker(this).apply{minValue=0;maxValue=24;value=p.getInt(key+"_h",6)};val m=NumberPicker(this).apply{minValue=0;maxValue=59;value=p.getInt(key+"_m",0)}
  row.addView(h,LinearLayout.LayoutParams(0,dp(95),1f));row.addView(TextView(this).apply{text="שעות";setTextColor(brown);gravity=Gravity.CENTER});row.addView(m,LinearLayout.LayoutParams(0,dp(95),1f));row.addView(TextView(this).apply{text="דקות";setTextColor(brown);gravity=Gravity.CENTER})
  h.setOnValueChangedListener{_,_,v->p.edit().putInt(key+"_h",v).apply()};m.setOnValueChangedListener{_,_,v->p.edit().putInt(key+"_m",v).apply()};root.addView(row)}
 private fun sw(root:LinearLayout,title:String,key:String,def:Boolean){root.addView(Switch(this).apply{text=title;setTextColor(brown);isChecked=p.getBoolean(key,def);setOnCheckedChangeListener{_,v->p.edit().putBoolean(key,v).apply();if(key=="persistent")persistent()}},LinearLayout.LayoutParams(-1,dp(50)))}
 private fun schedule(){val end=p.getLong("end",0);val am=getSystemService(AlarmManager::class.java);val pi=PendingIntent.getBroadcast(this,1,Intent(this,TimerReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,pi) else am.setExact(AlarmManager.RTC_WAKEUP,end,pi)}catch(_:SecurityException){am.set(AlarmManager.RTC_WAKEUP,end,pi)};persistent()}
 private fun persistent(){if(!p.getBoolean("persistent",true)||p.getLong("end",0)<=System.currentTimeMillis()){getSystemService(NotificationManager::class.java).cancel(1);return};val open=PendingIntent.getActivity(this,10,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val n=NotificationCompat.Builder(this,p.getString("channel","parve_default")!!).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("אני פרווה בעוד").setContentText("הטיימר פעיל").setWhen(p.getLong("end",0)).setUsesChronometer(true).setChronometerCountDown(true).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open).build();try{NotificationManagerCompat.from(this).notify(1,n)}catch(_:SecurityException){}}
 private fun notifyDone(){getSystemService(NotificationManager::class.java).cancel(1);val open=PendingIntent.getActivity(this,11,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val n=NotificationCompat.Builder(this,p.getString("channel","parve_default")!!).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("אני פרווה").setContentText("זמן ההמתנה הסתיים").setAutoCancel(true).setContentIntent(open).build();try{NotificationManagerCompat.from(this).notify(2,n)}catch(_:SecurityException){}}
 private fun cancel(){tick?.cancel();getSystemService(AlarmManager::class.java).cancel(PendingIntent.getBroadcast(this,1,Intent(this,TimerReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE));getSystemService(NotificationManager::class.java).cancel(1);p.edit().remove("end").remove("duration").putBoolean("finished",false).apply()}
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
}
