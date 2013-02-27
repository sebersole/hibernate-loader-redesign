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

import org.hibernate.loader.walking.spi.CollectionDefinition;
import org.hibernate.loader.walking.spi.CollectionElementDefinition;
import org.hibernate.loader.walking.spi.CollectionIndexDefinition;
import org.hibernate.loader.walking.spi.CompositeDefinition;
import org.hibernate.loader.walking.spi.EntityDefinition;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class CollectionDefinitionImpl implements CollectionDefinition {
	private final QueryableCollection persister;

	public CollectionDefinitionImpl(CollectionPersister persister) {
		this.persister = (QueryableCollection) persister;
	}

	@Override
	public CollectionType getType() {
		return persister.getCollectionType();
	}

	@Override
	public CollectionPersister getCollectionPersister() {
		return persister;
	}

	@Override
	public CollectionIndexDefinition getIndexDefinition() {
		if ( ! persister.hasIndex() ) {
			return null;
		}

		return new CollectionIndexDefinition() {
			@Override
			public CollectionDefinition getCollectionDefinition() {
				return CollectionDefinitionImpl.this;
			}

			@Override
			public Type getType() {
				return persister.getIndexType();
			}

			@Override
			public EntityDefinition toEntityDefinition() {
				if ( getType().isComponentType() ) {
					throw new IllegalStateException( "Cannot treat composite collection index type as entity" );
				}
				return new EntityDefinitionImpl(
						(EntityPersister) ( (AssociationType) persister.getIndexType() )
								.getAssociatedJoinable( persister.getFactory() )
				);
			}

			@Override
			public CompositeDefinition toCompositeDefinition() {
				if ( ! getType().isComponentType() ) {
					throw new IllegalStateException( "Cannot treat entity collection index type as composite" );
				}
				// todo : implement
				return null;
			}
		};
	}

	@Override
	public CollectionElementDefinition getElementDefinition() {
		return new CollectionElementDefinition() {
			@Override
			public CollectionDefinition getCollectionDefinition() {
				return CollectionDefinitionImpl.this;
			}

			@Override
			public Type getType() {
				return persister.getElementType();
			}

			@Override
			public EntityDefinition toEntityDefinition() {
				if ( getType().isComponentType() ) {
					throw new IllegalStateException( "Cannot treat composite collection element type as entity" );
				}
				return new EntityDefinitionImpl( persister.getElementPersister() );
			}

			@Override
			public CompositeDefinition toCompositeDefinition() {
				if ( ! getType().isComponentType() ) {
					throw new IllegalStateException( "Cannot treat entity collection element type as composite" );
				}
				// todo : implement
				throw new RuntimeException( "Not yet implemented" );
			}
		};
	}

}
