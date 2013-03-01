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
package org.hibernate.persister.walking.impl;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeDefinition implements AttributeDefinition {
	private final AttributeSource source;
	private final SessionFactoryImplementor sessionFactory;
	private final int attributeNumber;
	private final String attributeName;
	private final Type attributeType;

	protected AbstractAttributeDefinition(
			AttributeSource source,
			SessionFactoryImplementor sessionFactory,
			int attributeNumber,
			String attributeName,
			Type attributeType) {
		this.source = source;
		this.sessionFactory = sessionFactory;
		this.attributeNumber = attributeNumber;
		this.attributeName = attributeName;
		this.attributeType = attributeType;
	}

	@Override
	public String getName() {
		return attributeName;
	}

	@Override
	public Type getType() {
		return attributeType;
	}

	@Override
	public AttributeSource getSource() {
		return source;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	protected int attributeNumber() {
		return attributeNumber;
	}

	@Override
	public String toString() {
		return "AttributeDefinition(" + attributeName + ")";
	}
}
