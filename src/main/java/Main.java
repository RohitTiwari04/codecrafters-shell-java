import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        // Track current working directory manually
        File currentDir = new File(System.getProperty("user.dir"));

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

            // pwd builtin
            if (input.equals("pwd")) {
                System.out.println(currentDir.getAbsolutePath());
                continue;
            }

            // cd builtin (absolute paths only)
            if (input.startsWith("cd ")) {
                String path = input.substring(3);

                if (path.startsWith("/")) {
                    File newDir = new File(path);

                    if (newDir.exists() && newDir.isDirectory()) {
                        currentDir = newDir;
                    } else {
                        System.out.println("cd: " + path + ": No such file or directory");
                    }
                } else {
                    // relative paths handled in later stages
                    System.out.println("cd: " + path + ": No such file or directory");
                }
                continue;
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

                if (cmd.equals("exit") || cmd.equals("echo") || cmd.equals("type") || cmd.equals("pwd") || cmd.equals("cd")) {
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

            boolean found = false;
            String pathEnv = System.getenv("PATH");

            if (pathEnv != null) {
                String[] paths = pathEnv.split(File.pathSeparator);
                for (String dir : paths) {
                    File file = new File(dir, command);
                    if (file.exists() && file.isFile() && file.canExecute()) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                System.out.println(command + ": command not found");
                continue;
            }

            List<String> commandList = new ArrayList<>();
            commandList.add(command);

            for (int i = 1; i < parts.length; i++) {
                commandList.add(parts[i]);
            }

            try {
                ProcessBuilder pb = new ProcessBuilder(commandList);
                pb.directory(currentDir); // IMPORTANT
                pb.inheritIO();
                Process process = pb.start();
                process.waitFor();
            } catch (IOException e) {
                System.out.println(command + ": command not found");
            }
        }
    }
}
