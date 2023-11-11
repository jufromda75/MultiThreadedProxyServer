
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class My_Cache<T> {

	/** Object storage */
	private final Map<String, T> obj;
	/** stores the expiration time */
	private final Map<String, Long> expire;
	private final long default_Expire;
	/** used to enhance performance of operations */
	private final ExecutorService threads;

	/**Constructs the cache with a default expiration time of 86400 seconds**/
	
	public My_Cache() {
		this(86400);
	}

	/**Construct a cache with a custom expiration date for the object**/
	
	public My_Cache(final long default_Expire) {
		this.obj = Collections.synchronizedMap(new HashMap<String, T>());
		this.expire = Collections.synchronizedMap(new HashMap<String, Long>());

		this.default_Expire = default_Expire;

		this.threads = Executors.newFixedThreadPool(256);
		Executors.newScheduledThreadPool(2).scheduleWithFixedDelay(this.removeExpired(), this.default_Expire / 2, this.default_Expire, TimeUnit.SECONDS);
	}

	/**It removes expired object**/
	
	private final Runnable removeExpired() {
		return new Runnable() {
			public void run() {
				for (final String name : expire.keySet()) {
					if (System.currentTimeMillis() > expire.get(name)) {
						threads.execute(createRemoveRunnable(name));
					}
				}
			}
		};
	}

	/** It Returns a runnable that removes a specific object from the cache**/
	
	private final Runnable createRemoveRunnable(final String name) {
		return new Runnable() {
			public void run() {
				obj.remove(name);
				expire.remove(name);
			}
		};
	}

	/**Returns the default expiration time for the obj in the cache **/
	 
	public long getExpire() {
		return this.default_Expire;
	}

	/** Put an object into the cache **/
	
	public void put(final String name, final T obj) {
		this.put(name, obj, this.default_Expire);
	}

	/**Put an object into the cache with expiration date **/
	
	public void put(final String name, final T obj, final long expireTime) {
		this.obj.put(name, obj);
		this.expire.put(name, System.currentTimeMillis() + expireTime * 1000);
	}

	/**Returns an object from the cache**/
	
	public T get(final String name) {
		final Long expireTime = this.expire.get(name);
		if (expireTime == null) return null;
		if (System.currentTimeMillis() > expireTime) {
			this.threads.execute(this.createRemoveRunnable(name));
			return null;
		}
		return this.obj.get(name);
	}

	
	@SuppressWarnings("unchecked")
	public <R extends T> R get(final String name, final Class<R> type) {
		return (R) this.get(name);
	}
}