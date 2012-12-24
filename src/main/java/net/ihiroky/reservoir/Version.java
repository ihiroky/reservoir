package net.ihiroky.reservoir;

/**
 * Prints manifest properties; Implementation-Title and Implementation-Version.
 *
 * @author Hiroki Itoh
 */
public final class Version {

    private Version() {
        throw new AssertionError("Instantiation impossible.");
    }

    /**
     * Prints version.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        System.out.println(version());
    }

    /**
     * Create version {@code String}; manifest properties : Implementation-Title and Implementation-Version.
     * @return version {@code String}; manifest properties : Implementation-Title and Implementation-Version.
     */
    public static String version() {
        Package pkg = Version.class.getPackage();
        String title = pkg.getImplementationTitle();
        String version = pkg.getImplementationVersion();
        return title + ' ' + version;
    }
}
