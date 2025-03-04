package io.webetl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Configuration class that loads properties from external locations.
 * This provides a way to override application properties without modifying the JAR.
 */
@Configuration
@PropertySources({
    // Load from the application directory first (takes precedence)
    @PropertySource(value = "file:./cfg/application.properties", ignoreResourceNotFound = true),
    @PropertySource(value = "file:./cfg/application-secrets.properties", ignoreResourceNotFound = true),
    
    // Then from the user's home directory
    @PropertySource(value = "file:${user.home}/.webetl/application.properties", ignoreResourceNotFound = true),
    @PropertySource(value = "file:${user.home}/.webetl/application-secrets.properties", ignoreResourceNotFound = true)
})
public class ExternalConfigLoader implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(ExternalConfigLoader.class);
    
    @Value("${data.dir:#{systemProperties['user.home']+'/.webetl'}}")
    private String dataDir;
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // Ensure configuration directories exist
        ensureConfigDirectories();
        
        // Check for template files that need to be copied
        copyTemplateIfNeeded("./cfg/application-secrets.properties.template", "./cfg/application-secrets.properties");
        copyTemplateIfNeeded("./cfg/application-secrets.properties.template", Paths.get(dataDir, "application-secrets.properties").toString());
    }
    
    private void ensureConfigDirectories() {
        // Create local cfg directory if it doesn't exist
        Path cfgDir = Paths.get("./cfg");
        if (!Files.exists(cfgDir)) {
            try {
                Files.createDirectories(cfgDir);
                log.info("Created configuration directory: {}", cfgDir.toAbsolutePath());
            } catch (Exception e) {
                log.warn("Could not create configuration directory: {}", cfgDir.toAbsolutePath(), e);
            }
        }
        
        // Create data directory if it doesn't exist
        Path dataDirPath = Paths.get(dataDir);
        if (!Files.exists(dataDirPath)) {
            try {
                Files.createDirectories(dataDirPath);
                log.info("Created data directory: {}", dataDirPath.toAbsolutePath());
            } catch (Exception e) {
                log.warn("Could not create data directory: {}", dataDirPath.toAbsolutePath(), e);
            }
        }
    }
    
    private void copyTemplateIfNeeded(String templatePath, String targetPath) {
        Path template = Paths.get(templatePath);
        Path target = Paths.get(targetPath);
        
        // Only copy if target doesn't exist but template does
        if (!Files.exists(target) && Files.exists(template)) {
            try {
                Files.copy(template, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Created configuration file from template: {}", target.toAbsolutePath());
            } catch (Exception e) {
                log.warn("Could not copy template to {}: {}", target.toAbsolutePath(), e.getMessage());
            }
        }
    }
} 