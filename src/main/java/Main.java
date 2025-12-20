import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        File currentDir = new File(System.getProperty("user.dir"));

        while (true) {
            System.out.print("$ ");
            System.out.flush();

            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            // exit
            if (input.equals("exit")) break;

            // pwd
            if (input.equals("pwd")) {
                System.out.println(currentDir.getAbsolutePath());
                continue;
            }

            // cd (absolute, relative, ~)
            if (input.startsWith("cd ")) {
                String path = input.substring(3);
                File newDir = null;

                if (path.equals("~")) {
                    String home = System.getenv("HOME");
                    if (home != null) {
                        newDir = new File(home);
                    }
                } else if (path.startsWith("/")) {
                    newDir = new File(path);
                } else {
                    newDir = new File(currentDir, path);
                }

                try {
                    if (newDir != null) {
                        newDir = newDir.getCanonicalFile();
                    }

                    if (newDir != null && newDir.exists() && newDir.isDirectory()) {
                        currentDir = newDir;
                    } else {
                        System.out.println("cd: " + path + ": No such file or directory");
                    }
                } catch (IOException e) {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
                continue;
            }

            // echo
            if (input.startsWith("echo")) {
                if (input.length() == 4) System.out.println();
                else if (input.charAt(4) == ' ') System.out.println(input.substring(5));
                else System.out.println(input + ": command not found");
                continue;
            }

            // type
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

                if (cmd.equals("exit") || cmd.equals("echo") || cmd.equals("type")
                        || cmd.equals("pwd") || cmd.equals("cd")) {
                    System.out.println(cmd + " is a shell builtin");
                    continue;
                }

                boolean found = false;
                String pathEnv = System.getenv("PATH");

                if (pathEnv != null) {
                    for (String dir : pathEnv.split(File.pathSeparator)) {
                        File f = new File(dir, cmd);
                        if (f.exists() && f.isFile() && f.canExecute()) {
                            System.out.println(cmd + " is " + f.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) System.out.println(cmd + ": not found");
                continue;
            }

            // external command
            String[] parts = input.split("\\s+");
            String command = parts[0];

            boolean found = false;
            String pathEnv = System.getenv("PATH");

            if (pathEnv != null) {
                for (String dir : pathEnv.split(File.pathSeparator)) {
                    File f = new File(dir, command);
                    if (f.exists() && f.isFile() && f.canExecute()) {
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

            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.directory(currentDir);
            pb.inheritIO();
            pb.start().waitFor();
        }
    }
}
