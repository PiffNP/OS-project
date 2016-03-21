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
						System.out.println("running target");
						return;
					}else{
						System.out.println("running join");
						thread.join();
						System.out.println("end join");
					}
				}
				
				private KThread thread;
			}
			
			KThread target = new KThread(new JoinTest(null)).setName("target");
		    KThread joiner = new KThread(new JoinTest(target)).setName("join");
		    joiner.fork();
		    target.fork();
		    KThread.yield();
		    KThread.yield();
		}
	}
	
	/** tests type */
	public static final int KThreadJoinTest = 0;
}

