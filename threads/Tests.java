package nachos.threads;
import nachos.machine.*;

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
			
		    unitTestInit("basic two threads joining");
			KThread target = new KThread(new JoinTest(null)).setName("target");
		    KThread joiner = new KThread(new JoinTest(target)).setName("joiner");
		    joiner.fork();
			target.fork();
			unitTestStart();
		    unitTestCheck(true);
		    
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
			
		    unitTestInit("call joining twice");
		    target = new KThread(new JoinTest(null)).setName("target");
		    joiner = new KThread(new JoinTest(target)).setName("joiner1");
		    KThread joiner2 = new KThread(new JoinTest(target)).setName("joiner2");
			joiner.fork();
			joiner2.fork();
			target.fork();
			unitTestStart();
			unitTestCheck(false);
		} else if(testType == PrioritySchedulerTest){
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
		    unitTestCheck(true);
			PriorityScheduler.selfTest();
		} else if(testType == BoatTest){
		    Boat.selfTest();
		}
	}
	
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
}

