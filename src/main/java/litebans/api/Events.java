package litebans.api;

/**
 * COMPILE-TIME STUB — excluded from the built jar (see maven-jar-plugin
 * excludes in pom.xml). The real class ships with LiteBans at runtime.
 * Listener's methods are deliberately non-abstract, mirroring the real API,
 * so subclasses override only what they need.
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
