package net.ihiroky.reservoir;

/**
 * Prints manifest properties : Implementation-Title and Implementation-Version.
 *
 * @author Hiroki Itoh
 */
public final class Version {

    private Version() {
        throw new AssertionError("Instantiation impossible.");
    }

    /**
     * Prints manifest properties : Implementation-Title and Implementation-Version.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        Package pkg = Version.class.getPackage();
        String title = pkg.getImplementationTitle();
        String version = pkg.getImplementationVersion();
        System.out.println(title + ' ' + version);
    }
}
