package com.miniudemy.common.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Modüller arası kurallar (#86; docs/architecture.md §4). Bir modül başka bir modülün yalnız {@code service} ve
 * {@code dto} paketlerini, domain'inden yalnız olayları (record) ve enum'ları kullanabilir; repository, entity,
 * controller ve mapper yasaktır. Hangi modülün hangisine bağımlı olabileceği §4'teki tablonun aynısıdır
 * ({@link #ALLOWED}); tablo değişirse ikisi aynı PR'da değişir.
 */
@AnalyzeClasses(packages = "com.miniudemy", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleArchitectureTest {

    /** docs/architecture.md › İzin verilen modül bağımlılıkları ("Service'ini kullanabileceği modüller"). */
    static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry("auth", Set.of()),
            Map.entry("course", Set.of("auth")),
            Map.entry("enrollment", Set.of("course")),
            Map.entry("content", Set.of("course", "enrollment")),
            Map.entry("progress", Set.of("course", "enrollment")),
            Map.entry("quiz", Set.of("course", "enrollment")),
            Map.entry("certificate", Set.of("course", "progress", "quiz")),
            Map.entry("review", Set.of("auth", "course", "enrollment")),
            Map.entry("notification", Set.of("auth")),
            Map.entry("admin", Set.of("auth", "course", "enrollment", "progress", "certificate", "review")));

    private static final String COMMON = "common";

    @ArchTest
    static final ArchRule modulesOnlyUseServiceDtoEventsAndEnumsOfOtherModules = classes()
            .that().resideInAPackage("com.miniudemy.*..")
            .should(onlyUseAllowedPartsOfOtherModules())
            .because("modüller yalnız service arayüzü, DTO ve olaylarla konuşur; repository ve entity'ye erişilmez (§4)");

    @ArchTest
    static final ArchRule modulesOnlyDependOnModulesInTheTable = classes()
            .that().resideInAPackage("com.miniudemy.*..")
            .should(onlyDependOnModulesInTheTable())
            .because("tabloda olmayan bir bağımlılık için önce docs/architecture.md güncellenir (§4)");

    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices().matching("com.miniudemy.(*)..")
            .should().beFreeOfCycles()
            .because("modüller arası döngüsel bağımlılık olmaz (§4)");

    @ArchTest
    static final ArchRule commonDependsOnNoModule = noClasses()
            .that().resideInAPackage("com.miniudemy.common..")
            .should().dependOnClassesThat().resideInAnyPackage(ALLOWED.keySet().stream()
                    .map(module -> "com.miniudemy." + module + "..").toArray(String[]::new))
            .because("common hiçbir modüle bağımlı değildir; gerekirse arayüzü common tanımlar, modül uygular (§4)");

    /** Kurallar bütün modülleri kapsar: kaynak dizinindeki modüller ile tablodaki modüller birebir aynı. */
    @Test
    void everyModuleInTheSourceTreeIsInTheRules() throws IOException {
        Set<String> modulesOnDisk;
        try (Stream<Path> dirs = Files.list(Path.of("src/main/java/com/miniudemy"))) {
            modulesOnDisk = dirs.filter(Files::isDirectory).map(d -> d.getFileName().toString())
                    .filter(name -> !name.equals(COMMON)).collect(Collectors.toCollection(TreeSet::new));
        }
        assertThat(modulesOnDisk).containsExactlyInAnyOrderElementsOf(ALLOWED.keySet()).hasSize(10);
    }

    // --- yardımcılar ---

    /** {@code com.miniudemy.<modül>...} içindeki modül adı; kök paket ve dışarısı için null. */
    static String moduleOf(JavaClass javaClass) {
        String pkg = javaClass.getPackageName();
        if (!pkg.startsWith("com.miniudemy.")) {
            return null;
        }
        String rest = pkg.substring("com.miniudemy.".length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    /** {@code com.miniudemy.<modül>.<katman>...} içindeki katman adı; yoksa boş. */
    static String layerOf(JavaClass javaClass) {
        String[] parts = javaClass.getPackageName().split("\\.");
        return parts.length > 3 ? parts[3] : "";
    }

    /** Başka bir modüldeki hedef: kaynak ve hedef modül farklı, ikisi de common değil. Değilse null. */
    private static JavaClass otherModuleTarget(JavaClass source, Dependency dependency) {
        JavaClass target = dependency.getTargetClass().getBaseComponentType();
        String from = moduleOf(source);
        String to = moduleOf(target);
        if (from == null || to == null || from.equals(to) || from.equals(COMMON) || to.equals(COMMON)) {
            return null;
        }
        return target;
    }

    /** Domain'deki record'lar olaydır (Observer, ADR-009); başka modül onları dinleyebilir. */
    private static boolean isEvent(JavaClass target) {
        return layerOf(target).equals("domain") && target.isRecord();
    }

    private static ArchCondition<JavaClass> onlyUseAllowedPartsOfOtherModules() {
        return new ArchCondition<>("başka modülden yalnız service, dto, olay (record) ve enum kullanmalı") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = otherModuleTarget(source, dependency);
                    if (target == null) {
                        continue;
                    }
                    String layer = layerOf(target);
                    boolean allowed = layer.equals("service") || layer.equals("dto")
                            || (layer.equals("domain") && (target.isRecord() || target.isEnum()));
                    if (!allowed) {
                        events.add(SimpleConditionEvent.violated(dependency,
                                dependency.getDescription() + " — başka modülün " + layer + " paketi"));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> onlyDependOnModulesInTheTable() {
        return new ArchCondition<>("yalnız tablodaki modüllere bağımlı olmalı (olay dinlemek hariç)") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                    JavaClass target = otherModuleTarget(source, dependency);
                    if (target == null || isEvent(target)) {
                        continue;
                    }
                    String from = moduleOf(source);
                    String to = moduleOf(target);
                    if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
                        events.add(SimpleConditionEvent.violated(dependency,
                                dependency.getDescription() + " — tabloda " + from + " → " + to + " yok"));
                    }
                }
            }
        };
    }
}
