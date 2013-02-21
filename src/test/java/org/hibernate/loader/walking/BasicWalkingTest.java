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
package org.hibernate.loader.walking;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import java.util.List;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.loader.walking.spi.AssociationVisitationStrategy;
import org.hibernate.loader.walking.spi.AttributeDefinition;
import org.hibernate.loader.walking.spi.CollectionDefinition;
import org.hibernate.loader.walking.spi.CompositeDefinition;
import org.hibernate.loader.walking.spi.EntityDefinition;
import org.hibernate.loader.walking.spi.MetadataDrivenAssociationVisitor;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class BasicWalkingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Message.class, Poster.class };
	}

	@Test
	public void testIt() {
		EntityPersister ep = (EntityPersister) sessionFactory().getClassMetadata(Message.class);
		MetadataDrivenAssociationVisitor.visitEntity(
				new AssociationVisitationStrategy() {
					private int depth = 0;

					@Override
					public void start() {
						System.out.println( ">> Start" );
					}

					@Override
					public void finish() {
						System.out.println( "<< Finish" );
					}

					@Override
					public void startingEntity(EntityDefinition entityDefinition) {
						System.out.println(
								String.format(
										"%s Starting entity (%s)",
										StringHelper.repeat( ">>", ++depth ),
										entityDefinition.toString()
								)
						);
					}

					@Override
					public void finishingEntity(EntityDefinition entityDefinition) {
						System.out.println(
								String.format(
										"%s Finishing entity (%s)",
										StringHelper.repeat( "<<", --depth ),
										entityDefinition.toString()
								)
						);
					}

					@Override
					public void startingCollection(CollectionDefinition collectionDefinition) {
						System.out.println(
								String.format(
										"%s Starting collection (%s)",
										StringHelper.repeat( ">>", ++depth ),
										collectionDefinition.toString()
								)
						);
					}

					@Override
					public void finishingCollection(CollectionDefinition collectionDefinition) {
						System.out.println(
								String.format(
										"%s Finishing collection (%s)",
										StringHelper.repeat( ">>", --depth ),
										collectionDefinition.toString()
								)
						);
					}

					@Override
					public void startingComposite(CompositeDefinition compositeDefinition) {
						System.out.println(
								String.format(
										"%s Starting composite (%s)",
										StringHelper.repeat( ">>", ++depth ),
										compositeDefinition.toString()
								)
						);
					}

					@Override
					public void finishingComposite(CompositeDefinition compositeDefinition) {
						System.out.println(
								String.format(
										"%s Finishing composite (%s)",
										StringHelper.repeat( ">>", --depth ),
										compositeDefinition.toString()
								)
						);
					}

					@Override
					public boolean handleAttribute(AttributeDefinition attributeDefinition) {
						System.out.println(
								String.format(
										"%s Handling attribute (%s)",
										StringHelper.repeat( ">>", depth+1 ),
										attributeDefinition.toString()
								)
						);
						return true;
					}
				},
				ep
		);
	}

	@Entity( name = "Message" )
	public static class Message {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		@JoinColumn
		private Poster poster;
	}

	@Entity( name = "Poster" )
	public static class Poster {
		@Id
		private Integer id;
		private String name;
		@OneToMany(mappedBy = "poster")
		private List<Message> messages;
	}
}
