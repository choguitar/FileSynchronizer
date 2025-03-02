package Server;

import java.io.*;
import java.net.*;

class Config {
    static final String CLIENT_DIR = "_client_dir";
    static final String SERVER_DIR = "_server_dir";
    static final int SERVER_PORT = 8000;
}

public class Server {
    public static void main(String[] args) {
        File serverDir = new File(Config.SERVER_DIR);
        if (!serverDir.exists()) serverDir.mkdirs();	
        
        ServerSocket serverSocket = null;
        
        try {
        	serverSocket = new ServerSocket(Config.SERVER_PORT);
        	
            System.out.println("Server started...");
            
            while (true) {
            	Socket clientSocket = null;
            	DataInputStream dis = null;
            	
                try {
                	clientSocket = serverSocket.accept();
                    dis = new DataInputStream(clientSocket.getInputStream());
                    
                    String command = dis.readUTF();
                    if (command.equals("UPLOAD")) {
                    	insertFile(dis);
                    } else if (command.equals("DELETE")) {
                        deleteFile(dis.readUTF());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                	try {
                		if (clientSocket != null) clientSocket.close();
                		if (dis != null) dis.close();
                	} catch (Exception e) {
                        e.printStackTrace();                		
                	}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
        		if (serverSocket != null) serverSocket.close();
        	} catch (Exception e) {
                e.printStackTrace();
        	}
        }
    }

    private static void insertFile(DataInputStream dis) throws Exception { // update file
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        File file = new File(Config.SERVER_DIR, fileName);
        
        FileOutputStream fos = null;
        
        try {
        	fos = new FileOutputStream(file);
        	
            byte[] buf = new byte[1024];
            int nRead;
            
            while (fileSize > 0 && (nRead = dis.read(buf, 0, (int)Math.min(buf.length, fileSize))) != -1) {
                fos.write(buf, 0, nRead);
                fileSize -= nRead;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
        		if (fos != null) fos.close();
        	} catch (Exception e) {
                e.printStackTrace();        		
        	}
        }
        
        System.out.println("Inserted: " + fileName);
    }

    private static void deleteFile(String fileName) {	// remove file
        File file = new File(Config.SERVER_DIR, fileName);
        if (file.exists() && file.delete()) {
            System.out.println("Deleted: " + fileName);
        } else {
            System.out.println("Failed to delete: " + fileName);
        }
    }
}