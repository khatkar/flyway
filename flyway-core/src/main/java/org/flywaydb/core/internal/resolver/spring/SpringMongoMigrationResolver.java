/*
 * Copyright 2010-2017 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.resolver.spring;

import org.flywaydb.core.api.configuration.FlywayConfiguration;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationType;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.MigrationChecksumProvider;
import org.flywaydb.core.api.migration.MigrationInfoProvider;
import org.flywaydb.core.api.migration.spring.SpringMongoMigration;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.resolver.MigrationInfoHelper;
import org.flywaydb.core.internal.resolver.ResolvedMigrationComparator;
import org.flywaydb.core.internal.resolver.ResolvedMigrationImpl;
import org.flywaydb.core.internal.util.ClassUtils;
import org.flywaydb.core.internal.util.ConfigurationInjectionUtils;
import org.flywaydb.core.internal.util.Location;
import org.flywaydb.core.internal.util.Pair;
import org.flywaydb.core.internal.util.StringUtils;
import org.flywaydb.core.internal.util.scanner.Scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Migration resolver for Spring Mongo migrations. The classes must have a name like V1 or V1_1_3 or
 * V1__Description or V1_1_3__Description.
 */
public class SpringMongoMigrationResolver implements MigrationResolver {
	
	/**
	 * The name of the database to execute migrations against.
	 */
	private final String databaseName;
	
	/**
	 * The base package on the classpath where to migrations are located.
	 */
	private final Location location;

	/**
	 * The Scanner to use.
	 */
	private Scanner scanner;

	/**
	 * The configuration to inject (if necessary) in the migration classes.
	 */
	private FlywayConfiguration configuration;

	/**
	 * Creates a new instance.
	 *
	 * @param databaseName  The database to use.
	 * @param location      The base package on the classpath where to migrations are located.
	 * @param scanner       The Scanner for loading migrations on the classpath.
	 * @param configuration The configuration to inject (if necessary) in the migration classes.
	 */
	public SpringMongoMigrationResolver(String databaseName, Scanner scanner,
										Location location, FlywayConfiguration configuration) {
		this.databaseName = databaseName;
		this.location = location;
		this.scanner = scanner;
		this.configuration = configuration;
	}

	@Override
	public Collection<ResolvedMigration> resolveMigrations() {
		List<ResolvedMigration> migrations = new ArrayList<ResolvedMigration>();

		if (!location.isClassPath()) {
			return migrations;
		}

		try {
			Class<?>[] classes = scanner.scanForClasses(location, SpringMongoMigration.class);
			for (Class<?> clazz : classes) {
				SpringMongoMigration springMongoMigration = ClassUtils.instantiate(clazz.getName(), scanner.getClassLoader());
				ConfigurationInjectionUtils.injectFlywayConfiguration(springMongoMigration, configuration);

				ResolvedMigrationImpl migrationInfo = (ResolvedMigrationImpl) extractMigrationInfo(springMongoMigration);
				migrationInfo.setPhysicalLocation(ClassUtils.getLocationOnDisk(clazz));
				migrationInfo.setExecutor(new SpringMongoMigrationExecutor(databaseName, springMongoMigration));

				migrations.add(migrationInfo);
			}
		} catch (Exception e) {
			throw new FlywayException("Unable to resolve Spring Jdbc Java migrations in location: " + location, e);
		}

		Collections.sort(migrations, new ResolvedMigrationComparator());
		return migrations;
	}

	/**
	 * Extracts the migration info from this migration.
	 *
	 * @param springMongoMigration The migration to analyse.
	 * @return The migration info.
	 */
	/* private -> testing */
	ResolvedMigration extractMigrationInfo(SpringMongoMigration springMongoMigration) {
		Integer checksum = null;
		if (springMongoMigration instanceof MigrationChecksumProvider) {
			MigrationChecksumProvider checksumProvider = (MigrationChecksumProvider) springMongoMigration;
			checksum = checksumProvider.getChecksum();
		}

		MigrationVersion version;
		String description;
		if (springMongoMigration instanceof MigrationInfoProvider) {
			MigrationInfoProvider infoProvider = (MigrationInfoProvider) springMongoMigration;
			version = infoProvider.getVersion();
			description = infoProvider.getDescription();
			if (!StringUtils.hasText(description)) {
				throw new FlywayException("Missing description for migration " + version);
			}
		} else {
			String shortName = ClassUtils.getShortName(springMongoMigration.getClass());
			String prefix;
			boolean repeatable = shortName.startsWith("R");
			if (shortName.startsWith("V") || shortName.startsWith("R")) {
				prefix = shortName.substring(0, 1);
			} else {
				throw new FlywayException("Invalid MongoDB migration class name: " +
                        springMongoMigration.getClass().getName() + " => ensure it starts with V or R," +
                        " or implement org.flywaydb.core.api.migration.MigrationInfoProvider for non-default naming");
			}
			Pair<MigrationVersion, String> info = MigrationInfoHelper.extractVersionAndDescription(shortName, prefix, "__", "", repeatable);
			version = info.getLeft();
			description = info.getRight();
		}

		ResolvedMigrationImpl resolvedMigration = new ResolvedMigrationImpl();
		resolvedMigration.setVersion(version);
		resolvedMigration.setDescription(description);
		resolvedMigration.setScript(springMongoMigration.getClass().getName());
		resolvedMigration.setChecksum(checksum);
		resolvedMigration.setType(MigrationType.MONGODB);
		return resolvedMigration;
	}
}