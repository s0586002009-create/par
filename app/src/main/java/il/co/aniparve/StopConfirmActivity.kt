package il.co.aniparve

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class StopConfirmActivity : AppCompatActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val action=intent.action
  if(action=="open_timer"){
   startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("show_timers",true))
   finish();return
  }
  val id=intent.getLongExtra("timer_id",0L)
  val timer=TimerStore.all(this).firstOrNull{it.id==id&&it.status=="active"}
  if(timer==null){finish();return}
  val source=intent.getStringExtra("source")?:"widget"
  val pref=when(source){"notification"->"stop_confirm_notification";else->"stop_confirm_widget"}
  if(!TimerStore.p(this).getBoolean(pref,true)){StopActions.stop(this,id);finish();return}
  val dialog=AlertDialog.Builder(this)
   .setTitle("עצירת טיימר")
   .setMessage("לעצור את הטיימר של ${timer.name}?")
   .setNegativeButton("ביטול"){_,_->finish()}
   .setPositiveButton("עצור"){_,_->StopActions.stop(this,id);finish()}
   .create()
  dialog.setOnCancelListener{finish()}
  dialog.setOnDismissListener{if(!isFinishing)finish()}
  dialog.show()
 }
}