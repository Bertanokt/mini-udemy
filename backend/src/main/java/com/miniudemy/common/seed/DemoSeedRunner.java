package com.miniudemy.common.seed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Demo ve manuel test verisi (#67). Yalnız {@code dev} profilinde (docker compose ve yerel geliştirme) ve veritabanında
 * hiç kullanıcı yokken çalışır; {@code prod} ve {@code test} profillerinde bean oluşmaz.
 *
 * <ul>
 *   <li>{@code seed/demo-seed.sql}'i tek işlemde çalıştırır; hesapların parolası {@link #PASSWORD}, BCrypt özeti çalışma
 *       anında üretilir (repoda özet tutulmaz).</li>
 *   <li>{@code stored_files}'taki her anahtar için {@code seed/files/<anahtar>} dosyasını depolama dizinine
 *       ({@code app.storage.root}) kopyalar ve gerçek boyutunu yazar. Kopyalama da işlemin içinde: biri başarısız
 *       olursa veri geri alınır, yarım seed kalmaz.</li>
 * </ul>
 *
 * Modüllerin repository'lerine bağımlı olmamak için (common hiçbir modüle bağımlı değildir, ArchUnit) yalnız SQL
 * kullanır.
 */
@Component
@Profile("dev")
public class DemoSeedRunner implements ApplicationRunner {

    /** Bütün demo hesaplarının parolası (README › Demo hesapları). Yalnız yerel demo içindir. */
    public static final String PASSWORD = "Demo1234!";

    static final String SCRIPT = "seed/demo-seed.sql";
    static final String FILES = "seed/files/";
    private static final String HASH_PLACEHOLDER = "{{PASSWORD_HASH}}";
    private static final Logger log = LoggerFactory.getLogger(DemoSeedRunner.class);

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaction;
    private final Path storageRoot;

    public DemoSeedRunner(JdbcTemplate jdbc, DataSource dataSource, PasswordEncoder passwordEncoder,
            PlatformTransactionManager transactionManager, @Value("${app.storage.root}") Path storageRoot) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.transaction = new TransactionTemplate(transactionManager);
        this.storageRoot = storageRoot;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    /** Veritabanı boşsa seed verisini yükler ve true döner; kullanıcı varsa hiçbir şeye dokunmaz. */
    public boolean seed() {
        Long users = jdbc.queryForObject("SELECT count(*) FROM users", Long.class);
        if (users != null && users > 0) {
            log.info("Demo verisi atlandı: veritabanında {} kullanıcı var.", users);
            return false;
        }
        transaction.executeWithoutResult(status -> {
            runScript();
            copyFiles();
        });
        log.info("Demo verisi yüklendi (#67). Hesaplar README › Demo hesapları'nda.");
        return true;
    }

    private void runScript() {
        String sql;
        try (InputStream in = new ClassPathResource(SCRIPT).getInputStream()) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Seed betiği okunamadı: " + SCRIPT, e);
        }
        sql = sql.replace(HASH_PLACEHOLDER, passwordEncoder.encode(PASSWORD));
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.populate(DataSourceUtils.getConnection(dataSource));
    }

    private void copyFiles() {
        List<String> keys = jdbc.queryForList("SELECT storage_key FROM stored_files", String.class);
        try {
            Files.createDirectories(storageRoot);
            for (String key : keys) {
                ClassPathResource source = new ClassPathResource(FILES + key);
                Path target = storageRoot.resolve(key);
                try (InputStream in = source.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                jdbc.update("UPDATE stored_files SET size_bytes = ? WHERE storage_key = ?", Files.size(target), key);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Seed dosyaları depolama dizinine kopyalanamadı.", e);
        }
    }
}
