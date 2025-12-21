import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // Shared shell state
    static File currentDir;

    // ================= ARGUMENT PARSER =================
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

    // ================= MAIN =================
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
                    executeCommand(line);
                }

                System.out.print("$ ");
                System.out.flush();
                continue;
            }

            // TAB AUTOCOMPLETE (FIXED)
            if (ch == '\t') {
                String cur = buffer.toString();

                if ("echo".startsWith(cur)) {
                    buffer.setLength(0);
                    buffer.append("echo ");
                    System.out.print("\r\033[2K$ echo ");
                } else if ("exit".startsWith(cur)) {
                    buffer.setLength(0);
                    buffer.append("exit ");
                    System.out.print("\r\033[2K$ exit ");
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

    // ================= COMMAND EXECUTION =================
    private static void executeCommand(String input) throws Exception {

        // exit
        if (input.equals("exit")) {
            System.exit(0);
        }

        // pwd
        if (input.equals("pwd")) {
            System.out.println(currentDir.getAbsolutePath());
            return;
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
            return;
        }

        List<String> tokens = parseArguments(input);

        File stdout = null;
        boolean stdoutAppend = false;
        File stderr = null;
        boolean stderrAppend = false;

        List<String> cmd = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);

            if ((t.equals(">") || t.equals("1>")) && i + 1 < tokens.size()) {
                stdout = new File(tokens.get(++i));
            } else if ((t.equals(">>") || t.equals("1>>")) && i + 1 < tokens.size()) {
                stdout = new File(tokens.get(++i));
                stdoutAppend = true;
            } else if (t.equals("2>") && i + 1 < tokens.size()) {
                stderr = new File(tokens.get(++i));
            } else if (t.equals("2>>") && i + 1 < tokens.size()) {
                stderr = new File(tokens.get(++i));
                stderrAppend = true;
            } else {
                cmd.add(t);
            }
        }

        if (cmd.isEmpty()) return;

        // echo builtin
        if (cmd.get(0).equals("echo")) {
            String out = String.join(" ", cmd.subList(1, cmd.size())) + "\n";

            if (stdout != null) {
                if (stdoutAppend) {
                    Files.write(stdout.toPath(), out.getBytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } else {
                    Files.write(stdout.toPath(), out.getBytes());
                }
            } else {
                System.out.print(out);
            }

            // stderr file creation if redirected
            if (stderr != null) {
                if (stderrAppend) {
                    Files.write(stderr.toPath(), new byte[0],
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } else {
                    Files.write(stderr.toPath(), new byte[0]);
                }
            }
            return;
        }

        // External command lookup
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
            if (stdoutAppend) {
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(stdout));
            } else {
                pb.redirectOutput(stdout);
            }
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }

        // stderr
        if (stderr != null) {
            if (stderrAppend) {
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
