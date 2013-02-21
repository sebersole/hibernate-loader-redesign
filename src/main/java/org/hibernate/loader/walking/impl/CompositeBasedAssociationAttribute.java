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
package org.hibernate.loader.walking.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.walking.spi.AssociationAttributeDefinition;
import org.hibernate.loader.walking.spi.AssociationKey;
import org.hibernate.loader.walking.spi.CollectionDefinition;
import org.hibernate.loader.walking.spi.CompositeDefinition;
import org.hibernate.loader.walking.spi.EntityDefinition;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.type.AssociationType;

/**
 * @author Steve Ebersole
 */
public class CompositeBasedAssociationAttribute
		extends AbstractCompositeBasedAttribute
		implements AssociationAttributeDefinition {

	private final AssociationKey associationKey;
	private Joinable joinable;

	public CompositeBasedAssociationAttribute(
			CompositeDefinition source,
			SessionFactoryImplementor factory,
			int attributeNumber,
			AssociationKey associationKey,
			String attributeName,
			AssociationType attributeType) {
		super( source, factory, attributeNumber, attributeName, attributeType );
		this.associationKey = associationKey;
	}

	@Override
	public AssociationType getType() {
		return (AssociationType) super.getType();
	}

	protected Joinable getJoinable() {
		if ( joinable == null ) {
			joinable = getType().getAssociatedJoinable( getSessionFactory() );
		}
		return joinable;
	}

	@Override
	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	@Override
	public boolean isCollection() {
		return getJoinable().isCollection();
	}

	@Override
	public EntityDefinition toEntityDefinition() {
		if ( isCollection() ) {
			throw new IllegalStateException( "Cannot treat collection attribute as entity type" );
		}
		// todo : implement
		return null;
	}

	@Override
	public CollectionDefinition toCollectionDefinition() {
		if ( isCollection() ) {
			throw new IllegalStateException( "Cannot treat entity attribute as collection type" );
		}
		// todo : implement
		return null;
	}
}
