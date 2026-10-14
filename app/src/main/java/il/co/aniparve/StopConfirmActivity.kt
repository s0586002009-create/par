package il.co.aniparve

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class StopConfirmActivity : AppCompatActivity() {
 private var dialog:AlertDialog?=null
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);handle(intent)}
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);dialog?.setOnDismissListener(null);dialog?.dismiss();dialog=null;handle(intent)}
 private fun handle(i:Intent?){
  if(i==null){finish();return}
  if(i.action=="open_timer"){startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("show_timers",true));finish();return}
  val id=i.getLongExtra("timer_id",0L)
  val timer=TimerStore.all(this).firstOrNull{it.id==id&&it.status=="active"}
  if(timer==null){finish();return}
  val source=i.getStringExtra("source")?:"widget"
  val pref=when(source){"notification"->"stop_confirm_notification";"app"->"stop_confirm_app";else->"stop_confirm_widget"}
  if(!TimerStore.p(this).getBoolean(pref,source!="notification")){StopActions.stop(this,id);finish();return}
  dialog=AlertDialog.Builder(this).setTitle("עצירת טיימר").setMessage("לעצור את הטיימר של "+timer.name+"?")
   .setNegativeButton("ביטול"){_,_->finish()}
   .setPositiveButton("עצור"){_,_->StopActions.stop(this,id);finish()}
   .create()
  dialog?.setOnCancelListener{finish()}
  dialog?.setOnDismissListener{if(!isFinishing)finish()}
  dialog?.show()
 }
 override fun onDestroy(){dialog?.setOnDismissListener(null);dialog?.dismiss();dialog=null;super.onDestroy()}
}