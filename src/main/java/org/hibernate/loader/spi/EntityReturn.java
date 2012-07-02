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

import org.hibernate.loader.EntityAliases;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Common contract for entity returns whether root or fetched.
 *
 * @author Steve Ebersole
 */
public interface EntityReturn extends FetchReturnOwner {
	/**
	 * Retrieves the EntityPersister describing the entity associated with this Return.
	 *
	 * @return The EntityPersister.
	 */
	public EntityPersister getEntityPersister();

	/**
	 * Returns the description of the aliases in the JDBC ResultSet that identify values "belonging" to the this entity.
	 *
	 * @return The ResultSet alias descriptor.
	 */
	public EntityAliases getEntityAliases();

	/**
	 * Obtain the SQL table alias associated with this entity.
	 *
	 * TODO : eventually this needs to not be a String, but a representation like I did for the Antlr3 branch
	 * 		(AliasRoot, I think it was called)
	 *
	 * @return The SQL table alias for this entity
	 */
	public String getSqlTableAlias();
}
