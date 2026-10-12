package il.co.aniparve
import android.app.*
import android.content.*
import android.os.Build
class TimerReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent?){val id=i?.getLongExtra("timer_id",0L)?:0L;val t=TimerStore.all(c).firstOrNull{it.id==id}?:return;if(t.status!="active")return;if(t.endAt>System.currentTimeMillis()){TimerScheduler.schedule(c,t);return};val done=TimerStore.pending(c,id)?:return;TimerNotifications.clear(c,id);TimerNotifications.done(c,done);TimerNotifications.refresh(c);il.co.aniparve.widget.WidgetUpdater.updateAll(c)}}
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent?){if(i?.action!=Intent.ACTION_BOOT_COMPLETED&&i?.action!=Intent.ACTION_MY_PACKAGE_REPLACED)return;TimerStore.all(c).filter{it.status=="active"}.forEach{if(it.endAt<=System.currentTimeMillis())TimerStore.pending(c,it.id)?.let{t->TimerNotifications.done(c,t)}else TimerScheduler.schedule(c,it)};TimerNotifications.refresh(c);il.co.aniparve.widget.WidgetUpdater.updateAll(c)}}
object TimerScheduler{
 private fun code(id:Long)=(id xor (id ushr 32)).toInt()
 fun schedule(c:Context,t:ParveTimer){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val f=PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0;val pi=PendingIntent.getBroadcast(c,code(t.id),Intent(c,TimerReceiver::class.java).putExtra("timer_id",t.id),f);try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t.endAt,pi)else am.setExact(AlarmManager.RTC_WAKEUP,t.endAt,pi)}catch(_:SecurityException){am.set(AlarmManager.RTC_WAKEUP,t.endAt,pi)}}
 fun cancel(c:Context,id:Long){val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val f=PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0;am.cancel(PendingIntent.getBroadcast(c,code(id),Intent(c,TimerReceiver::class.java).putExtra("timer_id",id),f));TimerNotifications.clear(c,id)}
}