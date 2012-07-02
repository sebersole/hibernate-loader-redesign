package org.hibernate.loader.spi;

/**
 * This package is "experimental".  It is currently nothing more than an initial proof-of-concept of a way
 * I want to explore for possible redesigning Loaders into pieces that individually know how to:<ul>
 *     <li>Building a {@link java.sql.Statement}</li>
 *     <li>Executing a {@link java.sql.Statement}</li>
 *     <li>Processing any {@link java.sql.ResultSet} objects resulting from {@link java.sql.Statement} execution</li>
 * </ul>
 */