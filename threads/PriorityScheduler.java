package nachos.threads;

import nachos.machine.*;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Comparator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}
	
	/** increase the priority of the thread by 1 if possible*/
	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean flag = true;
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			flag = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return flag;
	}

	/** decrease the priority of the thread by 1 if possible*/
	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean flag = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			flag = false;
		else
			setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return false;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		protected PriorityQueue(){this.me = this;}
		protected PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.holderThread = null;
			this.me = this;
			/** override a Comparator to implement an FIFO priority queue*/
			this.queue = new TreeMap<ThreadState, KThread> (
	                new Comparator<ThreadState>() {
	                    public int compare(ThreadState threadX, ThreadState threadY) {
	                    	int xEfficientPriority = threadX.getEffectivePriority();
	                    	int yEfficientPriority = threadY.getEffectivePriority();
	                    	if(xEfficientPriority > yEfficientPriority)
	                    		return 1;
	                    	else if(xEfficientPriority < yEfficientPriority)
	                    		return -1;
	                    	else{
	                    		Long xEnterQueueTime = threadX.waitingQueue.get(me);
		                    	Long yEnterQueueTime = threadY.waitingQueue.get(me);
		                    	if(xEnterQueueTime < yEnterQueueTime)
		                    		return 1;
		                    	else if(xEnterQueueTime > yEnterQueueTime)
		                    		return -1;
		                    	//WARNING: system clock may be the same!
		                    	else{
		                    		//return 0;
		                    		return Integer.signum(threadX.thread.toString().compareTo(threadY.thread.toString()));
		                    	}
	                    	}
	                    }
	                });
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}
		
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			if(queue.size() != 0){
				acquire(queue.pollLastEntry().getValue());
				return holderThread;
			} else {
				return null;
			}
			// finish implementation
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			// implement me
			if(queue.size() != 0)
				return queue.lastKey();
			else
				return null;
			// finish implementation
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
			// I am lazy XD
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		/** The thread holds the lock of this priority queue. */
		private KThread holderThread;
		/** The priority waiting queue.*/
		private TreeMap<ThreadState, KThread> queue;
		/** Used to replace this in comparator*/
		private final PriorityQueue me;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			this.waitingQueue = new HashMap<PriorityQueue, Long>();
			this.holdingQueue = new HashSet<PriorityQueue>();
			this.effectivePriority = priorityDefault;
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			return effectivePriority;
			// finish implementation
		}
		
		protected void updateEffectivePriority() {
			// implement me
			/** need to remove it here and add again later to maintain order*/
			for (PriorityQueue priorityQueue : waitingQueue.keySet())
				priorityQueue.queue.remove(this);
			
			int oldEffectivePriority = this.effectivePriority;
			this.effectivePriority = this.priority;

			/** calculate the effective priority here*/
			for (PriorityQueue priorityQueue : holdingQueue)
				if (priorityQueue.transferPriority && priorityQueue.queue.size() > 0){
					int donationPriority = priorityQueue.queue.lastKey().getEffectivePriority();
					if (effectivePriority < donationPriority)
						effectivePriority = donationPriority;
				}
			
			for (PriorityQueue priorityQueue : waitingQueue.keySet())
				priorityQueue.queue.put(this, thread);
			
			/** decide whether is required to donate its effective priority*/
			if (oldEffectivePriority != effectivePriority)
				for (PriorityQueue priorityQueue: waitingQueue.keySet())
					if (priorityQueue.transferPriority && priorityQueue.holderThread != null)
						/** pruning to speed up*/
						if(oldEffectivePriority == getThreadState(priorityQueue.holderThread).getEffectivePriority()
							|| effectivePriority > getThreadState(priorityQueue.holderThread).getEffectivePriority())
							getThreadState(priorityQueue.holderThread).updateEffectivePriority();	
			// finish implementation
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			/** pruning to speed up*/
			if (this.priority == priority)
				return;
			this.priority = priority;
			// implement me
			updateEffectivePriority();
			// finish implementation
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 *
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 *
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
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
			if(waitQueue.holderThread != null
					&& this.getEffectivePriority() > getThreadState(waitQueue.holderThread).getEffectivePriority())
				getThreadState(waitQueue.holderThread).updateEffectivePriority();
			// finish implementation
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
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
		/** 
		 * Called when the associated thread has released access to to whatever is
		 * guarded by <tt>waitQueue</tt>
		 */
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

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effectivePriority;
		/** The key store the system clock when associated enters the queue*/
		protected HashMap<PriorityQueue, Long> waitingQueue;
		protected HashSet<PriorityQueue> holdingQueue;
	}
}
