import java.io.File;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        while (true) {
            // Prompt
            System.out.print("$ ");
            System.out.flush();

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();

            // exit builtin
            if (input.equals("exit")) {
                break;
            }

            // echo builtin
            if (input.startsWith("echo")) {
                if (input.length() == 4) {
                    System.out.println();
                } else if (input.charAt(4) == ' ') {
                    System.out.println(input.substring(5));
                } else {
                    System.out.println(input + ": command not found");
                }
                continue;
            }

            // type builtin (extended)
            if (input.startsWith("type")) {
                if (input.equals("type")) {
                    System.out.println("type is a shell builtin");
                    continue;
                }

                if (input.charAt(4) != ' ') {
                    System.out.println(input + ": not found");
                    continue;
                }

                String cmd = input.substring(5);

                // Step 1: builtin check
                if (cmd.equals("exit") || cmd.equals("echo") || cmd.equals("type")) {
                    System.out.println(cmd + " is a shell builtin");
                    continue;
                }

                // Step 2: search PATH
                boolean found = false;
                String pathEnv = System.getenv("PATH");

                if (pathEnv != null) {
                    String[] paths = pathEnv.split(File.pathSeparator);

                    for (String dir : paths) {
                        File file = new File(dir, cmd);

                        if (file.exists() && file.isFile() && file.canExecute()) {
                            System.out.println(cmd + " is " + file.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }
                }

                // Step 3: not found
                if (!found) {
                    System.out.println(cmd + ": not found");
                }

                continue;
            }

            // invalid command
            System.out.println(input + ": command not found");
        }
    }
}
