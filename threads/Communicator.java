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
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        this.messages=new LinkedList<Integer>();
        this.lock=new Lock();
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
        lock.acquire();
        messages.add(word);
        listenCanSee.wake();
        speakCanQuit.sleep();
        listenCanQuit.wake();
        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        lock.acquire();
        listenCanSee.sleep();
        assert(messages.size()>0);
        int message=messages.poll();
        speakCanQuit.wake();
        listenCanQuit.sleep();
        lock.release();
        return message;
    }
    
    Queue<Integer> messages;
    Lock lock;
    Condition speakCanQuit;
    Condition listenCanSee;
    Condition listenCanQuit;
}
