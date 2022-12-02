package com.zuzex.parse;

import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.nonNull;

public class DirectoryParser {
    public static final String ACTUAL_DATE_FILE_FORMAT = ".csv";
    public static final String ACTUAL_DATE_COLUMN = "actual_date";
    private final String currentPath;
    private final String archiveFolder;
    private final File archiveDirectory;

    public DirectoryParser(final String currentPath, final String archiveFolder) {
        this.currentPath = currentPath;
        this.archiveFolder = archiveFolder;
        archiveDirectory = findSubdirectoryWithArchives();
    }

    // достаем все файлы из архива
    public Path unzip(final File archive, int threadNumber) throws IOException {
        int len;
        FileOutputStream fos = null;
        final var buffer = new byte[1024];
        try (final var zis = new ZipInputStream(new FileInputStream(archive))) {
            ZipEntry entry;
            final var directory = createArchiveAndReturnName(archive, threadNumber);
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                fos = new FileOutputStream(directory + "\\" + entry.getName());
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                Arrays.fill(buffer, (byte) 0);
            }
            return directory;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (nonNull(fos)) {
                fos.close();
            }
        }
    }

    public byte[] readBytes(final ByteArrayOutputStream byteStream, final File file) throws IOException {
        int len;
        try (final var stream = new FileInputStream(file)) {
            while ((len = stream.read()) != -1) {
                byteStream.write(len);
            }
            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            byteStream.flush();
            byteStream.reset();
        }
    }

    public LinkedBlockingQueue<File> findArchives() throws FileNotFoundException {
        final var files = archiveDirectory
                .listFiles(file -> file.isFile() && file.getName().endsWith(".zip"));

        if (Objects.isNull(files)) {
            throw new FileNotFoundException("Current directory is empty");
        }

        return new LinkedBlockingQueue<>(Arrays.asList(files));
    }

    // Files.walk
    public File findSubdirectoryWithArchives() {
        final var currentDirectory = new File(this.currentPath);
        final var files = currentDirectory.listFiles(File::isDirectory);

        if (Objects.isNull(files)) {
            throw new RuntimeException("Current directory is empty");
        }

        for (final var file : files) {
            if (this.archiveFolder.equalsIgnoreCase(file.getName())) {
                return file;
            }
        }

        throw new RuntimeException(String.format("Not found archive directory: '%s'", this.currentPath + "\\" + this.archiveFolder));
    }

    public static void recreateFile(final String fileName) {
        final var currentDirectory = Path.of(fileName);
        try {
            Files.deleteIfExists(currentDirectory);
            Files.createDirectories(Path.of(fileName.split("\\\\")[0]));
            Files.createFile(currentDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path createArchiveAndReturnName(final File archive, final int threadNumber) throws IOException {
        return Files.createDirectory(Path.of(archive.getParent() + "\\unzip\\" + threadNumber + "-" + FilenameUtils.removeExtension(archive.getName())));
    }
}
