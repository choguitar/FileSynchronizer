package Client;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;


class Config {
    static final String CLIENT_DIR = "_client_dir";
    static final String SERVER_DIR = "_server_dir";
    static final int SERVER_PORT = 8000;
}

class FileObserver {
    private final Path dir;
    private final Map<String, String> fileHashes = new HashMap<>();
	private final FileHandler fileHandler;

    public FileObserver(String dir, FileHandler handler) {
        this.dir = Paths.get(dir);
        this.fileHandler = handler;
        initHash();
    }

    private void initHash() {	// current directory information
    	DirectoryStream<Path> stream = null;
    	
        try {
        	stream = Files.newDirectoryStream(dir);
        	
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    fileHashes.put(file.toString(), getFileChecksum(file));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
        		if (stream != null) stream.close();
        	} catch (Exception e) {
                e.printStackTrace();
        	}
        }
    }

    public void observe() {	// observe files and synchronize with server
    	WatchService watchService = null;
    	
        try {
        	watchService = FileSystems.getDefault().newWatchService();
        			
            dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                         StandardWatchEventKinds.ENTRY_MODIFY,
                         StandardWatchEventKinds.ENTRY_DELETE);
            
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
        } finally {
        	try {
        		if (watchService != null) watchService.close();
        	} catch (Exception e) {
                e.printStackTrace();
        	}
        }
    }

    private void handleFileChange(WatchEvent.Kind<?> kind, Path file) {	// handle upload or delete
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            fileHashes.remove(file.toString());
            fileHandler.deleteFile(file.getFileName().toString());
        } else {
            try {
                String hash = getFileChecksum(file);
                if (!hash.equals(fileHashes.get(file.toString()))) {
                    fileHashes.put(file.toString(), hash);
                    fileHandler.sendFile(file.toFile());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFileChecksum(Path file) throws IOException {	// check file using hash
    	InputStream fis = null;
    	
        try {
        	fis = Files.newInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[1024];
            int nRead;
            
            while ((nRead = fis.read(buf)) != -1) {
                digest.update(buf, 0, nRead);
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
        	try {
        		if (fis != null) fis.close();
        	} catch (Exception e) {
                e.printStackTrace();
        	}
        }
    }
}

class FileHandler {
    private final String serverAddr;

    public FileHandler(String addr) {
        this.serverAddr = addr;
    }

    public void sendFile(File file) {	// file UPLOAD
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
            int nRead;
            while ((nRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, nRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
                if (socket != null) socket.close();
                if (fis != null) fis.close();
                if (dos != null) dos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void deleteFile(String fileName) {	// file DELETE
    	DataOutputStream dos = null;
    	Socket socket = null;
    	
        try {
        	dos = new DataOutputStream(socket.getOutputStream());
        	socket = new Socket(serverAddr, Config.SERVER_PORT);
            
            dos.writeUTF("DELETE");
            dos.writeUTF(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	try {
        		if (dos != null) dos.close();
        		if (socket != null) socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

public class Client {
    public static void main(String[] args) {
        FileHandler handler = new FileHandler("localhost");
		FileObserver observer = new FileObserver(Config.CLIENT_DIR, handler);
        observer.observe();
    }
}
