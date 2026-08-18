/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.oritech.profile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/** Finds the exact Oritech 1.2.10 artifact installed by All the Mons 1.2.0. */
public final class ExactOritechArtifactDetector {

    private static final long SIZE = 10_990_540L;
    private static final String SHA256 =
            "7c17c78ac55d9cbb71a9108a2bec7e2659192e08c5a1b49026088f875dbde821";

    private ExactOritechArtifactDetector() {
    }

    public static Optional<Path> find(Iterable<Path> roots) {
        int inspected = 0;
        for (Path root : roots) {
            if (++inspected > 4_096 || Thread.currentThread().isInterrupted()) {
                return Optional.empty();
            }
            try {
                if (root != null && Files.isRegularFile(root) && Files.size(root) == SIZE
                        && SHA256.equals(digest(root))) {
                    return Optional.of(root.toRealPath());
                }
            } catch (IOException exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static String digest(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1_024];
            try (InputStream input = Files.newInputStream(path)) {
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
