package lock;

/*
 * Example of how to create a barrier for threads that use a striped lock
 * based on a lock name. Might be useful to ensure initialization of a resources only happens once
 * when there might be multiple requests from a single user.
 */

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Striped;

public class LockByName {

	public static final Striped<Lock> locks = Striped.lazyWeakLock(32768);
	public static final ConcurrentHashMap<String, String> session = new ConcurrentHashMap<String, String>();
	
	AtomicInteger lockNoSleep = new AtomicInteger();
	AtomicInteger lockSleep = new AtomicInteger();
	AtomicInteger noLockNoSleep = new AtomicInteger();
	AtomicInteger acquiredLocks = new AtomicInteger();

	public LockByName() {

		ExecutorService executor = Executors.newCachedThreadPool();
		int count = 100;

		ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
		for (int i = 0; i < count; i++) {
			CallableUserRequest c = new CallableUserRequest("thread " + i);
			FutureTask<String> f = new FutureTask<String>(c);
			futures.add(executor.submit(f));
		}

		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		System.err.println("Completed All Futures : lock size : " + locks.size());
		executor.shutdownNow();

		System.err.println(Joiner.on(",").join("lockNoSleep=" + lockNoSleep, "lockSleep=" + lockSleep, "noLockNoSleep=" + noLockNoSleep,
				"acquiredLocks=" + acquiredLocks));

	}

	class CallableUserRequest implements Callable<String> {

		String name;

		public CallableUserRequest(String name) {
			this.name = name;
		}

		public String call() throws Exception {

			// System.err.println(name + " : Running");

			// String lockName = "Hello"; // All requests from one user
			String lockName = "" + new Random().nextInt(10); // Some requests from the same, some from others
			// String lockName = UUID.randomUUID().toString(); // All random users

			// Checked to make sure nobody has done the expensive call yet.
			if (!session.containsKey(lockName)) {
				System.err.println(name + " : Getting Lock " + lockName);
				Lock lock = locks.get(lockName);
				try {
					lock.lock();
					System.err.println(name + " : Got The Lock : " + lockName);
					acquiredLocks.incrementAndGet();

					if (session.containsKey(lockName)) {
						System.err.println(name + " : Inner Lock Someone already waited on " + lockName);
						lockNoSleep.incrementAndGet();
					} else {
						// This is the expensive call
						System.err.println(name + " : Sleeping : " + lockName);
						Thread.sleep(5000);
						session.put(lockName, "filled");
						lockSleep.incrementAndGet();
					}
				} finally {
					System.err.println(name + " : Releasing The Lock : " + lockName);
					lock.unlock();
				}
			} else {
				System.err.println(name + " : No Lock Needed. Someone already waited on " + lockName);
				noLockNoSleep.incrementAndGet();
			}

			System.err.println(name + " : Finished");

			return "";
		}
	}

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		new LockByName();
		System.err.println("end : " + (System.currentTimeMillis() - start));

	}
}
