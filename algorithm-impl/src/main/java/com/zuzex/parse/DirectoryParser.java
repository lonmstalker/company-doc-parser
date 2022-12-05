package com.zuzex.parse;

import com.zuzex.config.AppProperties;
import com.zuzex.data.algorithm.ArchiveData;
import com.zuzex.data.parse.ArchiveMetadata;
import com.zuzex.reader.MetaCsvReader;
import com.zuzex.utils.PathUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.zuzex.constants.Constants.*;
import static java.util.Objects.nonNull;
import static org.apache.commons.io.FilenameUtils.isExtension;

@Component
public class DirectoryParser {
    private final String archiveFolder;
    private final File archiveDirectory;
    private final MetaCsvReader metaCsvReader;

    public DirectoryParser(final AppProperties appProperties, final MetaCsvReader metaCsvReader) {
        this.metaCsvReader = metaCsvReader;
        this.archiveFolder = appProperties.getArchiveFolder();
        archiveDirectory = findSubdirectoryWithArchives();
    }

    public PriorityQueue<ArchiveData> unzipAndReadMeta(final Queue<File> archives, final Consumer<ArchiveData> unzipCallback) throws IOException {
        final var threadArchives = new PriorityQueue<ArchiveData>();
        while (!archives.isEmpty()) {
            final var archive = archives.poll();
            if (nonNull(archive)) {
                final var archivePair = this.unzip(archive);
                final var csvMeta = this.metaCsvReader.readDate(Path.of(archivePair.getKey()));
                if (csvMeta.isEmpty()) {
                    continue;
                }
                final var archiveDate = csvMeta.values()
                        .stream()
                        .findFirst()
                        .map(ArchiveMetadata::actualDate)
                        .orElseThrow(() -> new RuntimeException("Не найдены метаданные в файле: " + archivePair.getValue().toString()));
                final var archiveData = new ArchiveData(archivePair.getValue(), archiveDate);
                threadArchives.add(archiveData);
                unzipCallback.accept(archiveData);
            }
        }
        return threadArchives;
    }

    // достаем все файлы из архива
    public Pair<String, Path> unzip(final File archive) throws IOException {
        int len;
        String metadata = null;
        FileOutputStream fos = null;
        final var buffer = new byte[1024];
        try (final var zis = new ZipInputStream(new FileInputStream(archive))) {
            ZipEntry entry;
            final var directory = createArchiveAndReturnName(archive);
            while (nonNull(entry = zis.getNextEntry())) {
                if (entry.isDirectory()) {
                    continue;
                }
                final var filePath = directory + PATH_DELIMITER + entry.getName();
                if (isExtension(entry.getName(), ACTUAL_DATE_FILE_FORMAT)) {
                    metadata = filePath;
                }
                fos = new FileOutputStream(filePath);
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                Arrays.fill(buffer, (byte) 0);
            }
            return Pair.of(metadata, directory);
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
        } finally {
            byteStream.flush();
            byteStream.reset();
        }
    }

    public LinkedBlockingQueue<File> findArchives() throws FileNotFoundException {
        final var files = archiveDirectory.listFiles(file -> file.isFile() && file.getName().endsWith(".zip"));

        if (Objects.isNull(files)) {
            throw new FileNotFoundException("Current directory is empty");
        }

        return new LinkedBlockingQueue<>(Arrays.asList(files));
    }

    // Files.walk
    public File findSubdirectoryWithArchives() {
        final var files = new File(CURRENT_PATH).listFiles(File::isDirectory);

        if (Objects.isNull(files)) {
            throw new RuntimeException("Current directory is empty");
        }

        for (final var file : files) {
            if (this.archiveFolder.equalsIgnoreCase(file.getName())) {
                return file;
            }
        }

        throw new RuntimeException(String.format("Not found archive directory: '%s'", CURRENT_PATH + PATH_DELIMITER + this.archiveFolder));
    }

    public static void recreateFile(final String fileName) throws IOException {
        final var currentDirectory = Path.of(fileName);
        Files.deleteIfExists(currentDirectory);
        Files.createDirectories(Path.of(PathUtils.removeLastPackageFromPath(fileName)));
        Files.createFile(currentDirectory);
    }

    private static Path createArchiveAndReturnName(final File archive) throws IOException {
        return Files.createDirectory(Path.of(archive.getParent() + UNZIP_PREFIX + FilenameUtils.removeExtension(archive.getName())));
    }
}
