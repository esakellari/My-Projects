// Έχω παραδόσει αυτό το αρχείο όχι για να μετρήσει στην τελική
// βαθμολόγηση της εργασίας, αλλά απλά επειδή υλοποίησα το aspect 
// και με locks και θα ήθελα απλά να δείτε αν θα δούλευε επίσης σωστά.
// Για περισσότερες λεπτομέρειες δείτε το README. 
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

@Aspect
public class AspectLocks {
	int headOps;
	Lock lock;
	Condition condition;

	AspectLocks() {
		headOps = 0;
		lock = new ReentrantLock();
		condition = lock.newCondition();
	}

    @Around("execution (* List.prepend(Object)) && args(obj) && target(list)")
    public void advicePrepend(ProceedingJoinPoint joinPoint, Object obj, List list) 
    throws InterruptedException {
    	System.out.printf("'prepend' called with argument: %s.\n", (String)obj);

    	// The 'prepend' operation will block on the lock only when a 'pop'
    	// or another 'prepend' operation takes place.
		if(lock.tryLock(100, TimeUnit.MILLISECONDS)) {
			try{
	    		// If there are 'head' operations in action, then block.
		    	while ( headOps > 0 ) { System.out.println("prepend will wait for " + headOps + " headOps");
		    		condition.await();
		    	}
		    	// I have been woken up and I can proceed.
				joinPoint.proceed();
			} finally {
				System.out.println("Prepended : " + (String)obj);
				// Wake up any potential 'pop' or 'prepend' operations.
				condition.signal();
				lock.unlock();
			}
		} else { 
    		System.out.println("Prepend did not acquire lock."); 
    	}
    }

    @Around("execution (* List.pop(..)) && target(list)")
    public Object advicePop(ProceedingJoinPoint joinPoint, List list) 
    throws InterruptedException {
    	System.out.println("'pop' called.");
		Object obj = null;
    	// The 'pop' operation will block on the lock only when a 'prepend'
    	// or another 'pop' operation takes place.
    	if(lock.tryLock(100, TimeUnit.MILLISECONDS)) {
			try{
			    // If there are 'head' operations in action, then block.
				while (headOps > 0) { System.out.println("pop will wait for " + headOps + " headOps");
					condition.await();
				}
				// I have been woken up and I can proceed.
				obj = joinPoint.proceed();
			} finally {
				System.out.println( "Poped : " + (String)obj);
				// Wake up any potential 'pop' or 'prepend' operations.
				condition.signal();
				lock.unlock();
			}
    	} else { 
    		System.out.println("Pop did not acquire lock."); 
    	}
    	return obj;
    }

    @Around("execution (* List.head(..)) && target(list)")
    public Object adviceHead(ProceedingJoinPoint joinPoint, List list) 
    throws InterruptedException {
    	System.out.println("'head' called.");
    	Object obj = null;
    	boolean acquiredLock = false;

    	// The 'head' operation will block only when a 'pop' or 'prepend'
    	// operation takes place (or when another 'head' operation is changing
    	// the headOps counter).
    	// Every 'head' operation passes through this stage in order to increase
    	// the 'headOps' counter by 1. This will make any 'pop' or 'prepend'
    	// operation wait on the condition until headOps == 0.
    	// On the other hand, this does not affect any other 'head' operations
    	// which do not check on the 'headOps' counter and are allowed to operate
    	// simultaneously.
    	if(lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    		// If the lock has been successfully acquired, it means that no
    		// 'prepend','pop', or 'head' operations are in action.
    		acquiredLock = true;
    		try {
    			// Increase the counter 'headOps' in order to block 'pop' or 
				// 'prepend' actions.
	    		headOps++;
    		} finally {
    			// And unlock to allow potential 'head' operations to take place.
	    		lock.unlock();
    		}
    	} else {
    		acquiredLock = false;
    	}

    	if(acquiredLock) {
    		obj = joinPoint.proceed();
	    	System.out.println( "Head :	" + (String)obj);
    	
			// I have finished reading, if someone wants to do a 
			// 'pop','prepend', or 'head' operation, it's ok.
			// Use simple lock here because we surely want to 
			// access the critical section in order to reduce
			// the 'headOps' counter that was previously increased.
			lock.lock();
			try {
				headOps--;
				// Wake up any potential 'pop' or 'prepend' operations.
				condition.signal();
			} finally {
				lock.unlock();
			}
		}
		return obj;
    }
}