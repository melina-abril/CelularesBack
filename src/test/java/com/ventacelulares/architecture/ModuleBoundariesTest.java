package com.ventacelulares.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ModuleBoundariesTest {
    private static final String MODULES = "com.ventacelulares.modules.";

    @Test
    void domainDoesNotDependOnSpring() {
        var classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.ventacelulares");
        noClasses().that().resideInAPackage("..modules..domain..").should()
                .dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .check(classes);
    }

    @Test
    void modulesDoNotDependOnOtherModules() {
        var classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.ventacelulares");
        for (JavaClass source : classes) {
            String sourceModule = moduleName(source.getPackageName());
            if (sourceModule == null) {
                continue;
            }
            source.getDirectDependenciesFromSelf().forEach(dependency -> {
                String targetModule = moduleName(dependency.getTargetClass().getPackageName());
                if (targetModule != null) {
                    assertEquals(sourceModule, targetModule, dependency.getDescription());
                }
            });
        }
    }

    private String moduleName(String packageName) {
        if (!packageName.startsWith(MODULES)) {
            return null;
        }
        String remainder = packageName.substring(MODULES.length());
        int separator = remainder.indexOf('.');
        return separator < 0 ? remainder : remainder.substring(0, separator);
    }
}
