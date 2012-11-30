package org.hibernate.loader.plan.spi;

/**
 * Describes the SPI contract for load plans, which control how Hibernate processes a {@link java.sql.ResultSet}.
 * <p/>
 * The collective contract entry-point is the {@link LoadPlan} interface.
 * <p/>
 * However, the bulk of the load plan SPI contract centers around the individual {@link Return} descriptor.  At the
 * highest level, a return can be either a {@link ScalarReturn} or a {@link NonScalarReturn}; scalar returns are simple
 * values like Strings or Dates (components/embeddables fall into this category as well), while non-scalar returns are
 * entities and mapped collections.
 */