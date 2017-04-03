import javaslang.collection.List;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import utils.Console;
import utils.Run;

public class Main {

    public static void main(String... args) {
        // System.out.println("Scanning methods with @Run annotations ...\n");
        Reflections reflections = new Reflections("coding", new MethodAnnotationsScanner());
        List.ofAll(reflections.getMethodsAnnotatedWith(Run.class)).forEach(method -> {
            try {
                System.out.println("\n====================================================================================");
                Console.log("Running ", method.getDeclaringClass().getName() + "#" +  method.getName());
                System.out.println("====================================================================================\n");
                method.setAccessible(true);
                method.invoke(null);
            } catch (Exception e) {
                System.out.println("Error while invoking @Run function");
                e.printStackTrace();
            }
        });
    }
}
