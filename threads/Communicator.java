package nachos.threads;

import nachos.machine.*;
import java.util.Queue;
import java.util.LinkedList;
/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */

/**
 * We give two kinds of implementation for communicator. In the first implementation, 
 * speaker and listerner are of dual behaviors. In the second implementation, the speakers
 * are more active while listener are more passive. In default we use the first implementatiion.
 * However, you can set the value of variable solutionFlag as false to use the second one.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        this.lock = new Lock();
        this.speaker = new Condition(this.lock);
        this.listener = new Condition(this.lock);
        this.channel = new Condition(this.lock);
        this.activeSpeaker = 0;
        this.waitingSpeaker = 0;
        this.activeListener = 0;
        this.waitingListener = 0;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	if(solutionFlag){
    		lock.acquire();
    		while(activeListener + waitingListener == 0
    				|| activeSpeaker > 0){
    			waitingSpeaker++;
    			speaker.sleep();
    			waitingSpeaker--;
    		}
    		activeSpeaker++;
    		this.message = word;
    		if(activeListener == 0){
    			listener.wake();
    			channel.sleep();
    			activeSpeaker--;
    			activeListener--;
    			if(waitingSpeaker > 0)
    				speaker.wake();
    			if(waitingListener > 0)
    				listener.wake();
    		} else {
    			channel.wake();
    		}
        	lock.release();
    	} else {
    		lock.acquire();
    		while(waitingListener == 0 || activeSpeaker > 0)
    			speaker.sleep();
    		activeSpeaker++;
    		this.message = word;
    		listener.wake();
        	lock.release();
    	}
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	int word;
    	if(solutionFlag){
    		lock.acquire();
    		while(activeSpeaker + waitingSpeaker == 0
    				|| activeListener > 0){
    			waitingListener++;
    			listener.sleep();
    			waitingListener--;
    		}
    		activeListener++;
    		if(activeSpeaker == 0){
    			speaker.wake();
    			channel.sleep();
        		word = this.message;
    			activeSpeaker--;
    			activeListener--;
    			if(waitingSpeaker > 0)
    				speaker.wake();
    			if(waitingListener > 0)
    				listener.wake();
    		} else {
        		word = this.message;
    			channel.wake();
    		}
    		lock.release();
    		return word;
    	} else {
    		lock.acquire();
    		waitingListener++;
    		speaker.wake();
    		listener.sleep();
    		word = this.message;
    		activeSpeaker--;
    		waitingListener--;
    		speaker.wake();
    		lock.release();
    		return word;
    	}
    }
    
    public static void setSolutionFlag(boolean flag){
    	solutionFlag = flag;
    }
    
    public static boolean getSolutionFlag(){
    	return solutionFlag;
    }
    
    /** the shared varible for speaker and listener*/
    int message;
    Lock lock;
    int activeSpeaker;
    int waitingSpeaker;
    int activeListener;
    int waitingListener;
    /** the condition for waiting speaker*/
    Condition speaker;
    /** the condition for waiting listener*/
    Condition listener;
    /** the condition for active speaker and active listener*/
    Condition channel;
    /** Flag variable to decide to use solution 1 or 2*/
    private static boolean solutionFlag = true;
}
