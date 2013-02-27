/*
 * jDocBook, processing of DocBook sources
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.impl;

import java.util.ArrayDeque;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.DefaultEntityAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.FetchPlan;
import org.hibernate.loader.GeneratedCollectionAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.AbstractFetchOwner;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.LoadPlanBuilderStrategy;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.walking.spi.AssociationAttributeDefinition;
import org.hibernate.loader.walking.spi.AttributeDefinition;
import org.hibernate.loader.walking.spi.CollectionDefinition;
import org.hibernate.loader.walking.spi.CompositeDefinition;
import org.hibernate.loader.walking.spi.EntityDefinition;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * LoadPlanBuilderStrategy implementation used for processing RootEntity LoadPlan building.
 *
 * Really this is a single-root LoadPlan building strategy for building LoadPlans for:<ul>
 *     <li>entity load plans</li>
 *     <li>cascade load plans</li>
 *     <li>collection initializer plans</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class RootEntityLoadPlanBuilderStrategy implements LoadPlanBuilderStrategy {
	private final SessionFactoryImplementor sessionFactory;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final String rootAlias;
	private int currentSuffixBase;

	private Return rootReturn;

	private ArrayDeque<FetchOwner> fetchOwnerStack = new ArrayDeque<FetchOwner>();
//
//	private int currentDepth = 0;
//	private Object previousReturnOrFetch;

	private PropertyPath propertyPath = new PropertyPath( "" );

	public RootEntityLoadPlanBuilderStrategy(
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers,
			String rootAlias,
			int suffixSeed) {
		this.sessionFactory = sessionFactory;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.rootAlias = rootAlias;
		this.currentSuffixBase = suffixSeed;
	}

	@Override
	public void start() {
		// nothing to do
	}

	@Override
	public void finish() {
		// nothing to do
	}

	@Override
	public void startingEntity(EntityDefinition entityDefinition) {
		if ( rootReturn == null ) {
			// this is a root...
			final EntityReturn entityReturn = buildRootEntityReturn( entityDefinition );
			rootReturn = entityReturn;
			fetchOwnerStack.push( entityReturn );
		}
		// otherwise this call should represent a fetch which should have been handled in #startingAttribute
	}

	@Override
	public void finishingEntity(EntityDefinition entityDefinition) {
		// nothing to do
	}

	@Override
	public void startingCollection(CollectionDefinition collectionDefinition) {
		if ( rootReturn == null ) {
			// this is a root...
			final CollectionReturn collectionReturn = buildRootCollectionReturn( collectionDefinition );
			rootReturn = collectionReturn;
			fetchOwnerStack.push( collectionReturn );
		}
	}

	@Override
	public void finishingCollection(CollectionDefinition collectionDefinition) {
		// nothing to do
	}

	@Override
	public void startingComposite(CompositeDefinition compositeDefinition) {
		if ( rootReturn == null ) {
			throw new HibernateException( "A component cannot be the root of a walk nor a graph" );
		}
	}

	@Override
	public void finishingComposite(CompositeDefinition compositeDefinition) {
		// nothing to do
	}

	@Override
	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
		final Type attributeType = attributeDefinition.getType();

		final boolean isComponentType = attributeType.isComponentType();
		final boolean isBasicType = ! ( isComponentType || attributeType.isAssociationType() );

		if ( isBasicType ) {
			return true;
		}
		else if ( isComponentType ) {
			propertyPath = propertyPath.append( attributeDefinition.getName() );
			try {
				return handleCompositeAttribute( (CompositeDefinition) attributeDefinition );
			}
			finally {
				propertyPath = propertyPath.getParent();
			}
		}
		else {
			propertyPath = propertyPath.append( attributeDefinition.getName() );
			try {
				return handleAssociationAttribute( (AssociationAttributeDefinition) attributeDefinition );
			}
			finally {
				propertyPath = propertyPath.getParent();
			}
		}
	}


	@Override
	public void finishingAttribute(AttributeDefinition attributeDefinition) {
		final Type attributeType = attributeDefinition.getType();

		final boolean isComponentType = attributeType.isComponentType();
		final boolean isBasicType = ! ( isComponentType || attributeType.isAssociationType() );

		if ( ! isBasicType ) {
			fetchOwnerStack.removeLast();
		}
	}

	protected boolean handleCompositeAttribute(CompositeDefinition attributeDefinition) {
		final FetchOwner fetchOwner = fetchOwnerStack.peekLast();
		final CompositeFetch fetch = buildCompositeFetch( fetchOwner, attributeDefinition );
		fetchOwnerStack.addLast( fetch );
		return true;
	}

	protected boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
		final FetchPlan fetchPlan = determineFetchPlan( attributeDefinition );
		if ( fetchPlan.getTiming() != FetchTiming.IMMEDIATE ) {
			return false;
		}

		final FetchOwner fetchOwner = fetchOwnerStack.peekLast();
		fetchOwner.validateFetchPlan( fetchPlan );

		final Fetch associationFetch;
		if ( attributeDefinition.isCollection() ) {
			associationFetch = buildCollectionFetch( fetchOwner, attributeDefinition, fetchPlan );
		}
		else {
			associationFetch = buildEntityFetch( fetchOwner, attributeDefinition, fetchPlan );
		}
		fetchOwnerStack.addLast( associationFetch );

		return true;
	}

	protected FetchPlan determineFetchPlan(AssociationAttributeDefinition attributeDefinition) {
		FetchPlan fetchPlan = attributeDefinition.determineFetchPlan( loadQueryInfluencers, propertyPath );
		if ( fetchPlan.getTiming() == FetchTiming.IMMEDIATE && fetchPlan.getStyle() == FetchStyle.JOIN ) {
			// see if we need to alter the join fetch to another form for any reason
			fetchPlan = adjustJoinFetchIfNeeded( attributeDefinition, fetchPlan );
		}
		return fetchPlan;
	}

	private FetchPlan adjustJoinFetchIfNeeded(
			AssociationAttributeDefinition attributeDefinition,
			FetchPlan fetchPlan) {
		if ( currentDepth() > sessionFactory.getSettings().getMaximumFetchDepth() ) {
			return new FetchPlan( fetchPlan.getTiming(), FetchStyle.SELECT );
		}

		if ( attributeDefinition.getType().isCollectionType() && isTooManyCollections() ) {
			// todo : have this revert to batch or subselect fetching once "sql gen redesign" is in place
			return new FetchPlan( fetchPlan.getTiming(), FetchStyle.SELECT );
		}

		return fetchPlan;
	}

	protected int currentDepth() {
		return fetchOwnerStack.size();
	}

	protected boolean isTooManyCollections() {
		return false;
	}

	@Override
	public LoadPlan buildLoadPlan() {
		return new LoadPlanImpl( false, rootReturn );
	}

	protected EntityReturn buildRootEntityReturn(EntityDefinition entityDefinition) {
		final String entityName = entityDefinition.getEntityPersister().getEntityName();
		return new EntityReturn(
				sessionFactory,
				rootAlias,
				LockMode.NONE, // todo : for now
				entityName,
				StringHelper.generateAlias( StringHelper.unqualifyEntityName( entityName ), currentDepth() ),
				new DefaultEntityAliases(
						(Loadable) entityDefinition.getEntityPersister(),
						Integer.toString( currentSuffixBase++ ) + '_'
				)
		);
	}

	protected CollectionReturn buildRootCollectionReturn(CollectionDefinition collectionDefinition) {
		final CollectionPersister persister = collectionDefinition.getCollectionPersister();
		final String collectionRole = persister.getRole();

		final CollectionAliases collectionAliases = new GeneratedCollectionAliases(
				collectionDefinition.getCollectionPersister(),
				Integer.toString( currentSuffixBase++ ) + '_'
		);
		final Type elementType = collectionDefinition.getCollectionPersister().getElementType();
		final EntityAliases elementAliases;
		if ( elementType.isEntityType() ) {
			final EntityType entityElementType = (EntityType) elementType;
			elementAliases = new DefaultEntityAliases(
					(Loadable) entityElementType.getAssociatedJoinable( sessionFactory ),
					Integer.toString( currentSuffixBase++ ) + '_'
			);
		}
		else {
			elementAliases = null;
		}

		return new CollectionReturn(
				sessionFactory,
				rootAlias,
				LockMode.NONE, // todo : for now
				persister.getOwnerEntityPersister().getEntityName(),
				StringHelper.unqualify( collectionRole ),
				collectionAliases,
				elementAliases
		);
	}

	protected CollectionFetch buildCollectionFetch(
			FetchOwner fetchOwner,
			AssociationAttributeDefinition attributeDefinition,
			FetchPlan fetchPlan) {
		final CollectionDefinition collectionDefinition = attributeDefinition.toCollectionDefinition();
		final CollectionAliases collectionAliases = new GeneratedCollectionAliases(
				collectionDefinition.getCollectionPersister(),
				Integer.toString( currentSuffixBase++ ) + '_'
		);
		final Type elementType = collectionDefinition.getCollectionPersister().getElementType();
		final EntityAliases elementAliases;
		if ( elementType.isEntityType() ) {
			final EntityType entityElementType = (EntityType) elementType;
			elementAliases = new DefaultEntityAliases(
					(Loadable) entityElementType.getAssociatedJoinable( sessionFactory ),
					Integer.toString( currentSuffixBase++ ) + '_'
			);
		}
		else {
			elementAliases = null;
		}

		return new CollectionFetch(
				sessionFactory,
				createImplicitAlias(),
				LockMode.NONE, // todo : for now
				(AbstractFetchOwner) fetchOwner,
				fetchPlan,
				attributeDefinition.getName(),
				collectionAliases,
				elementAliases
		);
	}

	protected EntityFetch buildEntityFetch(
			FetchOwner fetchOwner,
			AssociationAttributeDefinition attributeDefinition,
			FetchPlan fetchPlan) {
		final EntityDefinition entityDefinition = attributeDefinition.toEntityDefinition();

		return new EntityFetch(
				sessionFactory,
				createImplicitAlias(),
				LockMode.NONE, // todo : for now
				(AbstractFetchOwner) fetchOwner,
				attributeDefinition.getName(),
				fetchPlan,
				StringHelper.generateAlias( entityDefinition.getEntityPersister().getEntityName(), currentDepth() ),
				new DefaultEntityAliases(
						(Loadable) entityDefinition.getEntityPersister(),
						Integer.toString( currentSuffixBase++ ) + '_'
				)
		);
	}

	private CompositeFetch buildCompositeFetch(FetchOwner fetchOwner, CompositeDefinition attributeDefinition) {
		return new CompositeFetch(
				sessionFactory,
				createImplicitAlias(),
				(AbstractFetchOwner) fetchOwner,
				attributeDefinition.getName()
		);
	}

	private int implicitAliasUniqueness = 0;

	private String createImplicitAlias() {
		return "ia" + implicitAliasUniqueness++;
	}
}
