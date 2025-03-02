package Server;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class Config {
    public static final String CLIENT_DIR = "_client_dir";
    public static final String SERVER_DIR = "_server_dir";
    public static final int SERVER_PORT = 8000;
}

public class Server {
    public static void main(String[] args) {
        File serverDir = new File(Config.SERVER_DIR);
        if (!serverDir.exists()) serverDir.mkdirs();
        
        try (ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT)) {
            System.out.println("Server started...");
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                    
                    String command = dis.readUTF();
                    if (command.equals("UPLOAD")) {
                        receiveFile(dis);
                    } else if (command.equals("DELETE")) {
                        deleteFile(dis.readUTF());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(DataInputStream dis) throws IOException {
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        File file = new File(Config.SERVER_DIR, fileName);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                fos.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
        }
        System.out.println("Received: " + fileName);
    }

    private static void deleteFile(String fileName) {
        File file = new File(Config.SERVER_DIR, fileName);
        if (file.exists() && file.delete()) {
            System.out.println("Deleted: " + fileName);
        } else {
            System.out.println("Failed to delete: " + fileName);
        }
    }
}