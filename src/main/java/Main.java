import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static File currentDir;

    // ---------------- Argument parser ----------------
    private static List<String> parseArguments(String input) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingle = false;
        boolean inDouble = false;
        boolean escape = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }

            if (c == '\\' && !inSingle) {
                if (inDouble && i + 1 < input.length()) {
                    char n = input.charAt(i + 1);
                    if (n == '"' || n == '\\') {
                        current.append(n);
                        i++;
                        continue;
                    }
                }
                escape = true;
                continue;
            }

            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }

            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }

            if (Character.isWhitespace(c) && !inSingle && !inDouble) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    // ---------------- MAIN ----------------
    public static void main(String[] args) throws Exception {

        InputStream in = System.in;
        StringBuilder buffer = new StringBuilder();
        currentDir = new File(System.getProperty("user.dir"));

        System.out.print("$ ");
        System.out.flush();

        while (true) {
            int ch = in.read();
            if (ch == -1) break;

            // ENTER
            if (ch == '\n') {
                System.out.println();
                String line = buffer.toString().trim();
                buffer.setLength(0);

                if (!line.isEmpty()) {
                    execute(line);
                }

                System.out.print("$ ");
                System.out.flush();
                continue;
            }

            // TAB (Codecrafters-compatible)
            if (ch == '\t') {
                String cur = buffer.toString();

                if ("echo".startsWith(cur)) {
                    buffer.setLength(0);
                    buffer.append("echo ");
                    System.out.print("\n$ echo ");
                } else if ("exit".startsWith(cur)) {
                    buffer.setLength(0);
                    buffer.append("exit ");
                    System.out.print("\n$ exit ");
                }

                System.out.flush();
                continue;
            }

            // NORMAL CHAR
            buffer.append((char) ch);
            System.out.print((char) ch);
            System.out.flush();
        }
    }

    // ---------------- EXECUTION ----------------
    private static void execute(String input) throws Exception {

        if (input.equals("exit")) {
            System.exit(0);
        }

        if (input.equals("pwd")) {
            System.out.println(currentDir.getAbsolutePath());
            return;
        }

        if (input.startsWith("cd ")) {
            String path = input.substring(3);
            File next;

            if (path.equals("~")) {
                next = new File(System.getenv("HOME"));
            } else if (path.startsWith("/")) {
                next = new File(path);
            } else {
                next = new File(currentDir, path);
            }

            try {
                next = next.getCanonicalFile();
                if (next.exists() && next.isDirectory()) {
                    currentDir = next;
                } else {
                    System.out.println("cd: " + path + ": No such file or directory");
                }
            } catch (IOException e) {
                System.out.println("cd: " + path + ": No such file or directory");
            }
            return;
        }

        List<String> tokens = parseArguments(input);

        File stdout = null, stderr = null;
        boolean outAppend = false, errAppend = false;
        List<String> cmd = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);

            if ((t.equals(">") || t.equals("1>")) && i + 1 < tokens.size()) {
                stdout = new File(tokens.get(++i));
            } else if ((t.equals(">>") || t.equals("1>>")) && i + 1 < tokens.size()) {
                stdout = new File(tokens.get(++i));
                outAppend = true;
            } else if (t.equals("2>") && i + 1 < tokens.size()) {
                stderr = new File(tokens.get(++i));
            } else if (t.equals("2>>") && i + 1 < tokens.size()) {
                stderr = new File(tokens.get(++i));
                errAppend = true;
            } else {
                cmd.add(t);
            }
        }

        if (cmd.isEmpty()) return;

        // echo builtin
        if (cmd.get(0).equals("echo")) {
            String out = String.join(" ", cmd.subList(1, cmd.size())) + "\n";

            if (stdout != null) {
                if (outAppend) {
                    Files.write(stdout.toPath(), out.getBytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } else {
                    Files.write(stdout.toPath(), out.getBytes());
                }
            } else {
                System.out.print(out);
            }

            // stderr file creation (2>> or 2>)
            if (stderr != null) {
                if (errAppend) {
                    Files.write(stderr.toPath(), new byte[0],
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } else {
                    Files.write(stderr.toPath(), new byte[0]);
                }
            }
            return;
        }

        // external command lookup
        boolean found = false;
        for (String dir : System.getenv("PATH").split(File.pathSeparator)) {
            File f = new File(dir, cmd.get(0));
            if (f.exists() && f.canExecute()) {
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println(cmd.get(0) + ": command not found");
            return;
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(currentDir);

        // stdout
        if (stdout != null) {
            if (outAppend) {
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(stdout));
            } else {
                pb.redirectOutput(stdout);
            }
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }

        // stderr
        if (stderr != null) {
            if (errAppend) {
                pb.redirectError(ProcessBuilder.Redirect.appendTo(stderr));
            } else {
                pb.redirectError(stderr);
            }
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }

        pb.start().waitFor();
    }
}
