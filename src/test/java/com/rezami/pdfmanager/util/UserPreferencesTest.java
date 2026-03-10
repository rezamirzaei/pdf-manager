package com.rezami.pdfmanager.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserPreferencesTest {
    private final Preferences preferences =
            Preferences.userRoot().node("/com/rezami/pdfmanager/tests/" + UUID.randomUUID());
    private final UserPreferences userPreferences = new UserPreferences(preferences);

    @TempDir Path tempDir;

    @AfterEach
    void tearDown() throws BackingStoreException {
        preferences.removeNode();
        preferences.flush();
    }

    @Test
    void saveLastDirectory_persistsExistingDirectory() {
        userPreferences.saveLastDirectory(tempDir);

        assertThat(userPreferences.loadLastDirectory()).contains(tempDir);
    }

    @Test
    void loadLastDirectory_ignoresMissingDirectory() {
        Path missingDirectory = tempDir.resolve("missing");
        preferences.put("lastDirectory", missingDirectory.toString());

        assertThat(userPreferences.loadLastDirectory()).isEmpty();
    }

    @Test
    void saveRecursiveSelected_persistsSelection() {
        userPreferences.saveRecursiveSelected(true);

        assertThat(userPreferences.loadRecursiveSelected()).isTrue();
    }
}
