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
 override fun onAppWidgetOptionsChanged(c:Context,m:AppWidgetManager,id:Int,o:Bundle)=WidgetUpdater.updateAll(c)
 override fun onReceive(c:Context,i:Intent){super.onReceive(c,i);if(i.action==TICK)WidgetUpdater.updateAll(c)}
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
  v.setOnClickFillInIntent(R.id.row_root,Intent().putExtra("timer_id",t.id).putExtra("timer_name",t.name));return v
 }
 override fun getLoadingView():RemoteViews?=RemoteViews(c.packageName,R.layout.timer_widget_row)
 override fun getViewTypeCount()=1
 override fun getItemId(position:Int)=if(position in timers.indices)timers[position].id else position.toLong()
 override fun hasStableIds()=true
}
object WidgetUpdater{
 fun updateAll(c:Context){
  val m=AppWidgetManager.getInstance(c);val ids=m.getAppWidgetIds(ComponentName(c,TimerWidgetProvider::class.java));if(ids.isEmpty())return
  val active=TimerStore.all(c).filter{it.status=="active"}.sortedBy{it.endAt}
  ids.forEach{id->
   val v=RemoteViews(c.packageName,R.layout.timer_widget)
   v.setViewVisibility(R.id.widget_empty,if(active.isEmpty())android.view.View.VISIBLE else android.view.View.GONE)
   v.setViewVisibility(R.id.widget_active,if(active.isEmpty())android.view.View.GONE else android.view.View.VISIBLE)
   v.setOnClickPendingIntent(R.id.widget_open,pi(c,1,null,false));v.setOnClickPendingIntent(R.id.widget_meat,pi(c,2,"meat",false));v.setOnClickPendingIntent(R.id.widget_dairy,pi(c,3,"dairy",false))
   v.setOnClickPendingIntent(R.id.widget_add,pi(c,6,active.firstOrNull()?.type?:"meat",false,true))
   if(active.isNotEmpty()){
    v.setRemoteAdapter(R.id.widget_list,Intent(c,TimerWidgetService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id))
    val template=PendingIntent.getActivity(c,9100+id,Intent(c,StopConfirmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),flags())
    v.setPendingIntentTemplate(R.id.widget_list,template);v.setEmptyView(R.id.widget_list,R.id.widget_list_empty)
    v.setOnClickPendingIntent(R.id.widget_active,pi(c,4,null,true))
   }
   m.updateAppWidget(id,v)
  }
  m.notifyAppWidgetViewDataChanged(ids,R.id.widget_list)
  if(active.any{Build.VERSION.SDK_INT<24})minuteTick(c) else cancelTick(c)
 }
 private fun flags()=PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0
 private fun pi(c:Context,req:Int,type:String?,timers:Boolean,openSheet:Boolean=false):PendingIntent{
  val i=Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
  if(type!=null)i.putExtra("start_type",type);if(openSheet)i.putExtra("open_start_sheet",true);if(timers)i.putExtra("show_timers",true)
  return PendingIntent.getActivity(c,req,i,flags())
 }
 private fun minuteTick(c:Context){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val pi=PendingIntent.getBroadcast(c,7001,Intent(c,TimerWidgetProvider::class.java).setAction(TimerWidgetProvider.TICK),flags());try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60000,pi)else am.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60000,pi)}catch(_:SecurityException){am.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+60000,pi)}}
 private fun cancelTick(c:Context){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;am.cancel(PendingIntent.getBroadcast(c,7001,Intent(c,TimerWidgetProvider::class.java).setAction(TimerWidgetProvider.TICK),flags()))}
}