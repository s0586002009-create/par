package il.co.aniparve
import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject

data class ParveTimer(val id:Long,val name:String,val type:String,val startAt:Long,val endAt:Long,val duration:Long,val status:String,val ateAt:Long=0L)
data class ParvePerson(val name:String,val meatWait:Long?=null,val dairyWait:Long?=null)
object TimerStore{
 private const val LIST="timers_json"
 fun p(c:Context)=c.getSharedPreferences("ani_parve",0)
 @Synchronized fun all(c:Context):MutableList<ParveTimer>{
  val prefs=p(c);val raw=prefs.getString(LIST,null)
  if(raw==null){val end=prefs.getLong("end",0L);val dur=prefs.getLong("duration",0L);if(end>0&&dur>0){val st=end-dur;val t=ParveTimer(1L,"אני",prefs.getString("food","meat")?:"meat",st,end,dur,if(prefs.getBoolean("finished",false)||end<=System.currentTimeMillis())"pending" else "active",st);save(c,mutableListOf(t));return mutableListOf(t)};prefs.edit().putString(LIST,"[]").apply();return mutableListOf()}
  val out=mutableListOf<ParveTimer>();try{val a=JSONArray(raw);for(i in 0 until a.length()){val o=a.getJSONObject(i);out.add(ParveTimer(o.optLong("id"),o.optString("name","אני"),o.optString("type","meat"),o.optLong("startAt"),o.optLong("endAt"),o.optLong("duration"),o.optString("status","active"),o.optLong("ateAt")))}}catch(_:Exception){};return out
 }
 @Synchronized fun save(c:Context,list:List<ParveTimer>){val a=JSONArray();list.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("type",it.type).put("startAt",it.startAt).put("endAt",it.endAt).put("duration",it.duration).put("status",it.status).put("ateAt",it.ateAt))};p(c).edit().putString(LIST,a.toString()).apply()}
 fun add(c:Context,t:ParveTimer){val a=all(c);a.add(t);save(c,a)}
 fun remove(c:Context,id:Long){save(c,all(c).filterNot{it.id==id})}
 fun pending(c:Context,id:Long):ParveTimer?{val a=all(c);val i=a.indexOfFirst{it.id==id};if(i<0)return null;if(a[i].status=="active"){a[i]=a[i].copy(status="pending");save(c,a)};return a[i]}
 fun duration(c:Context,type:String):Long{val p=p(c);val k=if(type=="meat")"meat" else "dairy";return (p.getInt(k+"_h",6).coerceIn(0,6)*60L+p.getInt(k+"_m",0).coerceIn(0,59)).coerceAtMost(360L)*60000L}
 fun people(c:Context):MutableList<ParvePerson>{val out=mutableListOf<ParvePerson>();try{val a=JSONArray(p(c).getString("people_json","[]"));for(i in 0 until a.length()){val o=a.getJSONObject(i);out.add(ParvePerson(o.optString("name"),if(o.isNull("meat"))null else o.optLong("meat"),if(o.isNull("dairy"))null else o.optLong("dairy")))}}catch(_:Exception){};return out}
 fun savePeople(c:Context,list:List<ParvePerson>){val a=JSONArray();list.forEach{a.put(JSONObject().put("name",it.name).put("meat",it.meatWait?:JSONObject.NULL).put("dairy",it.dairyWait?:JSONObject.NULL))};p(c).edit().putString("people_json",a.toString()).apply()}
 fun newId(c:Context)=maxOf(System.currentTimeMillis(),(all(c).maxOfOrNull{it.id}?:0L)+1L)
}
object TimerNotifications{
 private fun flags()=PendingIntent.FLAG_UPDATE_CURRENT or if(Build.VERSION.SDK_INT>=23)PendingIntent.FLAG_IMMUTABLE else 0
 private fun channel(c:Context){if(Build.VERSION.SDK_INT>=26){val id="parve_"+(TimerStore.p(c).getString("sound","default")?:"default");val nm=c.getSystemService(NotificationManager::class.java);if(nm.getNotificationChannel(id)==null){val ch=NotificationChannel(id,"אני פרווה",NotificationManager.IMPORTANCE_HIGH);ch.enableVibration(TimerStore.p(c).getBoolean("vibrate",true));if(id.endsWith("silent"))ch.setSound(null,null);nm.createNotificationChannel(ch)};TimerStore.p(c).edit().putString("channel",id).apply()}}
 fun refresh(c:Context){channel(c);val p=TimerStore.p(c);val nm=NotificationManagerCompat.from(c);nm.cancel(1);nm.cancel(2);val active=TimerStore.all(c).filter{it.status=="active"}.sortedBy{it.endAt};nm.cancel(19000);if(active.size>3&&p.getBoolean("persistent",true)){active.forEach{nm.cancel(it.id.hashCode())};val t=active.first();val pi=PendingIntent.getActivity(c,20999,Intent(c,MainActivity::class.java).putExtra("show_timers",true),flags());val n=NotificationCompat.Builder(c,p.getString("channel","parve_default")!!).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(t.name+" פרווה בעוד").setContentText(active.size.toString()+" טיימרים פעילים · בשעה "+android.text.format.DateFormat.getTimeFormat(c).format(java.util.Date(t.endAt))).setWhen(t.endAt).setUsesChronometer(true).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(pi).build();try{nm.notify(19000,n)}catch(_:SecurityException){};return};active.forEach{t->val id=t.id.hashCode();if(p.getBoolean("persistent",true)){val pi=PendingIntent.getActivity(c,20000+(t.id%1000).toInt(),Intent(c,MainActivity::class.java).putExtra("show_timers",true),flags());val title=if(t.name=="אני")"אני פרווה בעוד" else t.name+" פרווה בעוד";val n=NotificationCompat.Builder(c,p.getString("channel","parve_default")!!).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText("בשעה "+android.text.format.DateFormat.getTimeFormat(c).format(java.util.Date(t.endAt))).setWhen(t.endAt).setUsesChronometer(true).setOngoing(true).setOnlyAlertOnce(true).setContentIntent(pi).build();try{nm.notify(id,n)}catch(_:SecurityException){}}else nm.cancel(id)}}
 fun done(c:Context,t:ParveTimer){channel(c);val pi=PendingIntent.getActivity(c,30000+(t.id%1000).toInt(),Intent(c,MainActivity::class.java).putExtra("show_timers",true),flags());val title=if(t.name=="אני")"אני פרווה" else t.name+" פרווה";val n=NotificationCompat.Builder(c,TimerStore.p(c).getString("channel","parve_default")!!).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText("זמן ההמתנה הסתיים").setAutoCancel(true).setContentIntent(pi).build();try{NotificationManagerCompat.from(c).notify(t.id.hashCode() xor Int.MIN_VALUE,n)}catch(_:SecurityException){}}
 fun clear(c:Context,id:Long){val n=NotificationManagerCompat.from(c);n.cancel(id.hashCode());n.cancel(id.hashCode() xor Int.MIN_VALUE)}
}