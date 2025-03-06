package Client;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

class FileHandler {
    private static final int SERVER_PORT = 8000;
    private final String serverAddr;

    public FileHandler(String addr) {
        this.serverAddr = addr;
    }

    public void insertFile(File file, String path) {
        try (Socket socket = new Socket(serverAddr, SERVER_PORT);
        	 FileInputStream fis = new FileInputStream(file);
        	 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())){
        	
            dos.writeUTF("UPLOAD");
            dos.writeUTF(path);
            dos.writeLong(file.length());
            
            byte[] buffer = new byte[1024];
            int nRead;
            
            while ((nRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, nRead);
            }
            
            System.out.println("Uploaded: " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void deleteFile(String path) {
        try (Socket socket = new Socket(serverAddr, SERVER_PORT);
        	 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
        	
            dos.writeUTF("DELETE");
            dos.writeUTF(path);

            System.out.println("Deleted: " + path);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    public void createDir(String path) {
        try (Socket socket = new Socket(serverAddr, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            dos.writeUTF("CREATE_DIR");
            dos.writeUTF(path);

            System.out.println("Uploaded: " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class Client {
	private static final String CLIENT_DIR = "_client_dir";
	private static final int BACKUP_INTERVAL = 10;
	
    private static final Map<String, String> fileHashes = new HashMap<String, String>();
    private static final Set<String> dirHashes = new HashSet<String>();
    
    public static void main(String[] args) {
        FileHandler handler = new FileHandler("localhost");
        File clientDir = new File(CLIENT_DIR);
        if (!clientDir.exists()) clientDir.mkdir();
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> backupFiles(handler), 0, BACKUP_INTERVAL, TimeUnit.SECONDS);
    }
    
    private static void backupFiles(FileHandler handler) {
        File dir = new File(CLIENT_DIR);
        if (!dir.exists()) return;

        Set<String> curFiles = new HashSet<String>();
        Set<String> curDirs = new HashSet<String>();
        rBackupFiles(dir, handler, curFiles, curDirs);
        
        Iterator<String> it;
        it = fileHashes.keySet().iterator();
        while (it.hasNext()) {
            String path = it.next();
            if (!curFiles.contains(path)) {
                handler.deleteFile(path);
                it.remove();
            }
        }
        
        it = dirHashes.iterator();
        while (it.hasNext()) {
            String path = it.next();
            if (!curDirs.contains(path)) {
                handler.deleteFile(path);
                it.remove();
            }
        }
    }
    
    private static void rBackupFiles(File dir, FileHandler handler, Set<String> curFiles, Set<String> curDirs) {
    	for (File file : dir.listFiles()) {
            String path = file.getAbsolutePath().substring(new File(CLIENT_DIR).getAbsolutePath().length() + 1); 

            if (file.isDirectory()) {
                curDirs.add(path);
                if (!dirHashes.contains(path)) {
                    handler.createDir(path);
                    dirHashes.add(path);
                }
            	rBackupFiles(file, handler, curFiles, curDirs);
            } else {
            	curFiles.add(path);
                try {
                    String hash = fileHash(file);
                    if (!hash.equals(fileHashes.get(path))) {
                        fileHashes.put(path, hash);
                        handler.insertFile(file, path);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String fileHash(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)){
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[1024];
            int nRead;
            
            while ((nRead = is.read(buf)) != -1) {
                digest.update(buf, 0, nRead);
            }
            
            return Base64.getEncoder().encodeToString(digest.digest());
        }
    }
}
