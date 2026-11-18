package com.miniudemy.common.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.HashSet;
import java.util.Set;

import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.persistence.Entity;

/**
 * Katman kuralları (#86; docs/architecture.md §3). Bütün modüller için paket kalıbıyla yazılır:
 * {@code com.miniudemy.<modül>.<katman>}. Modüller arası kurallar {@link ModuleArchitectureTest}'te, Observer kuralı
 * {@link ObserverArchitectureTest}'te.
 */
@AnalyzeClasses(packages = "com.miniudemy", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerArchitectureTest {

    private static final String CONTROLLER = "com.miniudemy.*.controller..";
    private static final String SERVICE = "com.miniudemy.*.service..";
    private static final String REPOSITORY = "com.miniudemy.*.repository..";
    private static final String DTO = "com.miniudemy.*.dto..";
    private static final String MAPPER = "com.miniudemy.*.mapper..";
    private static final String DOMAIN = "com.miniudemy.*.domain..";

    @ArchTest
    static final ArchRule controllersDoNotUseRepositoriesOrMappers = noClasses()
            .that().resideInAPackage(CONTROLLER)
            .should().dependOnClassesThat().resideInAnyPackage(REPOSITORY, MAPPER)
            .because("controller yalnız service ve DTO kullanır; veri erişimi ve dönüşüm service'tedir (§3)");

    @ArchTest
    static final ArchRule controllersDoNotTakeOrReturnEntities = classes()
            .that().resideInAPackage(CONTROLLER).and().areAnnotatedWith(RestController.class)
            .should(notExposeEntities())
            .because("entity API'ye çıkmaz; controller yalnız DTO alır ve döner (§3, ADR-006)");

    @ArchTest
    static final ArchRule servicesDoNotUseControllers = noClasses()
            .that().resideInAPackage(SERVICE)
            .should().dependOnClassesThat().resideInAPackage(CONTROLLER)
            .because("bağımlılık yönü controller → service → repository (§3)");

    @ArchTest
    static final ArchRule repositoriesOnlyUseDomain = noClasses()
            .that().resideInAPackage(REPOSITORY)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, DTO, MAPPER)
            .because("repository yalnız domain'i kullanır (§3)");

    @ArchTest
    static final ArchRule dtosDoNotUseOtherLayers = noClasses()
            .that().resideInAPackage(DTO)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, REPOSITORY, MAPPER)
            .because("DTO düz veri taşır; katmanlara bağımlı değildir (§3)");

    @ArchTest
    static final ArchRule mappersDoNotUseUpperLayersOrRepositories = noClasses()
            .that().resideInAPackage(MAPPER)
            .should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE, REPOSITORY)
            .because("mapper yalnız domain ↔ DTO dönüştürür (§3)");

    /**
     * Domain hiçbir katmana bağımlı değildir; dışarıya tek izni {@code common.exception}'daki ortak hata tipleridir
     * (ADR-010). {@code common.security}, {@code common.config} ve {@code common.mapper} yasaktır. Başka modülün
     * domain'ine erişim kuralı {@link ModuleArchitectureTest}'te.
     */
    @ArchTest
    static final ArchRule domainOnlyDependsOnPersistenceJavaAndCommonException = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..", "jakarta.persistence..", "org.hibernate.annotations..", "org.hibernate.type..",
                    DOMAIN, "com.miniudemy.common.exception..")
            .because("domain diğer katmanlardan bağımsızdır; yalnız ortak hata tiplerini kullanır (§3, ADR-010)");

    /** Controller'ın public metotlarında dönüş ve parametre tiplerinde (generic argümanlar dahil) entity olmamalı. */
    private static ArchCondition<JavaClass> notExposeEntities() {
        return new ArchCondition<>("entity almamalı ve döndürmemeli") {
            @Override
            public void check(JavaClass controller, ConditionEvents events) {
                for (JavaMethod method : controller.getMethods()) {
                    if (!method.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC)) {
                        continue;
                    }
                    Set<JavaClass> involved = new HashSet<>(method.getReturnType().getAllInvolvedRawTypes());
                    method.getParameterTypes().forEach(p -> involved.addAll(p.getAllInvolvedRawTypes()));
                    involved.stream().filter(t -> t.isAnnotatedWith(Entity.class)).forEach(entity ->
                            events.add(SimpleConditionEvent.violated(method, method.getFullName()
                                    + " imzasında entity var: " + entity.getName())));
                }
            }
        };
    }
}
