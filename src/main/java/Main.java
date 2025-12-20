import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    // ---------- Argument parser ----------
    private static List<String> parseArguments(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaping = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }

            if (c == '\\') {
                if (inSingleQuote) {
                    current.append(c);
                } else if (inDoubleQuote) {
                    if (i + 1 < input.length()) {
                        char next = input.charAt(i + 1);
                        if (next == '"' || next == '\\') {
                            current.append(next);
                            i++;
                        } else {
                            current.append(c);
                        }
                    } else {
                        current.append(c);
                    }
                } else {
                    escaping = true;
                }
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
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

            // cd
            if (input.startsWith("cd ")) {
                String path = input.substring(3);
                File newDir;

                if (path.equals("~")) {
                    newDir = new File(System.getenv("HOME"));
                } else if (path.startsWith("/")) {
                    newDir = new File(path);
                } else {
                    newDir = new File(currentDir, path);
                }

                try {
                    newDir = newDir.getCanonicalFile();
                    if (newDir.exists() && newDir.isDirectory()) {
                        currentDir = newDir;
                    } else {
                        System.out.println("cd: " + path + ": No such file or directory");
                    }
                } catch (IOException e) {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
                continue;
            }

            // Parse arguments
            List<String> parts = parseArguments(input);

            // ---------- Handle output redirection ----------
            File redirectFile = null;
            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i).equals(">") || parts.get(i).equals("1>")) {
                    if (i + 1 < parts.size()) {
                        redirectFile = new File(parts.get(i + 1));
                        parts = new ArrayList<>(parts.subList(0, i));
                    }
                    break;
                }
            }

            // echo (builtin)
            if (parts.get(0).equals("echo")) {
                StringBuilder out = new StringBuilder();
                for (int i = 1; i < parts.size(); i++) {
                    if (i > 1) out.append(" ");
                    out.append(parts.get(i));
                }
                out.append("\n");

                if (redirectFile != null) {
                    java.nio.file.Files.write(
                        redirectFile.toPath(),
                        out.toString().getBytes()
                    );
                } else {
                    System.out.print(out.toString());
                }
                continue;
            }

            // type builtin
            if (parts.get(0).equals("type")) {
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

                if (!found) System.out.println(cmd + ": not found");
                continue;
            }

            // ---------- External command ----------
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

            ProcessBuilder pb = new ProcessBuilder(parts);
            pb.directory(currentDir);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            if (redirectFile != null) {
                pb.redirectOutput(redirectFile);
            } else {
                pb.inheritIO();
            }

            pb.start().waitFor();
        }
    }
}
