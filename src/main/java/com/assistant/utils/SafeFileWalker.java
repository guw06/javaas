package com.assistant.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

public final class SafeFileWalker {
    private SafeFileWalker() {
    }

    public static Stream<Path> walk(Path root, int maxDepth) {
        if (root == null || maxDepth < 0 || !Files.isDirectory(root)) {
            return Stream.empty();
        }

        List<Path> paths = new ArrayList<>();
        try {
            Files.walkFileTree(root, EnumSet.noneOf(java.nio.file.FileVisitOption.class), maxDepth, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    paths.add(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    paths.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | SecurityException ignored) {
        }

        return paths.stream();
    }
}
