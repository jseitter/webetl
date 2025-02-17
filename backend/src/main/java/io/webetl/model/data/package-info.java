/**
 * Contains the core data model classes for WebETL data processing.
 * 
 * <p>This package provides the fundamental data structures used throughout the ETL process:</p>
 * 
 * <ul>
 *   <li>{@link io.webetl.model.data.Row} - Represents a single row of data with typed values</li>
 *   <li>{@link io.webetl.model.data.Schema} - Defines the structure and validation rules for rows</li>
 *   <li>{@link io.webetl.model.data.ColumnDefinition} - Specifies the properties of a single column</li>
 *   <li>{@link io.webetl.model.data.DataType} - Enumerates supported data types with validation</li>
 *   <li>{@link io.webetl.model.data.RowMetadata} - Contains metadata about row origin and processing</li>
 * </ul>
 * 
 * <p>The classes in this package enforce type safety and data validation throughout the ETL pipeline,
 * ensuring data consistency from source to destination.</p>
 * 
 * @since 1.0
 */
package io.webetl.model.data; 