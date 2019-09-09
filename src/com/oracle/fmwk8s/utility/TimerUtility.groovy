package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Log
import java.time.LocalDateTime

class TimerUtility {
    private static int count =0
    private static int maxTimerPeriod =2

    static void main(String[] args) {
        TimerUtility timerUtility = new TimerUtility()
        timerUtility.startTimer()
    }

    static startTimer(script) {
        Log.info(script, "Timer check start")
        Timer timer = new Timer()
        TimerTask timerTask = new TimerTask() {
            @Override
            void run() {
                Log.info(script,"Inside the run method")
                count++
                Calendar cal = Calendar.getInstance()
                cal.setTimeInMillis(System.currentTimeMillis())
                String date = cal.get(Calendar.DATE)+"-"+cal.get(Calendar.MONTH)+"-"+cal.get(Calendar.YEAR)
                String time = cal.get(Calendar.HOUR)+"-"+cal.get(Calendar.MINUTE)+"-"+cal.get(Calendar.SECOND)
                println( "Date : " +  date + " time : "+ time + " count : " + count)
                if (count> maxTimerPeriod ){
                    timer.cancel()
                    timer.purge()
                }
            }
        }
        // scheduling the task for every 1 minute
        timer.schedule(timerTask,100, 60000)
    }

}
