package com.hartwig.hmftools.common.exception;

import org.jetbrains.annotations.NotNull;

public class MalformedFileException extends HartwigException {

    private static final long serialVersionUID = 8961394836549355461L;

    public MalformedFileException(@NotNull final String message) {
        super(message);
    }
}
