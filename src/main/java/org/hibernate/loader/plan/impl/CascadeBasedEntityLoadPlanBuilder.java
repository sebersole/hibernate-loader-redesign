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
package org.hibernate.loader.plan.impl;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.FetchPlan;
import org.hibernate.loader.walking.spi.AssociationAttributeDefinition;

/**
 * @author Steve Ebersole
 */
public class CascadeBasedEntityLoadPlanBuilder extends RootEntityLoadPlanBuilderStrategy {
	private static final FetchPlan EAGER = new FetchPlan( FetchTiming.IMMEDIATE, FetchStyle.JOIN );
	private static final FetchPlan DELAYED = new FetchPlan( FetchTiming.DELAYED, FetchStyle.SELECT );

	private final CascadingAction cascadeActionToMatch;

	public CascadeBasedEntityLoadPlanBuilder(
			CascadingAction cascadeActionToMatch,
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers,
			String rootAlias,
			int suffixSeed) {
		super( sessionFactory, loadQueryInfluencers, rootAlias, suffixSeed );
		this.cascadeActionToMatch = cascadeActionToMatch;
	}

	@Override
	protected FetchPlan determineFetchPlan(AssociationAttributeDefinition attributeDefinition) {
		return attributeDefinition.determineCascadeStyle().doCascade( cascadeActionToMatch ) ? EAGER : DELAYED;
	}
}
