package com.filesender.app.storage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DownloadStoreTest {

    @Test
    public void stripsDirectoryTraversal() {
        assertEquals("evil.txt", DownloadStore.sanitizeFileName("../../evil.txt"));
        assertEquals("evil.txt", DownloadStore.sanitizeFileName("..\\..\\evil.txt"));
    }

    @Test
    public void replacesUnsafeCharacters() {
        assertEquals("a_b_c_d_e_f.txt", DownloadStore.sanitizeFileName("a?b*c<d>e|f.txt"));
        assertEquals("foto.jpg", DownloadStore.sanitizeFileName("foto\u0000.jpg"));
    }

    @Test
    public void rejectsEmptyAndDotNames() {
        assertEquals("archivo", DownloadStore.sanitizeFileName(""));
        assertEquals("archivo", DownloadStore.sanitizeFileName("."));
        assertEquals("archivo", DownloadStore.sanitizeFileName(".."));
        assertEquals("archivo", DownloadStore.sanitizeFileName("   "));
    }

    @Test
    public void limitsVeryLongNames() {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            name.append('a');
        }
        name.append(".jpg");
        String result = DownloadStore.sanitizeFileName(name.toString());
        assertTrue(result.length() <= 180);
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    public void addsDuplicateSuffixBeforeExtension() {
        assertEquals("foto (1).jpg", DownloadStore.withSuffix("foto.jpg", 1));
        assertEquals("README (2)", DownloadStore.withSuffix("README", 2));
        assertEquals(".oculto (3)", DownloadStore.withSuffix(".oculto", 3));
    }
}
