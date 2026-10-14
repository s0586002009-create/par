package il.co.aniparve

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerStateTransitionsTest {
 private fun timer(id:Long,status:String="active")=ParveTimer(id,"אדם"+id,"meat",0L,100L,100L,status)
 @Test fun stoppingOneTimerLeavesOthersUntouched(){
  val input=listOf(timer(1),timer(2),timer(3,"pending"))
  assertEquals(listOf(timer(2),timer(3,"pending")),TimerStateTransitions.stopActive(input,1))
 }
 @Test fun stoppingSameTimerTwiceIsIdempotent(){
  val once=TimerStateTransitions.stopActive(listOf(timer(1),timer(2)),1)
  assertEquals(once,TimerStateTransitions.stopActive(once,1))
 }
 @Test fun stoppingUnknownOrPendingTimerDoesNotChangeState(){
  val input=listOf(timer(1),timer(2,"pending"))
  assertEquals(input,TimerStateTransitions.stopActive(input,99))
  assertEquals(input,TimerStateTransitions.stopActive(input,2))
 }
}
