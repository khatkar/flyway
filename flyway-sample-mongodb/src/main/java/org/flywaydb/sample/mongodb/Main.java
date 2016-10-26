/**
 * Copyright 2010-2016 Boxfuse GmbH
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
package org.flywaydb.sample.mongodb;

import org.flywaydb.core.MongoFlyway;

import java.util.Properties;

public class Main {
	private static final String MONGO_PREFIX = "flyway.mongo.";
	
	public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        MongoFlyway mongoFlyway = new MongoFlyway();

        props.setProperty(MONGO_PREFIX + "locations", "org.flywaydb.sample.mongodb.migrations, db/migration");
        props.setProperty(MONGO_PREFIX + "validateOnMigrate", "false");
        props.setProperty(MONGO_PREFIX + "uri", "mongodb://localhost:27017/db1");
        mongoFlyway.configure(props);

        mongoFlyway.migrate();
	}

}
