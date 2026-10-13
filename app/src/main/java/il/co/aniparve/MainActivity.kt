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
import android.text.TextWatcher
import android.text.Editable
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
 private var toolbarHost:LinearLayout?=null
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
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permissionScreen() else {draw();if(intent.getBooleanExtra("open_type_picker",false))window.decorView.post{chooseTimerType()}else if(intent.getBooleanExtra("open_start_sheet",false))window.decorView.post{startDialog(intent.getStringExtra("start_type")?:"meat")};if(!p.getBoolean("welcome_shown",false)){p.edit().putBoolean("welcome_shown",true).apply();AlertDialog.Builder(this).setTitle("ברוכים הבאים").setMessage("אני פרווה עוזרת לעקוב אחרי זמן ההמתנה.").setPositiveButton("הבנתי",null).setNeutralButton("איך זה עובד"){_,_->helpScreen()}.show()}}
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
 private fun root():LinearLayout{
 val outer=LinearLayout(this).apply{orientation=1;background=solid(bg,0);layoutDirection=View.LAYOUT_DIRECTION_RTL}
 val fixed=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(6),dp(20),dp(4));background=solid(bg,0);elevation=dp(3).toFloat()}
 toolbarHost=fixed
 val body=LinearLayout(this).apply{orientation=1;setPadding(dp(20),dp(12),dp(20),dp(24));background=solid(bg,0);layoutDirection=View.LAYOUT_DIRECTION_RTL}
 val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false;addView(body,ViewGroup.LayoutParams(-1,-2));background=solid(bg,0)}
 outer.addView(fixed,LinearLayout.LayoutParams(-1,-2));outer.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(outer)
 body.alpha=0f;body.animate().alpha(1f).setDuration(220).setInterpolator(AccelerateDecelerateInterpolator()).start();return body
}
 private fun space(parent:LinearLayout,n:Int){parent.addView(Space(this),LinearLayout.LayoutParams(1,dp(n)))}
 private fun label(parent:LinearLayout,s:String,size:Float,color:Int,bold:Boolean=false,gravity:Int=Gravity.CENTER){parent.addView(TextView(this).apply{text=s;textSize=size;setTextColor(color);this.gravity=gravity;typeface=if(bold)Typeface.create("sans-serif-medium",Typeface.BOLD) else Typeface.create("sans-serif",Typeface.NORMAL)},LinearLayout.LayoutParams(-1,-2))}
 private fun toolbar(r:LinearLayout,title:String?=null){val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  val isSettings=title=="הגדרות";row.addView(iconBtn(if(isSettings)"‹" else "⚙",if(isSettings)"חזרה" else "הגדרות"){if(isSettings){screen=if(p.getBoolean("finished",false))"done" else if(p.getLong("end",0)>System.currentTimeMillis())"timer" else "home";draw()}else{screen="settings";draw()}},LinearLayout.LayoutParams(dp(48),dp(48)))
  val brand=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_RTL}
  brand.addView(ImageView(this).apply{try{setImageResource(R.mipmap.ic_launcher)}catch(_:Exception){};scaleType=ImageView.ScaleType.FIT_CENTER},LinearLayout.LayoutParams(dp(34),dp(34)).apply{marginStart=dp(7)})
  brand.addView(TextView(this).apply{text=title?:"אני פרווה";textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink);gravity=Gravity.CENTER_VERTICAL})
  row.addView(brand,LinearLayout.LayoutParams(0,dp(48),1f));row.addView(Space(this),LinearLayout.LayoutParams(dp(48),dp(48)));(toolbarHost?:r).addView(row,LinearLayout.LayoutParams(-1,dp(52)))
 }
 private fun iconBtn(g:String,desc:String,action:()->Unit)=TextView(this).apply{text=g;contentDescription=desc;textSize=26f;gravity=Gravity.CENTER;setTextColor(ink);background=solid(card,18);elevation=dp(2).toFloat();isClickable=true;setOnClickListener{action()}}.also{}
 private fun home(){val r=root();toolbar(r);val activeCount=TimerStore.all(this).count{it.status=="active"};if(activeCount>0){space(r,8);r.addView(outlineButton("יש "+activeCount+" טיימרים פעילים  ›",copper){screen="timer";draw()},LinearLayout.LayoutParams(-1,dp(40)))};space(r,24);r.addView(TextView(this).apply{text="מה סיימת לאכול?";textSize=22f;maxLines=1;isSingleLine=true;if(Build.VERSION.SDK_INT>=26)setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);setTextColor(ink);typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,-2));space(r,6);label(r,"בחרו כדי להתחיל טיימר",16f,muted);space(r,28)
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
 private fun chooseTimerType(){AlertDialog.Builder(this).setTitle("הוספת טיימר").setItems(arrayOf("בשרי","חלבי")){_,which->startDialog(if(which==0)"meat" else "dairy")}.show()}
 private fun startDialog(type:String){
  val box=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(8),dp(18),dp(4));layoutDirection=View.LAYOUT_DIRECTION_RTL}
  val name=EditText(this).apply{hint="שם אחר (לא חובה)";setSingleLine(true);maxLines=1}
  box.addView(name,LinearLayout.LayoutParams(-1,dp(48)))
  val saveName=CheckBox(this).apply{text="שמור לפעמים הבאות";textSize=13f;setTextColor(ink);visibility=View.GONE}
  box.addView(saveName,LinearLayout.LayoutParams(-1,dp(40)))
  fun updateSaveName(){val entered=name.text.toString().trim();saveName.visibility=if(entered.isNotEmpty() && TimerStore.people(this).none{it.name.equals(entered,true)})View.VISIBLE else View.GONE;if(saveName.visibility==View.GONE)saveName.isChecked=false}
  name.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){updateSaveName()};override fun afterTextChanged(s:Editable?) {}})
  val people=TimerStore.people(this);var person:ParvePerson?=null
  if(people.isNotEmpty()){space(box,6);label(box,"בחירת שם קבוע",13f,muted,true,Gravity.START)
   val chips=FlowChipLayout(this).apply{layoutDirection=View.LAYOUT_DIRECTION_RTL}
   val personButtons=mutableListOf<Pair<TextView,ParvePerson>>()
   people.forEach{pp->val b=outlineButton(pp.name,copper){person=pp;name.setText(pp.name);personButtons.forEach{(v,p0)->v.background=chipBackground(p0==pp);v.setTextColor(if(p0==pp)Color.WHITE else copper)}}
    b.textSize=13f;b.maxLines=1;b.isSingleLine=true;b.gravity=Gravity.CENTER;b.setPadding(dp(16),dp(10),dp(16),dp(10));chips.addView(b,FlowChipLayout.LayoutParams(-2,dp(42)));personButtons.add(b to pp)}
   box.addView(chips,LinearLayout.LayoutParams(-1,-2))
  }
  space(box,8);label(box,"סיימתי לאכול לפני...",14f,ink,true,Gravity.START)
  val opts=listOf("עכשיו" to 0L,"5 דקות" to 5L,"10 דקות" to 10L,"15 דקות" to 15L,"20 דקות" to 20L,"30 דקות" to 30L,"שעה" to 60L,"שעה וחצי" to 90L,"שעתיים" to 120L,"3 שעות" to 180L)
  var elapsed=0L
  val row=FlowChipLayout(this).apply{layoutDirection=View.LAYOUT_DIRECTION_RTL}
  val elapsedButtons=mutableListOf<Pair<TextView,Long>>()
  fun selectElapsed(value:Long){elapsed=value;elapsedButtons.forEach{(v,m)->v.background=chipBackground(m==value);v.setTextColor(if(m==value)Color.WHITE else copper)}}
  opts.forEach{(title,mins)->val b=outlineButton(title,copper){selectElapsed(mins*60000L)};b.textSize=13f;b.maxLines=1;b.isSingleLine=true;b.gravity=Gravity.CENTER;b.setPadding(dp(16),dp(10),dp(16),dp(10));row.addView(b,FlowChipLayout.LayoutParams(-2,dp(42)));elapsedButtons.add(b to mins*60000L)}
  val other=outlineButton("אחר",copper){showElapsedPicker{selectElapsed(it)}}
  other.textSize=13f;other.maxLines=1;other.isSingleLine=true;other.gravity=Gravity.CENTER;other.setPadding(dp(16),dp(10),dp(16),dp(10));row.addView(other,FlowChipLayout.LayoutParams(-2,dp(42)))
  box.addView(row,LinearLayout.LayoutParams(-1,-2))
  // The selected preset is highlighted so the current choice is always clear.
  selectElapsed(0L)
  val dlg=AlertDialog.Builder(this).setTitle(if(type=="meat")"סיימתי לאכול בשרי" else "סיימתי לאכול חלבי").setView(box).setNegativeButton("ביטול",null).setPositiveButton("התחל",null).create()
  dlg.setOnShowListener{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
   val typedName=name.text.toString().trim()
    val n=typedName.ifBlank{person?.name?:"אני"}
    if(saveName.isChecked && typedName.isNotBlank() && TimerStore.people(this).none{it.name.equals(typedName,true)}){
     val current=TimerStore.people(this).toMutableList()
     current.add(ParvePerson(typedName,TimerStore.duration(this,"meat"),TimerStore.duration(this,"dairy")))
     TimerStore.savePeople(this,current)
    }
    val dur=(if(type=="meat")person?.meatWait else person?.dairyWait)?:TimerStore.duration(this,type)
   if(elapsed>=dur){Toast.makeText(this,"כבר עברו יותר מ-"+durationText((dur/3600000).toInt(),((dur%3600000)/60000).toInt())+", אתה פרווה",Toast.LENGTH_LONG).show();dlg.dismiss();return@setOnClickListener}
   if(TimerStore.all(this).count{it.status=="active"}>=10){Toast.makeText(this,"אפשר להפעיל עד 10 טיימרים במקביל",Toast.LENGTH_SHORT).show();dlg.dismiss();return@setOnClickListener}
   val now=System.currentTimeMillis();val st=now-elapsed;val t=ParveTimer(TimerStore.newId(this),n,type,st,st+dur,dur,"active",if(elapsed>0)st else 0L)
   TimerStore.add(this,t);TimerScheduler.schedule(this,t);p.edit().putLong("end",t.endAt).putLong("duration",t.duration).putString("food",type).putBoolean("finished",false).apply()
   TimerNotifications.refresh(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this);dlg.dismiss();screen="timer";draw()
  }}
  dlg.show()
 }
 private fun chipBackground(selected:Boolean)=GradientDrawable().apply{cornerRadius=dp(20).toFloat();setColor(if(selected)copper else card);setStroke(dp(1),copper)}
 private fun showElapsedPicker(onSelected:(Long)->Unit){val tabs=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_RTL};val content=LinearLayout(this).apply{orientation=1;setPadding(dp(16),dp(10),dp(16),dp(8))}
  val relative=RadioButton(this).apply{text="משך זמן";isChecked=true};val clockTime=RadioButton(this).apply{text="שעת סיום האכילה"}
  tabs.addView(relative,LinearLayout.LayoutParams(0,-2,1f));tabs.addView(clockTime,LinearLayout.LayoutParams(0,-2,1f));content.addView(tabs)
  val wheels=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_LTR;gravity=Gravity.CENTER}
  val h=WheelPicker(this,"שעות").apply{minValue=0;maxValue=23;value=0};val m=WheelPicker(this,"דקות").apply{minValue=0;maxValue=59;value=0}
  wheels.addView(h,LinearLayout.LayoutParams(0,dp(160),1f));wheels.addView(m,LinearLayout.LayoutParams(0,dp(160),1f));content.addView(wheels)
  fun updateMode(){val isClock=clockTime.isChecked;val cal=java.util.Calendar.getInstance();h.maxValue=if(isClock)23 else 6;h.value=if(isClock)cal.get(java.util.Calendar.HOUR_OF_DAY) else 0;m.value=if(isClock)cal.get(java.util.Calendar.MINUTE) else 0}
  relative.setOnClickListener{relative.isChecked=true;clockTime.isChecked=false;updateMode()};clockTime.setOnClickListener{clockTime.isChecked=true;relative.isChecked=false;updateMode()}
  AlertDialog.Builder(this).setTitle("הגדרת זמן").setView(content).setNegativeButton("ביטול",null).setPositiveButton("אישור"){_,_->val minutes=h.value*60L+m.value;val elapsed=if(clockTime.isChecked){val cal=java.util.Calendar.getInstance();cal.set(java.util.Calendar.HOUR_OF_DAY,h.value);cal.set(java.util.Calendar.MINUTE,m.value);cal.set(java.util.Calendar.SECOND,0);cal.set(java.util.Calendar.MILLISECOND,0);var at=cal.timeInMillis;if(at>System.currentTimeMillis())at-=TimeUnit.DAYS.toMillis(1);(System.currentTimeMillis()-at).coerceAtLeast(0L)}else TimeUnit.MINUTES.toMillis(minutes);onSelected(elapsed)}.show()
 }

 private fun peopleDialog(){
  var peopleDialogRef:AlertDialog?=null
  val outer=LinearLayout(this).apply{orientation=1;setPadding(dp(14),dp(6),dp(14),dp(6));layoutDirection=View.LAYOUT_DIRECTION_RTL}
  val scroll=ScrollView(this).apply{isFillViewport=false}
  val list=LinearLayout(this).apply{orientation=1}
  val people=TimerStore.people(this)
  if(people.isEmpty()){val empty=TextView(this).apply{text="עדיין לא נשמרו שמות. אפשר להוסיף שם ולשמור זמני המתנה אישיים.";textSize=14f;setTextColor(muted);setPadding(dp(8),dp(18),dp(8),dp(18))};list.addView(empty)}
  people.forEach{person->
   val tile=LinearLayout(this).apply{orientation=1;background=solid(card,16);setPadding(dp(12),dp(10),dp(12),dp(10));layoutDirection=View.LAYOUT_DIRECTION_RTL}
   val header=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
   header.addView(TextView(this).apply{text="♧";textSize=22f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(38),dp(44)))
   header.addView(LinearLayout(this).apply{orientation=1
    addView(TextView(this@MainActivity).apply{text=person.name;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)})
    addView(TextView(this@MainActivity).apply{text=if(person.meatWait!=null||person.dairyWait!=null)"זמני המתנה אישיים" else "זמני ההמתנה הכלליים";textSize=12f;setTextColor(muted);setPadding(0,dp(2),0,0)})
   },LinearLayout.LayoutParams(0,-2,1f))
   val arrow=TextView(this).apply{text="⌄";textSize=22f;setTextColor(copper);gravity=Gravity.CENTER}
   header.addView(arrow,LinearLayout.LayoutParams(dp(32),dp(44)));tile.addView(header)
   val actions=LinearLayout(this).apply{orientation=0;visibility=View.GONE;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(0,dp(8),0,0)}
   actions.addView(outlineButton("עריכה",copper){peopleDialogRef?.dismiss();editPerson(person)},LinearLayout.LayoutParams(0,dp(40),1f).apply{marginEnd=dp(5)})
   actions.addView(outlineButton("מחיקה",red){val current=TimerStore.people(this@MainActivity);if(current.size<=1){Toast.makeText(this@MainActivity,"חייב להישאר לפחות אדם אחד",Toast.LENGTH_SHORT).show()}else AlertDialog.Builder(this@MainActivity).setTitle("מחיקת שם").setMessage("למחוק את השם "+person.name+"?").setNegativeButton("ביטול",null).setPositiveButton("מחיקה"){_,_->TimerStore.savePeople(this@MainActivity,current.filterNot{it.name==person.name});peopleDialogRef?.dismiss();peopleDialog()}.show()},LinearLayout.LayoutParams(0,dp(40),1f).apply{marginStart=dp(5)})
   tile.addView(actions);header.setOnClickListener{val expanded=actions.visibility!=View.VISIBLE;actions.visibility=if(expanded)View.VISIBLE else View.GONE;arrow.text=if(expanded)"⌃" else "⌄"}
   list.addView(tile,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
  }
  scroll.addView(list);outer.addView(scroll,LinearLayout.LayoutParams(-1,dp(360)))
  val dlg=AlertDialog.Builder(this).setTitle("אנשים וזמני המתנה").setView(outer).setNegativeButton("סגירה",null).setPositiveButton("+ הוספת שם",null).create()
  peopleDialogRef=dlg
  dlg.setOnShowListener{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{dlg.dismiss();editPerson(null)}}
  dlg.show()
 }  private fun editPerson(old:ParvePerson?){
   val box=LinearLayout(this).apply{orientation=1;setPadding(dp(18),dp(6),dp(18),dp(4));layoutDirection=View.LAYOUT_DIRECTION_RTL}
   val name=EditText(this).apply{hint="שם";setSingleLine(true);setText(old?.name?:"")};box.addView(name)
   val row=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER}
   fun pick(v:Long)=WheelPicker(this,"שעות ודקות").apply{minValue=1;maxValue=360;value=(v/60000).toInt().coerceIn(1,360);setFormatter{String.format(Locale.getDefault(),"%d:%02d",it/60,it%60)}}
   val meat=pick(old?.meatWait?:TimerStore.duration(this,"meat"));val dairy=pick(old?.dairyWait?:TimerStore.duration(this,"dairy"))
   row.addView(LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;addView(TextView(this@MainActivity).apply{text="בשרי ← חלבי";textSize=13f;setTextColor(ink);gravity=Gravity.CENTER});addView(meat)},LinearLayout.LayoutParams(0,-2,1f))
   row.addView(LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;addView(TextView(this@MainActivity).apply{text="חלבי ← בשרי";textSize=13f;setTextColor(ink);gravity=Gravity.CENTER});addView(dairy)},LinearLayout.LayoutParams(0,-2,1f))
   box.addView(row)
   AlertDialog.Builder(this).setTitle(if(old==null)"הוספת שם" else "עריכת שם").setView(box).setNegativeButton("ביטול",null).setPositiveButton("שמירה"){_,_->
    val n=name.text.toString().trim();if(n.isBlank())return@setPositiveButton
    val list=TimerStore.people(this).toMutableList();val index=if(old==null)-1 else list.indexOfFirst{it.name.equals(old.name,true)}
    if(list.indices.any{it!=index&&list[it].name.equals(n,true)}){Toast.makeText(this,"השם כבר קיים",Toast.LENGTH_SHORT).show();return@setPositiveButton}
    val updated=ParvePerson(n,meat.value.coerceIn(1,360)*60000L,dairy.value.coerceIn(1,360)*60000L);if(index>=0)list[index]=updated else list.add(updated);TimerStore.savePeople(this,list);peopleDialog()
   }.show()
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
  val actions=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_LTR};actions.addView(primaryButton("+"){chooseTimerType()},LinearLayout.LayoutParams(dp(54),dp(54)));actions.addView(outlineButton("עצור",red){TimerStore.all(this).firstOrNull{it.status=="active"}?.let{confirmStop(it)}},LinearLayout.LayoutParams(0,dp(54),1f).apply{marginStart=dp(10)});r.addView(actions,LinearLayout.LayoutParams(-1,-2))
  startTick()
 }
 private fun startTick(){val end=p.getLong("end",0);val duration=p.getLong("duration",1).coerceAtLeast(1)
  fun update(){val left=(end-System.currentTimeMillis()).coerceAtLeast(0);clock?.text=String.format(Locale.US,"%02d:%02d:%02d",TimeUnit.MILLISECONDS.toHours(left),TimeUnit.MILLISECONDS.toMinutes(left)%60,TimeUnit.MILLISECONDS.toSeconds(left)%60)
   val n=(left*100/duration).toInt().coerceIn(0,100);bar?.progress=n;ring?.setProgress(n/100f);pct?.text="$n% נותרו"
   if(left==0L){val active=TimerStore.all(this).firstOrNull{it.status=="active"&&it.endAt<=System.currentTimeMillis()};if(active!=null){val ended=TimerStore.pending(this,active.id);if(ended!=null)TimerNotifications.done(this,ended);TimerScheduler.cancel(this,active.id)};p.edit().putBoolean("finished",true).apply();TimerNotifications.refresh(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this);screen="done";doneScreen()}}
  update();if(screen=="timer")tick=object:CountDownTimer((end-System.currentTimeMillis()).coerceAtLeast(0),1000){override fun onTick(ms:Long)=update();override fun onFinish()=update()}.start()
 }
 private fun multiTimersScreen(active:List<ParveTimer>){
  val r=root();toolbar(r,"הטיימרים");space(r,12);label(r,"הטיימרים הפעילים שלך",22f,ink,true)
  val widthDp=(resources.displayMetrics.widthPixels/resources.displayMetrics.density).toInt()
  val columns=when{widthDp>=600&&active.size>=5->3;widthDp>=360->2;else->1}
  val grid=GridLayout(this).apply{columnCount=columns;orientation=GridLayout.HORIZONTAL;alignmentMode=GridLayout.ALIGN_BOUNDS;useDefaultMargins=false}
  active.sortedBy{it.endAt}.forEachIndexed{index,t->
   val cardView=LinearLayout(this).apply{orientation=1;background=solid(card,20);elevation=dp(2).toFloat();setPadding(dp(if(columns>1)10 else 16),dp(12),dp(if(columns>1)10 else 16),dp(12));layoutDirection=View.LAYOUT_DIRECTION_RTL}
   val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
   row.addView(TextView(this@MainActivity).apply{text=if(t.type=="meat")"🥩" else "🧀";textSize=if(columns>1)20f else 24f},LinearLayout.LayoutParams(dp(32),dp(38)))
   row.addView(LinearLayout(this@MainActivity).apply{orientation=1
    addView(TextView(this@MainActivity).apply{text=t.name;textSize=if(columns>1)15f else 17f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)})
    addView(TextView(this@MainActivity).apply{text=if(t.type=="meat")"אחרי בשרי" else "אחרי חלבי";textSize=11f;setTextColor(if(t.type=="meat")red else blue)})
   },LinearLayout.LayoutParams(0,-2,1f))
   cardView.addView(row)
   val left=TextView(this).apply{text=leftText(t.endAt);tag="left_"+t.id;textSize=if(columns>1)22f else 26f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink);gravity=Gravity.CENTER;setPadding(0,dp(10),0,dp(4))}
   cardView.addView(left)
   val pct=((t.endAt-System.currentTimeMillis()).coerceAtLeast(0L)*100L/t.duration.coerceAtLeast(1L)).toInt().coerceIn(0,100)
   val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progress=pct;tag="bar_"+t.id;progressTintList=android.content.res.ColorStateList.valueOf(if(t.type=="meat")red else blue)}
   cardView.addView(bar,LinearLayout.LayoutParams(-1,dp(7)).apply{topMargin=dp(7)})
   cardView.addView(TextView(this).apply{text=pct.toString()+"% נותרו";textSize=12f;tag="pct_"+t.id;setTextColor(muted);gravity=Gravity.CENTER;setPadding(0,dp(6),0,dp(2))})
   cardView.addView(TextView(this).apply{text=if(t.name=="אני")endPhrase(t.endAt) else t.name+" פרווה "+endPhrase(t.endAt).removePrefix("אהיה פרווה ");textSize=11f;setTextColor(muted);gravity=Gravity.CENTER;setPadding(0,dp(2),0,dp(7))})
   if(t.ateAt>0)cardView.addView(TextView(this).apply{text="אכילה: "+DateFormat.getTimeFormat(this@MainActivity).format(java.util.Date(t.ateAt));textSize=10f;setTextColor(muted);gravity=Gravity.CENTER})
   cardView.addView(outlineButton("עצור",red){confirmStop(t)},LinearLayout.LayoutParams(-1,dp(40)).apply{topMargin=dp(8)})
   val lp=GridLayout.LayoutParams(GridLayout.spec(index/columns),GridLayout.spec(index%columns,1f)).apply{width=0;height=-2;setMargins(dp(4),dp(4),dp(4),dp(4))}
   grid.addView(cardView,lp)
  }
  r.addView(grid,LinearLayout.LayoutParams(-1,-2))
  space(r,14);r.addView(primaryButton("+ טיימר נוסף"){chooseTimerType()},LinearLayout.LayoutParams(-1,dp(52)))
  uiTick?.let{uiHandler.removeCallbacks(it)}
  val task=object:Runnable{override fun run(){if(screen!="timer")return
   val now=System.currentTimeMillis()
   val ended=active.filter{it.endAt<=now}
   if(ended.isNotEmpty()){ended.forEach{TimerStore.pending(this@MainActivity,it.id)?.let{done->TimerScheduler.cancel(this@MainActivity,done.id);TimerNotifications.done(this@MainActivity,done)}};TimerNotifications.refresh(this@MainActivity);il.co.aniparve.widget.WidgetUpdater.updateAll(this@MainActivity);screen=if(TimerStore.all(this@MainActivity).any{it.status=="pending"})"done" else if(TimerStore.all(this@MainActivity).any{it.status=="active"})"timer" else "home";draw();return}
   active.forEach{t->val v=findViewWithTag<TextView>("left_"+t.id);if(v!=null)v.text=leftText(t.endAt);val progress=((t.endAt-now).coerceAtLeast(0L)*100L/t.duration.coerceAtLeast(1L)).toInt().coerceIn(0,100);findViewWithTag<ProgressBar>("bar_"+t.id)?.progress=progress;findViewWithTag<TextView>("pct_"+t.id)?.text=progress.toString()+"% נותרו"}
   uiHandler.postDelayed(this,1000)
  }}
  uiTick=task;uiHandler.postDelayed(task,1000)
 }
 private fun leftText(end:Long):String{val ms=(end-System.currentTimeMillis()).coerceAtLeast(0L);return String.format(Locale.getDefault(),"%02d:%02d:%02d",TimeUnit.MILLISECONDS.toHours(ms),TimeUnit.MILLISECONDS.toMinutes(ms)%60,TimeUnit.MILLISECONDS.toSeconds(ms)%60)}
 private fun endPhrase(end:Long):String{val now=java.util.Date();val date=java.util.Date(end);val f=java.text.SimpleDateFormat("yyyyMMdd",Locale.getDefault());val day=when{f.format(now)==f.format(date)->"";f.format(java.util.Date(now.time+86400000L))==f.format(date)->"מחר ";else->"ביום "+java.text.SimpleDateFormat("EEEE",Locale.getDefault()).format(date)+" "};return (if(day.isBlank())"אהיה פרווה " else day+"אהיה פרווה ")+"בשעה "+DateFormat.getTimeFormat(this).format(date)}
 private fun <T:View> findViewWithTag(tag:Any):T?{fun find(v:View):View?{if(v.tag==tag)return v;if(v is ViewGroup)for(i in 0 until v.childCount)find(v.getChildAt(i))?.let{return it};return null};return find(window.decorView) as? T}
 private fun finishStop(t:ParveTimer){TimerScheduler.cancel(this,t.id);TimerStore.remove(this,t.id);TimerNotifications.refresh(this);il.co.aniparve.widget.WidgetUpdater.updateAll(this);screen=if(TimerStore.all(this).any{it.status=="active"})"timer" else if(TimerStore.all(this).any{it.status=="pending"})"done" else "home";draw()}
 private fun confirmStop(t:ParveTimer){if(!p.getBoolean("stop_confirm_app",true)){finishStop(t);return};AlertDialog.Builder(this).setTitle("עצירת טיימר").setMessage("לעצור את הטיימר של "+t.name+"?").setNegativeButton("ביטול",null).setPositiveButton("עצור"){_,_->finishStop(t)}.show()}
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
 section(r,"◷","זמני המתנה ואנשים"){peopleAccordion(this)}
 space(r,14);section(r,"✓","אישור לפני עצירה"){switchRow(this,"▣","בקש אישור לפני עצירה מהאפליקציה","אישור לפני עצירת טיימר מתוך האפליקציה","stop_confirm_app",true);divider(this);switchRow(this,"▣","בקש אישור לפני עצירה מהווידג'ט","אישור לפני עצירת טיימר בווידג'ט","stop_confirm_widget",true);divider(this);switchRow(this,"▣","בקש אישור לפני עצירה מהתראת אנדרואיד","אישור לפני עצירה מההתראה הקבועה","stop_confirm_notification",true)}
 space(r,14);section(r,"♫","התראות"){switchRow(this,"◉","רטט בהתראה","רטט בעת סיום ההמתנה","vibrate",true);divider(this);switchRow(this,"▣","התראה קבועה בזמן הספירה","הצגת הטיימר בשורת ההתראות","persistent",true);divider(this);settingRow(this,"♫","צליל התראה",if(p.getString("sound","default")=="silent")"שקט" else "ברירת מחדל","›"){AlertDialog.Builder(this@MainActivity).setTitle("צליל התראה").setItems(arrayOf("שקט","ברירת מחדל")){_,i->p.edit().putString("sound",if(i==0)"silent" else "default").apply();channel();TimerNotifications.refresh(this@MainActivity);settings()}.show()}}
 space(r,14);section(r,"▣","ווידג'ט"){settingRow(this,"▣","הוספה למסך הבית","צפייה בטיימר בלי לפתוח את האפליקציה","›"){pinWidget()};space(this,6);val selected=p.getString("widget_theme","system")?:"system";val row=LinearLayout(this@MainActivity).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER};listOf("light" to "בהיר","dark" to "כהה","system" to "לפי הגדרות המערכת").forEach{(key,name)->val chosen=selected==key;row.addView(TextView(this@MainActivity).apply{text=name;textSize=12f;gravity=Gravity.CENTER;setTextColor(if(chosen)Color.WHITE else ink);background=solid(if(chosen)copper else bg,18);setPadding(dp(7),dp(11),dp(7),dp(11));setOnClickListener{p.edit().putString("widget_theme",key).apply();il.co.aniparve.widget.WidgetUpdater.updateAll(this@MainActivity);settings()}},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(3);marginEnd=dp(3)})};addView(row)}
 space(r,14);section(r,"☼","מראה"){val current=p.getString("theme","system");val row=LinearLayout(this@MainActivity).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_RTL;gravity=Gravity.CENTER};listOf("system" to "לפי המערכת","light" to "בהיר","dark" to "כהה").forEach{(key,name)->val selected=current==key;row.addView(TextView(this@MainActivity).apply{text=name;textSize=13f;gravity=Gravity.CENTER;setTextColor(if(selected)Color.WHITE else ink);background=solid(if(selected)copper else bg,22);setPadding(dp(8),dp(12),dp(8),dp(12));setOnClickListener{p.edit().putString("theme",key).apply();applyTheme();screen="settings";recreate()}},LinearLayout.LayoutParams(0,-2,1f).apply{marginStart=dp(4);marginEnd=dp(4)})};addView(row)}
 space(r,14);section(r,"⚙","הרשאות ואמינות"){settingRow(this,"♧","הגדרות התראות וחיסכון בסוללה","ניהול הרשאות והתנהגות ברקע","›"){try{if(Build.VERSION.SDK_INT>=31)startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))else startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")));if(Build.VERSION.SDK_INT>=23&&!getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName))startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:$packageName")))}catch(_:Exception){}}}
 space(r,14);section(r,"?","איך זה עובד"){settingRow(this,"?","פתיחת מסך ההסבר","הוראות קצרות לשימוש","›"){helpScreen()}}
}
private fun savePeopleAndDefaults(list:List<ParvePerson>){val valid=list.filter{it.name.isNotBlank()};TimerStore.savePeople(this,valid);valid.firstOrNull()?.let{a->val m=(a.meatWait?:TimerStore.duration(this,"meat")).coerceIn(60000L,21600000L);val d=(a.dairyWait?:TimerStore.duration(this,"dairy")).coerceIn(60000L,21600000L);p.edit().putInt("meat_h",(m/3600000L).toInt()).putInt("meat_m",((m%3600000L)/60000L).toInt()).putInt("dairy_h",(d/3600000L).toInt()).putInt("dairy_m",((d%3600000L)/60000L).toInt()).apply()}}
private fun peopleAccordion(parent:LinearLayout){
 val wrap=LinearLayout(this).apply{orientation=1};var opened=p.getString("expanded_person",TimerStore.people(this).firstOrNull()?.name?:"אני")?:"אני"
 fun render(){
  wrap.removeAllViews();val people=TimerStore.people(this@MainActivity).toMutableList()
  people.forEach{person->
   val open=opened==person.name;var meat=person.meatWait?:TimerStore.duration(this,"meat");var dairy=person.dairyWait?:TimerStore.duration(this,"dairy")
   val tile=LinearLayout(this).apply{orientation=1;background=solid(if(open)bg else card,18);setPadding(dp(12),dp(10),dp(12),dp(10));layoutDirection=View.LAYOUT_DIRECTION_RTL}
   val head=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
   head.addView(TextView(this).apply{text="♧";textSize=22f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(34),dp(42)))
   head.addView(LinearLayout(this).apply{orientation=1;addView(TextView(this@MainActivity).apply{text=person.name;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)});addView(TextView(this@MainActivity).apply{text="בשרי ${fmtWait(meat)} · חלבי ${fmtWait(dairy)}";textSize=11f;setTextColor(muted)})},LinearLayout.LayoutParams(0,-2,1f))
   head.addView(TextView(this).apply{text=if(open)"⌃" else "⌄";textSize=22f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(28),dp(42)));tile.addView(head)
   head.setOnClickListener{opened=if(open)"" else person.name;p.edit().putString("expanded_person",opened).apply();render()}
   if(open){
    var originalName=person.name
    val name=EditText(this).apply{hint="שם האדם";setSingleLine(true);setText(person.name)};tile.addView(name,LinearLayout.LayoutParams(-1,dp(46)))
    fun save(){val all=TimerStore.people(this@MainActivity).toMutableList();val i=all.indexOfFirst{it.name==originalName};if(i<0)return;val n=name.text.toString().trim().ifBlank{originalName};if(all.indices.any{it!=i&&all[it].name.equals(n,true)})return;all[i]=ParvePerson(n,meat,dairy);savePeopleAndDefaults(all);if(n!=originalName){originalName=n;opened=n;p.edit().putString("expanded_person",n).apply()}}
    fun picker(title:String,start:Long,onChange:(Long)->Unit){
     val h0=(start/3600000L).toInt().coerceIn(0,6);val m0=((start%3600000L)/60000L).toInt().coerceIn(0,59);val row=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_LTR;gravity=Gravity.CENTER}
     val h=WheelPicker(this,"שעות").apply{minValue=0;maxValue=6;value=h0;setFormatter{String.format(Locale.US,"%02d",it)}}
     val m=WheelPicker(this,"דקות").apply{minValue=0;maxValue=59;value=m0.coerceIn(0,59);cyclic=true;setFormatter{String.format(Locale.US,"%02d",it)}}
     var lock=false
     fun commit(){val total=(h.value*60+m.value).coerceIn(1,360);lock=true;h.value=total/60;m.value=total%60;lock=false;onChange(total*60000L)}
     h.setOnValueChangedListener{_,_,_->if(!lock)commit()}
     m.setOnValueChangedListener{_,old,v->if(!lock){var total=h.value*60+v;if(old==0&&v==59)total=h.value*60-1 else if(old==59&&v==0)total=h.value*60+1;total=total.coerceIn(1,360);lock=true;h.value=total/60;m.value=total%60;lock=false;onChange(total*60000L)}}
     tile.addView(TextView(this).apply{text=title;textSize=13f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink);gravity=Gravity.CENTER;setPadding(0,dp(8),0,dp(2))})
     row.addView(h,LinearLayout.LayoutParams(0,dp(150),1f));row.addView(TextView(this).apply{text=":";textSize=22f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(20),dp(150)));row.addView(m,LinearLayout.LayoutParams(0,dp(150),1f));tile.addView(row,LinearLayout.LayoutParams(-1,dp(150)))
    }
    picker("בשרי ← חלבי",meat){meat=it;save()};picker("חלבי ← בשרי",dairy){dairy=it;save()}
    name.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){if(!s.isNullOrBlank())save()};override fun afterTextChanged(s:Editable?) {}})
    if(people.size>1)tile.addView(outlineButton("מחיקת אדם",red){AlertDialog.Builder(this@MainActivity).setTitle("מחיקת אדם").setMessage("למחוק את ${person.name}?").setNegativeButton("ביטול",null).setPositiveButton("מחיקה"){_,_->savePeopleAndDefaults(TimerStore.people(this@MainActivity).filterNot{it.name==originalName});opened=TimerStore.people(this@MainActivity).firstOrNull()?.name?:"אני";settings()}.show()},LinearLayout.LayoutParams(-1,dp(40)))
   }
   wrap.addView(tile,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)})
  }
 }
 render();parent.addView(wrap,LinearLayout.LayoutParams(-1,-2))
 parent.addView(primaryButton("+ הוסף איש"){val tile=LinearLayout(this).apply{orientation=1;background=solid(bg,18);setPadding(dp(12),dp(10),dp(12),dp(10));layoutDirection=View.LAYOUT_DIRECTION_RTL};val field=EditText(this).apply{hint="שם האדם";setSingleLine(true)};tile.addView(TextView(this).apply{text="איש חדש · זמני ברירת מחדל";textSize=13f;setTextColor(muted)});tile.addView(field,LinearLayout.LayoutParams(-1,dp(46)));tile.addView(primaryButton("שמור איש"){val n=field.text.toString().trim();val all=TimerStore.people(this).toMutableList();if(n.isNotBlank()&&all.none{it.name.equals(n,true)}){all.add(ParvePerson(n,TimerStore.duration(this,"meat"),TimerStore.duration(this,"dairy")));savePeopleAndDefaults(all);opened=n;p.edit().putString("expanded_person",n).apply();settings()}else if(n.isNotBlank())Toast.makeText(this,"השם כבר קיים",Toast.LENGTH_SHORT).show()else Toast.makeText(this,"יש להזין שם",Toast.LENGTH_SHORT).show()},LinearLayout.LayoutParams(-1,dp(42)));wrap.addView(tile,0,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)});field.requestFocus()},LinearLayout.LayoutParams(-1,dp(48)))
}
private fun fmtWait(ms:Long):String{val n=(ms/60000L).toInt().coerceIn(1,360);return "${n/60}:${String.format(Locale.US,"%02d",n%60)}"}
private fun section(r:LinearLayout,g:String,title:String,content:LinearLayout.()->Unit){val box=LinearLayout(this).apply{orientation=1;background=solid(card,24);elevation=dp(2).toFloat();setPadding(dp(18),dp(16),dp(18),dp(16))}
  val h=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;addView(TextView(this@MainActivity).apply{text=g;textSize=22f;setTextColor(copper);gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(34),dp(36)));addView(TextView(this@MainActivity).apply{text=title;textSize=16f;typeface=Typeface.DEFAULT_BOLD;setTextColor(copper)})};box.addView(h);space(box,8);box.content();r.addView(box,LinearLayout.LayoutParams(-1,-2))}
 private fun waitCard(parent:LinearLayout,title:String,key:String){val box=LinearLayout(this).apply{orientation=1;background=solid(bg,20);setPadding(dp(14),dp(14),dp(14),dp(12))}
  box.addView(TextView(this).apply{text=title;textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(ink)});space(box,10)
  val wheels=LinearLayout(this).apply{orientation=0;layoutDirection=View.LAYOUT_DIRECTION_LTR;gravity=Gravity.CENTER}
  fun picker(max:Int,value:Int)=WheelPicker(this).apply{minValue=0;maxValue=max;this.value=value;setFormatter{v->String.format(Locale.US,"%02d",v)}}
  val hours=picker(6,p.getInt(key+"_h",6).coerceIn(0,6));val minutes=picker(59,p.getInt(key+"_m",0).coerceIn(0,59))
  val hourCol=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;addView(hours,LinearLayout.LayoutParams(dp(84),dp(176)));addView(TextView(this@MainActivity).apply{text="שעות";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER})}
  val minCol=LinearLayout(this).apply{orientation=1;gravity=Gravity.CENTER;addView(minutes,LinearLayout.LayoutParams(dp(84),dp(176)));addView(TextView(this@MainActivity).apply{text="דקות";textSize=12f;setTextColor(muted);gravity=Gravity.CENTER})}
  wheels.addView(hourCol);wheels.addView(TextView(this).apply{text=":";textSize=25f;typeface=Typeface.DEFAULT_BOLD;setTextColor(copper);gravity=Gravity.CENTER;setPadding(0,0,0,dp(16))},LinearLayout.LayoutParams(dp(28),dp(176)));wheels.addView(minCol);box.addView(wheels,LinearLayout.LayoutParams(-1,-2))
  val summary=TextView(this).apply{text=durationText(hours.value,minutes.value);textSize=12f;setTextColor(muted);gravity=Gravity.CENTER;setPadding(0,dp(8),0,0)}
  fun update(){summary.text=durationText(hours.value,minutes.value)}
  var adjusting=false
  fun savePicker(){val total=(hours.value*60+minutes.value).coerceIn(1,360);adjusting=true;hours.value=total/60;minutes.value=total%60;adjusting=false;p.edit().putInt(key+"_h",hours.value).putInt(key+"_m",minutes.value).apply();update()}
  hours.setOnValueChangedListener{_,_,_->if(!adjusting)savePicker()}
  minutes.setOnValueChangedListener{_,old,v->if(!adjusting){var total=hours.value*60+v;if(old==0&&v==59)total=hours.value*60-1 else if(old==59&&v==0)total=hours.value*60+1;total=total.coerceIn(1,360);adjusting=true;hours.value=total/60;minutes.value=total%60;adjusting=false;p.edit().putInt(key+"_h",hours.value).putInt(key+"_m",minutes.value).apply();update()}}
  if(hours.value==0&&minutes.value==0)minutes.value=1;box.addView(summary)
  val quick=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR}
  fun setMinutes(total:Int){val safe=total.coerceIn(1,360);hours.value=safe/60;minutes.maxValue=59;minutes.value=safe%60;p.edit().putInt(key+"_h",hours.value).putInt(key+"_m",minutes.value).apply();update()}
  quick.addView(outlineButton("− 30 דקות",muted){setMinutes(hours.value*60+minutes.value-30)},LinearLayout.LayoutParams(0,dp(40),1f).apply{marginEnd=dp(6)})
  quick.addView(outlineButton("+ 30 דקות",copper){setMinutes(hours.value*60+minutes.value+30)},LinearLayout.LayoutParams(0,dp(40),1f).apply{marginStart=dp(6)})
  box.addView(quick,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)});parent.addView(box,LinearLayout.LayoutParams(-1,-2))
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