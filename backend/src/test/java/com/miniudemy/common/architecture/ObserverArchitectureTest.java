package com.miniudemy.common.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Observer deseni (#42, ADR-009): yayıncı service'ler dinleyicileri tanımaz. Yalnız bu kural denetlenir; genel modül
 * sınırı kuralları ayrı issue'dadır (#86).
 */
@AnalyzeClasses(packages = "com.miniudemy", importOptions = ImportOption.DoNotIncludeTests.class)
class ObserverArchitectureTest {

    @ArchTest
    static final ArchRule servicesDoNotDependOnListeners = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..listener..")
            .because("service olay yayınlar, dinleyiciyi doğrudan çağırmaz (ADR-009)");
}
