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

/**
 * @author Steve Ebersole
 */
public interface ReturnVisitationStrategy {
	/**
	 * Notification we are preparing to start visitation.
	 */
	public void prepare();

	/**
	 * Notification that a new RootReturn branch is being started.
	 *
	 * @param rootReturn The RootReturn at the root of the branch.
	 */
	public void startingRootReturn(RootReturn rootReturn);

	/**
	 * Notification that we are finishing up processing a RootReturn branch
	 *
	 * @param rootReturn The RootReturn we are finishing up processing.
	 */
	public void finishingRootReturn(RootReturn rootReturn);

	/**
	 * Notification that we are about to start processing the fetches for the given fetch owner.
	 *
	 * @param fetchOwner The fetch owner.
	 */
	public void startingFetches(FetchReturnOwner fetchOwner);

	/**
	 * Notification that we are finishing up processing the fetches for the given fetch owner.
	 *
	 * @param fetchOwner The fetch owner.
	 */
	public void finishingFetches(FetchReturnOwner fetchOwner);

	public void handleScalarReturn(ScalarReturn scalarReturn);

	public void handleRootEntityReturn(RootEntityReturn rootEntityReturn);

	public void handleRootCollectionReturn(RootCollectionReturn rootCollectionReturn);

	public void handleFetchedEntityReturn(FetchedEntityReturn fetchedEntityReturn);

	public void handleFetchedCollectionReturn(FetchedCollectionReturn fetchedCollectionReturn);
}
