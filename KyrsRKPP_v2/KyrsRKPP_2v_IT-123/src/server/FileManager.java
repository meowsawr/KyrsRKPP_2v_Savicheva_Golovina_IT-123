package server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// работа с файловой системой (хранилище данных)
public class FileManager {
    private final String FILES_FOLDER = "server_files/";

    public FileManager() {
        File folder = new File(FILES_FOLDER);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    // получение списка доступных файлов
    public List<String> getAvailableFiles() {
        List<String> fileList = new ArrayList<>();
        File folder = new File(FILES_FOLDER);

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getName());
                }
            }
        }
        return fileList;
    }

    public File getFile(String filename) {
        File file = new File(FILES_FOLDER + filename);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }
}