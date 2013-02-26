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

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.impl.RootEntityLoadPlanBuilderStrategy;
import org.hibernate.loader.walking.spi.MetadataDrivenAssociationVisitor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class LoadPlanBuilder {
	private final SessionFactoryImplementor sessionFactory;

	public LoadPlanBuilder(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public static LoadPlan buildEntityLoadPlan(LoadPlanBuilderStrategy strategy, EntityPersister persister) {
		MetadataDrivenAssociationVisitor.visitEntity( strategy, persister );
		return strategy.buildLoadPlan();
	}

	public LoadPlan buildEntityLoadPlan(
			LoadQueryInfluencers loadQueryInfluencers,
			EntityPersister persister,
			String alias,
			int suffixSeed) {
		return buildEntityLoadPlan(
				new RootEntityLoadPlanBuilderStrategy( sessionFactory, loadQueryInfluencers, alias, suffixSeed ),
				persister
		);
	}
}
