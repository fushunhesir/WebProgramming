import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Server {
    private static final int PORT = 12345;
    private static final String PASSWD_FILE = "passwd.txt";
    private static final String SUCCESS_MSG = "success";
    private static final int REGISTRATION_REQUEST_MSG = 1;
    private static final int REGISTRATION_RESPONSE_MSG = 2;
    private static final int LOGIN_REQUEST_MSG = 3;
    private static final int LOGIN_RESPONSE_MSG = 4;
    private static final int STATUS_LEN = 1;
    private static final int HEADER_LEN = 8;

    private static Map<String, String> passwdMap = new HashMap<>();

    public static void main(String[] args) {
        loadPasswdFile();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());

                // Handle client request in a new thread
                new Thread(() -> {
                    try {
                        // 读取包头
                        InputStream in = socket.getInputStream();
                        byte[] header = new byte[8];
                        byte[] body = new byte[50];

                        while (true) {
                            in.read(header);

                            // 解析包头中的数据包长度和类型
                            int messageLength = byteArrayToInt(header, 0);
                            int messageType = byteArrayToInt(header, 4);
                            System.out.println(messageLength);
                            System.out.println(messageType);

                            // 读取包体
                            in.read(body);
                            System.out.println(body.length);

                            // 解析包体中的状态和描述
                            String username = new String(body, 0, 20, "UTF-8").trim();
                            String password = new String(body, 20, 30, "UTF-8").trim();
                            // Handle registration request
                            if (messageType == REGISTRATION_REQUEST_MSG) {
                                if (username == null || password == null) {
                                    sendMessage(socket, createResponseMsg(false, "Invalid register request",
                                            REGISTRATION_RESPONSE_MSG));
                                }

                                if (passwdMap.containsKey(username)) {
                                    sendMessage(socket,
                                            createResponseMsg(false, "Username already exists",
                                                    REGISTRATION_RESPONSE_MSG));
                                } else {
                                    String hashedPassword = hashPassword(password);
                                    passwdMap.put(username, hashedPassword);
                                    savePasswdFile();
                                    sendMessage(socket,
                                            createResponseMsg(true, SUCCESS_MSG, REGISTRATION_RESPONSE_MSG));
                                }
                            }
                            // Handle login request
                            else if (messageType == LOGIN_REQUEST_MSG) {
                                if (username == null || password == null) {
                                    sendMessage(socket,
                                            createResponseMsg(false, "Invalid login request", LOGIN_RESPONSE_MSG));
                                }

                                String hashedPassword = passwdMap.get(username);
                                if (hashedPassword == null || !hashedPassword.equals(hashPassword(password))) {
                                    sendMessage(socket,
                                            createResponseMsg(false, "Invalid username or password",
                                                    LOGIN_RESPONSE_MSG));
                                } else {
                                    sendMessage(socket, createResponseMsg(true, SUCCESS_MSG, LOGIN_RESPONSE_MSG));
                                }
                            }
                            // Unknown request
                            else {
                                sendMessage(socket, createResponseMsg(false, "Unknown request", 3));
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error handling client request: " + e.getMessage());
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    private static byte[] createResponseMsg(boolean success, String message, int action) throws IOException {
        // head length plus body length
        byte[] status = { (byte) (success ? 1 : 0) };
        int totalLength = message.length() + HEADER_LEN + STATUS_LEN;
        int commandID = action == REGISTRATION_RESPONSE_MSG ? 2 : 4;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 包头写入
        baos.write(intToByteArray(totalLength));
        baos.write(intToByteArray(commandID));
        baos.write(status);
        // 包体写入
        baos.write(String.format("%-64s", message).getBytes("UTF-8"));

        return baos.toByteArray();
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error hashing password: " + e.getMessage());
            return "";
        }
    }

    private static void loadPasswdFile() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(PASSWD_FILE));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    passwdMap.put(parts[0], parts[1]);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Error loading passwd file: " + e.getMessage());
        }
    }

    private static void savePasswdFile() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(PASSWD_FILE));
            for (Map.Entry<String, String> entry : passwdMap.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving passwd file: " + e.getMessage());
        }
    }

    public static int byteArrayToInt(byte[] byteArray, int pos) {
        int result = 0;
        byte[] src = Arrays.copyOfRange(byteArray, pos, pos + 4);
        result = ByteBuffer.wrap(src).order(ByteOrder.BIG_ENDIAN).getInt();
        return result;
    }

    private static void sendMessage(Socket socket, byte[] message) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(message);
    }

    // 将int类型的值转换成4字节的byte数组
    public static byte[] intToByteArray(int value) {
        byte[] byteArray = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
        return byteArray;
    }
}
