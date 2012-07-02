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

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.EntityAliases;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Represents a return which names a "root" entity.
 * <p/>
 * A root entity means it is explicitly a value in the result, as opposed to a fetched association.
 *
 * @author Steve Ebersole
 */
public class RootEntityReturn extends AbstractFetchOwner implements RootReturn, EntityReturn {
	private final EntityAliases entityAliases;
	private final String sqlTableAlias;

	private final EntityPersister persister;

	public RootEntityReturn(
			SessionFactoryImplementor sessionFactory,
			String alias,
			LockMode lockMode,
			String entityName,
			String sqlTableAlias,
			EntityAliases entityAliases) {
		super( sessionFactory, alias, lockMode );
		this.entityAliases = entityAliases;
		this.sqlTableAlias = sqlTableAlias;

		this.persister = sessionFactory.getEntityPersister( entityName );
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public EntityAliases getEntityAliases() {
		return entityAliases;
	}

	@Override
	public String getSqlTableAlias() {
		return sqlTableAlias;
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return persister;
	}
}
