package il.co.aniparve
import android.app.*
import android.content.*
import android.os.Build
class TimerReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent?){val id=i?.getLongExtra("timer_id",0L)?:0L;val t=TimerStore.all(c).firstOrNull{it.id==id}?:return;if(t.status!="active")return;if(t.endAt>System.currentTimeMillis()){TimerScheduler.schedule(c,t);return};val done=TimerStore.pending(c,id)?:return;TimerNotifications.clear(c,id);TimerNotifications.done(c,done);TimerNotifications.refresh(c);il.co.aniparve.widget.WidgetUpdater.updateAll(c)}}
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent?){if(i?.action!=Intent.ACTION_BOOT_COMPLETED&&i?.action!=Intent.ACTION_MY_PACKAGE_REPLACED)return;TimerMaintenance.cleanupExpired(c);TimerStore.all(c).filter{it.status=="active"}.forEach{TimerScheduler.schedule(c,it)};TimerNotifications.refresh(c);try{il.co.aniparve.widget.WidgetUpdater.updateAll(c)}catch(_:Exception){}}}
object TimerScheduler{
 private fun code(id:Long)=(id xor (id ushr 32)).toInt()
 fun schedule(c:Context,t:ParveTimer){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val f=PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0;val i=Intent(c,TimerReceiver::class.java).putExtra("timer_id",t.id).setData(android.net.Uri.parse("ani-parve://timer/alarm/"+t.id));val pi=PendingIntent.getBroadcast(c,code(t.id),i,f);try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t.endAt,pi)else am.setExact(AlarmManager.RTC_WAKEUP,t.endAt,pi)}catch(_:SecurityException){am.set(AlarmManager.RTC_WAKEUP,t.endAt,pi)}}
 fun cancel(c:Context,id:Long){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val f=PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0;val i=Intent(c,TimerReceiver::class.java).putExtra("timer_id",id).setData(android.net.Uri.parse("ani-parve://timer/alarm/"+id));am.cancel(PendingIntent.getBroadcast(c,code(id),i,f));TimerNotifications.clear(c,id)}
}
class StopActionReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent?){
  val action=i?.action?:"";val id=i?.getLongExtra("timer_id",0L)?:0L
  if(action=="open_timer"){c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("show_timers",true));return}
  if(action!="stop_timer"||id==0L)return
  StopActions.stop(c,id)
 }
}
object StopActions{
 @Synchronized fun stop(c:Context,id:Long):Boolean{
  if(id==0L)return false
  val t=TimerStore.all(c).firstOrNull{it.id==id&&it.status=="active"}?:return false
  if(!TimerStore.removeActive(c,id))return false
  try{TimerScheduler.cancel(c,t.id)}catch(_:Exception){}
  try{TimerNotifications.clear(c,t.id)}catch(_:Exception){}
  try{TimerNotifications.refresh(c)}catch(_:Exception){}
  try{il.co.aniparve.widget.WidgetUpdater.updateAll(c)}catch(_:Exception){}
  return true
 }
}
object TimerMaintenance{
 @Synchronized fun cleanupExpired(c:Context){val expired=TimerStore.markExpired(c);expired.forEach{t->try{TimerScheduler.cancel(c,t.id)}catch(_:Exception){};try{TimerNotifications.done(c,t)}catch(_:Exception){}};if(expired.isNotEmpty())try{TimerNotifications.refresh(c)}catch(_:Exception){}}
}
