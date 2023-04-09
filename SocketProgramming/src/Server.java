import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.json.*;

public class Server {
    private static final int PORT = 12345;
    private static final String PASSWD_FILE = "passwd.txt";
    private static final String SUCCESS_MSG = "success";
    private static final String FAIL_MSG = "fail";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";

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
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                        String msg = reader.readLine();
                        if (msg == null) {
                            System.err.println("Error: empty message from client");
                            writer.println(createResponseMsg(false, "Empty message from client"));
                            return;
                        }

                        // Handle registration request
                        if (msg.startsWith("register")) {
                            Map<String, String> data = parseMsg(msg);
                            String username = data.get(USERNAME_KEY);
                            String password = data.get(PASSWORD_KEY);
                            if (username == null || password == null) {
                                writer.println(createResponseMsg(false, "Invalid registration request"));
                                return;
                            }

                            if (passwdMap.containsKey(username)) {
                                writer.println(createResponseMsg(false, "Username already exists"));
                                return;
                            }

                            String hashedPassword = hashPassword(password);
                            passwdMap.put(username, hashedPassword);
                            savePasswdFile();
                            writer.println(createResponseMsg(true, SUCCESS_MSG));
                        }
                        // Handle login request
                        else if (msg.startsWith("login")) {
                            Map<String, String> data = parseMsg(msg);
                            String username = data.get(USERNAME_KEY);
                            String password = data.get(PASSWORD_KEY);
                            if (username == null || password == null) {
                                writer.println(createResponseMsg(false, "Invalid login request"));
                                return;
                            }

                            String hashedPassword = passwdMap.get(username);
                            if (hashedPassword == null || !hashedPassword.equals(hashPassword(password))) {
                                writer.println(createResponseMsg(false, "Invalid username or password"));
                                return;
                            }

                            writer.println(createResponseMsg(true, SUCCESS_MSG));
                        }
                        // Unknown request
                        else {
                            writer.println(createResponseMsg(false, "Unknown request"));
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

    private static Map<String, String> parseMsg(String msg) {
        Map<String, String> data = new HashMap<>();
        try {
            JSONObject json = new JSONObject(msg);
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

    private static String createResponseMsg(boolean success, String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("success", success);
            json.put("message", message);
            return json.toString();
        } catch (JSONException e) {
            System.err.println("Error creating response message: " + e.getMessage());
            return "";
        }
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
}
