package com.rezami.pdfmanager.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RenameStatusTest {
    @Test
    void isSkipped_isTrueOnlyForSkippedStatuses() {
        assertThat(RenameStatus.SKIPPED_NO_TITLE.isSkipped()).isTrue();
        assertThat(RenameStatus.SKIPPED_SAME_NAME.isSkipped()).isTrue();

        assertThat(RenameStatus.READY.isSkipped()).isFalse();
        assertThat(RenameStatus.ERROR.isSkipped()).isFalse();
    }
}

