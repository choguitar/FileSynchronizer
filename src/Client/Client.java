package Client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


class Config {
    static final String CLIENT_DIR = "_client_dir";
    static final String SERVER_DIR = "_server_dir";
    static final int SERVER_PORT = 8000;
}

class FileObserver {
    private final Path dir;
    private final Map<String, String> fileHashes = new HashMap<>();
    private final FileHandler transferHandler;

    public FileObserver(String directory, FileHandler handler) {
        this.dir = Paths.get(directory);
        this.transferHandler = handler;
        loadInitialHashes();
    }

    private void loadInitialHashes() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    fileHashes.put(file.toString(), getFileChecksum(file));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void watch() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                         StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            
            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changedFile = dir.resolve((Path) event.context());
                    handleFileChange(event.kind(), changedFile);
                }
                key.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFileChange(WatchEvent.Kind<?> kind, Path file) {
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            fileHashes.remove(file.toString());
            transferHandler.deleteFile(file.getFileName().toString());
        } else {
            try {
                String newHash = getFileChecksum(file);
                if (!newHash.equals(fileHashes.get(file.toString()))) {
                    fileHashes.put(file.toString(), newHash);
                    transferHandler.sendFile(file.toFile());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFileChecksum(Path file) throws IOException {
        try (InputStream fis = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesRead);
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

class FileHandler {
    private final String serverAddr;

    public FileHandler(String addr) {
        this.serverAddr = addr;
    }

    public void sendFile(File file) {
    	Socket socket = null;
    	FileInputStream fis = null;
    	DataOutputStream dos = null;
    	
        try {
        	socket = new Socket(serverAddr, Config.SERVER_PORT);
            fis = new FileInputStream(file);
            dos = new DataOutputStream(socket.getOutputStream());
           
            dos.writeUTF("UPLOAD");
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
                if (dos != null) dos.close();
                if (fis != null) fis.close();
                if (socket != null) socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void deleteFile(String fileName) {
        try (Socket socket = new Socket(serverAddr, Config.SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
            
            dos.writeUTF("DELETE");
            dos.writeUTF(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class Client {
    public static void main(String[] args) {
        FileHandler handler = new FileHandler("localhost");
		FileObserver observer = new FileObserver(Config.CLIENT_DIR, handler);
        observer.watch();
    }
}
