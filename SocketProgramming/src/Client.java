
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String SUCCESS_KEY = "success";
    private static final String MESSAGE_KEY = "message";

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            System.out.println("Connected to server: " + socket.getInetAddress().getHostAddress());

            while (true) {
                System.out.println("Choose an option:");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");

                int option = scanner.nextInt();
                scanner.nextLine(); // Consume newline character

                switch (option) {
                    case 1:
                        register(socket);
                        break;
                    case 2:
                        login(socket);
                        break;
                    case 3:
                        return;
                    default:
                        System.out.println("Invalid option");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    private static void register(Socket socket) throws IOException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        byte[] msg= createRequestMsg("register", username, password);
        sendMessage(socket, msg);
        String responseMsg = receiveMessage(socket);
        data = parseResponseMsg(responseMsg);


        String message = data.get(MESSAGE_KEY);
        System.out.println(message);
    }
    
    private static void login(Socket socket) throws IOException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
    
        byte[] msg = createRequestMsg("login", username, password);
        sendMessage(socket, msg);
    
        String responseMsg = receiveMessage(socket);
        data = parseResponseMsg(responseMsg);
        boolean success = Boolean.parseBoolean(data.get(SUCCESS_KEY));
        String message = data.get(MESSAGE_KEY);
        if (success) {
            System.out.println("Login successful");
        } else {
            System.out.println("Login failed: " + message);
        }
    }
    
    private static byte[] createRequestMsg(String action, String username, String password) throws IOException {
        // head length plus body length
        int totalLength = username.getBytes().length + password.getBytes().length + 8;
        int commandID = action.equals("register")? 1 : 3;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 包头写入
        baos.write(intToByteArray(totalLength));
        baos.write(intToByteArray(commandID));  
        // 包体写入
        baos.write(String.format("%-20s", username).getBytes("UTF-8"));
        baos.write(String.format("%-30s", password).getBytes("UTF-8"));

        return baos.toByteArray();
    }
    
    private static void sendMessage(Socket socket, byte[] message) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(message);
    }
    
    private static byte[] receiveMessage(Socket socket) throws IOException {
        byte[] msg = new byte[73];
        InputStream inputStream = socket.getInputStream();
        inputStream.read(msg);
        return msg;
    }
    
    private static Map<String, String> parseResponseMsg(String message) {
        Map<String, String> data = new HashMap<>();
        try {
            JSONObject json = new JSONObject(message);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = json.getString(key);
                data.put(key, value);
            }
        } catch (JSONException e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
        return data;
    }

    // 将int类型的值转换成4字节的byte数组
    public static byte[] intToByteArray(int value) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (value >> 24);
        byteArray[1] = (byte) (value >> 16);
        byteArray[2] = (byte) (value >> 8);
        byteArray[3] = (byte) value;
        return byteArray;
    }
}
    
