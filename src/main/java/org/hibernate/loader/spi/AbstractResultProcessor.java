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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.Loadable;

/**
 * Convenience base class for various ResultSet processor implementations.  Provides centralized common functionality.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractResultProcessor {

	private final SessionFactoryImplementor sessionFactory;
	private final LoadPlan loadPlan;

	public AbstractResultProcessor(SessionFactoryImplementor sessionFactory, LoadPlan loadPlan) {
		this.sessionFactory = sessionFactory;
		this.loadPlan = loadPlan;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected LoadPlan loadPlan() {
		return loadPlan;
	}

	protected static EntityKey getOptionalObjectKey(QueryParameters queryParameters, SessionImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters.getOptionalEntityName();

		if ( optionalObject != null && optionalEntityName != null ) {
			return session.generateEntityKey( optionalId, session.getEntityPersister( optionalEntityName, optionalObject ) );
		}
		else {
			return null;
		}
	}

	protected SubSelectFetchHandler buildSubSelectFetchHandler() {
		return new StandardSubSelectFetchHandler( this );
	}

	protected void createSubselects(List<EntityKey[]> keys, QueryParameters queryParameters, SessionImplementor session) {
		if ( keys.size() <= 1 ) {
			// if we only returned one entity, query by key is more efficient
			return;
		}

	}

	protected int[] getNamedParameterLocs(String name) {
		throw new AssertionFailure( "no named parameters" );
	}


	/**
	 * Comes into play during processing rows of a ResultSet to handle spawning any needed "subselect" fetches.
	 */
	protected static interface SubSelectFetchHandler {
		public void addHydratedEntityKeys(EntityKey[] entityKeys);
		public void buildSubSelectFetches(SessionImplementor session, QueryParameters queryParameters);
	}

	protected static class StandardSubSelectFetchHandler implements SubSelectFetchHandler {
		private final AbstractResultProcessor resultProcessor;
		private List<EntityKey[]> subSelectResultKeys;

		public StandardSubSelectFetchHandler(AbstractResultProcessor resultProcessor) {
			this.resultProcessor = resultProcessor;
		}

		@Override
		public void addHydratedEntityKeys(EntityKey[] entityKeys) {
			if ( subSelectResultKeys == null ) {
				subSelectResultKeys = new ArrayList<EntityKey[]>();
			}
			EntityKey[] copy = new EntityKey[ entityKeys.length ];
			System.arraycopy( entityKeys, 0, copy, 0, entityKeys.length );
			subSelectResultKeys.add( copy );
		}

		@Override
		public void buildSubSelectFetches(SessionImplementor session, QueryParameters queryParameters) {
			if ( subSelectResultKeys == null || subSelectResultKeys.size() <= 1 ) {
				return;
			}

			final Map namedParameterLocMap = buildNamedParameterLocMap( queryParameters );

			final EntityReturn[] entityReturns = resultProcessor.loadPlan().collectEntityReturns();
			final Set<EntityKey>[] transposedKeySets = groupKeysByEntityTypeRelativeToReturns( subSelectResultKeys );

			if ( entityReturns.length != transposedKeySets.length ) {
				throw new AssertionFailure( "EntityReturn array and EntityKey-set array did not have same lengths" );
			}

			// Iterate primarily on the "transposed" grouping of keys by "entity return".  This allows us to
			// create a single SubselectFetch instance for registration against all the related keys
			for ( int i = 0; i < transposedKeySets.length; i++ ) {
				final Set<EntityKey> transposedKeySet = transposedKeySets[i];
				if ( ! entityReturns[i].getEntityPersister().hasSubselectLoadableCollections() ) {
					continue;
				}

				final SubselectFetch subselectFetch = new SubselectFetch(
						//getSQLString(),
						entityReturns[i].getSqlTableAlias(),
						(Loadable) entityReturns[i].getEntityPersister(),
						queryParameters,
						transposedKeySet,
						namedParameterLocMap
				);

				for ( EntityKey key : transposedKeySet ) {
					if ( key == null ) {
						continue;
					}

					session.getPersistenceContext().getBatchFetchQueue().addSubselect( key, subselectFetch );
				}
			}
		}

		/**
		 * The incoming collection of keys essentially looks a lot like a result set.  The keys grouped primarily
		 * by "rows" as found in the original result set, and secondarily by (mostly) entity type .
		 *
		 * This method instead groups them primarily by entity type.  The resulting arrays each contain a Set whose
		 * individual EntityKeys are all related to the same entity type.
		 *
		 * For example, we have something like this incoming:<ol>
		 *     <li>
		 *         <ol>
		 *             <li>Person:1</li>
		 *             <li>Company:10</li>
		 *         </ol>
		 *     </li>
		 *     <li>
		 *         <ol>
		 *             <li>Person:2</li>
		 *             <li>Company:20</li>
		 *         </ol>
		 *     </li>
		 * </ol>
		 *
		 * The method transposes these to result in:<ol>
		 *     <li>
		 *         <ol>
		 *             <li>Person:1</li>
		 *             <li>Person:2</li>
		 *         </ol>
		 *     </li>
		 *     <li>
		 *         <ol>
		 *             <li>Company:10</li>
		 *             <li>Company:20</li>
		 *         </ol>
		 *     </li>
		 * </ol>
		 *
		 * @param keys The incoming collection of EntityKey arrays
		 *
		 * @return The keys, grouped primarily by entity type
		 */
		@SuppressWarnings("unchecked")
		private static Set<EntityKey>[] groupKeysByEntityTypeRelativeToReturns(List<EntityKey[]> keys) {
			final int length = keys.get(0).length;	// assumption: they are all the same length, so we just use the first
			final Set<EntityKey>[] result = new Set[length];
			for ( int j = 0; j < length; j++ ) {
				result[j] = new HashSet<EntityKey>( keys.size() );
				for ( EntityKey[] subKeys : keys ) {
					result[j].add( subKeys[j] );
				}
			}
			return result;
		}

		private Map<String, int[]> buildNamedParameterLocMap(QueryParameters queryParameters) {
			if ( queryParameters.getNamedParameters() == null ) {
				return null;
			}

			final Map<String, int[]> namedParameterLocMap = new HashMap<String, int[]>();
			for ( String name : queryParameters.getNamedParameters().keySet() ) {
				namedParameterLocMap.put( name, resultProcessor.getNamedParameterLocs( name ) );
			}
			return namedParameterLocMap;
		}
	}

	protected static class NoOpSubSelectFetchHandler implements SubSelectFetchHandler {
		@Override
		public void addHydratedEntityKeys(EntityKey[] entityKeys) {
			// do nothing
		}

		@Override
		public void buildSubSelectFetches(SessionImplementor session, QueryParameters queryParameters) {
			// do nothing
		}
	}
}
