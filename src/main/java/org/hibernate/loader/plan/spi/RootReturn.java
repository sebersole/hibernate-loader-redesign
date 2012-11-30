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

/**
 * Root returns are return values that describe a root value in the result.  This includes scalar results
 * as well collection and entity returns that are not fetched.
 *
 * The result of a result set processor is ultimately a {@code List<Object[]>}.  Each element in the
 * {@code Object[]} is represented by a Return.  However, not all Returns require an index in the
 * {@code Object[]} (mainly this comes down to join fetches).  This interfaces marks Return objects that
 * need an index in the {@code Object[]}
 *
 * @author Steve Ebersole
 */
public interface RootReturn extends Return {
}
