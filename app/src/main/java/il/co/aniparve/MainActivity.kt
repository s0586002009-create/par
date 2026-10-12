package il.co.aniparve

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
 private val p by lazy { getSharedPreferences("ani_parve", MODE_PRIVATE) }
 private var screen="home"; private var tick:CountDownTimer?=null
 private var clock:TextView?=null; private var pct:TextView?=null; private var ring:RingView?=null; private var bar:ProgressBar?=null
 private val dark get()=when(p.getString("theme","system")){"dark"->true;"light"->false;else->(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES}
 private fun c(s:String)=Color.parseColor(s)
 private val bg get()=c(if(dark)"#1B1410" else "#FFF8EC")
 private val card get()=c(if(dark)"#2A201A" else "#FFFFFF")
 private val ink get()=c(if(dark)"#F5E9DA" else "#3B2314")
 private val muted get()=c(if(dark)"#C7B5A3" else "#8D7766")
 private val copper get()=c("#B26A2B"); private val red get()=c(if(dark)"#F0645D" else "#D9433C"); private val blue get()=c(if(dark)"#80B9EA" else "#4F8BC8")
 override fun onCreate(savedInstanceState:Bundle?){applyTheme();super.onCreate(savedInstanceState);channel()
  screen=when{p.getBoolean("finished",false)->"done";p.getLong("end",0)>System.currentTimeMillis()->"timer";p.getLong("end",0)>0->{p.edit().putBoolean("finished",true).apply();"done"};else->"home"}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permissionScreen() else draw()
 }
 private fun applyTheme(){AppCompatDelegate.setDefaultNightMode(when(p.getString("theme","system")){"light"->AppCompatDelegate.MODE_NIGHT_NO;"dark"->AppCompatDelegate.MODE_NIGHT_YES;else->AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM})}
 private fun channel(){if(Build.VERSION.SDK_INT>=26){val id="parve_"+p.getString("sound","default");val ch=NotificationChannel(id,"אני פרווה",NotificationManager.IMPORTANCE_HIGH);ch.enableVibration(p.getBoolean("vibrate",true));if(p.getString("sound","default")=="silent")ch.setSound(null,null);getSystemService(NotificationManager::class.java).createNotificationChannel(ch);p.edit().putString("channel",id).apply()}}
 private fun draw(){tick?.cancel();when(screen){"timer"->timerScreen();"done"->doneScreen();"settings"->settings();else->home()}}
 private fun solid(color:Int,r:Int=20)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;cornerRadius=dp(r).toFloat();setColor(color)}
 private fun grad(colors:IntArray,r:Int=24)=GradientDrawable(GradientDrawable.Orientation.TL_BR,colors).apply{cornerRadius=dp(r).toFloat()}
 private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
 private fun ripple()=if(Build.VERSION.SDK_INT>=21)android.util.TypedValue().let{android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33B26A2B),null,null)}else null
 private fun root():LinearLayout{val body=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(12),dp(20),dp(24));background=solid(bg,0);layoutDirection=View.LAYOUT_DIRECTION_RTL}
  val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false;addView(body,ViewGroup.LayoutParams(-1,-2));background=solid(bg,0)};setContentView(scroll)
  body.alpha=0f;body.animate().alpha(1f).setDuration(220).setInterpolator(AccelerateDecelerateInterpolator()).start();return body}
 private fun space(parent:LinearLayout,n:Int){parent.addView(Space(this),LinearLayout.LayoutParams(1,dp(n)))}
 private fun label(parent:LinearLayout,s:String,size:Float,color:Int,bold:Boolean=false,gravity:Int=Gravity.CENTER){parent.addView(TextView(this).apply{text=s;textSize=size;setTextColor(color);this.gravity=gravity;typeface=if(bold)Typeface.create("sans-serif-medium",Typeface.BOLD) else Typeface.create("sans-serif",Typeface.NORMAL)},LinearLayout.LayoutParams(-1,-2))}
 private fun toolbar(r:LinearLayout,title:String?=null){val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  val isSettings=title=="הגדרות";row.addView(iconBtn(if(isSettings)"‹" else "⚙",if(isSettings)"חזרה" else "הגדרות"){if(isSettings){screen=if(p.getBoolean("finished",false))"done" else if(p.getLong("end",0)>System.currentTimeMillis())"timer" else "home";draw()}else{screen="settings";draw()}},LinearLayout.LayoutParams(dp(48),dp(48)))
  val brand=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_RTL}
  brand.addView(ImageView(this).apply{try{setImageResource(R.mipmap.ic_launcher)}catch(_:Exception){};scaleType=ImageView.ScaleType.FIT_CENTER},LinearLayout.LayoutParams(dp(34),dp(34)).apply{marginStart=dp(7)})
  brand.addView(TextView(this).apply{text=title?:"אני פרווה";textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink);gravity=Gravity.CENTER_VERTICAL})
  row.addView(brand,LinearLayout.LayoutParams(0,dp(48),1f));row.addView(Space(this),LinearLayout.LayoutParams(dp(48),dp(48)));r.addView(row,LinearLayout.LayoutParams(-1,dp(52)))
 }
 private fun iconBtn(g:String,desc:String,action:()->Unit)=TextView(this).apply{text=g;contentDescription=desc;textSize=26f;gravity=Gravity.CENTER;setTextColor(ink);background=solid(card,18);elevation=dp(2).toFloat();isClickable=true;setOnClickListener{action()}}.also{}
 private fun home(){val r=root();toolbar(r);space(r,24);label(r,"עכשיו אכלתי",32f,ink,true);space(r,6);label(r,"בחרו כדי להתחיל טיימר",16f,muted);space(r,28)
  val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  row.addView(food("🥩","בשרי","meat"),LinearLayout.LayoutParams(0,dp(228),1f).apply{marginEnd=dp(8)});row.addView(food("🧀","חלבי","dairy"),LinearLayout.LayoutParams(0,dp(228),1f).apply{marginStart=dp(8)});r.addView(row)
  space(r,22);panel(r){orientation=0;gravity=Gravity.CENTER_VERTICAL
   addView(TextView(this@MainActivity).apply{text="◷";textSize=26f;setTextColor(copper)},LinearLayout.LayoutParams(dp(40),-2))
   addView(LinearLayout(this@MainActivity).apply{orientation=1;addView(TextView(this@MainActivity).apply{text="זמני ההמתנה שלך";textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)});addView(TextView(this@MainActivity).apply{text="אפשר לשנות את הזמנים בהגדרות";textSize=12f;setTextColor(muted);setPadding(0,dp(3),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
   addView(TextView(this@MainActivity).apply{text="›";textSize=26f;setTextColor(muted)})
   setOnClickListener{screen="settings";draw()}
  };space(r,18);label(r,"פשוט בוחרים, ממתינים — וחוזרים לשגרה.",13f,muted)
 }
 private fun food(emoji:String,title:String,key:String)=LinearLayout(this).apply{
  orientation=1;gravity=Gravity.CENTER;setPadding(dp(8),dp(18),dp(8),dp(18));layoutDirection=View.LAYOUT_DIRECTION_RTL
  background=grad(if(key=="meat")intArrayOf(c("#E5534B"),c("#B8322C")) else intArrayOf(c("#6FA8DC"),c("#3F7DBF")),26);elevation=dp(3).toFloat()
  addView(TextView(this@MainActivity).apply{text=emoji;textSize=52f;gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,dp(94)))
  space(this,10);addView(TextView(this@MainActivity).apply{text=title;textSize=23f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER})
  space(this,8);val h=p.getInt(key+"_h",6);val m=p.getInt(key+"_m",0)
  addView(TextView(this@MainActivity).apply{text="המתנה: "+durationText(h,m);textSize=12f;setTextColor(Color.WHITE);gravity=Gravity.CENTER})
  isClickable=true;setOnClickListener{val h2=p.getInt(key+"_h",6);val m2=p.getInt(key+"_m",0);val d=TimeUnit.HOURS.toMillis(h2.toLong())+TimeUnit.MINUTES.toMillis(m2.toLong());p.edit().putLong("duration",d).putLong("end",System.currentTimeMillis()+d).putBoolean("finished",false).putString("food",key).apply();schedule();screen="timer";draw()}
  setOnTouchListener{v,e->when(e.action){MotionEvent.ACTION_DOWN->v.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start();MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()};false}
 }
 private fun panel(r:LinearLayout,content:LinearLayout.()->Unit){val box=LinearLayout(this).apply{orientation=1;background=solid(card,22);elevation=dp(2).toFloat();setPadding(dp(16),dp(14),dp(16),dp(14));content()};r.addView(box,LinearLayout.LayoutParams(-1,-2))}
 private fun timerScreen(){val r=root();toolbar(r);val meat=p.getString("food","meat")=="meat";val accent=if(meat)red else blue
  space(r,16);label(r,"אני פרווה בעוד",26f,ink,true);space(r,8);label(r,if(meat)"המתנה אחרי בשרי" else "המתנה אחרי חלבי",14f,accent,true);space(r,8)
  val box=FrameLayout(this);ring=RingView(this,accent);box.addView(ring,FrameLayout.LayoutParams(dp(270),dp(270),Gravity.CENTER))
  clock=TextView(this).apply{textSize=37f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink);gravity=Gravity.CENTER;includeFontPadding=false};box.addView(clock,FrameLayout.LayoutParams(-1,-1));r.addView(box,LinearLayout.LayoutParams(-1,dp(292)))
  bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progressTintList=android.content.res.ColorStateList.valueOf(accent);progressBackgroundTintList=android.content.res.ColorStateList.valueOf(if(dark)c("#45362B") else c("#F0E3D5"))}
  r.addView(bar,LinearLayout.LayoutParams(-1,dp(10)).apply{topMargin=dp(14)})
  pct=TextView(this).apply{textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent);gravity=Gravity.CENTER;setPadding(0,dp(12),0,dp(24))};r.addView(pct)
  r.addView(outlineButton("עצור",red){val d=AlertDialog.Builder(this).setTitle("עצירת הטיימר").setMessage("האם אתם בטוחים שאתם רוצים להפסיק את הטיימר?").setNegativeButton("לא",null).setPositiveButton("כן"){_,_->cancel();screen="home";draw()}.create();d.setOnShowListener{d.getButton(-1)?.setTextColor(red);d.getButton(-2)?.setTextColor(ink)};d.show()},LinearLayout.LayoutParams(-1,dp(54)))
  startTick()
 }
 private fun startTick(){val end=p.getLong("end",0);val duration=p.getLong("duration",1).coerceAtLeast(1)
  fun update(){val left=(end-System.currentTimeMillis()).coerceAtLeast(0);clock?.text=String.format(Locale.US,"%02d:%02d:%02d",TimeUnit.MILLISECONDS.toHours(left),TimeUnit.MILLISECONDS.toMinutes(left)%60,TimeUnit.MILLISECONDS.toSeconds(left)%60)
   val n=(left*100/duration).toInt().coerceIn(0,100);bar?.progress=n;ring?.setProgress(1f-n/100f);pct?.text="$n% נותרו"
   if(left==0L){p.edit().putBoolean("finished",true).apply();notifyDone();screen="done";doneScreen()}}
  update();if(screen=="timer")tick=object:CountDownTimer((end-System.currentTimeMillis()).coerceAtLeast(0),1000){override fun onTick(ms:Long)=update();override fun onFinish()=update()}.start()
 }
 private fun doneScreen(){tick?.cancel();val r=root();toolbar(r);space(r,48)
  val check=TextView(this).apply{text="✓";textSize=64f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER;background=solid(copper,100);elevation=dp(4).toFloat()}
  r.addView(check,LinearLayout.LayoutParams(dp(112),dp(112)).apply{gravity=Gravity.CENTER});check.scaleX=.7f;check.scaleY=.7f;check.animate().scaleX(1f).scaleY(1f).setDuration(350).start()
  space(r,26);label(r,"אני פרווה",36f,ink,true);space(r,8);label(r,"זמן ההמתנה הסתיים",17f,muted);space(r,34)
  r.addView(primaryButton("חזרה"){p.edit().remove("end").remove("duration").putBoolean("finished",false).apply();getSystemService(NotificationManager::class.java).cancel(2);screen="home";draw()},LinearLayout.LayoutParams(-1,dp(56)))
 }
 private fun settings(){val r=root();toolbar(r,"הגדרות");space(r,12)
  section(r,"◷","זמני המתנה"){waitCard(this,"בשרי  ←  חלבי","meat");space(this,12);waitCard(this,"חלבי  ←  בשרי","dairy")}
  space(r,16);section(r,"♧","התראות"){switchRow(this,"◉","רטט בהתראה","רטט בעת סיום ההמתנה","vibrate",true);divider(this);switchRow(this,"▣","התראה קבועה בזמן הספירה","הצגת הטיימר בשורת ההתראות","persistent",true);divider(this);settingRow(this,"♫","צליל התראה",if(p.getString("sound","default")=="silent")"שקט" else "ברירת מחדל","›"){AlertDialog.Builder(this@MainActivity).setTitle("צליל התראה").setItems(arrayOf("שקט","ברירת מחדל")){_,i->p.edit().putString("sound",if(i==0)"silent" else "default").apply();channel();settings()}.show()}}
  space(r,16);section(r,"☼","מראה"){val current=p.getString("theme","system");val row=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER}
   listOf("system" to "לפי המערכת","light" to "בהיר","dark" to "כהה").forEach{(key,name)->val selected=current==key;row.addView(TextView(this@MainActivity).apply{text=name;textSize=13f;gravity=Gravity.CENTER;setTextColor(if(selected)Color.WHITE else ink);background=solid(if(selected)copper else bg,22);setPadding(dp(8),dp(12),dp(8),dp(12));setOnClickListener{p.edit().putString("theme",key).apply();applyTheme();screen="settings";recreate()}},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(4);marginEnd=dp(4)})};addView(row)}
  space(r,16);section(r,"⚙","הרשאות ואמינות"){settingRow(this,"♧","הגדרות התראות וחיסכון בסוללה","ניהול הרשאות והתנהגות ברקע","›"){try{if(Build.VERSION.SDK_INT>=31)startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))else startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")));if(Build.VERSION.SDK_INT>=23&&!getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName))startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:$packageName")))}catch(_:Exception){}}}
 }
 private fun section(r:LinearLayout,g:String,title:String,content:LinearLayout.()->Unit){val box=LinearLayout(this).apply{orientation=1;background=solid(card,24);elevation=dp(2).toFloat();setPadding(dp(18),dp(16),dp(18),dp(16))}
  val h=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;addView(TextView(this@MainActivity).apply{text=g;textSize=22f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(34),dp(36)));addView(TextView(this@MainActivity).apply{text=title;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(copper)})};box.addView(h);space(box,8);box.content();r.addView(box,LinearLayout.LayoutParams(-1,-2))}
 private fun waitCard(parent:LinearLayout,title:String,key:String){val box=LinearLayout(this).apply{orientation=1;background=solid(bg,20);setPadding(dp(14),dp(14),dp(14),dp(12))}
  box.addView(TextView(this).apply{text=title;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)});space(box,10)
  val wheels=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_LTR;gravity=Gravity.CENTER}
  fun picker(max:Int,value:Int)=NumberPicker(this).apply{minValue=0;maxValue=max;this.value=value;setFormatter{v->String.format(Locale.US,"%02d",v)};wrapSelectorWheel=true;descendantFocusability=NumberPicker.FOCUS_BLOCK_DESCENDANTS;try{val f=NumberPicker::class.java.getDeclaredField("mSelectionDivider");f.isAccessible=true;f.set(this,null);val paint=NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint");paint.isAccessible=true;(paint.get(this) as? Paint)?.color=ink}catch(_:Exception){}}
  val hours=picker(24,p.getInt(key+"_h",6));val minutes=picker(59,p.getInt(key+"_m",0))
  val hourCol=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;addView(hours,LinearLayout.LayoutParams(dp(84),dp(100)));addView(TextView(this@MainActivity).apply{text="שעות";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER})}
  val minCol=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;addView(minutes,LinearLayout.LayoutParams(dp(84),dp(100)));addView(TextView(this@MainActivity).apply{text="דקות";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER})}
  wheels.addView(hourCol);wheels.addView(TextView(this).apply{text=":";textSize=25f;typeface=Typeface.DEFAULT_BOLD;setTextColor(copper);gravity=Gravity.CENTER;setPadding(0,0,0,dp(16))},LinearLayout.LayoutParams(dp(28),dp(100)));wheels.addView(minCol);box.addView(wheels,LinearLayout.LayoutParams(-1,-2))
  val summary=TextView(this).apply{text=durationText(hours.value,minutes.value);textSize=12f;setTextColor(muted);gravity=Gravity.CENTER;setPadding(0,dp(8),0,0)}
  fun update(){summary.text=durationText(hours.value,minutes.value)}
  hours.setOnValueChangedListener{_,_,v->p.edit().putInt(key+"_h",v).apply();update()};minutes.setOnValueChangedListener{_,_,v->p.edit().putInt(key+"_m",v).apply();update()};box.addView(summary);parent.addView(box,LinearLayout.LayoutParams(-1,-2))
 }
 private fun switchRow(parent:LinearLayout,g:String,title:String,sub:String,key:String,def:Boolean){val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(0,dp(10),0,dp(10))}
  row.addView(TextView(this).apply{text=g;textSize=23f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(40),dp(44)))
  row.addView(LinearLayout(this).apply{orientation=1;addView(TextView(this@MainActivity).apply{text=title;textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)});addView(TextView(this@MainActivity).apply{text=sub;textSize=12f;setTextColor(muted);setPadding(0,dp(3),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
  row.addView(SwitchCompat(this).apply{isChecked=p.getBoolean(key,def);thumbTintList=android.content.res.ColorStateList.valueOf(if(isChecked)copper else muted);trackTintList=android.content.res.ColorStateList.valueOf(if(isChecked)c(if(dark)"#805C3C" else "#E8C9A6") else c(if(dark)"#51443A" else "#E3D9CF"));setOnCheckedChangeListener{_,v->p.edit().putBoolean(key,v).apply();if(key=="persistent")persistent();if(key=="vibrate")channel()}},LinearLayout.LayoutParams(dp(54),dp(48)));parent.addView(row)}
 private fun settingRow(parent:LinearLayout,g:String,title:String,sub:String,end:String,action:()->Unit){val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(0,dp(12),0,dp(12));setOnClickListener{action()}}
  row.addView(TextView(this).apply{text=g;textSize=23f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(40),dp(44)))
  row.addView(LinearLayout(this).apply{orientation=1;addView(TextView(this@MainActivity).apply{text=title;textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)});addView(TextView(this@MainActivity).apply{text=sub;textSize=12f;setTextColor(muted);setPadding(0,dp(3),0,0)})},LinearLayout.LayoutParams(0,-2,1f))
  row.addView(TextView(this).apply{text=end;textSize=27f;setTextColor(muted);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(26),dp(40)));parent.addView(row)}
 private fun divider(parent:LinearLayout){parent.addView(View(this).apply{setBackgroundColor(c(if(dark)"#45362B" else "#F0E7DC"))},LinearLayout.LayoutParams(-1,dp(1)))}
 private fun primaryButton(title:String,action:()->Unit)=TextView(this).apply{text=title;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.WHITE);gravity=Gravity.CENTER;background=solid(copper,20);elevation=dp(2).toFloat();setOnClickListener{action()}}
 private fun outlineButton(title:String,stroke:Int,action:()->Unit)=TextView(this).apply{text=title;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(stroke);gravity=Gravity.CENTER;background=GradientDrawable().apply{cornerRadius=dp(20).toFloat();setColor(card);setStroke(dp(1),stroke)};setOnClickListener{action()}}
 private fun permissionScreen(){val r=root();space(r,60);label(r,"♧",60f,copper,true);space(r,22);label(r,"כדי שהטיימר יעבוד בצורה אמינה",25f,ink,true);space(r,12);label(r,"אפשרו התראות כדי לקבל עדכון כשההמתנה מסתיימת. אפשר לשנות זאת בכל עת בהגדרות המכשיר.",16f,muted);space(r,30)
  r.addView(primaryButton("המשך"){if(Build.VERSION.SDK_INT>=33)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),3);draw()},LinearLayout.LayoutParams(-1,dp(56)));space(r,12);r.addView(outlineButton("אחר כך",muted){draw()},LinearLayout.LayoutParams(-1,dp(52)))}
 private fun durationText(h:Int,m:Int)=when{h==0->"$m דקות";m==0->"$h שעות";else->"$h שעות ו־$m דקות"}
 private class RingView(ctx:Context,private val accent:Int):View(ctx){private val track=Paint(3).apply{style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;color=if((ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES)Color.rgb(69,54,43) else Color.rgb(240,227,213)};private val paint=Paint(3).apply{style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;color=accent};private val rect=RectF();private var value=0f
  fun setProgress(v:Float){value=v.coerceIn(0f,1f);invalidate()}
  override fun onDraw(canvas:Canvas){super.onDraw(canvas);val sw=resources.displayMetrics.density*10f;val pad=sw/2+resources.displayMetrics.density*3;rect.set(pad,pad,width-pad,height-pad);track.strokeWidth=sw;paint.strokeWidth=sw;canvas.drawArc(rect,-90f,360f,false,track);canvas.drawArc(rect,-90f,360f*value,false,paint)}
 }
 private fun schedule(){val end=p.getLong("end",0);val am=getSystemService(AlarmManager::class.java);val pi=PendingIntent.getBroadcast(this,1,Intent(this,TimerReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,pi)else am.setExact(AlarmManager.RTC_WAKEUP,end,pi)}catch(_:SecurityException){am.set(AlarmManager.RTC_WAKEUP,end,pi)};persistent()}
 private fun persistent(){if(!p.getBoolean("persistent",true)||p.getLong("end",0)<=System.currentTimeMillis()){getSystemService(NotificationManager::class.java).cancel(1);return};val open=PendingIntent.getActivity(this,10,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val n=NotificationCompat.Builder(this,p.getString("channel","parve_default")!!).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("אני פרווה בעוד").setContentText("הטיימר פעיל").setWhen(p.getLong("end",0)).setUsesChronometer(true).setChronometerCountDown(true).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open).build();try{NotificationManagerCompat.from(this).notify(1,n)}catch(_:SecurityException){}}
 private fun notifyDone(){getSystemService(NotificationManager::class.java).cancel(1);val open=PendingIntent.getActivity(this,11,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val n=NotificationCompat.Builder(this,p.getString("channel","parve_default")!!).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("אני פרווה").setContentText("זמן ההמתנה הסתיים").setAutoCancel(true).setContentIntent(open).build();try{NotificationManagerCompat.from(this).notify(2,n)}catch(_:SecurityException){}}
 private fun cancel(){tick?.cancel();getSystemService(AlarmManager::class.java).cancel(PendingIntent.getBroadcast(this,1,Intent(this,TimerReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE));getSystemService(NotificationManager::class.java).cancel(1);p.edit().remove("end").remove("duration").putBoolean("finished",false).apply()}
}