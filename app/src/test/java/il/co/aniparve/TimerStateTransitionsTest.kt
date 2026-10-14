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
 @Test fun widgetUsesFreshActiveTimersSortedByEndTime(){
  val input=listOf(timer(1).copy(endAt=300L),timer(2,"pending").copy(endAt=10L),timer(3).copy(endAt=100L))
  assertEquals(listOf(3L,1L),TimerStateTransitions.activeSorted(input).map{it.id})
 }
 @Test fun hidingDairyAlwaysRoutesAddToMeat(){
  assertEquals("meat",TimerStateTransitions.typeForAdd(false,"dairy"))
  assertEquals("meat",TimerStateTransitions.typeForAdd(false,null))
 }
 @Test fun visibleDairyCanBeChosen(){
  assertEquals("dairy",TimerStateTransitions.typeForAdd(true,"dairy"))
  assertEquals("meat",TimerStateTransitions.typeForAdd(true,"meat"))
 }
}
