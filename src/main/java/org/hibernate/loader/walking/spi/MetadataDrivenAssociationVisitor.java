/*
 * Hibernate, Relational Persistence for Idiomatic Java
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
package org.hibernate.loader.walking.spi;

import java.util.HashSet;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.walking.impl.AbstractCompositeBasedAttribute;
import org.hibernate.loader.walking.impl.CollectionDefinitionImpl;
import org.hibernate.loader.walking.impl.EntityBasedCompositeAttribute;
import org.hibernate.loader.walking.impl.EntityDefinitionImpl;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.type.AssociationType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

/**
 * Re-implementation of the legacy {@link org.hibernate.loader.JoinWalker} contract to leverage load plans.
 *
 * @author Steve Ebersole
 */
public class MetadataDrivenAssociationVisitor {
	private static final Logger log = Logger.getLogger( MetadataDrivenAssociationVisitor.class );

	public static void visitEntity(AssociationVisitationStrategy strategy, EntityPersister persister) {
		strategy.start();
		try {
			new MetadataDrivenAssociationVisitor( strategy, persister.getFactory() )
					.visitEntityDefinition( new EntityDefinitionImpl( persister ));
		}
		finally {
			strategy.finish();
		}
	}

	public static void visitCollection(AssociationVisitationStrategy strategy, QueryableCollection persister) {
		strategy.start();
		try {
			new MetadataDrivenAssociationVisitor( strategy, persister.getFactory() )
					.visitCollectionDefinition( new CollectionDefinitionImpl( persister ) );
		}
		finally {
			strategy.finish();
		}
	}

	private final AssociationVisitationStrategy strategy;
	private final SessionFactoryImplementor factory;

	// todo : add a getDepth() method to PropertyPath
	private PropertyPath currentPropertyPath = new PropertyPath();

	public MetadataDrivenAssociationVisitor(AssociationVisitationStrategy strategy, SessionFactoryImplementor factory) {
		this.strategy = strategy;
		this.factory = factory;
	}

	private void visitEntityDefinition(EntityDefinition entityDefinition) {
		strategy.startingEntity( entityDefinition );
		try {
			visitAttributes( entityDefinition );
			optionallyVisitEmbeddedCompositeIdentifier( entityDefinition );
		}
		finally {
			strategy.finishingEntity( entityDefinition );
		}
	}

	private void optionallyVisitEmbeddedCompositeIdentifier(EntityDefinition entityDefinition) {
		// if the entity has a composite identifier, see if we need to handle
		// its sub-properties separately
//		final Type idType = entityDefinition.getPersister().getIdentifierType();
//		if ( idType.isComponentType() ) {
//			final CompositeType cidType = (CompositeType) idType;
//			if ( cidType.isEmbedded() ) {
//				// we have an embedded composite identifier.  Most likely we need to process the composite
//				// properties separately, although there is an edge case where the identifier is really
//				// a simple identifier (single value) wrapped in a JPA @IdClass or even in the case of a
//				// a simple identifier (single value) wrapped in a Hibernate composite type.
//				//
//				// We really do not have a built-in method to determine that.  However, generally the
//				// persister would report that there is single, physical identifier property which is
//				// explicitly at odds with the notion of "embedded composite".  So we use that for now
//				if ( entityDefinition.getPersister().getEntityMetamodel().getIdentifierProperty().isEmbedded() ) {
//					walkComponentDefinition( cidType );
//				}
//			}
//		}
	}

	private void visitAttributes(AttributeSource attributeSource) {
		for ( AttributeDefinition attributeDefinition : attributeSource.getAttributes() ) {
			final PropertyPath subPath = currentPropertyPath.append( attributeDefinition.getName() );
			log.debug( "Visiting attribute path : " + subPath.getFullPath() );

			final boolean continueWalk = strategy.handleAttribute( attributeDefinition );
			if ( continueWalk ) {
				final PropertyPath old = currentPropertyPath;
				currentPropertyPath = subPath;
				try {
					if ( attributeDefinition.getType().isAssociationType() ) {
						visitAssociation( (AssociationAttributeDefinition) attributeDefinition );
					}
					else if ( attributeDefinition.getType().isComponentType() ) {
						visitCompositeDefinition( (CompositeDefinition) attributeDefinition );
					}
				}
				finally {
					currentPropertyPath = old;
				}
			}
		}
	}

	private void visitAssociation(AssociationAttributeDefinition attribute) {
		// todo : do "too deep" checks; but see note about adding depth to PropertyPath

		if ( isDuplicateAssociation( attribute.getAssociationKey() ) ) {
			log.debug( "Property path deemed to be circular : " + currentPropertyPath.getFullPath() );
			return;
		}

		if ( attribute.isCollection() ) {
			visitCollectionDefinition( attribute.toCollectionDefinition() );
		}
		else {
			visitEntityDefinition( attribute.toEntityDefinition() );
		}
	}

	private void visitCompositeDefinition(CompositeDefinition compositeDefinition) {
		strategy.startingComposite( compositeDefinition );
		try {
			visitAttributes( compositeDefinition );
		}
		finally {
			strategy.finishingComposite( compositeDefinition );
		}
	}

	private void visitCollectionDefinition(CollectionDefinition collectionDefinition) {
		visitCollectionIndex( collectionDefinition.getIndexDefinition() );

		final CollectionElementDefinition elementDefinition = collectionDefinition.getElementDefinition();
		if ( elementDefinition.getType().isComponentType() ) {
			visitCompositeDefinition( elementDefinition.toCompositeDefinition() );
		}
		else {
			visitEntityDefinition( elementDefinition.toEntityDefinition() );
		}
	}

	private void visitCollectionIndex(CollectionIndexDefinition collectionIndexDefinition) {
		if ( collectionIndexDefinition == null ) {
			return;
		}

		log.debug( "Visiting collection index :  " + currentPropertyPath.getFullPath() );
		currentPropertyPath = currentPropertyPath.append( "<key>" );
		try {
			final Type collectionIndexType = collectionIndexDefinition.getType();
			if ( collectionIndexType.isComponentType() ) {
				visitCompositeDefinition( collectionIndexDefinition.toCompositeDefinition() );
			}
			else if ( collectionIndexType.isAssociationType() ) {
				visitEntityDefinition( collectionIndexDefinition.toEntityDefinition() );
			}
		}
		finally {
			currentPropertyPath = currentPropertyPath.getParent();
		}
	}


	private final Set<AssociationKey> visitedAssociationKeys = new HashSet<AssociationKey>();

	protected boolean isDuplicateAssociation(AssociationKey associationKey) {
		return !visitedAssociationKeys.add( associationKey );
	}

}
