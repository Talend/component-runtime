package org.talend.sdk.component.starter.server.service.facet.test;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class Gradle {

    @Test
    public void test() throws IOException, InterruptedException {
        final File gradleHome = new File(
                "C:\\Users\\akhabali\\dev\\github\\component-runtime\\component-starter-server\\target\\gradle-4.4");
        final File fakeproject = new File("target/gradlefakeproject");
        fakeproject.mkdirs();

        // todo generate wrapper commande
        final int exit = new ProcessBuilder()
                .inheritIO()
                .directory(fakeproject)
                .command(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(), "-cp",
                        new File(gradleHome, "lib").listFiles(
                                (dir, name) -> name.startsWith("gradle-launcher-") && name.endsWith(".jar"))[0]
                                        .getAbsolutePath(),
                        "org.gradle.launcher.GradleMain", "--no-daemon", "wrapper")
                .start()
                .waitFor();
        if (exit != 0) {
            throw new IllegalStateException("bad exit status generating gradle wrapper: " + exit);
        }
    }

}
