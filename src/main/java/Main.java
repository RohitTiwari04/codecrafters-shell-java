import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

            if (input.isEmpty()) {
                continue;
            }

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

            // type builtin
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

                if (cmd.equals("exit") || cmd.equals("echo") || cmd.equals("type")) {
                    System.out.println(cmd + " is a shell builtin");
                    continue;
                }

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

                if (!found) {
                    System.out.println(cmd + ": not found");
                }
                continue;
            }

            // -------- External command execution --------

            String[] parts = input.split("\\s+");
            String command = parts[0];

            String executablePath = null;
            String pathEnv = System.getenv("PATH");

            if (pathEnv != null) {
                String[] paths = pathEnv.split(File.pathSeparator);
                for (String dir : paths) {
                    File file = new File(dir, command);
                    if (file.exists() && file.isFile() && file.canExecute()) {
                        executablePath = file.getAbsolutePath();
                        break;
                    }
                }
            }

            if (executablePath == null) {
                System.out.println(command + ": command not found");
                continue;
            }

            // Build command with arguments
            List<String> commandList = new ArrayList<>();
            commandList.add(executablePath);
            for (int i = 1; i < parts.length; i++) {
                commandList.add(parts[i]);
            }

            try {
                ProcessBuilder pb = new ProcessBuilder(commandList);
                pb.inheritIO(); // show program output directly
                Process process = pb.start();
                process.waitFor();
            } catch (IOException e) {
                System.out.println(command + ": command not found");
            }
        }
    }
}
