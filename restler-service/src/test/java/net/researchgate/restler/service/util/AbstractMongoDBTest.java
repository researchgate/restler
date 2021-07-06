/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Researchgate GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.researchgate.restler.service.util;

import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMongoDBTest {
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractMongoDBTest.class);
    /**
     * please store Starter or RuntimeConfig in a static final field
     * if you want to use artifact store caching (or else disable caching)
     */
    private static final MongodStarter starter = MongodStarter.getDefaultInstance();
    private static final int PORT = 27085;

    private static MongodExecutable _mongodExe;
    private static MongodProcess _mongod;

    @BeforeClass
    public static void setUp() throws Exception {
        LOGGER.warn("Starting embedded mongo on port: " + PORT);
        _mongodExe = starter.prepare(new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(PORT, Network.localhostIsIPv6()))
                .build());
        _mongod = _mongodExe.start();

        // This is needed, for now at least. Remove this when not needed. Without, we currently get:
        // java.lang.RuntimeException: java.lang.ClassNotFoundException: Provider org.glassfish.jersey.internal.RuntimeDelegateImpl could not be instantiated: java.lang.IllegalStateException: It appears there is no ServiceLocatorGenerator installed.
        // See: https://github.com/dropwizard/dropwizard/issues/1772
        JerseyGuiceUtils.install((s, serviceLocator) -> null);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        System.out.println("Shutting down embedded mongo");
        _mongod.stop();
        _mongodExe.stop();
    }

}