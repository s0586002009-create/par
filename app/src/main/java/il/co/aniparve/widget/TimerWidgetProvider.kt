package il.co.aniparve.widget
import android.app.*
import android.appwidget.*
import android.content.*
import android.os.*
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import il.co.aniparve.*
import android.text.format.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
class TimerWidgetProvider:AppWidgetProvider(){
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray)=WidgetUpdater.updateAll(c)
 override fun onEnabled(c:Context){super.onEnabled(c);WidgetUpdater.updateAll(c)}
 override fun onRestored(c:Context,m:AppWidgetManager,oldIds:IntArray,newIds:IntArray){super.onRestored(c,m,oldIds,newIds);WidgetUpdater.updateAll(c)}
 override fun onAppWidgetOptionsChanged(c:Context,m:AppWidgetManager,id:Int,o:Bundle){super.onAppWidgetOptionsChanged(c,m,id,o);WidgetUpdater.updateAll(c)}
 override fun onReceive(c:Context,i:Intent){super.onReceive(c,i);if(i.action==TICK||i.action==Intent.ACTION_TIME_CHANGED||i.action==Intent.ACTION_TIMEZONE_CHANGED||i.action==Intent.ACTION_DATE_CHANGED||i.action==Intent.ACTION_MY_PACKAGE_REPLACED)WidgetUpdater.updateAll(c)}
 companion object{const val TICK="il.co.aniparve.widget.TICK"}
}
class TimerWidgetService:RemoteViewsService(){override fun onGetViewFactory(intent:Intent):RemoteViewsFactory=TimerWidgetFactory(applicationContext)}
private class TimerWidgetFactory(private val c:Context):RemoteViewsService.RemoteViewsFactory{
 private var timers:List<ParveTimer> = emptyList()
 override fun onCreate(){load()}
 override fun onDataSetChanged(){load()}
 private fun load(){timers=TimerStore.all(c).filter{it.status=="active"}.sortedBy{it.endAt}}
 override fun onDestroy(){timers=emptyList()}
 override fun getCount()=timers.size
 override fun getViewAt(position:Int):RemoteViews?{
  if(position !in timers.indices)return null
  val t=timers[position];val v=RemoteViews(c.packageName,R.layout.timer_widget_row)
  v.setTextViewText(R.id.row_name,t.name);v.setTextViewText(R.id.row_type,if(t.type=="meat")"🥩 בשרי" else "🧀 חלבי")
  val left=(t.endAt-System.currentTimeMillis()).coerceAtLeast(0L)
  if(Build.VERSION.SDK_INT>=24){v.setViewVisibility(R.id.row_chrono,android.view.View.VISIBLE);v.setViewVisibility(R.id.row_remaining,android.view.View.GONE);v.setChronometer(R.id.row_chrono,SystemClock.elapsedRealtime()+left,null,true);v.setChronometerCountDown(R.id.row_chrono,true)}
  else{v.setViewVisibility(R.id.row_chrono,android.view.View.GONE);v.setViewVisibility(R.id.row_remaining,android.view.View.VISIBLE);val mins=TimeUnit.MILLISECONDS.toMinutes(left+59999);v.setTextViewText(R.id.row_remaining,String.format(java.util.Locale.getDefault(),"%d:%02d",mins/60,mins%60))}
  v.setTextViewText(R.id.row_end,"עד "+DateFormat.getTimeFormat(c).format(Date(t.endAt)))
  val fill=Intent().setData(android.net.Uri.parse("ani-parve://widget/timer/"+t.id)).putExtra("timer_id",t.id).putExtra("timer_name",t.name)
  v.setOnClickFillInIntent(R.id.row_root,Intent(fill).setAction("open_timer"))
  v.setOnClickFillInIntent(R.id.row_stop,Intent(fill).setAction("stop_timer").putExtra("source","widget"))
  WidgetUpdater.applyWidgetTheme(v,c,true)
  return v
 }
 override fun getLoadingView():RemoteViews?=RemoteViews(c.packageName,R.layout.timer_widget_row)
 override fun getViewTypeCount()=1
 override fun getItemId(position:Int)=if(position in timers.indices)timers[position].id else position.toLong()
 override fun hasStableIds()=true
}
object WidgetUpdater{
 fun updateAll(c:Context){
  TimerMaintenance.cleanupExpired(c)
  val m=AppWidgetManager.getInstance(c);val ids=m.getAppWidgetIds(ComponentName(c,TimerWidgetProvider::class.java));if(ids.isEmpty()){cancelTick(c);return}
  val active=TimerStore.all(c).filter{it.status=="active"}.sortedBy{it.endAt}
  ids.forEach{id->
   val v=RemoteViews(c.packageName,R.layout.timer_widget)
   v.setViewVisibility(R.id.widget_empty,if(active.isEmpty())android.view.View.VISIBLE else android.view.View.GONE)
   v.setViewVisibility(R.id.widget_active,if(active.isEmpty())android.view.View.GONE else android.view.View.VISIBLE)
   val dairyVisible=TimerStore.p(c).getBoolean("show_dairy",true);v.setViewVisibility(R.id.widget_dairy,if(dairyVisible)android.view.View.VISIBLE else android.view.View.GONE);v.setTextViewText(R.id.widget_open,if(active.isEmpty())"אכלתי עכשיו" else "טיימרים פעילים")
   val opts=m.getAppWidgetOptions(id);val minW=opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,180);val minH=opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,110);val compact=minW<150||minH<100
   v.setTextViewText(R.id.widget_meat,if(compact)"🥩" else "בשרי");v.setTextViewText(R.id.widget_dairy,if(compact)"🧀" else "חלבי");v.setTextViewTextSize(R.id.widget_open,android.util.TypedValue.COMPLEX_UNIT_SP,if(compact)11f else 14f);v.setViewVisibility(R.id.widget_add,if(compact)android.view.View.GONE else android.view.View.VISIBLE)
   v.setOnClickPendingIntent(R.id.widget_open,pi(c,1,null,false));v.setOnClickPendingIntent(R.id.widget_meat,pi(c,2,"meat",false));v.setOnClickPendingIntent(R.id.widget_dairy,pi(c,3,"dairy",false))
   v.setOnClickPendingIntent(R.id.widget_add,pi(c,6,active.firstOrNull()?.type?:"meat",false,true))
   if(active.isNotEmpty()){
    v.setRemoteAdapter(R.id.widget_list,Intent(c,TimerWidgetService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).setData(android.net.Uri.parse("ani-parve://widget/list/"+id)))
    val confirm=TimerStore.p(c).getBoolean("stop_confirm_widget",true)
    val target=if(confirm)StopConfirmActivity::class.java else StopActionReceiver::class.java
    val ti=Intent(c,target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val template=if(confirm)PendingIntent.getActivity(c,9100+id,ti,templateFlags()) else PendingIntent.getBroadcast(c,9100+id,ti,templateFlags())
    v.setPendingIntentTemplate(R.id.widget_list,template);v.setEmptyView(R.id.widget_list,R.id.widget_list_empty)
    v.setOnClickPendingIntent(R.id.widget_active,pi(c,4,null,true))
   }
   applyWidgetTheme(v,c,false)
   m.updateAppWidget(id,v)
  }
  m.notifyAppWidgetViewDataChanged(ids,R.id.widget_list)
  if(active.isNotEmpty())minuteTick(c) else cancelTick(c)
 }
 private fun flags()=PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0
 private fun templateFlags()=PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=31)PendingIntent.FLAG_MUTABLE else 0
 internal fun applyWidgetTheme(v:RemoteViews,c:Context,row:Boolean){
  val mode=TimerStore.p(c).getString("widget_theme","system")?:"system"
  if(mode=="system")return
  val dark=mode=="dark";val background=if(dark)0xFF2A201A.toInt() else 0xFFFFF8EC.toInt();val text=if(dark)0xFFF5E9DA.toInt() else 0xFF3B2314.toInt()
  if(row){
   v.setInt(R.id.row_root,"setBackgroundResource",if(dark)R.drawable.widget_background_dark else R.drawable.widget_background_light)
   listOf(R.id.row_name,R.id.row_type,R.id.row_chrono,R.id.row_remaining,R.id.row_end).forEach{v.setTextColor(it,text)}
  }else{
   v.setInt(R.id.widget_root,"setBackgroundResource",if(dark)R.drawable.widget_background_dark else R.drawable.widget_background_light)
   v.setInt(R.id.widget_empty,"setBackgroundResource",if(dark)R.drawable.widget_background_dark else R.drawable.widget_background_light)
   v.setInt(R.id.widget_active,"setBackgroundResource",if(dark)R.drawable.widget_background_dark else R.drawable.widget_background_light)
   v.setInt(R.id.widget_add,"setBackgroundResource",if(dark)R.drawable.widget_background_dark else R.drawable.widget_background_light)
   v.setTextColor(R.id.widget_open,text);v.setTextColor(R.id.widget_add,text);v.setTextColor(R.id.widget_list_empty,text)
  }
 }
 private fun pi(c:Context,req:Int,type:String?,timers:Boolean,openSheet:Boolean=false):PendingIntent{
  val i=Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
  if(req==6){i.putExtra("open_type_picker",true).putExtra("widget_start_sheet",true)}else if(type!=null){i.putExtra("start_type",type).putExtra("open_start_sheet",true).putExtra("widget_start_sheet",true)}else if(openSheet){i.putExtra("open_start_sheet",true).putExtra("widget_start_sheet",true)}
  if(timers)i.putExtra("show_timers",true);i.data=android.net.Uri.parse("ani-parve://widget/action/"+req+"/"+(type?:"none"))
  return PendingIntent.getActivity(c,req,i,flags())
 }
 private fun minuteTick(c:Context){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val pi=PendingIntent.getBroadcast(c,7001,Intent(c,TimerWidgetProvider::class.java).setAction(TimerWidgetProvider.TICK),flags());try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+300000,pi)else am.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60000,pi)}catch(_:SecurityException){am.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60000,pi)}}
 private fun cancelTick(c:Context){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;am.cancel(PendingIntent.getBroadcast(c,7001,Intent(c,TimerWidgetProvider::class.java).setAction(TimerWidgetProvider.TICK),flags()))}
}