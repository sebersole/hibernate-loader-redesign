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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
*
* NOTE : most of this logic taken from {@link org.hibernate.loader.Loader#doQuery}, but only the portion about actually
* processing the ResultSet.  Also, the logic is changed up a bit to use the notion of {@link Return} instead of
* directly relying on the old Loader contract; that should let this be used in many more situations without hackery.
*
* @author Steve Ebersole
* @author Gavin King
*/
public class BasicResultSetProcessor extends AbstractResultProcessor implements ResultSetProcessor {
	private static final Logger log = Logger.getLogger( BasicResultSetProcessor.class );

	public BasicResultSetProcessor(SessionFactoryImplementor sessionFactory, LoadPlan loadPlan) {
		super( sessionFactory, loadPlan );
	}

	@Override
	public List extractResults(
			ResultSet resultSet,
			SessionImplementor session,
			QueryParameters queryParameters,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer) throws SQLException {

		final RowSelection selection = queryParameters.getRowSelection();
		final int maxRows = selection != null && selection.getMaxRows() != null && selection.getMaxRows() > 0
				? selection.getMaxRows()
				: Integer.MAX_VALUE;

		final SubSelectFetchHandler subSelectFetchHandler = buildSubSelectFetchHandler();
		final int entitySpan = loadPlan().collectEntityReturns().length;
		final ArrayList hydratedObjects = entitySpan > 0 ? new ArrayList() : null;
		final EntityKey optionalObjectKey = getOptionalObjectKey( queryParameters, session );
		final List results = new ArrayList();

		handleEmptyCollections( queryParameters.getCollectionKeys(), resultSet, session );

		log.trace( "Processing result set" );
		EntityKey[] keys = new EntityKey[entitySpan]; //we can reuse it for each row
		int count;
		for ( count = 0; count < maxRows && resultSet.next(); count++ ) {
			log.debugf( "Result set row: %s", count );
			Object result = getRowFromResultSet(
					resultSet,
					session,
					queryParameters,
					optionalObjectKey,
					hydratedObjects,
					keys,
					returnProxies,
					forcedResultTransformer
			);
			results.add( result );
			subSelectFetchHandler.addHydratedEntityKeys( keys );
		}

		log.tracev( "Done processing result set ({0} rows)", count );

		initializeEntitiesAndCollections( hydratedObjects, resultSet, session, queryParameters.isReadOnly( session ) );
		subSelectFetchHandler.buildSubSelectFetches( session, queryParameters );

		return results;
	}

	/**
	 * If this is a collection initializer, we need to tell the session that a collection
	 * is being initialized, to account for the possibility of the collection having
	 * no elements (hence no rows in the result set).
	 */
	private void handleEmptyCollections(
			final Serializable[] keys,
			final Object resultSetId,
			final SessionImplementor session) {

		if ( keys == null ) {
			// this is not a collection initializer (and empty collections will be detected by looking
			// for the owner's identifier in the result set)
			return;
		}

		// Otherwise. this is a collection initializer, so we must create a collection for each of the
		// passed-in keys, to account for the possibility that the collection is empty and has no rows
		// in the result set

		for ( CollectionReturn collectionReturn : loadPlan().collectCollectionReturns() ) {
			for ( Serializable key : keys ) {
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Result set contains (possibly empty) collection: %s",
							MessageHelper.collectionInfoString(
									collectionReturn.getCollectionPersister(),
									key,
									sessionFactory()
							)
					);
				}

				session.getPersistenceContext()
						.getLoadContexts()
						.getCollectionLoadContext( (ResultSet) resultSetId )
						.getLoadingCollection( collectionReturn.getCollectionPersister(), key );
			}
		}
	}

	private Object getRowFromResultSet(
			final ResultSet resultSet,
			final SessionImplementor session,
			final QueryParameters queryParameters,
			final EntityKey optionalObjectKey,
			final List hydratedObjects,
			final EntityKey[] keys,
			boolean returnProxies,
			ResultTransformer forcedResultTransformer) throws SQLException {

		extractKeysFromResultSet( queryParameters, resultSet, session, keys, hydratedObjects );

		registerNonExists( keys, session );

		// this call is side-effecty
		Object[] row = getRow(
				resultSet,
				keys,
				queryParameters.getOptionalObject(),
				optionalObjectKey,
				hydratedObjects,
				session
		);

		readCollectionElements( row, resultSet, session );

		if ( returnProxies ) {
			// now get an existing proxy for each row element (if there is one)
			int i = -1;
			for ( EntityReturn entityReturn : loadPlan().collectEntityReturns() ) {
				i++;
				final Object entity = row[i];
				final Object proxy = session.getPersistenceContext().proxyFor( entityReturn.getEntityPersister(), keys[i], entity );
				if ( entity != proxy ) {
					// force the proxy to resolve itself and set the proxy into the return row
					( (HibernateProxy) proxy ).getHibernateLazyInitializer().setImplementation( entity );
					row[i] = proxy;
				}
			}
		}

		return forcedResultTransformer == null
				? getResultColumnOrRow( row, queryParameters.getResultTransformer(), resultSet, session )
				: forcedResultTransformer.transformTuple( getResultRow( row, resultSet, session ), getResultRowAliases() );
	}

	protected void extractKeysFromResultSet(
			QueryParameters queryParameters,
			ResultSet resultSet,
			SessionImplementor session,
			EntityKey[] keys,
			List hydratedObjects) throws SQLException {

		final Serializable optionalId = queryParameters.getOptionalId();

		final int entitySpan = loadPlan().collectEntityReturns().length;

		final int numberOfPersistersToProcess;
		if ( isSingleRowLoader() && optionalId != null ) {
			keys[ entitySpan - 1 ] = session.generateEntityKey( optionalId, persisters[ entitySpan - 1 ] );
			// skip the last persister below...
			numberOfPersistersToProcess = entitySpan - 1;
		}
		else {
			numberOfPersistersToProcess = entitySpan;
		}

		final Object[] hydratedKeyState = new Object[numberOfPersistersToProcess];
		for ( int i = 0; i < numberOfPersistersToProcess; i++ ) {
			final EntityReturn entityReturn = loadPlan().collectEntityReturns()[i];
			final Type idType = entityReturn.getEntityPersister().getIdentifierType();
			hydratedKeyState[i] = idType.hydrate( resultSet, entityReturn.getEntityAliases().getSuffixedKeyAliases(), session, null );
		}

		for ( int i = 0; i < numberOfPersistersToProcess; i++ ) {
			final EntityReturn entityReturn = loadPlan().collectEntityReturns()[i];
			final Type idType = entityReturn.getEntityPersister().getIdentifierType();
			if ( idType.isComponentType() && getCompositeKeyManyToOneTargetIndices() != null ) {
				// we may need to force resolve any key-many-to-one(s)
				int[] keyManyToOneTargetIndices = getCompositeKeyManyToOneTargetIndices()[i];
				// todo : better solution is to order the index processing based on target indices
				//		that would account for multiple levels whereas this scheme does not
				if ( keyManyToOneTargetIndices != null ) {
					for ( int targetIndex : keyManyToOneTargetIndices ) {
						final EntityReturn targetEntityReturn = loadPlan().collectEntityReturns()[targetIndex];
						if ( targetIndex < numberOfPersistersToProcess ) {
							final Type targetIdType = targetEntityReturn.getEntityPersister().getIdentifierType();
							final Serializable targetId = (Serializable) targetIdType.resolve(
									hydratedKeyState[targetIndex],
									session,
									null
							);
							// todo : need a way to signal that this key is resolved and its data resolved
							keys[targetIndex] = session.generateEntityKey( targetId, targetEntityReturn.getEntityPersister() );
						}

						// this part copied from #getRow, this section could be refactored out
						Object object = session.getEntityUsingInterceptor( keys[targetIndex] );
						if ( object != null ) {
							//its already loaded so don't need to hydrate it
							instanceAlreadyLoaded(
									resultSet,
									targetIndex,
									targetEntityReturn,
									keys[targetIndex],
									object,
									session
							);
						}
						else {
							instanceNotYetLoaded(
									resultSet,
									targetIndex,
									targetEntityReturn,
									keys[targetIndex],
									getOptionalObjectKey( queryParameters, session ),
									queryParameters.getOptionalObject(),
									hydratedObjects,
									session
							);
						}
					}
				}
			}
			final Serializable resolvedId = (Serializable) idType.resolve( hydratedKeyState[i], session, null );
			keys[i] = resolvedId == null ? null : session.generateEntityKey( resolvedId, entityReturn.getEntityPersister() );
		}
	}

	private Object[] getRow(
			final ResultSet rs,
			final EntityKey[] keys,
			final Object optionalObject,
			final EntityKey optionalObjectKey,
			final List hydratedObjects,
			final SessionImplementor session)
			throws HibernateException, SQLException {

		if ( log.isDebugEnabled() ) {
			log.debugf( "Result row: %s", StringHelper.toString( keys ) );
		}

		final int cols = loadPlan().collectEntityReturns().length;
		final Object[] rowResults = new Object[cols];

		for ( int i = 0; i < cols; i++ ) {
			final EntityReturn entityReturn = loadPlan().collectEntityReturns()[i];
			final EntityKey key = keys[i];
			Object object = null;

			if ( keys[i] == null ) {
				// do nothing
			}
			else {
				// If the object is already loaded, return the loaded one
				object = session.getEntityUsingInterceptor( key );
				if ( object != null ) {
					// its already loaded so don't need to hydrate it
					instanceAlreadyLoaded(
							rs,
							i,
							entityReturn,
							key,
							object,
							session
					);
				}
				else {
					object = instanceNotYetLoaded(
							rs,
							i,
							entityReturn,
							key,
							optionalObjectKey,
							optionalObject,
							hydratedObjects,
							session
					);
				}
			}

			rowResults[i] = object;
		}

		return rowResults;
	}

	/**
	 * Interpretation of {@link org.hibernate.loader.Loader#registerNonExists}
	 */
	private void registerNonExists(final EntityKey[] keys, final SessionImplementor session) {

		ReturnVisitor.visit(
				loadPlan().getRootReturns(),
				new ReturnVisitationStrategyAdapter() {
					@Override
					public void handleFetchedEntityReturn(FetchedEntityReturn fetchedEntityReturn) {
						// todo : for this to work we need to be able to resolve the "index" of the owner return.
						// knowing the owner is easy as the FetchReturns expose access to that information.
						// but this is another bridge point between the old and new.  2 options:
						//		1) Returns keep an index; still problematic since entity/collections are split
						//		2) Model the notion of "ResultSetRowProcessingState" and link Returns with Results (keys, entities, proxies, etc).
						super.handleFetchedEntityReturn( fetchedEntityReturn );
					}
				}
		);
		final int[] owners = getOwners();
		if ( owners != null ) {

			EntityType[] ownerAssociationTypes = getOwnerAssociationTypes();
			for ( int i = 0; i < keys.length; i++ ) {

				int owner = owners[i];
				if ( owner > -1 ) {
					EntityKey ownerKey = keys[owner];
					if ( keys[i] == null && ownerKey != null ) {

						final PersistenceContext persistenceContext = session.getPersistenceContext();

						/*final boolean isPrimaryKey;
						final boolean isSpecialOneToOne;
						if ( ownerAssociationTypes == null || ownerAssociationTypes[i] == null ) {
							isPrimaryKey = true;
							isSpecialOneToOne = false;
						}
						else {
							isPrimaryKey = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName()==null;
							isSpecialOneToOne = ownerAssociationTypes[i].getLHSPropertyName()!=null;
						}*/

						//TODO: can we *always* use the "null property" approach for everything?
						/*if ( isPrimaryKey && !isSpecialOneToOne ) {
							persistenceContext.addNonExistantEntityKey(
									new EntityKey( ownerKey.getIdentifier(), persisters[i], session.getEntityMode() )
							);
						}
						else if ( isSpecialOneToOne ) {*/
						boolean isOneToOneAssociation = ownerAssociationTypes!=null &&
								ownerAssociationTypes[i]!=null &&
								ownerAssociationTypes[i].isOneToOne();
						if ( isOneToOneAssociation ) {
							persistenceContext.addNullProperty( ownerKey,
																ownerAssociationTypes[i].getPropertyName() );
						}
						/*}
						else {
							persistenceContext.addNonExistantEntityUniqueKey( new EntityUniqueKey(
									persisters[i].getEntityName(),
									ownerAssociationTypes[i].getRHSUniqueKeyPropertyName(),
									ownerKey.getIdentifier(),
									persisters[owner].getIdentifierType(),
									session.getEntityMode()
							) );
						}*/
					}
				}
			}
		}
	}

	/**
	 * Read any collection elements contained in a single row of the result set
	 */
	private void readCollectionElements(Object[] row, ResultSet resultSet, SessionImplementor session)
			throws SQLException, HibernateException {

		//TODO: make this handle multiple collection roles!

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if ( collectionPersisters != null ) {

			final CollectionAliases[] descriptors = getCollectionAliases();
			final int[] collectionOwners = getCollectionOwners();

			for ( int i=0; i<collectionPersisters.length; i++ ) {

				final boolean hasCollectionOwners = collectionOwners !=null &&
						collectionOwners[i] > -1;
				//true if this is a query and we are loading multiple instances of the same collection role
				//otherwise this is a CollectionInitializer and we are loading up a single collection or batch

				final Object owner = hasCollectionOwners ?
						row[ collectionOwners[i] ] :
						null; //if null, owner will be retrieved from session

				final CollectionPersister collectionPersister = collectionPersisters[i];
				final Serializable key;
				if ( owner == null ) {
					key = null;
				}
				else {
					key = collectionPersister.getCollectionType().getKeyOfOwner( owner, session );
					//TODO: old version did not require hashmap lookup:
					//keys[collectionOwner].getIdentifier()
				}

				readCollectionElement(
						owner,
						key,
						collectionPersister,
						descriptors[i],
						resultSet,
						session
				);

			}

		}
	}

	private void initializeEntitiesAndCollections(
			final List hydratedObjects,
			final Object resultSetId,
			final SessionImplementor session,
			final boolean readOnly)
			throws HibernateException {

		final CollectionPersister[] collectionPersisters = loadPlan().collectCollectionPersistersForLoading();
		if ( collectionPersisters != null ) {
			for ( CollectionPersister collectionPersister : collectionPersisters ) {
				if ( collectionPersister.isArray() ) {
					//for arrays, we should end the collection load before resolving
					//the entities, since the actual array instances are not instantiated
					//during loading
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					session.getPersistenceContext()
							.getLoadContexts()
							.getCollectionLoadContext( ( ResultSet ) resultSetId )
							.endLoadingCollections( collectionPersister );
				}
			}
		}

		//important: reuse the same event instances for performance!
		final PreLoadEvent pre;
		final PostLoadEvent post;
		if ( session.isEventSource() ) {
			pre = new PreLoadEvent( (EventSource) session );
			post = new PostLoadEvent( (EventSource) session );
		}
		else {
			pre = null;
			post = null;
		}

		if ( hydratedObjects!=null ) {
			int hydratedObjectsSize = hydratedObjects.size();
			log.tracev( "Total objects hydrated: {0}", hydratedObjectsSize );
			for ( Object hydratedObject : hydratedObjects ) {
				TwoPhaseLoad.initializeEntity( hydratedObject, readOnly, session, pre, post );
			}
		}

		if ( collectionPersisters != null ) {
			for ( CollectionPersister collectionPersister : collectionPersisters ) {
				if ( !collectionPersister.isArray() ) {
					//for sets, we should end the collection load after resolving
					//the entities, since we might call hashCode() on the elements
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					session.getPersistenceContext()
							.getLoadContexts()
							.getCollectionLoadContext( ( ResultSet ) resultSetId )
							.endLoadingCollections( collectionPersister );
				}
			}
		}
	}
}
