package com.miniudemy.common.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * UML bileşen diyagramı (#91) kodla tutarlı mı: {@code docs/diagrams/component.puml} (modüller ve service
 * bağımlılıkları) ve {@code component-events.puml} (olaylar).
 *
 * <ul>
 *   <li>Her modül ve {@code common} diyagramda bir bileşen.</li>
 *   <li>Düz oklar ({@code a --> b}) modül bağımlılıklarıdır ve {@link ModuleArchitectureTest#ALLOWED} tablosunun
 *       birebir aynısıdır; ArchUnit kodun bu tabloya uyduğunu ayrıca denetler.</li>
 *   <li>Olay görünümündeki kesikli oklar ({@code a ..> b : Olay, Olay}): yayıncı modülün {@code domain}'indeki olayı alıcı
 *       modülün {@code listener} paketindeki bir {@link TransactionalEventListener} metodu dinler. Diyagramdaki her
 *       olay kodda dinleniyor, koddaki her dinleyici diyagramda var.</li>
 * </ul>
 *
 * Diyagram değişip kod değişmezse (ya da tersi) bu test kırılır; ikisi aynı PR'da güncellenir.
 */
class ComponentDiagramTest {

    private static final Path DIAGRAM = Path.of("..", "docs", "diagrams", "component.puml");
    private static final Path EVENTS_DIAGRAM = Path.of("..", "docs", "diagrams", "component-events.puml");
    private static final Pattern COMPONENT = Pattern.compile("^\\s*component\\s+\"[^\"]*\"\\s+as\\s+(\\w+)", Pattern.MULTILINE);
    private static final Pattern DEPENDENCY = Pattern.compile("^\\s*(\\w+)\\s+-+>\\s+(\\w+)\\s*:", Pattern.MULTILINE);
    private static final Pattern EVENT = Pattern.compile("^\\s*(\\w+)\\s+\\.+>\\s+(\\w+)\\s*:\\s*(.+)$", Pattern.MULTILINE);

    private static String diagram;
    private static String eventsDiagram;
    private static JavaClasses classes;

    @BeforeAll
    static void load() throws IOException {
        diagram = Files.readString(DIAGRAM);
        eventsDiagram = Files.readString(EVENTS_DIAGRAM);
        classes = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.miniudemy");
    }

    @Test
    void everyModuleAndCommonIsAComponent() {
        Set<String> components = new TreeSet<>();
        Matcher m = COMPONENT.matcher(diagram);
        while (m.find()) {
            components.add(m.group(1));
        }
        Set<String> expected = new TreeSet<>(ModuleArchitectureTest.ALLOWED.keySet());
        expected.add("common");

        assertThat(components).containsAll(expected);
    }

    @Test
    void moduleDependencyArrows_areExactlyTheArchUnitTable() {
        Set<String> drawn = new TreeSet<>();
        Matcher m = DEPENDENCY.matcher(diagram);
        while (m.find()) {
            if (ModuleArchitectureTest.ALLOWED.containsKey(m.group(1))) {
                drawn.add(m.group(1) + " -> " + m.group(2));
            }
        }
        Set<String> allowed = new TreeSet<>();
        ModuleArchitectureTest.ALLOWED.forEach((from, targets) -> targets.forEach(to -> allowed.add(from + " -> " + to)));

        assertThat(drawn).as("diyagramdaki modül okları = ModuleArchitectureTest.ALLOWED").isEqualTo(allowed);
    }

    @Test
    void eventArrows_areExactlyTheListenersInCode() {
        Set<String> drawn = new TreeSet<>();
        Matcher m = EVENT.matcher(eventsDiagram);
        while (m.find()) {
            if (!ModuleArchitectureTest.ALLOWED.containsKey(m.group(1))) {
                continue; // frontend okları (sayfa → API istemcisi → REST) olay değildir
            }
            for (String event : m.group(3).split(",")) {
                drawn.add(m.group(1) + "." + event.strip() + " -> " + m.group(2));
            }
        }
        Set<String> listened = new TreeSet<>();
        for (JavaClass listener : classes) {
            if (!listener.getPackageName().endsWith(".listener")) {
                continue;
            }
            String listenerModule = moduleOf(listener);
            for (JavaMethod method : listener.getMethods()) {
                if (!method.isAnnotatedWith(TransactionalEventListener.class) || method.getRawParameterTypes().size() != 1) {
                    continue;
                }
                JavaClass event = method.getRawParameterTypes().get(0);
                if (!moduleOf(event).equals(listenerModule)) {
                    // Modül içi olay (ör. progress'in kendi LessonCompleted'ı) bileşen diyagramında ok değildir.
                    listened.add(moduleOf(event) + "." + event.getSimpleName() + " -> " + listenerModule);
                }
            }
        }

        assertThat(listened).as("koddaki dinleyiciler").isNotEmpty();
        assertThat(drawn).as("diyagramdaki olay okları = koddaki dinleyiciler").isEqualTo(listened);
    }

    /** {@code com.miniudemy.<modül>.…} → modül adı. */
    private static String moduleOf(JavaClass javaClass) {
        return javaClass.getPackageName().split("\\.")[2];
    }
}
