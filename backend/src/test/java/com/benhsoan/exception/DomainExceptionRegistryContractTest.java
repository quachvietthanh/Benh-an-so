package com.benhsoan.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

class DomainExceptionRegistryContractTest {

    private static final Path DOMAIN_SOURCE_ROOT = Path.of("src", "main", "java", "com", "benhsoan", "domain");
    private static final String DOMAIN_PACKAGE = "com.benhsoan.domain.";
    private static final Pattern ERROR_CODE_REFERENCE = Pattern.compile("DomainErrorCode\\.([A-Z0-9_]+)");

    @Test
    void registersEveryConcreteDomainExceptionAndDoesNotLeaveUnusedCodes() throws IOException {
        Set<Class<? extends DomainException>> exceptionTypes = concreteDomainExceptionTypes();
        Set<String> missingCodes = new LinkedHashSet<>();
        Set<DomainErrorCode> registeredCodes = EnumSet.noneOf(DomainErrorCode.class);

        for (Class<? extends DomainException> exceptionType : exceptionTypes) {
            Matcher matcher = ERROR_CODE_REFERENCE.matcher(sourceOf(exceptionType));
            if (!matcher.find()) {
                missingCodes.add(exceptionType.getName());
                continue;
            }
            registeredCodes.add(DomainErrorCode.valueOf(matcher.group(1)));
        }

        assertTrue(missingCodes.isEmpty(), "Domain exceptions without a stable error code: " + missingCodes);
        assertEquals(EnumSet.allOf(DomainErrorCode.class), registeredCodes,
                "Domain error codes without a concrete exception registration");
    }

    @Test
    void mapsEveryNotFoundCodeToHttpNotFound() {
        for (DomainErrorCode code : DomainErrorCode.values()) {
            if (code.name().endsWith("_NOT_FOUND")) {
                assertEquals(HttpStatus.NOT_FOUND, DomainExceptionHttpStatusMapper.statusFor(code),
                        () -> code + " must map to HTTP 404");
            }
        }
    }

    private Set<Class<? extends DomainException>> concreteDomainExceptionTypes() throws IOException {
        try (Stream<Path> files = Files.walk(DOMAIN_SOURCE_ROOT)) {
            Set<Class<? extends DomainException>> types = new LinkedHashSet<>();
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith("Exception.java")).toList()) {
                String relativeClassName = DOMAIN_SOURCE_ROOT.relativize(file)
                        .toString()
                        .replace(file.getFileSystem().getSeparator(), ".")
                        .replaceFirst("\\.java$", "");
                Class<?> type = loadClass(DOMAIN_PACKAGE + relativeClassName);
                if (DomainException.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers())) {
                    types.add(type.asSubclass(DomainException.class));
                }
            }
            return types;
        }
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("Unable to load domain exception " + className, ex);
        }
    }

    private String sourceOf(Class<? extends DomainException> exceptionType) throws IOException {
        String relativePath = exceptionType.getName()
                .substring(DOMAIN_PACKAGE.length())
                .replace('.', '/') + ".java";
        return Files.readString(DOMAIN_SOURCE_ROOT.resolve(relativePath));
    }
}
