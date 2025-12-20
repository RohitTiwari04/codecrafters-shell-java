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

            // type builtin
            if (input.startsWith("type")) {
                if (input.equals("type")) {
                    System.out.println("type is a shell builtin");
                } else if (input.charAt(4) == ' ') {
                    String cmd = input.substring(5);

                    if (cmd.equals("echo") || cmd.equals("exit") || cmd.equals("type")) {
                        System.out.println(cmd + " is a shell builtin");
                    } else {
                        System.out.println(cmd + ": not found");
                    }
                } else {
                    System.out.println(input + ": not found");
                }
                continue;
            }

            // invalid command
            System.out.println(input + ": command not found");
        }
    }
}
