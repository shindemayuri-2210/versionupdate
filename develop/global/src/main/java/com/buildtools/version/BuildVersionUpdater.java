package com.buildtools.version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Updates build version numbers in project files during the build process.
 *
 * This utility reads the build number from the BuildNum environment variable
 * and updates version references in SConstruct and VERSION files.
 */
public class BuildVersionUpdater {

    private static final String BUILD_NUM_ENV_VAR = "BuildNum";
    private static final String SOURCE_PATH_ENV_VAR = "SourcePath";
    private static final String RELATIVE_SOURCE_DIR = "develop/global/src";

    private final String buildNumber;
    private final Path sourceDirectory;


    /**
     * Constructs a BuildVersionUpdater with environment-based configuration.
     *
     * @throws BuildVersionException if required environment variables are missing or invalid
     */
    public BuildVersionUpdater() throws BuildVersionException {
        this.buildNumber = validateAndGetBuildNumber();
        this.sourceDirectory = validateAndGetSourceDirectory();
    }

    BuildVersionUpdater(String buildNumber, Path sourceDirectory) {
        this.buildNumber = buildNumber;
        this.sourceDirectory = sourceDirectory;
    }

    /**
     * Updates version numbers in all configured files.
     *
     * @throws BuildVersionException if any file update operation fails
     */
    public void updateVersionFiles() throws BuildVersionException {
        for (FileConfig config : FileConfig.values()) {
            updateFile(config);
        }
    }

    /**
     * Updates a single file according to its configuration.
     */
    private void updateFile(FileConfig config) throws BuildVersionException {
        Path filePath = sourceDirectory.resolve(config.fileName);

        try {
            validateFileExists(filePath);

            List<String> lines = Files.readAllLines(filePath);
            List<String> updatedLines = updateLines(lines, config);

            writeUpdatedContent(filePath, updatedLines);
            System.out.printf("Successfully updated %s with build number %s%n",
                    config.fileName, buildNumber);

        } catch (IOException e) {
            throw new BuildVersionException(
                    String.format("Failed to update file %s: %s", config.fileName, e.getMessage()), e);
        }
    }

    /**
     * Updates lines in the file content according to the pattern matching rules.
     */
    private List<String> updateLines(List<String> lines, FileConfig config) {
        return lines.stream()
                .map(line -> updateLineIfMatches(line, config))
                .collect(Collectors.toList());
    }

    /**
     * Updates a single line if it matches the pattern.
     */
    private String updateLineIfMatches(String line, FileConfig config) {
        if (config.pattern.matcher(line).find()) {
            return config.pattern.matcher(line)
                    .replaceAll(config.replacementPrefix + buildNumber);
        }
        return line;
    }

    /**
     * Safely writes updated content to file using atomic operations.
     */
    private void writeUpdatedContent(Path filePath, List<String> updatedLines) throws IOException {
        Path tempFile = Files.createTempFile(filePath.getParent(),
                filePath.getFileName().toString(), ".tmp");

        try {
            Files.write(tempFile, updatedLines);
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Clean up temp file if something goes wrong
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    /**
     * Validates that the required file exists.
     */
    private void validateFileExists(Path filePath) throws BuildVersionException {
        if (!Files.exists(filePath)) {
            throw new BuildVersionException(
                    String.format("File not found: %s", filePath.toAbsolutePath()));
        }
    }

    /**
     * Validates and retrieves the build number from environment variables.
     */
    private String validateAndGetBuildNumber() throws BuildVersionException {
        String buildNum = System.getenv(BUILD_NUM_ENV_VAR);

        if (buildNum == null || buildNum.trim().isEmpty()) {
            throw new BuildVersionException(
                    String.format("Environment variable %s is not set or empty", BUILD_NUM_ENV_VAR));
        }

        if (!isValidBuildNumber(buildNum.trim())) {
            throw new BuildVersionException(
                    String.format("Invalid build number format: %s. Expected numeric value.", buildNum));
        }

        return buildNum.trim();
    }

    /**
     * Validates and retrieves the source directory path from environment variables.
     */
    private Path validateAndGetSourceDirectory() throws BuildVersionException {
        String sourcePath = String.valueOf(Paths.get("").toAbsolutePath());

        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            throw new BuildVersionException(
                    String.format("Environment variable %s is not set or empty", SOURCE_PATH_ENV_VAR));
        }

        Path sourceDir = Paths.get(sourcePath.trim(), RELATIVE_SOURCE_DIR);

        if (!Files.exists(sourceDir)) {
            throw new BuildVersionException(
                    String.format("Source directory does not exist: %s", sourceDir.toAbsolutePath()));
        }

        if (!Files.isDirectory(sourceDir)) {
            throw new BuildVersionException(
                    String.format("Source path is not a directory: %s", sourceDir.toAbsolutePath()));
        }

        return sourceDir;
    }

    /**
     * Validates that the build number contains only digits.
     */
    private boolean isValidBuildNumber(String buildNumber) {
        return buildNumber.matches("\\d+");
    }

    /**
     * Main entry point for the build version updater.
     */
    public static void main(String[] args) {
        try {
            BuildVersionUpdater updater = new BuildVersionUpdater();
            updater.updateVersionFiles();
            System.out.println("Build version update completed successfully.");

        } catch (BuildVersionException e) {
            System.err.println("Build version update failed: " + e.getMessage());
        }
    }
}