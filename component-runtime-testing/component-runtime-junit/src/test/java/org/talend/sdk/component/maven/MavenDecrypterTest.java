package org.talend.sdk.component.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.junit.ExceptionVerifier;

public class MavenDecrypterTest {

    @Rule
    public ExceptionVerifier<IllegalArgumentException> exceptions = new ExceptionVerifier<>();

    @Test
    public void encrypted() {
        final ClassLoader loader = MavenDecrypterTest.class.getClassLoader();
        final File settings = new File(loader.getResource("maven/settings.xml").getFile());
        final File settingsSecurity = new File(loader.getResource("maven/settings-security.xml").getFile());
        MavenDecrypter decrypter = new MavenDecrypter(settings, settingsSecurity);

        final Server encrypted = decrypter.find("encrypted");
        assertEquals("repouser", encrypted.getUsername());
        assertEquals("encrypted", encrypted.getPassword());
    }

    @Test
    public void clear() {
        final ClassLoader loader = MavenDecrypterTest.class.getClassLoader();
        final File settings = new File(loader.getResource("maven/settings.xml").getFile());
        final File settingsSecurity = new File(loader.getResource("maven/settings-security.xml").getFile());
        MavenDecrypter decrypter = new MavenDecrypter(settings, settingsSecurity);

        final Server encrypted = decrypter.find("clear");
        assertEquals("repouser", encrypted.getUsername());
        assertEquals("repopwd", encrypted.getPassword());
    }

    @Test()
    public void noMasterPassword() {
        exceptions.assertWith(e -> {
            assertEquals("Master password can't be null or empty.", e.getMessage());
        });

        final ClassLoader loader = MavenDecrypterTest.class.getClassLoader();
        final File settings = new File(loader.getResource("maven/settings.xml").getFile());
        final File settingsSecurity = new File(loader.getResource("maven/settings-security-null.xml").getFile());
        MavenDecrypter decrypter = new MavenDecrypter(settings, settingsSecurity);
        final Server encrypted = decrypter.find("encrypted");
        assertEquals("repouser", encrypted.getUsername());
        assertEquals("repopwd", encrypted.getPassword());

    }

    @Test
    public void emptyMasterPassword() {
        exceptions.assertWith(e -> {
            assertEquals("Master password can't be null or empty.", e.getMessage());
        });

        final ClassLoader loader = MavenDecrypterTest.class.getClassLoader();
        final File settings = new File(loader.getResource("maven/settings-empty.xml").getFile());
        final File settingsSecurity = new File(loader.getResource("maven/settings-security-empty.xml").getFile());
        MavenDecrypter decrypter = new MavenDecrypter(settings, settingsSecurity);
        final Server encrypted = decrypter.find("encrypted-empty-master");
        assertEquals("repouser", encrypted.getUsername());
        assertEquals("encrypted", encrypted.getPassword());
    }

}
