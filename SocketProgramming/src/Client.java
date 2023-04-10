
import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

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

        receiveAndparseResponseMsg(socket);
    }
    
    private static void login(Socket socket) throws IOException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
    
        byte[] msg = createRequestMsg("login", username, password);
        sendMessage(socket, msg);
    
        receiveAndparseResponseMsg(socket);
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
    
    private static void receiveAndparseResponseMsg(Socket socket) throws IOException{
        // 读取包头
        InputStream in = socket.getInputStream();
        byte[] header = new byte[8];
        in.read(header);

        // 解析包头中的数据包长度和类型
        int messageLength = byteArrayToInt(header, 0);
        int messageType = byteArrayToInt(header, 4);

        // 读取包体
        byte[] body = new byte[65];
        in.read(body);

        // 解析包体中的状态和描述
        String status = new String(body, 0, 1, "UTF-8").trim();
        System.out.println("response status: " + status);
        String discription = new String(body, 1, 64, "UTF-8").trim();
        System.out.println("response description: " + status);

        if(status.equals("1") && messageType == 2){
            System.out.println("register succeed!");
        } else if(status.equals("1") && messageType == 4){
            System.out.println("login succeed!");
        } else if(status.equals("0") && messageType == 1){
            System.out.println(discription);
        } else {
            System.out.println(discription);
        }
    }

    // 将int类型的值转换成4字节的byte数组
    public static byte[] intToByteArray(int value) {
        byte[] byteArray = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
        return byteArray;
    }

    public static int byteArrayToInt(byte[] byteArray, int pos) {
        int result = 0;
        byte[] src = Arrays.copyOfRange(byteArray, pos, pos+4);
        result = ByteBuffer.wrap(src).order(ByteOrder.BIG_ENDIAN).getInt();
        return result;
    }    
}
    
