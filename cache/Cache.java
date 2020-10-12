package cache;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Cache {
    private HashMap<String, String> pairs = new HashMap<String, String>();
    private HashMap<String, Long> expiration = new HashMap<String, Long>();
    private static int CACHE_TTL = 60000;

    public Cache() {
        Timer t = new Timer();

        // Delete expired pairs in a separate thread every CACHE_TTL milliseconds
        t.scheduleAtFixedRate(this.removeExpiredPairs(), CACHE_TTL, CACHE_TTL);
    }

    public synchronized String get(String key) {
        if (this.isExpired(key)) {
            return null;
        }

        return this.pairs.get(key);
    }

    public synchronized void put(String key, String value) {
        this.pairs.put(key, value);
        this.expiration.put(key, System.currentTimeMillis() + CACHE_TTL);
    }

    /* HELPERS */

    private boolean isExpired(String key) {
        Long ttl = this.expiration.get(key);

        if (ttl == null) {
            return true;
        }

        return System.currentTimeMillis() > ttl;
    }

    private TimerTask removeExpiredPairs() {
        return new TimerTask() {
            @Override
            public void run() {
                pairs.keySet().removeIf(key -> isExpired(key));
                expiration.keySet().removeIf(key -> isExpired(key));
            }
        };
    }
}