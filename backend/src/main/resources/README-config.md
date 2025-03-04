# WebETL Configuration

This directory contains configuration templates for WebETL. These templates provide a starting point for configuring the application.

## Configuration Files

- `application-secrets.properties.template`: Contains template settings for sensitive configuration like API keys and database credentials

## How to Use

1. Copy the template files without the `.template` extension:
   ```
   cp application-secrets.properties.template application-secrets.properties
   ```

2. Edit the properties files with your specific configuration:
   ```
   nano application-secrets.properties
   ```

3. Keep your configuration secure - don't commit the actual properties files to source control.

## Configuration Locations

WebETL loads configuration from multiple locations in the following order of precedence:

1. `./cfg/` directory (relative to the application's working directory)
2. User's home directory: `~/.webetl/`
3. Classpath (embedded in JAR)

This means you can override any configuration by placing a property file in one of these locations.

## First-time Setup

When you start WebETL for the first time, the application will:

1. Create the `./cfg/` directory if it doesn't exist
2. Copy template files to the appropriate locations if they don't already exist

## Sensitive Configuration

The `application-secrets.properties` file is intended for sensitive configuration such as:

- Database credentials
- API keys
- Authentication secrets

This file is intentionally excluded from the packaged application to prevent accidental leakage of credentials. 