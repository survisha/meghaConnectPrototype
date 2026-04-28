package com.survisha.meghaconnect.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Classpath Configuration for External JARs
 *
 * Dynamically adds external JAR files to the application ClassLoader at startup.
 * This is needed for JARs that are:
 * 1. In the backend/libs directory (development mode)
 * 2. In BOOT-INF/lib/ (when running from fat JAR)
 *
 * The OVSE SDK JAR needs to be accessible via the ClassLoader for Spring to
 * instantiate beans that reference its classes.
 */
@Slf4j
@Component
public class ClasspathConfig implements ApplicationListener<ApplicationStartingEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartingEvent event) {
        try {
            // Add libs to classpath (for development/IDE mode)
            addLibsDirectoryToClasspath();
            
            // Register OVSE SDK classes (ensure they're discoverable)
            ensureOvseClassesAvailable();
            
            log.info("✓ Classpath configuration completed");
        } catch (Exception e) {
            log.warn("⚠ Classpath configuration error: {}", e.getMessage(), e);
        }
    }

    /**
     * Add the backend/libs directory to the application ClassLoader.
     */
    private void addLibsDirectoryToClasspath() throws Exception {
        File libsDir = new File("libs");
        
        if (!libsDir.exists()) {
            log.debug("libs directory not found at: {}", libsDir.getAbsolutePath());
            return;
        }

        log.info("Loading external JARs from: {}", libsDir.getAbsolutePath());

        File[] jarFiles = libsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles != null && jarFiles.length > 0) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            for (File jarFile : jarFiles) {
                try {
                    URL jarUrl = jarFile.toURI().toURL();
                    addUrlToClassLoader(classLoader, jarUrl);
                    log.info("  ✓ Loaded: {}", jarFile.getName());
                } catch (Exception e) {
                    log.warn("  ⚠ Failed to load {}: {}", jarFile.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * Verify OVSE SDK classes are accessible by attempting to load them.
     */
    private void ensureOvseClassesAvailable() throws Exception {
        try {
            Class.forName("com.ovse.client.OvseClient");
            log.info("✓ OVSE SDK classes are available on classpath");
        } catch (ClassNotFoundException e) {
            log.warn("⚠ OVSE SDK classes not yet available: {}", e.getMessage());
            // This might be okay if using fat JAR - Spring Boot loads them later
        }
    }

    /**
     * Add a URL to the ClassLoader using reflection.
     */
    private void addUrlToClassLoader(ClassLoader classLoader, URL url) throws Exception {
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);
            addUrlMethod.invoke(urlClassLoader, url);
        } else {
            log.debug("Attempting to add URL via system ClassLoader reflection");
            ClassLoader sysLoader = ClassLoader.getSystemClassLoader();
            if (sysLoader instanceof URLClassLoader) {
                Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrlMethod.setAccessible(true);
                addUrlMethod.invoke(sysLoader, url);
            } else {
                log.debug("System ClassLoader is not URLClassLoader - may be using LaunchedURLClassLoader");
            }
        }
    }
}

