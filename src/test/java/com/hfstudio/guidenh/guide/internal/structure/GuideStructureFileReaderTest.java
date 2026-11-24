package com.hfstudio.guidenh.guide.internal.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuideStructureFileReaderTest {

    @Test
    void readsOnlyBoundedStructureFilesUnderItsRoot(@TempDir Path sandbox) throws IOException {
        Path root = Files.createDirectory(sandbox.resolve("structures"));
        var reader = new GuideStructureFileReader(root, 64);
        String structure = "{size:[1,1,1],palette:[],blocks:[]}";
        Files.writeString(root.resolve("valid.snbt"), structure);
        Path outside = Files.writeString(sandbox.resolve("outside.snbt"), structure);
        Files.writeString(root.resolve("wrong.txt"), structure);
        Files.writeString(root.resolve("large.snbt"), " ".repeat(65));

        assertEquals(structure, reader.read(Path.of("valid.snbt")));
        assertThrows(IOException.class, () -> reader.read(outside));
        assertThrows(IOException.class, () -> reader.read(Path.of("wrong.txt")));
        assertThrows(IOException.class, () -> reader.read(Path.of("large.snbt")));
    }
}
