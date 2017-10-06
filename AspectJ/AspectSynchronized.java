import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;

@Aspect("pertarget(execution (* List.*(..)) && target(*))")
public class AspectSynchronized {
	int headOps;
	boolean popOrprepend = false;
	int attemptsLimit = 20;
	
	AspectSynchronized() {
		headOps = 0;
	}

    @Around("execution (* List.prepend(Object)) && args(obj) && target(list)")
    public void advicePrepend(ProceedingJoinPoint joinPoint, Object obj, List list) 
    throws InterruptedException {
		System.out.println( "Called 'prepend' for node: " + (String)obj 
							+ " on list " + list.getName() + " ("
							+ Thread.currentThread().getName() + ").");
		try {
			// If successfully acquired lock.
			if (this.LockListPopPrepend(list, obj, 
										Thread.currentThread().getName(),
										"prepend")) {
			joinPoint.proceed();
			System.out.println( "Prepended : " + (String)obj + " on list " 
								+ list.getName() + " ("
								+ Thread.currentThread().getName() + ").");
			} else { 
				// Return in the case you have tried too many times.
				return; 
			}
		} finally {
			this.UnlockListPopPrepend(list);
		}
    }

    @Around("execution (* List.pop(..)) && target(list)")
    public Object advicePop(ProceedingJoinPoint joinPoint, List list) 
    throws InterruptedException {
		Object obj = null;

		System.out.println( "Called 'pop' on list " + list.getName() 
							+ " (" + Thread.currentThread().getName()
							+  ").");
    	try {
    		// If successfully acquired lock.
			if (this.LockListPopPrepend(list, null, 
									    Thread.currentThread().getName(),
									    "pop")) {
				obj = joinPoint.proceed();
				System.out.println( "Poped : " + (String)obj +  " from list " 
									+ list.getName() + " ("
									+ Thread.currentThread().getName() + ").");
			} else {
				// Return in the case you have tried too many times.
				return null;
			}
		} finally {
			this.UnlockListPopPrepend(list);
		}
    	return obj;
    }

    @Around("execution (* List.head(..)) && target(list)")
    public Object adviceHead(ProceedingJoinPoint joinPoint, List list) 
    throws InterruptedException {
    	Object obj = null;

		System.out.println( "Called 'head' (" + Thread.currentThread().getName()
							+ ")" +  " on list " + list.getName() + ".");
		try {
			// If successfully acquired lock.
			if (this.LockListHead(list, Thread.currentThread().getName())) {
				obj = joinPoint.proceed();
	    		System.out.println( "Head : " + (String)obj + " on list " 
	    							+ list.getName() + " ("
	    							+ Thread.currentThread().getName() + ").");
			} else {
				// Return in the case you have tried too many times.
				return null;
			}
    	} finally {
			// I have finished reading, if someone wants to do a 
			// 'pop','prepend', or 'head' operation, it's ok.
			this.UnlockListHead(list);
		}
		return obj;
    }

    // The lock for the 'head' operation.
    private boolean LockListHead(List list, String threadName) 
    throws InterruptedException {
    	int attemptsCounter = 0;

    	synchronized(list) {
    		// While there is a 'pop' or 'prepend' in action, wait.
    		while (popOrprepend) {
    			attemptsCounter++;
		    	// If you have tried too many times,
			   	if (attemptsCounter == attemptsLimit) {
			   		// Stop trying.
			   		System.out.println( threadName 
			   							+ " failed to complete 'head'.");
		    		return false;	
		    	}
			   	// Else, wait.
    			list.wait();
    		}
    		// There are no 'pop' or 'prepend' in action, but it is ok if there
    		// are other 'head' operations in action. 
	    	headOps++;
    	}
    	return true;
    }

    private void UnlockListHead(List list) {
    	synchronized(list) {
			headOps--;
			// Wake up any potential 'pop' or 'prepend' operations.
			list.notify();
		}
    }

    // The lock for the 'pop' or 'prepend' operations.
    private boolean LockListPopPrepend(List list, Object obj, String threadName,
						  			String operation) 
	throws InterruptedException {
		int attemptsCounter = 0;
		synchronized(list) {
			// If there are 'head', 'pop', or 'prepend' operations in action, then block.
		    while (headOps > 0 || popOrprepend) {
		    	// Count how many times you tried to execute this action.
		    	attemptsCounter++;
		    	// If you have tried too many times,
			   	if (attemptsCounter == attemptsLimit) {
			   		// Stop trying.
			   		System.out.print( threadName + " failed to complete "
			   						  + operation);
			   		if (obj != null) {
		    			System.out.print(" of " + (String)obj + ".\n");
		    		} else {
		    			System.out.print(".\n");
		    		}		
		    		return false;	
		    	}
			   	// Else, wait.
	           	list.wait();
			}
			// There are no 'head', 'pop', or 'prepend' actions, so I can proceed.
			popOrprepend = true;
		}
		return true;
	}

	private void UnlockListPopPrepend(List list) {
		synchronized(list) {
			// Wake up any potential 'pop' or 'prepend' operations.
			popOrprepend = false;
			list.notify();
		}
	}
}