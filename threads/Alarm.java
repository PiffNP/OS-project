package nachos.threads;

import nachos.machine.*;
//import java.util.Queue;
//import java.util.Comparator;
//import java.util.PriorityQueue;
import java.util.TreeMap;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
    	Machine.timer().setInterruptHandler(new Runnable() {
    		public void run() { timerInterrupt(); }
	    	});
		queue = new TreeMap<Long, KThread>();
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	boolean intStatus = Machine.interrupt().disable();
	    //* critical section begins */
	    long currentTime = Machine.timer().getTime();
	    /** ready those threads which finish waiting*/
	    while(queue.size() > 0 && queue.firstKey() <= currentTime)
	    	queue.pollFirstEntry().getValue().ready();
	    Machine.interrupt().restore(intStatus);
	    
	    KThread.currentThread().yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	    boolean intStatus = Machine.interrupt().disable();
	    //* critical section begins */
	    long wakeTime = Machine.timer().getTime() + x;
	    queue.put(wakeTime, KThread.currentThread());
        KThread.sleep();
	    //* critical section ends */
        Machine.interrupt().restore(intStatus);
    }
    
    /** the TreeMap stores those waiting threads*/
    private TreeMap<Long, KThread> queue;
};
