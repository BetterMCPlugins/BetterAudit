package litebans.api;

/**
 * Compile-time stub of the LiteBans API, excluded from the built jar
 * (see the maven-jar-plugin excludes in pom.xml). At runtime the real
 * class is provided by the LiteBans plugin. Listener's methods are
 * non-abstract, matching the real API, so subclasses override only
 * what they need.
 */
@SuppressWarnings("unused")
public class Events {

    public static Events get() {
        throw new UnsupportedOperationException("stub");
    }

    public void register(Listener listener) {
        throw new UnsupportedOperationException("stub");
    }

    public void unregister(Listener listener) {
        throw new UnsupportedOperationException("stub");
    }

    public abstract static class Listener {

        public void entryAdded(Entry entry) {
        }

        public void entryRemoved(Entry entry) {
        }
    }
}
