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

import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.walking.impl.EntityBasedAssociationAttribute;
import org.hibernate.persister.walking.impl.EntityBasedBasicAttribute;
import org.hibernate.persister.walking.impl.EntityBasedCompositeAttribute;
import org.hibernate.persister.walking.impl.EntityDefinitionImpl;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.CompositeDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.CollectionElementNature;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.internal.StandardPersisterClassResolver;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class CustomPersisterClassResolver extends StandardPersisterClassResolver {
	@Override
	public Class<? extends EntityPersister> singleTableEntityPersister() {
		return CustomSingleTableEntityPersister.class;
	}

	@Override
	public Class<? extends EntityPersister> joinedSubclassEntityPersister() {
		throw new NotYetImplementedException();
	}

	@Override
	public Class<? extends EntityPersister> unionSubclassEntityPersister() {
		throw new NotYetImplementedException();
	}


	// todo : make #oneToManyPersister() and #basicCollectionPersister() public or protected on StandardPersisterClassResolver

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
		return metadata.isOneToMany()
				? oneToManyPersister()
				: basicCollectionPersister();
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(PluralAttributeBinding metadata) {
		return metadata.getCollectionElement().getCollectionElementNature() == CollectionElementNature.ONE_TO_MANY
				? oneToManyPersister()
				: basicCollectionPersister();
	}

	protected Class<? extends CollectionPersister> oneToManyPersister() {
		return CustomOneToManyPersister.class;
	}

	protected Class<? extends CollectionPersister> basicCollectionPersister() {
		throw new NotYetImplementedException();
	}


	/**
	 * essentially weaves EntityDefinition into EntityPersister
	 */
	public static class CustomSingleTableEntityPersister extends SingleTableEntityPersister
			implements EntityDefinition {
		public CustomSingleTableEntityPersister(
				PersistentClass persistentClass,
				EntityRegionAccessStrategy cacheAccessStrategy,
				NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
				SessionFactoryImplementor factory, Mapping mapping)
				throws HibernateException {
			super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory, mapping );
		}

		public CustomSingleTableEntityPersister(
				EntityBinding entityBinding,
				EntityRegionAccessStrategy cacheAccessStrategy,
				NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
				SessionFactoryImplementor factory, Mapping mapping) throws HibernateException {
			super( entityBinding, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory, mapping );
		}

		@Override
		public EntityPersister getEntityPersister() {
			return this;
		}

		@Override
		public Iterable<AttributeDefinition> getEmbeddedCompositeIdentifierAttributes() {
			final Type idType = getIdentifierType();
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
					if ( getEntityMetamodel().getIdentifierProperty().isEmbedded() ) {
						return new Iterable<AttributeDefinition>() {
							@Override
							public Iterator<AttributeDefinition> iterator() {
								return new Iterator<AttributeDefinition>() {
									private final int numberOfAttributes = countSubclassProperties();
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
		public Iterable<AttributeDefinition> getAttributes() {
			return new Iterable<AttributeDefinition>() {
				@Override
				public Iterator<AttributeDefinition> iterator() {
					return new Iterator<AttributeDefinition>() {
						private final int numberOfAttributes = countSubclassProperties();
						private int currentAttributeNumber = 0;

						@Override
						public boolean hasNext() {
							return currentAttributeNumber < numberOfAttributes;
						}

						@Override
						public AttributeDefinition next() {
							final int attributeNumber = currentAttributeNumber;
							currentAttributeNumber++;
							final Type attributeType = getSubclassPropertyType( attributeNumber );
							final String attributeName = getSubclassPropertyName( attributeNumber );

							if ( attributeType.isAssociationType() ) {
								return new EntityBasedAssociationAttribute(
										CustomSingleTableEntityPersister.this,
										getFactory(),
										attributeNumber,
										attributeName,
										(AssociationType) attributeType
								);
							}
							else if ( attributeType.isComponentType() ) {
								return new EntityBasedCompositeAttribute(
										CustomSingleTableEntityPersister.this,
										getFactory(),
										attributeNumber,
										attributeName,
										(CompositeType) attributeType
								);
							}
							else {
								return new EntityBasedBasicAttribute(
										CustomSingleTableEntityPersister.this,
										getFactory(),
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
	}

	/**
	 * essentially weaves EntityDefinition into EntityPersister
	 */
	public static class CustomOneToManyPersister extends OneToManyPersister implements CollectionDefinition {
		public CustomOneToManyPersister(
				Collection collection,
				CollectionRegionAccessStrategy cacheAccessStrategy,
				Configuration cfg, SessionFactoryImplementor factory)
				throws MappingException, CacheException {
			super( collection, cacheAccessStrategy, cfg, factory );
		}

		@Override
		public CollectionPersister getCollectionPersister() {
			return this;
		}

		@Override
		public CollectionType getType() {
			return getCollectionType();
		}

		@Override
		public CollectionIndexDefinition getIndexDefinition() {
			if ( ! hasIndex() ) {
				return null;
			}


			return new CollectionIndexDefinition() {
				@Override
				public CollectionDefinition getCollectionDefinition() {
					return CustomOneToManyPersister.this;
				}

				@Override
				public Type getType() {
					return getIndexType();
				}

				@Override
				public EntityDefinition toEntityDefinition() {
					if ( getType().isComponentType() ) {
						throw new IllegalStateException( "Cannot treat composite collection index type as entity" );
					}
					return new EntityDefinitionImpl(
							(EntityPersister) ( (AssociationType) getIndexType() ).getAssociatedJoinable( getFactory() )
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
					return CustomOneToManyPersister.this;
				}

				@Override
				public Type getType() {
					return getElementType();
				}

				@Override
				public EntityDefinition toEntityDefinition() {
					if ( getType().isComponentType() ) {
						throw new IllegalStateException( "Cannot treat composite collection element type as entity" );
					}
					return new EntityDefinitionImpl( getElementPersister() );
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

}
