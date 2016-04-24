package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Random;
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
    	public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}
    	
    	@Override
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}
		
    	@Override
    	public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			if(queue.isEmpty())
				return null;
			else{
				int cnt = 0;
				KThread ret = null;
				for (ThreadState threadState: queue.keySet())
					cnt += threadState.getEffectivePriority();
				int draw = rng.nextInt(cnt) + 1;
				for (ThreadState threadState: queue.keySet()){
					draw -= threadState.getEffectivePriority();
					if (draw <= 0) {
						threadState.acquire(this);
						ret = threadState.thread;
						break;
					}
				}
				return ret;
			}
		}

    	/*@Override
		protected ThreadState pickNextThread() {
			// implement me
			Lib.assertNotReached();
    		return null;
    		// finish implementation
		}*/
    	
    	private Random rng = new Random();
    	private HashMap<ThreadState, KThread> queue;
    }
    
    protected class ThreadState extends PriorityScheduler.ThreadState{
		public ThreadState(KThread thread) {
			this.thread = thread;
			this.waitingQueue = new HashMap<PriorityQueue, Long>();
			this.holdingQueue = new HashSet<PriorityQueue>();
			this.effectivePriority = priorityDefault;
			setPriority(priorityDefault);
		}
		
		public int getPriority() {
			return priority;
		}

		public int getEffectivePriority() {
			return effectivePriority;
		}
		
		public void setPriority(int priority) {
			/** pruning to speed up*/
			if (this.priority == priority)
				return;
			this.priority = priority;
			// implement me
			updateEffectivePriority();
			// finish implementation
		}
		
		@Override
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

		//@Override
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
		
		//@Override
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			if(waitQueue.holderThread != null)
				getThreadState(waitQueue.holderThread).release(waitQueue);
			if(waitingQueue.containsKey(waitQueue)){
				/** The order of following two lines is important
				 *  since we override the comparator. */
				waitQueue.queue.remove(this);
				waitingQueue.remove(waitQueue);
			}
			waitQueue.holderThread = thread;
			holdingQueue.add(waitQueue);
			updateEffectivePriority();
			// finish implementation
		}

		//@Override
		public void release(PriorityQueue waitQueue) {
			// implement me
			//only lock holder can release
			if(waitQueue.holderThread != thread)
				return;
			holdingQueue.remove(waitQueue);
			waitQueue.holderThread = null;
			updateEffectivePriority();
			return;
			// finish implementation
		}
		
		protected HashMap<PriorityQueue, Long> waitingQueue;
		protected HashSet<PriorityQueue> holdingQueue;
    }
}
