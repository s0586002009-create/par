package il.co.aniparve

import android.Manifest
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.text.format.DateFormat
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
 private var screen="home"; private var tick:CountDownTimer?=null; private val uiHandler=Handler(Looper.getMainLooper()); private var uiTick:Runnable?=null
 private var clock:TextView?=null; private var pct:TextView?=null; private var ring:RingView?=null; private var bar:ProgressBar?=null
 private val dark get()=when(p.getString("theme","system")){"dark"->true;"light"->false;else->(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES}
 private fun c(s:String)=Color.parseColor(s)
 private val bg get()=c(if(dark)"#1B1410" else "#FFF8EC")
 private val card get()=c(if(dark)"#2A201A" else "#FFFFFF")
 private val ink get()=c(if(dark)"#F5E9DA" else "#3B2314")
 private val muted get()=c(if(dark)"#C7B5A3" else "#8D7766")
 private val copper get()=c("#B26A2B"); private val red get()=c(if(dark)"#F0645D" else "#D9433C"); private val blue get()=c(if(dark)"#80B9EA" else "#4F8BC8")
 override fun onCreate(savedInstanceState:Bundle?){applyTheme();super.onCreate(savedInstanceState);channel()
  TimerStore.all(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this)
  screen=when{intent.getBooleanExtra("show_timers",false)->"timer";TimerStore.all(this).any{it.status=="pending"}->"done";TimerStore.all(this).any{it.status=="active"}->"timer";p.getBoolean("finished",false)->"done";else->"home"}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permissionScreen() else {draw();if(intent.getBooleanExtra("open_start_sheet",false))window.decorView.post{startDialog(intent.getStringExtra("start_type")?:"meat")};if(!p.getBoolean("welcome_shown",false)){p.edit().putBoolean("welcome_shown",true).apply();AlertDialog.Builder(this).setTitle("ברוכים הבאים").setMessage("אני פרווה עוזרת לעקוב אחרי זמן ההמתנה.").setPositiveButton("הבנתי",null).setNeutralButton("איך זה עובד"){_,_->helpScreen()}.show()}}
 }
 override fun onPause(){super.onPause();uiTick?.let{uiHandler.removeCallbacks(it)};tick?.cancel()}
 override fun onResume(){super.onResume();if(screen=="timer")draw()}
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
 private fun home(){val r=root();toolbar(r);val activeCount=TimerStore.all(this).count{it.status=="active"};if(activeCount>0){space(r,8);r.addView(outlineButton("יש "+activeCount+" טיימרים פעילים  ›",copper){screen="timer";draw()},LinearLayout.LayoutParams(-1,dp(40)))};space(r,24);label(r,"מה סיימת לאכול?",32f,ink,true);space(r,6);label(r,"בחרו כדי להתחיל טיימר",16f,muted);space(r,28)
  val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  row.addView(food("🧀","חלבי","dairy"),LinearLayout.LayoutParams(0,dp(228),1f).apply{marginEnd=dp(8)});row.addView(food("🥩","בשרי","meat"),LinearLayout.LayoutParams(0,dp(228),1f).apply{marginStart=dp(8)});r.addView(row)
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
  isClickable=true;setOnClickListener{startDialog(key)}
  setOnTouchListener{v,e->when(e.action){MotionEvent.ACTION_DOWN->v.animate().scaleX(.97f).scaleY(.97f).setDuration(90).start();MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()};false}
 }
 private fun panel(r:LinearLayout,content:LinearLayout.()->Unit){val box=LinearLayout(this).apply{orientation=1;background=solid(card,22);elevation=dp(2).toFloat();setPadding(dp(16),dp(14),dp(16),dp(14));content()};r.addView(box,LinearLayout.LayoutParams(-1,-2))}
 private fun startDialog(type:String){
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(8),dp(18),dp(4));layoutDirection=View.LAYOUT_DIRECTION_RTL}
  val name=EditText(this).apply{hint="שם אחר (לא חובה)";setSingleLine(true)}
  box.addView(name,LinearLayout.LayoutParams(-1,dp(48)))
  val people=TimerStore.people(this);var person:ParvePerson?=null
  if(people.isNotEmpty()){space(box,6);label(box,"בחירת שם קבוע",13f,muted,true,Gravity.START);val chips=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_RTL}
   people.forEach{pp->chips.addView(outlineButton(pp.name,copper){person=pp;name.setText(pp.name)},LinearLayout.LayoutParams(-2,dp(40)).apply{marginEnd=dp(5)})}
   box.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(chips)},LinearLayout.LayoutParams(-1,dp(45)))
  }
  space(box,8);label(box,"סיימתי לאכול לפני...",14f,ink,true,Gravity.START)
  val opts=listOf("עכשיו" to 0L,"15 דקות" to 15L,"30 דקות" to 30L,"שעה" to 60L,"שעה וחצי" to 90L,"שעתיים" to 120L,"3 שעות" to 180L,"4 שעות" to 240L,"6 שעות" to 360L)
  var elapsed=0L
  val row=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_RTL}
  opts.forEach{(title,mins)->row.addView(outlineButton(title,copper){elapsed=mins*60000L},LinearLayout.LayoutParams(-2,dp(38)).apply{marginEnd=dp(4)})}
  val other=outlineButton("אחר",copper){val h=WheelPicker(this,"שעות").apply{minValue=0;maxValue=12};val m=WheelPicker(this,"דקות").apply{minValue=0;maxValue=59};val w=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_LTR;addView(h,LinearLayout.LayoutParams(0,dp(176),1f));addView(m,LinearLayout.LayoutParams(0,dp(176),1f))};AlertDialog.Builder(this).setTitle("כמה זמן עבר?").setView(w).setPositiveButton("אישור"){_,_->elapsed=(h.value*60L+m.value)*60000L}.setNegativeButton("ביטול",null).show()}
  row.addView(other);box.addView(HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;addView(row)},LinearLayout.LayoutParams(-1,dp(46)))
  val dlg=AlertDialog.Builder(this).setTitle(if(type=="meat")"סיימתי לאכול בשרי" else "סיימתי לאכול חלבי").setView(box).setNegativeButton("ביטול",null).setPositiveButton("התחל",null).create()
  dlg.setOnShowListener{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
   val n=name.text.toString().trim().ifBlank{person?.name?:"אני"};val dur=(if(type=="meat")person?.meatWait else person?.dairyWait)?:TimerStore.duration(this,type)
   if(elapsed>=dur){Toast.makeText(this,"כבר עברו יותר מ-"+durationText((dur/3600000).toInt(),((dur%3600000)/60000).toInt())+", אתה פרווה",Toast.LENGTH_LONG).show();dlg.dismiss();return@setOnClickListener}
   if(TimerStore.all(this).count{it.status=="active"}>=10){Toast.makeText(this,"אפשר להפעיל עד 10 טיימרים במקביל",Toast.LENGTH_SHORT).show();dlg.dismiss();return@setOnClickListener}
   val now=System.currentTimeMillis();val st=now-elapsed;val t=ParveTimer(TimerStore.newId(this),n,type,st,st+dur,dur,"active",if(elapsed>0)st else 0L)
   TimerStore.add(this,t);TimerScheduler.schedule(this,t);p.edit().putLong("end",t.endAt).putLong("duration",t.duration).putString("food",type).putBoolean("finished",false).apply()
   TimerNotifications.refresh(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this);dlg.dismiss();screen="timer";draw()
  }}
  dlg.show()
 }
 private fun peopleDialog(){val people=TimerStore.people(this);val names=people.map{it.name}.toMutableList().apply{add("+ הוספת שם")}.toTypedArray()
  AlertDialog.Builder(this).setTitle("אנשים").setItems(names){_,which->
   if(which==names.lastIndex){editPerson(null)}
   else {val person=people[which];AlertDialog.Builder(this).setTitle(person.name).setItems(arrayOf("עריכה","מחיקה")){_,action->if(action==0)editPerson(person)else AlertDialog.Builder(this).setMessage("למחוק את השם "+person.name+"?").setNegativeButton("ביטול",null).setPositiveButton("מחיקה"){_,_->TimerStore.savePeople(this,TimerStore.people(this).filterNot{it.name==person.name});settings()}.show()}.show()}
  }.setNegativeButton("סגירה",null).show()
 }
 private fun editPerson(old:ParvePerson?){val box=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(6),dp(18),dp(4))};val name=EditText(this).apply{hint="שם";setSingleLine(true);setText(old?.name?:"")};box.addView(name)
  val sw=SwitchCompat(this).apply{text="זמנים אישיים";isChecked=old?.meatWait!=null||old?.dairyWait!=null};box.addView(sw)
  val row=LinearLayout(this).apply{orientation=0;visibility=if(sw.isChecked)View.VISIBLE else View.GONE}
  fun pick(v:Long)=WheelPicker(this,"שעות ודקות").apply{minValue=0;maxValue=360;value=(v/60000).toInt().coerceIn(0,360);setFormatter{String.format(Locale.getDefault(),"%02d:%02d",it/60,it%60)}}
  val meat=pick(old?.meatWait?:TimerStore.duration(this,"meat"));val dairy=pick(old?.dairyWait?:TimerStore.duration(this,"dairy"))
  row.addView(LinearLayout(this).apply{orientation=1;addView(TextView(this@MainActivity).apply{text="בשרי ← חלבי";setTextColor(ink)});addView(meat)});row.addView(LinearLayout(this).apply{orientation=1;addView(TextView(this@MainActivity).apply{text="חלבי ← בשרי";setTextColor(ink)});addView(dairy)});box.addView(row);sw.setOnCheckedChangeListener{_,v->row.visibility=if(v)View.VISIBLE else View.GONE}
  AlertDialog.Builder(this).setTitle(if(old==null)"הוספת שם" else "עריכת שם").setView(box).setNegativeButton("ביטול",null).setPositiveButton("שמירה"){_,_->val n=name.text.toString().trim();if(n.isBlank())return@setPositiveButton;val list=TimerStore.people(this).filterNot{it.name==old?.name}.toMutableList();if(list.any{it.name.equals(n,true)}){Toast.makeText(this,"השם כבר קיים",Toast.LENGTH_SHORT).show();return@setPositiveButton};list.add(ParvePerson(n,if(sw.isChecked)meat.value*60000L else null,if(sw.isChecked)dairy.value*60000L else null));TimerStore.savePeople(this,list);settings()}.show()
 }
 private fun pinWidget(){if(Build.VERSION.SDK_INT>=26){val m=AppWidgetManager.getInstance(this);if(m.isRequestPinAppWidgetSupported){m.requestPinAppWidget(ComponentName(this,il.co.aniparve.widget.TimerWidgetProvider::class.java),null,null);return}};AlertDialog.Builder(this).setTitle("הוספת וידג'ט").setMessage("לחצו לחיצה ארוכה על מסך הבית ← וידג'טים ← אני פרווה").setPositiveButton("הבנתי",null).show()}
 private fun helpScreen(){val r=root();toolbar(r,"איך זה עובד");val items=listOf(R.string.help_start_title to R.string.help_start_body,R.string.help_forgot_title to R.string.help_forgot_body,R.string.help_multi_title to R.string.help_multi_body,R.string.help_end_title to R.string.help_end_body,R.string.help_people_title to R.string.help_people_body,R.string.help_wait_title to R.string.help_wait_body,R.string.help_stop_title to R.string.help_stop_body,R.string.help_notifications_title to R.string.help_notifications_body,R.string.help_widget_title to R.string.help_widget_body,R.string.help_delay_title to R.string.help_delay_body);items.forEach{(a,b)->space(r,8);panel(r){addView(TextView(this@MainActivity).apply{text=getString(a);textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(copper)});addView(TextView(this@MainActivity).apply{text=getString(b);textSize=13f;setTextColor(ink);setPadding(0,dp(5),0,0)})}};space(r,12);label(r,getString(R.string.help_disclaimer),11f,muted)}

 private fun timerScreen(){val active=TimerStore.all(this).filter{it.status=="active"}.sortedBy{it.endAt};if(active.size>1){multiTimersScreen(active);return};if(active.size==1){val t=active[0];p.edit().putLong("end",t.endAt).putLong("duration",t.duration).putBoolean("finished",false).putString("food",t.type).apply()};val r=root();toolbar(r);val meat=p.getString("food","meat")=="meat";val accent=if(meat)red else blue
  space(r,16);val activeName=active.firstOrNull()?.name?:"אני";label(r,if(activeName=="אני")"אני פרווה בעוד" else activeName+" פרווה בעוד",26f,ink,true);space(r,8);label(r,if(meat)"המתנה אחרי בשרי" else "המתנה אחרי חלבי",14f,accent,true);space(r,8)
  val box=FrameLayout(this);ring=RingView(this,accent);box.addView(ring,FrameLayout.LayoutParams(dp(270),dp(270),Gravity.CENTER))
  clock=TextView(this).apply{textSize=37f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink);gravity=Gravity.CENTER;includeFontPadding=false};box.addView(clock,FrameLayout.LayoutParams(-1,-1));r.addView(box,LinearLayout.LayoutParams(-1,dp(292)))
  bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progressTintList=android.content.res.ColorStateList.valueOf(accent);progressBackgroundTintList=android.content.res.ColorStateList.valueOf(if(dark)c("#45362B") else c("#F0E3D5"))}
  r.addView(bar,LinearLayout.LayoutParams(-1,dp(10)).apply{topMargin=dp(14)})
  pct=TextView(this).apply{textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(accent);gravity=Gravity.CENTER;setPadding(0,dp(12),0,dp(8))};r.addView(pct)
  val endLabel=TextView(this).apply{text=(active.firstOrNull()?.let{if(it.name=="אני")endPhrase(it.endAt) else it.name+" פרווה "+endPhrase(it.endAt).removePrefix("אהיה פרווה ")}?:"");textSize=14f;typeface=Typeface.DEFAULT_BOLD;setTextColor(copper);gravity=Gravity.CENTER;setPadding(0,0,0,dp(16))};r.addView(endLabel);active.firstOrNull()?.takeIf{it.ateAt>0}?.let{label(r,"סיימתי לאכול בשעה "+DateFormat.getTimeFormat(this).format(java.util.Date(it.ateAt)),12f,muted)}
  val actions=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR};actions.addView(outlineButton("עצור",red){val d=AlertDialog.Builder(this).setTitle("עצירת הטיימר").setMessage("להפסיק את הטיימר של "+(TimerStore.all(this).filter{it.status=="active"}.minByOrNull{it.endAt}?.name?:"אני")+"?").setNegativeButton("לא",null).setPositiveButton("כן"){_,_->cancel();screen="home";draw()}.create();d.setOnShowListener{d.getButton(-1)?.setTextColor(red);d.getButton(-2)?.setTextColor(ink)};d.show()},LinearLayout.LayoutParams(0,dp(54),1f).apply{marginEnd=dp(10)});actions.addView(primaryButton("+"){screen="home";draw()},LinearLayout.LayoutParams(dp(54),dp(54)));r.addView(actions,LinearLayout.LayoutParams(-1,-2))
  startTick()
 }
 private fun startTick(){val end=p.getLong("end",0);val duration=p.getLong("duration",1).coerceAtLeast(1)
  fun update(){val left=(end-System.currentTimeMillis()).coerceAtLeast(0);clock?.text=String.format(Locale.US,"%02d:%02d:%02d",TimeUnit.MILLISECONDS.toHours(left),TimeUnit.MILLISECONDS.toMinutes(left)%60,TimeUnit.MILLISECONDS.toSeconds(left)%60)
   val n=(left*100/duration).toInt().coerceIn(0,100);bar?.progress=n;ring?.setProgress(n/100f);pct?.text="$n% נותרו"
   if(left==0L){val active=TimerStore.all(this).firstOrNull{it.status=="active"&&it.endAt<=System.currentTimeMillis()};if(active!=null){val ended=TimerStore.pending(this,active.id);if(ended!=null)TimerNotifications.done(this,ended);TimerScheduler.cancel(this,active.id)};p.edit().putBoolean("finished",true).apply();TimerNotifications.refresh(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this);screen="done";doneScreen()}}
  update();if(screen=="timer")tick=object:CountDownTimer((end-System.currentTimeMillis()).coerceAtLeast(0),1000){override fun onTick(ms:Long)=update();override fun onFinish()=update()}.start()
 }
 private fun multiTimersScreen(active:List<ParveTimer>){val r=root();toolbar(r,"הטיימרים");space(r,12);label(r,"הטיימרים הפעילים שלך",22f,ink,true)
  active.sortedBy{it.endAt}.forEach{t->space(r,10);panel(r){orientation=1
   val row=LinearLayout(this@MainActivity).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
   row.addView(TextView(this@MainActivity).apply{text=if(t.type=="meat")"🥩" else "🧀";textSize=24f},LinearLayout.LayoutParams(dp(38),dp(40)))
   row.addView(LinearLayout(this@MainActivity).apply{orientation=1
    addView(TextView(this@MainActivity).apply{text=t.name;textSize=17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)})
    addView(TextView(this@MainActivity).apply{text=if(t.type=="meat")"אחרי בשרי" else "אחרי חלבי";textSize=12f;setTextColor(if(t.type=="meat")red else blue)})
   },LinearLayout.LayoutParams(0,-2,1f))
   row.addView(TextView(this@MainActivity).apply{text=leftText(t.endAt);tag="left_"+t.id;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)})
   addView(row)
   val pct=((t.endAt-System.currentTimeMillis()).coerceAtLeast(0L)*100L/t.duration.coerceAtLeast(1L)).toInt().coerceIn(0,100)
   val bar=ProgressBar(this@MainActivity,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progress=pct;tag="bar_"+t.id;progressTintList=android.content.res.ColorStateList.valueOf(if(t.type=="meat")red else blue)}
   addView(bar,LinearLayout.LayoutParams(-1,dp(8)).apply{topMargin=dp(8)})
   addView(TextView(this@MainActivity).apply{text=pct.toString()+"% נותרו · "+(if(t.name=="אני")endPhrase(t.endAt) else t.name+" פרווה "+endPhrase(t.endAt).removePrefix("אהיה פרווה "));textSize=12f;tag="pct_"+t.id;setTextColor(muted);setPadding(0,dp(7),0,dp(8))})
   if(t.ateAt>0)addView(TextView(this@MainActivity).apply{text="סיימתי לאכול בשעה "+DateFormat.getTimeFormat(this@MainActivity).format(java.util.Date(t.ateAt));textSize=11f;setTextColor(muted)})
   addView(outlineButton("עצור",red){confirmStop(t)},LinearLayout.LayoutParams(-1,dp(42)))
  }}
  space(r,16);r.addView(primaryButton("+ טיימר נוסף"){screen="home";draw()},LinearLayout.LayoutParams(-1,dp(52)))
  uiTick?.let{uiHandler.removeCallbacks(it)}
  val task=object:Runnable{override fun run(){if(screen!="timer")return
   val now=System.currentTimeMillis()
   val ended=active.filter{it.endAt<=now}
   if(ended.isNotEmpty()){ended.forEach{TimerStore.pending(this@MainActivity,it.id)?.let{done->TimerScheduler.cancel(this@MainActivity,done.id);TimerNotifications.done(this@MainActivity,done)}};TimerNotifications.refresh(this@MainActivity);il.co.aniparve.widget.WidgetUpdater.updateAll(this@MainActivity);screen=if(TimerStore.all(this@MainActivity).any{it.status=="pending"})"done" else if(TimerStore.all(this@MainActivity).any{it.status=="active"})"timer" else "home";draw();return}
   active.forEach{t->val v=findViewWithTag<TextView>("left_"+t.id);if(v!=null)v.text=leftText(t.endAt);val progress=((t.endAt-now).coerceAtLeast(0L)*100L/t.duration.coerceAtLeast(1L)).toInt().coerceIn(0,100);findViewWithTag<ProgressBar>("bar_"+t.id)?.progress=progress;findViewWithTag<TextView>("pct_"+t.id)?.text=progress.toString()+"% נותרו · "+(if(t.name=="אני")endPhrase(t.endAt) else t.name+" פרווה "+endPhrase(t.endAt).removePrefix("אהיה פרווה "))}
   uiHandler.postDelayed(this,1000)
  }}
  uiTick=task;uiHandler.postDelayed(task,1000)
 }
 private fun leftText(end:Long):String{val ms=(end-System.currentTimeMillis()).coerceAtLeast(0L);return String.format(Locale.getDefault(),"%02d:%02d:%02d",TimeUnit.MILLISECONDS.toHours(ms),TimeUnit.MILLISECONDS.toMinutes(ms)%60,TimeUnit.MILLISECONDS.toSeconds(ms)%60)}
 private fun endPhrase(end:Long):String{val now=java.util.Date();val date=java.util.Date(end);val f=java.text.SimpleDateFormat("yyyyMMdd",Locale.getDefault());val day=when{f.format(now)==f.format(date)->"";f.format(java.util.Date(now.time+86400000L))==f.format(date)->"מחר ";else->"ביום "+java.text.SimpleDateFormat("EEEE",Locale.getDefault()).format(date)+" "};return (if(day.isBlank())"אהיה פרווה " else day+"אהיה פרווה ")+"בשעה "+DateFormat.getTimeFormat(this).format(date)}
 private fun <T:View> findViewWithTag(tag:Any):T?{fun find(v:View):View?{if(v.tag==tag)return v;if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i))?.let{return it};return null};return find(window.decorView) as? T}
 private fun confirmStop(t:ParveTimer){AlertDialog.Builder(this).setTitle("עצירת טיימר").setMessage("להפסיק את הטיימר של "+t.name+"?").setNegativeButton("לא",null).setPositiveButton("עצור"){_,_->TimerScheduler.cancel(this,t.id);TimerStore.remove(this,t.id);TimerNotifications.refresh(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this);screen=if(TimerStore.all(this).any{it.status=="active"})"timer" else if(TimerStore.all(this).any{it.status=="pending"})"done" else "home";draw()}.show()}
 private fun doneScreen(){val r=root();toolbar(r,"הסתיים");space(r,20);label(r,"זמן ההמתנה הסתיים",24f,ink,true)
  val pending=TimerStore.all(this).filter{it.status=="pending"}.sortedBy{it.endAt}
  pending.forEach{t->space(r,12);panel(r){orientation=0;gravity=Gravity.CENTER_VERTICAL
   addView(TextView(this@MainActivity).apply{text="✓";textSize=25f;setTextColor(copper)},LinearLayout.LayoutParams(dp(36),dp(42)))
   addView(LinearLayout(this@MainActivity).apply{orientation=1
    addView(TextView(this@MainActivity).apply{text=if(t.name=="אני")"אני פרווה" else t.name+" פרווה";textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)})
    addView(TextView(this@MainActivity).apply{text=endPhrase(t.endAt);textSize=12f;setTextColor(muted)})
   },LinearLayout.LayoutParams(0,-2,1f))
   addView(primaryButton("אישור"){TimerStore.remove(this@MainActivity,t.id);TimerScheduler.cancel(this@MainActivity,t.id);TimerNotifications.clear(this@MainActivity,t.id);il.co.aniparve.widget.WidgetUpdater.updateAll(this@MainActivity);val left=TimerStore.all(this@MainActivity);screen=if(left.any{it.status=="pending"})"done" else if(left.any{it.status=="active"})"timer" else "home";draw()})
  }}
  if(pending.isEmpty()){space(r,20);r.addView(primaryButton("חזרה"){screen=if(TimerStore.all(this).any{it.status=="active"})"timer" else "home";draw()},LinearLayout.LayoutParams(-1,dp(52)))}
 }
 private fun settings(){val r=root();toolbar(r,"הגדרות");space(r,12)
  section(r,"◷","זמני המתנה"){waitCard(this,"בשרי  ←  חלבי","meat");space(this,12);waitCard(this,"חלבי  ←  בשרי","dairy")}
  space(r,14);section(r,"♧","אנשים"){settingRow(this,"♧","ניהול שמות קבועים","שמות קבועים וזמני המתנה אישיים","›"){peopleDialog()}}
  space(r,14);section(r,"♫","התראות"){switchRow(this,"◉","רטט בהתראה","רטט בעת סיום ההמתנה","vibrate",true);divider(this);switchRow(this,"▣","התראה קבועה בזמן הספירה","הצגת הטיימר בשורת ההתראות","persistent",true);divider(this);settingRow(this,"♫","צליל התראה",if(p.getString("sound","default")=="silent")"שקט" else "ברירת מחדל","›"){AlertDialog.Builder(this@MainActivity).setTitle("צליל התראה").setItems(arrayOf("שקט","ברירת מחדל")){_,i->p.edit().putString("sound",if(i==0)"silent" else "default").apply();channel();TimerNotifications.refresh(this@MainActivity);settings()}.show()}}
  space(r,14);section(r,"▣","וידג'ט למסך הבית"){settingRow(this,"▣","הוספה למסך הבית","צפייה בטיימר בלי לפתוח את האפליקציה","›"){pinWidget()}}
  space(r,14);section(r,"☼","מראה"){val current=p.getString("theme","system");val row=LinearLayout(this@MainActivity).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER}
   listOf("system" to "לפי המערכת","light" to "בהיר","dark" to "כהה").forEach{(key,name)->val selected=current==key;row.addView(TextView(this@MainActivity).apply{text=name;textSize=13f;gravity=Gravity.CENTER;setTextColor(if(selected)Color.WHITE else ink);background=solid(if(selected)copper else bg,22);setPadding(dp(8),dp(12),dp(8),dp(12));setOnClickListener{p.edit().putString("theme",key).apply();applyTheme();screen="settings";recreate()}},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(4);marginEnd=dp(4)})};addView(row)}
  space(r,14);section(r,"⚙","הרשאות ואמינות"){settingRow(this,"♧","הגדרות התראות וחיסכון בסוללה","ניהול הרשאות והתנהגות ברקע","›"){try{if(Build.VERSION.SDK_INT>=31)startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))else startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")));if(Build.VERSION.SDK_INT>=23&&!getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName))startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:$packageName")))}catch(_:Exception){}}}
  space(r,14);section(r,"?","איך זה עובד"){settingRow(this,"?","פתיחת מסך ההסבר","הוראות קצרות לשימוש","›"){helpScreen()}}
 } private fun section(r:LinearLayout,g:String,title:String,content:LinearLayout.()->Unit){val box=LinearLayout(this).apply{orientation=1;background=solid(card,24);elevation=dp(2).toFloat();setPadding(dp(18),dp(16),dp(18),dp(16))}
  val h=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;addView(TextView(this@MainActivity).apply{text=g;textSize=22f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(34),dp(36)));addView(TextView(this@MainActivity).apply{text=title;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(copper)})};box.addView(h);space(box,8);box.content();r.addView(box,LinearLayout.LayoutParams(-1,-2))}
 private fun waitCard(parent:LinearLayout,title:String,key:String){val box=LinearLayout(this).apply{orientation=1;background=solid(bg,20);setPadding(dp(14),dp(14),dp(14),dp(12))}
  box.addView(TextView(this).apply{text=title;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)});space(box,10)
  val wheels=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_LTR;gravity=Gravity.CENTER}
  fun picker(max:Int,value:Int)=WheelPicker(this).apply{minValue=0;maxValue=max;this.value=value;setFormatter{v->String.format(Locale.US,"%02d",v)}}
  val hours=picker(6,p.getInt(key+"_h",6));val minutes=picker(59,p.getInt(key+"_m",0))
  val hourCol=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;addView(hours,LinearLayout.LayoutParams(dp(84),dp(176)));addView(TextView(this@MainActivity).apply{text="שעות";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER})}
  val minCol=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;addView(minutes,LinearLayout.LayoutParams(dp(84),dp(176)));addView(TextView(this@MainActivity).apply{text="דקות";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER})}
  wheels.addView(hourCol);wheels.addView(TextView(this).apply{text=":";textSize=25f;typeface=Typeface.DEFAULT_BOLD;setTextColor(copper);gravity=Gravity.CENTER;setPadding(0,0,0,dp(16))},LinearLayout.LayoutParams(dp(28),dp(176)));wheels.addView(minCol);box.addView(wheels,LinearLayout.LayoutParams(-1,-2))
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
 private fun schedule(){TimerStore.all(this).filter{it.status=="active"}.forEach{TimerScheduler.schedule(this,it)};TimerNotifications.refresh(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this)}
 private fun persistent(){TimerNotifications.refresh(this)}
 private fun cancel(){tick?.cancel();val t=TimerStore.all(this).filter{it.status=="active"}.minByOrNull{it.endAt};if(t!=null){TimerScheduler.cancel(this,t.id);TimerStore.remove(this,t.id)};TimerNotifications.refresh(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this);p.edit().remove("end").remove("duration").putBoolean("finished",false).apply()}

}