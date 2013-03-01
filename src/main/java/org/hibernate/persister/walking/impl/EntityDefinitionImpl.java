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
package org.hibernate.persister.walking.impl;

import java.util.Iterator;

import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class EntityDefinitionImpl implements EntityDefinition {
	private final OuterJoinLoadable persister;

	public EntityDefinitionImpl(EntityPersister persister) {
		this.persister = (OuterJoinLoadable) persister;
	}

	public OuterJoinLoadable getPersister() {
		return persister;
	}

	@Override
	public Iterable<AttributeDefinition> getAttributes() {
		return new Iterable<AttributeDefinition>() {
			@Override
			public Iterator<AttributeDefinition> iterator() {
				return new Iterator<AttributeDefinition>() {
					private final int numberOfAttributes = persister.countSubclassProperties();
					private int currentAttributeNumber = 0;

					@Override
					public boolean hasNext() {
						return currentAttributeNumber < numberOfAttributes;
					}

					@Override
					public AttributeDefinition next() {
						final int attributeNumber = currentAttributeNumber;
						currentAttributeNumber++;
						final Type attributeType = persister.getSubclassPropertyType( attributeNumber );
						final String attributeName = persister.getSubclassPropertyName( attributeNumber );

						if ( attributeType.isAssociationType() ) {
							return new EntityBasedAssociationAttribute(
									EntityDefinitionImpl.this,
									persister.getFactory(),
									attributeNumber,
									attributeName,
									(AssociationType) attributeType
							);
						}
						else if ( attributeType.isComponentType() ) {
							return new EntityBasedCompositeAttribute(
									EntityDefinitionImpl.this,
									persister.getFactory(),
									attributeNumber,
									attributeName,
									(CompositeType) attributeType
							);
						}
						else {
							return new EntityBasedBasicAttribute(
									EntityDefinitionImpl.this,
									persister.getFactory(),
									attributeNumber,
									attributeName,
									attributeType
							);
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException( "Remove operation not supported here" );
					}
				};
			}
		};
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public Iterable<AttributeDefinition> getEmbeddedCompositeIdentifierAttributes() {
		final Type idType = persister.getIdentifierType();
		if ( idType.isComponentType() ) {
			final CompositeType cidType = (CompositeType) idType;
			if ( cidType.isEmbedded() ) {
				// we have an embedded composite identifier.  Most likely we need to process the composite
				// properties separately, although there is an edge case where the identifier is really
				// a simple identifier (single value) wrapped in a JPA @IdClass or even in the case of a
				// a simple identifier (single value) wrapped in a Hibernate composite type.
				//
				// We really do not have a built-in method to determine that.  However, generally the
				// persister would report that there is single, physical identifier property which is
				// explicitly at odds with the notion of "embedded composite".  So we use that for now
				if ( persister.getEntityMetamodel().getIdentifierProperty().isEmbedded() ) {
					return new Iterable<AttributeDefinition>() {
						@Override
						public Iterator<AttributeDefinition> iterator() {
							return new Iterator<AttributeDefinition>() {
								private final int numberOfAttributes = persister.countSubclassProperties();
								private int currentAttributeNumber = 0;

								@Override
								public boolean hasNext() {
									return currentAttributeNumber < numberOfAttributes;
								}

								@Override
								public AttributeDefinition next() {
									// todo : implement
									return null;  //To change body of implemented methods use File | Settings | File Templates.
								}

								@Override
								public void remove() {
									throw new UnsupportedOperationException( "Remove operation not supported here" );
								}
							};
						}
					};
				}
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return "EntityDefinition(" + persister.getEntityName() + ")";
	}
}
