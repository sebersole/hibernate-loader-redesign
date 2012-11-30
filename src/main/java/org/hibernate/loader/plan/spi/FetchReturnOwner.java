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
package org.hibernate.loader.plan.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Contract for owners of fetches.  Any non-scalar return could be a fetch owner.
 *
 * @author Steve Ebersole
 */
public interface FetchReturnOwner extends NonScalarReturn {
	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	public static final FetchReturn[] NO_FETCHES = new FetchReturn[0];

	/**
	 * Retrieve the fetches owned by this return.
	 *
	 * @return The owned fetches.
	 */
	public FetchReturn[] getFetches();

	/**
	 * Retrieve the EntityPersister that is the base for any property references in the fetches it owns.
	 *
	 * @return The EntityPersister, for property name resolution.
	 */
	public EntityPersister retrieveFetchSourcePersister();
}
