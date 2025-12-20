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
                // If input is exactly "echo"
                if (input.length() == 4) {
                    System.out.println();
                } else if (input.charAt(4) == ' ') {
                    // Print everything after "echo "
                    System.out.println(input.substring(5));
                } else {
                    // e.g. "echoXYZ" -> invalid command
                    System.out.println(input + ": command not found");
                }
                continue;
            }

            // invalid command
            System.out.println(input + ": command not found");
        }
    }
}
