package Server;

import java.io.*;
import java.net.*;

public class Server {
    private static final String SERVER_DIR = "_server_dir";
    private static final int SERVER_PORT = 8000;
    
    public static void main(String[] args) {
        File serverDir = new File(SERVER_DIR);
        if (!serverDir.exists()) serverDir.mkdirs();
        
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started...");
            
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                	 DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                    
                    String command = dis.readUTF();
                    if (command.equals("UPLOAD")) {
                    	insertFile(dis);
                    } else if (command.equals("DELETE")) {
                        deleteFile(dis.readUTF());
                    } else if (command.equals("CREATE_DIR")) {
                    	createDir(dis.readUTF());
                    }                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertFile(DataInputStream dis) throws Exception {
        String path = dis.readUTF();
        long fileSize = dis.readLong();
        File file = new File(SERVER_DIR, path);
        
        file.getParentFile().mkdirs();
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buf = new byte[1024];
            int nRead;
            
            while (fileSize > 0 && (nRead = dis.read(buf, 0, (int)Math.min(buf.length, fileSize))) != -1) {
                fos.write(buf, 0, nRead);
                fileSize -= nRead;
            }
        }
        
        System.out.println("Inserted: " + path);
    }

    private static void deleteFile(String path) {
        File file = new File(SERVER_DIR, path);
        
        if (!file.exists()) {
        	System.out.println("Failed to delete: " + path);
        } else {
        	if (file.delete())
        		System.out.println("Deleted: " + path);
        	else
        		System.out.println("Failed to delete: " + path);
        }
    }
    
    private static void createDir(String path) throws Exception {
        File dir = new File(SERVER_DIR, path);

        if (!dir.exists() && dir.mkdirs()) {
            System.out.println("Created: " + path);
        } else {
            System.out.println("Failed to create: " + path);
        }
    }
}