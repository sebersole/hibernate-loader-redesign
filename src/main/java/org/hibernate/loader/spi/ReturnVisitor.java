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
 * Visitor for processing {@link Return} objects.
 *
 * @author Steve Ebersole
 */
public class ReturnVisitor {
	public static void visit(RootReturn[] rootReturns, ReturnVisitationStrategy strategy) {
		strategy.prepare();

		for ( RootReturn rootReturn : rootReturns ) {
			visitRootReturn( rootReturn, strategy );
		}
	}

	private static void visitRootReturn(RootReturn rootReturn, ReturnVisitationStrategy strategy) {
		strategy.startingRootReturn( rootReturn );

		if ( ScalarReturn.class.isInstance( rootReturn ) ) {
			strategy.handleScalarReturn( (ScalarReturn) rootReturn );
		}
		else {
			visitNonScalarRootReturn( rootReturn, strategy );
		}

		strategy.finishingRootReturn( rootReturn );
	}

	private static void visitNonScalarRootReturn(RootReturn rootReturn, ReturnVisitationStrategy strategy) {
		if ( RootEntityReturn.class.isInstance( rootReturn ) ) {
			strategy.handleRootEntityReturn( (RootEntityReturn) rootReturn );
			visitFetches( (RootEntityReturn) rootReturn, strategy );
		}
		else if ( RootCollectionReturn.class.isInstance( rootReturn ) ) {
			strategy.handleRootCollectionReturn( (RootCollectionReturn) rootReturn );
			visitFetches( (RootCollectionReturn) rootReturn, strategy );
		}
		else {
			throw new IllegalStateException(
					"Unexpected return type encountered; expecting a non-scalar root return, but found " +
							rootReturn.getClass().getName()
			);
		}
	}

	private static void visitFetches(FetchReturnOwner fetchOwner, ReturnVisitationStrategy strategy) {
		strategy.startingFetches( fetchOwner );

		for ( FetchReturn fetchReturn : fetchOwner.getFetches() ) {
			visitFetch( fetchReturn, strategy );
		}

		strategy.finishingFetches( fetchOwner );
	}

	private static void visitFetch(FetchReturn fetchReturn, ReturnVisitationStrategy strategy) {
		if ( FetchedEntityReturn.class.isInstance( fetchReturn ) ) {
			strategy.handleFetchedEntityReturn( ( FetchedEntityReturn) fetchReturn );
			visitFetches( fetchReturn, strategy );
		}
		else if ( FetchedCollectionReturn.class.isInstance( fetchReturn ) ) {
			strategy.handleFetchedCollectionReturn( ( FetchedCollectionReturn) fetchReturn );
			visitFetches( fetchReturn, strategy );
		}
		else {
			throw new IllegalStateException(
					"Unexpected return type encountered; expecting a fetch return, but found " +
							fetchReturn.getClass().getName()
			);
		}
	}

}
