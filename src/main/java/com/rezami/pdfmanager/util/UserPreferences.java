package com.rezami.pdfmanager.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

public final class UserPreferences {
    private static final String KEY_LAST_DIRECTORY = "lastDirectory";
    private static final String KEY_RECURSIVE = "recursive";

    private final Preferences preferences;

    public UserPreferences() {
        this(Preferences.userNodeForPackage(UserPreferences.class));
    }

    UserPreferences(Preferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    public Optional<Path> loadLastDirectory() {
        String rawValue = preferences.get(KEY_LAST_DIRECTORY, "");
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        Path candidate = Path.of(rawValue);
        return Files.isDirectory(candidate) ? Optional.of(candidate) : Optional.empty();
    }

    public void saveLastDirectory(Path directory) {
        Objects.requireNonNull(directory, "directory");
        preferences.put(KEY_LAST_DIRECTORY, directory.toAbsolutePath().normalize().toString());
    }

    public boolean loadRecursiveSelected() {
        return preferences.getBoolean(KEY_RECURSIVE, false);
    }

    public void saveRecursiveSelected(boolean recursiveSelected) {
        preferences.putBoolean(KEY_RECURSIVE, recursiveSelected);
    }
}
