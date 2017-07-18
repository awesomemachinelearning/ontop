package it.unibz.inf.ontop.sql.pool.impl;

import com.google.inject.Inject;
import it.unibz.inf.ontop.injection.OntopSystemSQLSettings;
import it.unibz.inf.ontop.sql.pool.JDBCConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;


/**
 * Not a SINGLETON!
 */
public class TomcatConnectionPool implements JDBCConnectionPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatConnectionPool.class);
    private final DataSource tomcatPool;

    @Inject
    private TomcatConnectionPool(OntopSystemSQLSettings settings) {
        PoolProperties poolProperties = new PoolProperties();
        poolProperties.setUrl(settings.getJdbcUrl());
        settings.getJdbcDriver()
                .ifPresent(poolProperties::setDriverClassName);
        poolProperties.setUsername(settings.getJdbcUser());
        poolProperties.setPassword(settings.getJdbcPassword());
        poolProperties.setJmxEnabled(true);

        // TEST connection before using it
        boolean keepAlive = settings.isKeepAliveEnabled();
        poolProperties.setTestOnBorrow(keepAlive);
        if (keepAlive) {
            // TODO: refactor this
            String driver = settings.getJdbcDriver()
                    .orElse("");
            if (driver.contains("oracle"))
                poolProperties.setValidationQuery("select 1 from dual");
            else if (driver.contains("db2"))
                poolProperties.setValidationQuery("select 1 from sysibm.sysdummy1");
            else
                poolProperties.setValidationQuery("select 1");
        }

        boolean removeAbandoned = settings.isRemoveAbandonedEnabled();
        int abandonedTimeout = settings.getAbandonedTimeout();
        int startPoolSize = settings.getConnectionPoolInitialSize();
        int maxPoolSize = settings.getConnectionPoolMaxSize();

        poolProperties.setTestOnReturn(false);
        poolProperties.setMaxActive(maxPoolSize);
        poolProperties.setMaxIdle(maxPoolSize);
        poolProperties.setInitialSize(startPoolSize);
        poolProperties.setMaxWait(30000);
        poolProperties.setRemoveAbandonedTimeout(abandonedTimeout);
        poolProperties.setMinEvictableIdleTimeMillis(30000);
        poolProperties.setLogAbandoned(false);
        poolProperties.setRemoveAbandoned(removeAbandoned);
        poolProperties.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
                + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        tomcatPool = new DataSource();
        tomcatPool.setPoolProperties(poolProperties);

        LOGGER.debug("Connection Pool Properties:");
        LOGGER.debug("Start size: " + startPoolSize);
        LOGGER.debug("Max size: " + maxPoolSize);
        LOGGER.debug("Remove abandoned connections: " + removeAbandoned);
    }

    @Override
    public void close() {
        tomcatPool.close();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return tomcatPool.getConnection();
    }
}
