package il.co.aniparve

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class StopConfirmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra("timer_id", 0L)
        val name = intent.getStringExtra("timer_name") ?: "אני"
        val timer = TimerStore.all(this).firstOrNull { it.id == id && it.status == "active" }
        if (timer == null) {
            finish()
            return
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("עצירת טיימר")
            .setMessage("להפסיק את הטיימר של $name?")
            .setNegativeButton("לא") { _, _ -> finish() }
            .setPositiveButton("כן") { _, _ ->
                TimerScheduler.cancel(this, id)
                TimerStore.remove(this, id)
                TimerNotifications.refresh(this)
                il.co.aniparve.widget.WidgetUpdater.updateAll(this)
                finish()
            }
            .create()
        dialog.setOnCancelListener { finish() }
        dialog.setOnDismissListener { if (!isFinishing) finish() }
        dialog.show()
    }
}
