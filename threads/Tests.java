package nachos.threads;
import nachos.machine.*;
import nachos.ag.BoatGrader;
import java.util.Random;
/** Various tests done here*/
public class Tests{	
	public static void test(int testType){		
		if(testType == KThreadJoinTest){
			class JoinTest implements Runnable {
				JoinTest(KThread thread) {
					this.thread = thread;
				}

				public void run() {
					if(thread == null){
						orderCheck(2, "running target");
						return;
					}else{
						orderCheck(1, "running join");
						try{
							thread.join();
							orderCheck(3, "end join");
						} catch (Error e){
							System.out.println("Error caught.");
							flag = false;
						} finally{
							unitTestEnd();
						}
					}
				}
				
				private KThread thread;
			}
			{
				/** basic function test*/
				unitTestInit("basic two threads joining");
				KThread target = new KThread(new JoinTest(null)).setName("target");
				KThread joiner = new KThread(new JoinTest(target)).setName("joiner");
				joiner.fork();
				target.fork();
				unitTestStart();
				unitTestCheck(true);
			}
			
			{
				/** test if a thread can join itself*/
				unitTestInit("self joining");
				final KThread selfJoin = new KThread().setName("selfJoin");
				selfJoin.setTarget(new Runnable() {
					public void run() {
						try{
							selfJoin.join();
						} catch (Error e){
							System.out.println("Error caught.");
							flag = false;
						} finally{
							unitTestEnd();
						}
					}
				});
				selfJoin.fork();
				unitTestStart();
				unitTestCheck(false);
			}
			
			class JoinTest2 implements Runnable {
				JoinTest2(KThread thread) {
					this.thread = thread;
				}

				public void run() {
					if(thread == null){
						Lib.debug(dbgThread, "running target");
						return;
					}else{
						Lib.debug(dbgThread, "running join");
						try{
							thread.join();
							Lib.debug(dbgThread, "end join");
						} catch (Error e){
							System.out.println("Error caught.");
							flag = false;
						} finally{
							unitTestEnd();
						}
					}
				}
				
				private KThread thread;
			}
			{
				/** test if a thread can be joined twice*/
				unitTestInit("call joining twice");
				KThread target = new KThread(new JoinTest2(null)).setName("target");
				KThread joiner = new KThread(new JoinTest2(target)).setName("joiner1");
				KThread joiner2 = new KThread(new JoinTest2(target)).setName("joiner2");
				joiner.fork();
				joiner2.fork();
				target.fork();
				unitTestStart();
				unitTestCheck(false);
			}
			
			class JoinTest3 implements Runnable {
				JoinTest3(KThread thread) {
					this.thread = thread;
				}

				public void run() {
					if(thread == null){
						orderCheck(1, "running target");
						return;
					}else{
						orderCheck(2, "running join");
						try{
							thread.join();
							orderCheck(3, "end join");
						} catch (Error e){
							System.out.println("Error caught.");
							flag = false;
						} finally{
							unitTestEnd();
						}
					}
				}
				
				private KThread thread;
			}
			{
				/** test if it works well when a finished thread being joined*/
				unitTestInit("join a finished thread");
				KThread target = new KThread(new JoinTest(null)).setName("target");
				KThread joiner = new KThread(new JoinTest(target)).setName("joiner");
				target.fork();
				joiner.fork();
				unitTestStart();
				unitTestCheck(true);
			}
			
			class JoinDonationTest implements Runnable {
				JoinDonationTest(int id, KThread thread) {
					this.id = id;
					this.thread = thread;
				}

				public void run() {
					if(thread == null){
						Lib.debug(dbgThread, id + " runs");
						if(id == 2)
							unitTestEnd();
						return;
					}else{
						Lib.debug(dbgThread, id + " starts joining");
						try{
							thread.join();
							Lib.debug(dbgThread, id + " ends joining");
						} catch (Error e){
							System.out.println("Error caught.");
							flag = false;
						} finally{
							
						}
					}
				}
				private int id;
				private KThread thread;
			}
			{
				/** test if priority donation works well when a thread being joined*/
				unitTestInit("priority donation when joining");
				KThread kt1 = new KThread(new JoinDonationTest(1, null)).setName("1");
				KThread kt2 = new KThread(new JoinDonationTest(2, null)).setName("2");
				KThread kt3 = new KThread(new JoinDonationTest(3, kt1)).setName("3");
				kt1.fork();
				kt2.fork();
				kt3.fork();
				boolean intStatus = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority(kt1, 2);
				ThreadedKernel.scheduler.setPriority(kt2, 3);
				ThreadedKernel.scheduler.setPriority(kt3, 4);
				Machine.interrupt().restore(intStatus);
				unitTestStart();
				System.out.println("(Required to be checked manually.)");
			}
		} else if(testType == ConditionTest){
			class ConditionTest implements Runnable {
				public void run() {
					Condition2 cond = new Condition2(new Lock());
					try {
						cond.sleep();
						flag = false;
					}
					catch (Error e) {
						System.out.println("error caught: sleep without lock");
					}
					try {
						cond.wake();
						flag = false;
					}
					catch (Error e) {
						System.out.println("error caught: wake without lock");
					}
					try {
						cond.wakeAll();
						flag = false;
					}
					catch (Error e) {
						System.out.println("error caught: wakeAll without lock");
					}
					unitTestEnd();
				}
			}
			{
				unitTestInit("Condition2 basic security test");
				KThread kt1 = new KThread(new ConditionTest()).setName("ConditionTest1");
				kt1.fork();
				unitTestStart();
				unitTestCheck(true);
			}
			
			class ConditionTest2 implements Runnable {
				ConditionTest2(Lock lock, Condition2 cond, KThread target){
					this.lock = lock;
					this.cond = cond;
					this.target = target;
				}
				public void run() {
					if(target == null){
						lock.acquire();
						cond.sleep();
						flag = lock.isHeldByCurrentThread();
						lock.release();
						unitTestEnd();
					} else {
						lock.acquire();
						cond.wake();
						lock.release();
					}
				}
				private Lock lock;
				private Condition2 cond;
				private KThread target = null;				
			}
			{
				unitTestInit("Condition2 wake up with holding the lock.");
				Lock lock = new Lock();
				Condition2 cond = new Condition2(lock);
				KThread kt1 = new KThread(new ConditionTest2(lock, cond, null)).setName("sleeper");
				KThread kt2 = new KThread(new ConditionTest2(lock, cond, kt1)).setName("waker");
				kt1.fork();
				kt2.fork();
				unitTestStart();
				unitTestCheck(true);
			}
			{
				class ZCounter {
					public ZCounter init(){
						sleepCounter = awakeCounter = 0;
						return this;
					}
					
					public int sleepCounter;
					public int awakeCounter;
				}
				
				class ConditionTest3 implements Runnable {
					ConditionTest3(Lock lock, Condition2 cond, ZCounter mCounter){
						this.lock = lock;
						this.cond = cond;
						this.mCounter = mCounter;
					}
					
					public void run() {
						lock.acquire();
						mCounter.sleepCounter++;
						cond.sleep();
						mCounter.awakeCounter++;
						lock.release();
					}
					private Lock lock;
					private Condition2 cond;
					private ZCounter mCounter;
				}
								
				ZCounter mCounter = new ZCounter().init();

				Lock lock = new Lock();
				Condition2 cond = new Condition2(lock);
				final int total = 5;
				
				unitTestInit("Condition2 wake() wakes only one process.");
				for(int i = 0; i < total; i++)
					new KThread(new ConditionTest3(lock, cond, mCounter)).fork();
				while(mCounter.sleepCounter != total){
					KThread.yield();
				}
				lock.acquire();
				cond.wake();
				lock.release();
				KThread.yield();
				flag = (mCounter.awakeCounter == 1);
				unitTestCheck(true);
				
				unitTestInit("Condition2 wakeall() wakes all processes.");
				lock.acquire();
				cond.wakeAll();
				lock.release();
				KThread.yield();
				flag = (mCounter.awakeCounter == total);
				unitTestCheck(true);
				
			}
		} else if(testType == AlarmTest){
			unitTestInit("waitUntil test");
			Alarm test = new Alarm();
			for (int i = 0; i < 5; i++){
				long waitTime = (long)(Math.random() * 1000000);
				long s, t;
				System.out.println((s = Machine.timer().getTime()) + ": Plan to wait for " + waitTime + " ticks.");
				test.waitUntil(waitTime);
				System.out.println((t = Machine.timer().getTime()) + ": Wake after " + (t - s) + " ticks.");
			}
		} else if(testType == CommunicatorTest){
			/**
			 * we have implemented two kinds of Communicators. We only give test for the first
			 * implementation. To test the other one, use Communicator.setSolutionFlag(false)
			 * and change orderCheck number respectively. 
			 * */
			
			class CommunicatorTest implements Runnable {
				CommunicatorTest(int which, Communicator communicator, int pre, int suf, boolean endFlag){
		    		this.which = which;
		    		this.communicator = communicator;
		    		this.pre = pre;
		    		this.suf = suf;
		    		this.endFlag = endFlag;
		    	}
				CommunicatorTest(int which, Communicator communicator, int pre, int suf){
		    		this.which = which;
		    		this.communicator = communicator;
		    		this.pre = pre;
		    		this.suf = suf;
		    		this.endFlag = false;
		    	}
				public void run() {
					orderCheck(pre);
					if(which < 0){
						Lib.debug(dbgThread, "listener " + (-which) + ": ready");
						Lib.debug(dbgThread, "listener " + (-which) + ": " + communicator.listen());
						Lib.debug(dbgThread, "listener " + (-which) + ": finishes listening");
					} else {
						Lib.debug(dbgThread, "speaker " + which + ": ready");
						communicator.speak(which);
						Lib.debug(dbgThread, "speaker " + which + ": finishes speaking");
					}
					orderCheck(suf);
					if(endFlag)
						unitTestEnd();
				}
				private int which, pre, suf;
				private boolean endFlag;
		    	private Communicator communicator;
		    }
			{
				unitTestInit("1-1 communication: speaker comes first");
				Communicator mCom = new Communicator();
				KThread kt1 = new KThread(new CommunicatorTest(-1, mCom, 1, 3));
				KThread kt2 = new KThread(new CommunicatorTest(1, mCom, 2, 4, true));
				kt1.fork();
				kt2.fork();
				unitTestStart();
				unitTestCheck(true);
			}
			{
				unitTestInit("1-1 communication: listener comes first");
				Communicator mCom = new Communicator();
				KThread kt1 = new KThread(new CommunicatorTest(1, mCom, 1, 3));
				KThread kt2 = new KThread(new CommunicatorTest(-1, mCom, 2, 4, true));
				kt1.fork();
				kt2.fork();
				unitTestStart();
				unitTestCheck(true);
			}
			{
				unitTestInit("multiple listeners and speakers");
				Communicator mCom = new Communicator();
				KThread kt[] = new KThread[6];
				kt[0] = new KThread(new CommunicatorTest(-1, mCom, 0, 0));
				kt[1] = new KThread(new CommunicatorTest(-2, mCom, 0, 0));
				kt[2] = new KThread(new CommunicatorTest(1, mCom, 0, 0));
				kt[3] = new KThread(new CommunicatorTest(2, mCom, 0, 0));
				kt[4] = new KThread(new CommunicatorTest(3, mCom, 0, 0));
				kt[5] = new KThread(new CommunicatorTest(-3, mCom, 0, 0));
				for(int i = 0; i < 6; i++)
					kt[i].fork();
				while(orderCounter != 12)
					KThread.yield();
				System.out.println("(Required to be checked manually.)");
			}
		} else if(testType == PrioritySchedulerTest){
			class LockDonationTest implements Runnable {
				LockDonationTest(Lock lock) {
					this.lock = lock;
				}

				public void run() {
					Lib.debug(dbgThread, "thread with high priority asks for the lock.");
					orderCheck(1);
					lock.acquire();
					orderCheck(3);
					Lib.debug(dbgThread, "thread with high priority gets the lock.");
					lock.release();
					Lib.debug(dbgThread, "thread with high priority releases the lock.");
				}
				private Lock lock;
			}
	
			{
				unitTestInit("priority donation when waiting access to a lock");
				Lock lock = new Lock();
				lock.acquire();
				Lib.debug(dbgThread, "main thread acquires the lock.");
				KThread kt1 = new KThread(new LockDonationTest(lock)).setName("high");
				boolean intStatus = Machine.interrupt().disable();
				ThreadedKernel.scheduler.setPriority(kt1, 2);				
				Machine.interrupt().restore(intStatus);
				kt1.fork();
				KThread.yield();
				Lib.debug(dbgThread, "main thread releases the lock.");
				orderCheck(2);
				lock.release();
				KThread.yield();
				orderCheck(4);
				unitTestCheck(true);
			}
			{
				unitTestInit("priority donation transitivity");
				Lock lock = new Lock();
				lock.acquire();
				Lib.debug(dbgThread, "main thread acquires the lock.");
				final int thread_num = 6;
				final int queue_num = 3;
				KThread kt[] = new KThread[thread_num];
				for(int i = 0; i < thread_num; i++)
					kt[i] = new KThread().setName(i + "");
				ThreadQueue queue[] = new ThreadQueue[queue_num];
				for(int i = 0; i < queue_num; i++)
					queue[i] = ThreadedKernel.scheduler.newThreadQueue(true);
				boolean intStatus = Machine.interrupt().disable();
				try{
					for(int i = 0; i < 3; i++){
						queue[i].acquire(kt[i + 1]);
						queue[i].waitForAccess(kt[i]);
					}
					ThreadedKernel.scheduler.setPriority(kt[0], 2);
					Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt[3]) == 2);
					ThreadedKernel.scheduler.setPriority(kt[4], 3);
					queue[0].waitForAccess(kt[4]);
					Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt[3]) == 3);
					queue[0].acquire(kt[4]);
					Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt[3]) == 1);
					queue[0].waitForAccess(kt[1]);
					queue[2].waitForAccess(kt[4]);
					ThreadedKernel.scheduler.setPriority(kt[1], 4);					
					Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt[3]) == 4);
					ThreadedKernel.scheduler.setPriority(kt[1], 1);
					Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt[3]) == 3);
				} catch (Error e){
					flag = false;
				}
				Machine.interrupt().restore(intStatus);
				unitTestCheck(true);
				unitTestInit("pressure test, designate locks to random processes with random priority");
				
				class LockPressureTest implements Runnable {
					LockPressureTest(Lock lock, Lock lock2) {
						this.lock = lock;
						this.lock2 = lock2;
					}

					public void run() {
						if(lock==lock2){
							return;	
						}
						for(int i=0;i<100;i++){
							lock.acquire();
							lock2.acquire();
							lock2.release();
							lock.release();
						}
					}
					private Lock lock;
					private Lock lock2;
				}	
			{
				Lock  locks[]=new Lock[20];
				KThread threads[]=new KThread[200];
				Random r1=new Random(42);
				intStatus = Machine.interrupt().disable();
				for(int i=0;i<200;i++){
					threads[i] = new KThread(new LockPressureTest(locks[r1.nextInt(20)],locks[r1.nextInt(20)]));
					ThreadedKernel.scheduler.setPriority(threads[i], 2+r1.nextInt(6));				
				}
				for(int i=0;i<200;i++){
					threads[i].fork();
				}
				Machine.interrupt().restore(intStatus);
				KThread.yield();
			}
				
			}
		} else if(testType == BoatTest){
		    BoatGrader b = new BoatGrader();
		   	unitTestInit("BoatTest with 0 adult, 2 children");
		    Boat.begin(0, 2, b);
		    unitTestInit("BoatTest with 2 adult, 4 children");
		    Boat.begin(2, 4, b);
		   	unitTestInit("BoatTest with10 adult, 10 children");
		    Boat.begin(10, 10, b);
		} 	
	}	
	
	/** Test Tools */
	private static void unitTestInit(String description){
		System.out.println("\nTest #" + (++testCounter) + ": " + description);
		lock = new Lock();
		condition = new Condition(lock);
		flag = true;
		orderCounter = 0;
	}
	
	private static void unitTestStart(){
	    lock.acquire();
	    condition.sleep();
		lock.release();
	}
	
	private static void unitTestEnd(){
	    lock.acquire();
	    condition.wake();
		lock.release();
	}
	
	/** order index starts with 1*/
	private static void orderCheck(int order){
		if(++orderCounter != order)
			flag = false;
		//System.out.println(orderCounter + " " + order);
	}
	private static void orderCheck(int order, String message){
		orderCheck(order);
		Lib.debug(dbgThread, message);
	}
	
	private static void unitTestCheck(boolean flagObjState){
		System.out.println("Test #" + testCounter + ' ' + (flag == flagObjState? "passed" : "failed") + ".\n");
		KThread.yield();
	}

	/** Test Variables*/
	private static int testCounter = 0;
	private static Lock lock;
	private static Condition condition;
	private static boolean flag;
	private static int orderCounter;
	
	private static final char dbgThread = 'z';
	
	/** tests type */
	public static final int KThreadJoinTest = 0;
	public static final int PrioritySchedulerTest = KThreadJoinTest + 1;
	public static final int BoatTest = PrioritySchedulerTest + 1;
	public static final int CommunicatorTest = BoatTest + 1;
	public static final int AlarmTest = CommunicatorTest + 1;
	public static final int ConditionTest = AlarmTest + 1;
}

