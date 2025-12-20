import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    // ---------- Argument parser with single-quote support ----------
    private static List<String> parseArguments(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\'') {
                inSingleQuote = !inSingleQuote;
            } else if (Character.isWhitespace(c) && !inSingleQuote) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        File currentDir = new File(System.getProperty("user.dir"));

        while (true) {
            // Prompt
            System.out.print("$ ");
            System.out.flush();

            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            // exit
            if (input.equals("exit")) {
                break;
            }

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

            // echo (with quote support)
            if (input.startsWith("echo")) {
                List<String> parts = parseArguments(input);

                if (parts.size() == 1) {
                    System.out.println();
                } else {
                    for (int i = 1; i < parts.size(); i++) {
                        if (i > 1) System.out.print(" ");
                        System.out.print(parts.get(i));
                    }
                    System.out.println();
                }
                continue;
            }

            // type builtin
            if (input.startsWith("type")) {
                List<String> parts = parseArguments(input);

                if (parts.size() == 1) {
                    System.out.println("type is a shell builtin");
                    continue;
                }

                String cmd = parts.get(1);

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

                if (!found) {
                    System.out.println(cmd + ": not found");
                }
                continue;
            }

            // ---------- External command execution ----------
            List<String> parts = parseArguments(input);
            String command = parts.get(0);

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

            try {
                ProcessBuilder pb = new ProcessBuilder(parts);
                pb.directory(currentDir);
                pb.inheritIO();
                pb.start().waitFor();
            } catch (IOException e) {
                System.out.println(command + ": command not found");
            }
        }
    }
}
