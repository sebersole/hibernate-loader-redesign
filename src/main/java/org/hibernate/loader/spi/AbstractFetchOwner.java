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

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchOwner extends AbstractReturn implements FetchReturnOwner {
	private final String alias;
	private final LockMode lockMode;

	private List<FetchReturn> fetchReturns;

	public AbstractFetchOwner(SessionFactoryImplementor factory, String alias, LockMode lockMode) {
		super( factory );
		this.alias = alias;
		if ( alias == null ) {
			throw new HibernateException( "alias must be specified" );
		}
		this.lockMode = lockMode;
	}

	public String getAlias() {
		return alias;
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	void addFetch(FetchReturn fetchReturn) {
		if ( fetchReturn.getOwner() != this ) {
			throw new IllegalArgumentException( "Fetch and owner did not match" );
		}

		if ( fetchReturns == null ) {
			fetchReturns = new ArrayList<FetchReturn>();
		}

		fetchReturns.add( fetchReturn );
	}

	@Override
	public FetchReturn[] getFetches() {
		return fetchReturns == null ? NO_FETCHES : fetchReturns.toArray( new FetchReturn[ fetchReturns.size() ] );
	}
}
