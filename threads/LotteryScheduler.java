package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
	@Override
    public ThreadQueue newThreadQueue(boolean transferPriority) {
    	// implement me
		return new PriorityQueue(transferPriority);
		// finish implementation
    }
    
	public static final int priorityDefault = 1;
	public static final int priorityMinimum = 1;
	public static final int priorityMaximum = Integer.MAX_VALUE;

	@Override
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);
		return (ThreadState) thread.schedulingState;
	}
    
    protected class PriorityQueue extends PriorityScheduler.PriorityQueue{
    	protected PriorityQueue(boolean transferPriority) {
    		this.transferPriority = transferPriority;
    		this.holderThread = null;
    		this.queue = new HashMap<ThreadState, KThread>();
    	}

    	@Override
    	public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			return null;
			// implement me
		}

    	@Override
		protected ThreadState pickNextThread() {
			// implement me
    		return null;
		}
    	private KThread holderThread;
    	private HashMap<ThreadState, KThread> queue;
    }
    
    protected class ThreadState extends PriorityScheduler.ThreadState{
		public ThreadState(KThread thread) {
			super(thread);
		}
		
		protected void updateEffectivePriority() {
			// implement me			
			int oldEffectivePriority = this.effectivePriority;

			/** calculate the effective priority here*/
			long cnt = this.priority;
			for (PriorityQueue priorityQueue : holdingQueue)
				if (priorityQueue.transferPriority && priorityQueue.queue.size() > 0){
						for (ThreadState key : priorityQueue.queue.keySet())
							cnt += key.getEffectivePriority();
				}
			if(cnt > Integer.MAX_VALUE)
				cnt = Integer.MAX_VALUE;
			this.effectivePriority = (int)cnt;
			
			/** decide whether is required to donate its effective priority*/
			if (oldEffectivePriority != effectivePriority)
				for (PriorityQueue priorityQueue: waitingQueue.keySet())
					if (priorityQueue.transferPriority && priorityQueue.holderThread != null)
							getThreadState(priorityQueue.holderThread).updateEffectivePriority();	
			// finish implementation
		}

		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			if(waitingQueue.containsKey(waitQueue))
				return;
			/** It works when a lock holder go to sleep */
			release(waitQueue);
			/**
			 * The method should only be called if the associated thread cannot
			 * immediately obtain access
			 */
			long startWaitingTime = Machine.timer().getTime();
			/** The order of following two lines is important
			 *  since we override the comparator. */
			waitingQueue.put(waitQueue, startWaitingTime);
			waitQueue.queue.put(this, thread);
			/** Donate its priority if needed*/
			if(waitQueue.holderThread != null && waitQueue.transferPriority)
				getThreadState(waitQueue.holderThread).updateEffectivePriority();
			// finish implementation
		}
		protected HashMap<PriorityQueue, Long> waitingQueue;
		protected HashSet<PriorityQueue> holdingQueue;
    }
}
