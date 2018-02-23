package net.maxgigapop.mrs.driver.ccsn;

import java.util.HashMap;

class GlobalPersistentStore {
	
	private static
	GlobalPersistentStore store = null;
	
	static
	GlobalPersistentStore get() {
		if (store == null)
			store = new GlobalPersistentStore();
		return store;
	}
	
	private
	HashMap<String, HashMap<String, Object>> registry = null;
	
	void create(String uid) {
		if (registry == null)
			registry = new HashMap<>();
		registry.put(uid, new HashMap<String, Object>());
	}
	
	boolean exists(String uid) {
		return (registry != null && registry.containsKey(uid));
	}
	
	boolean contains(String uid, String key) {
		return (exists(uid) && registry.get(uid).containsKey(key));
	}
	
	Object get(String uid, String key) {
		if (!registry.containsKey(uid))
			throw new IllegalArgumentException("Object store with uid:" + uid + " not found");
		return registry.get(uid).get(key);			
	}

	void add(String uid, String key, Object val) {
		if (!registry.containsKey(uid))
			throw new IllegalArgumentException("Object store with uid:" + uid + " not found");
		registry.get(uid).put(key, val);
	}

	private GlobalPersistentStore() {}
}
