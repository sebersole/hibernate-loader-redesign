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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchedCollectionReturn;
import org.hibernate.loader.plan.spi.FetchedEntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.ReturnVisitationStrategyAdapter;
import org.hibernate.loader.plan.spi.ReturnVisitor;
import org.hibernate.loader.plan.spi.RootCollectionReturn;
import org.hibernate.loader.plan.spi.RootEntityReturn;
import org.hibernate.loader.plan.spi.RootReturn;

/**
 * @author Steve Ebersole
 */
public class BasicLoadPlan implements LoadPlan {
	private final SessionFactoryImplementor sessionFactory;
	private final RootReturn[] rootReturns;

	private final EntityReturn[] allEntityReturns;
	private final CollectionReturn[] allCollectionReturns;

	public BasicLoadPlan(SessionFactoryImplementor sessionFactory, RootReturn[] rootReturns) {
		this.sessionFactory = sessionFactory;
		this.rootReturns = rootReturns;

		final List<EntityReturn> allEntityReturnsList = new ArrayList<EntityReturn>();
		final List<CollectionReturn> allCollectionReturnsList = new ArrayList<CollectionReturn>();

		ReturnVisitor.visit(
				rootReturns,
				new ReturnVisitationStrategyAdapter() {
					@Override
					public void handleRootEntityReturn(RootEntityReturn rootEntityReturn) {
						handleEntityReturn( rootEntityReturn );
					}

					private void handleEntityReturn(EntityReturn entityReturn) {
						allEntityReturnsList.add( entityReturn );
					}

					@Override
					public void handleRootCollectionReturn(RootCollectionReturn rootCollectionReturn) {
						handleCollectionReturn( rootCollectionReturn );
					}

					private void handleCollectionReturn(CollectionReturn collectionReturn) {
						allCollectionReturnsList.add( collectionReturn );
					}

					@Override
					public void handleFetchedEntityReturn(FetchedEntityReturn fetchedEntityReturn) {
						handleEntityReturn( fetchedEntityReturn );
					}

					@Override
					public void handleFetchedCollectionReturn(FetchedCollectionReturn fetchedCollectionReturn) {
						handleCollectionReturn( fetchedCollectionReturn );
					}
				}
		);

		this.allEntityReturns = allEntityReturnsList.toArray( new EntityReturn[ allEntityReturnsList.size() ] );
		this.allCollectionReturns = allCollectionReturnsList.toArray( new CollectionReturn[ allCollectionReturnsList.size() ] );
	}

	@Override
	public RootReturn[] getRootReturns() {
		return rootReturns;
	}

	@Override
	public EntityReturn[] collectEntityReturns() {
		return allEntityReturns;
	}

	@Override
	public CollectionReturn[] collectCollectionReturns() {
		return allCollectionReturns;
	}
}
