import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        while (true) {
            // Print prompt
            System.out.print("$ ");
            System.out.flush();

            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();

            // exit builtin
            if (input.equals("exit")) {
                break; // terminate shell
            }

            // invalid command
            System.out.println(input + ": command not found");
        }
    }
}
