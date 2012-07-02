/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.transform.ResultTransformer;

/**
 * Contract for processing JDBC ResultSets.
 *
 * NOTE : these methods initially taken directly from {@link org.hibernate.loader.Loader} counterparts in an effort
 * to break Loader into manageable pieces, especially in regards to the processing of result sets.
 *
 * @author Steve Ebersole
 */
public interface ResultSetProcessor {

	/**
	 * Process an entire ResultSet, performing all extractions.
	 *
	 * Semi-copy of {@link org.hibernate.loader.Loader#doQuery}, with focus on just the ResultSet processing bit.
	 *
	 * @param resultSet The result set being processed.
	 * @param session The originating session
	 * @param queryParameters The "parameters" used to build the query
	 * @param returnProxies Can proxies be returned (not the same as can they be created!)
	 * @param forcedResultTransformer My old "friend" ResultTransformer...
	 *
	 * @return The extracted results list.
	 *
	 * @throws SQLException Indicates a problem access the JDBC ResultSet
	 */
	public List extractResults(
			ResultSet resultSet,
			SessionImplementor session,
			QueryParameters queryParameters,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer) throws SQLException;
}
