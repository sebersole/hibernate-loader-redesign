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
package org.hibernate.loader.plan.spi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.hibernate.loader.walking.spi.AssociationAttributeDefinition;
import org.hibernate.loader.walking.spi.AssociationVisitationStrategy;
import org.hibernate.loader.walking.spi.AttributeDefinition;
import org.hibernate.loader.walking.spi.CollectionDefinition;
import org.hibernate.loader.walking.spi.CompositeDefinition;
import org.hibernate.loader.walking.spi.EntityDefinition;
import org.hibernate.loader.walking.spi.MetadataDrivenAssociationVisitor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class LoadPlanBuilder {
	private final SessionFactoryImplementor sessionFactory;

	public LoadPlanBuilder(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public LoadPlan buildEntityLoadPlan(
			LoadQueryInfluencers loadQueryInfluencers,
			EntityPersister persister,
			String alias,
			int suffixSeed) {
		final LoadPlanBuilderStrategy strategy = buildRootEntityStrategy( loadQueryInfluencers, alias, suffixSeed );
		MetadataDrivenAssociationVisitor.visitEntity( strategy, persister );
		return strategy.buildLoadPlan();
	}

	protected static interface LoadPlanBuilderStrategy extends AssociationVisitationStrategy {
		public LoadPlan buildLoadPlan();
	}

	private LoadPlanBuilderStrategy buildRootEntityStrategy(
			LoadQueryInfluencers loadQueryInfluencers,
			String alias,
			int suffixSeed) {
		return new VisitorStrategy( loadQueryInfluencers, alias, suffixSeed );
	}

	private class VisitorStrategy implements LoadPlanBuilderStrategy {
		private final LoadQueryInfluencers loadQueryInfluencers;
		private final String rootAlias;
		private int currentSuffixBase;

		private final List<Return> returns = new ArrayList<Return>();
		private boolean hasScalars = false;

		private int currentDepth = 0;
		private Object previousReturnOrFetch;

		private PropertyPath propertyPath = new PropertyPath( "" );

		public VisitorStrategy(LoadQueryInfluencers loadQueryInfluencers, String rootAlias, int suffixSeed) {
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
			if ( previousReturnOrFetch == null ) {
				// this is a root...
				final EntityReturn entityReturn = buildRootEntityReturn( entityDefinition );
				returns.add( entityReturn );
				previousReturnOrFetch = entityReturn;
			}
			// otherwise this call should represent a fetch which should have been handled in #handleAttribute
		}

		@Override
		public void finishingEntity(EntityDefinition entityDefinition) {
			// nothing to do
		}

		@Override
		public void startingCollection(CollectionDefinition collectionDefinition) {
			if ( previousReturnOrFetch == null ) {
				// this is a root...
				final CollectionReturn collectionReturn = buildRootCollectionReturn( collectionDefinition );;
				returns.add( collectionReturn );
				previousReturnOrFetch = collectionReturn;
			}
		}

		@Override
		public void finishingCollection(CollectionDefinition collectionDefinition) {
			// nothing to do
		}

		@Override
		public void startingComposite(CompositeDefinition compositeDefinition) {
			if ( previousReturnOrFetch == null ) {
				throw new HibernateException( "A component cannot be the root of a walk nor a graph" );
			}
		}

		@Override
		public void finishingComposite(CompositeDefinition compositeDefinition) {
			// nothing to do
		}

		@Override
		public boolean handleAttribute(AttributeDefinition attributeDefinition) {
			currentDepth++;

			final Type attributeType = attributeDefinition.getType();

			final boolean isComponentType = attributeType.isComponentType();
			final boolean isBasicType = ! ( isComponentType || attributeType.isAssociationType() );

			if ( isComponentType || isBasicType ) {
				return true;
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
			if ( previousFetchOwner != null ) {
				previousReturnOrFetch = previousFetchOwner;
				previousFetchOwner = null;
			}

			currentDepth--;
		}

		private FetchOwner previousFetchOwner;

		private boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
			final FetchPlan fetchPlan = determineFetchPlan( attributeDefinition );
			if ( fetchPlan.getTiming() == FetchTiming.IMMEDIATE ) {
				boolean continueWithGraph = true;
				if ( ! FetchOwner.class.isInstance( previousReturnOrFetch ) ) {
					throw new HibernateException( "Cannot build fetch from non-fetch-owner" );
				}
				final FetchOwner fetchOwner = (FetchOwner) previousReturnOrFetch;
				fetchOwner.validateFetchPlan( fetchPlan );
				previousFetchOwner = fetchOwner;

				final Fetch associationFetch;
				if ( attributeDefinition.isCollection() ) {
					associationFetch = buildCollectionFetch( fetchOwner, attributeDefinition, fetchPlan );
				}
				else {
					associationFetch = buildEntityFetch( fetchOwner, attributeDefinition, fetchPlan );
				}
				previousReturnOrFetch = associationFetch;

				return continueWithGraph;
			}
			else {
				return false;
			}
		}

		private FetchPlan determineFetchPlan(AssociationAttributeDefinition attributeDefinition) {
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
			if ( currentDepth > sessionFactory.getSettings().getMaximumFetchDepth() ) {
				return new FetchPlan( fetchPlan.getTiming(), FetchStyle.SELECT );
			}

			if ( attributeDefinition.getType().isCollectionType() && isTooManyCollections() ) {
				// todo : have this revert to batch or subselect fetching once "sql gen redesign" is in place
				return new FetchPlan( fetchPlan.getTiming(), FetchStyle.SELECT );
			}

			return fetchPlan;
		}

		protected boolean isTooManyCollections() {
			return false;
		}

		@Override
		public LoadPlan buildLoadPlan() {
			return new LoadPlan() {
				@Override
				public boolean hasAnyScalarReturns() {
					return hasScalars;
				}

				@Override
				public List<Return> getReturns() {
					return returns;
				}
			};
		}

		protected EntityReturn buildRootEntityReturn(EntityDefinition entityDefinition) {
			final String entityName = entityDefinition.getEntityPersister().getEntityName();
			return new EntityReturn(
					sessionFactory,
					rootAlias,
					LockMode.NONE, // todo : for now
					entityName,
					StringHelper.generateAlias( StringHelper.unqualifyEntityName( entityName ), currentDepth ),
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
					StringHelper.generateAlias( entityDefinition.getEntityPersister().getEntityName(), currentDepth ),
					new DefaultEntityAliases(
							(Loadable) entityDefinition.getEntityPersister(),
							Integer.toString( currentSuffixBase++ ) + '_'
					)
			);
		}

		private int implicitAliasUniqueness = 0;

		private String createImplicitAlias() {
			return "ia" + implicitAliasUniqueness++;
		}
	}
}
