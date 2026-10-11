package il.co.aniparve
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent?){val p=c.getSharedPreferences("ani_parve",0);val end=p.getLong("end",0);if(end<=0||p.getBoolean("finished",false))return
if(end<=System.currentTimeMillis()){p.edit().putBoolean("finished",true).apply();val pi=PendingIntent.getActivity(c,11,Intent(c,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val n=NotificationCompat.Builder(c,p.getString("channel","parve_default")!!).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("אני פרווה").setContentText("זמן ההמתנה הסתיים").setContentIntent(pi).setAutoCancel(true).build();try{NotificationManagerCompat.from(c).notify(2,n)}catch(_:SecurityException){}}else{val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val pi=PendingIntent.getBroadcast(c,1,Intent(c,TimerReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);try{if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,end,pi)else am.setExact(AlarmManager.RTC_WAKEUP,end,pi)}catch(_:SecurityException){am.set(AlarmManager.RTC_WAKEUP,end,pi)}}}}
