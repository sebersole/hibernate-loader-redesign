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

import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.RootReturn;

/**
 * Represents a plan of how to load results from a ResultSet.
 *
 * NOTE : alot of this simply tries to bridge to how {@link org.hibernate.loader.Loader} expects this information
 * to be available (aka, "parallel arrays").  The plan is to change the way that this information is expected/consumed
 * based on this contract.
 *
 * @author Steve Ebersole
 */
public interface LoadPlan {
	public RootReturn[] getRootReturns();

	public EntityReturn[] collectEntityReturns();

	public CollectionReturn[] collectCollectionReturns();
}
