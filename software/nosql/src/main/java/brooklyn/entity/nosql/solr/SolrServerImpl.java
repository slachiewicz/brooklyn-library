/*
 * Copyright 2012-2014 by Cloudsoft Corp.
 */
package brooklyn.entity.nosql.solr;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.RollingTimeWindowMeanEnricher;
import brooklyn.enricher.TimeWeightedDeltaEnricher;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.java.JavaAppUtils;
import brooklyn.entity.webapp.WebAppServiceMethods;
import brooklyn.event.AttributeSensor;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.event.feed.jmx.JmxAttributePollConfig;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.event.feed.jmx.JmxOperationPollConfig;
import brooklyn.location.MachineLocation;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.basic.Machines;
import brooklyn.location.cloud.CloudLocationConfig;
import brooklyn.util.collections.MutableSet;
import brooklyn.util.text.Strings;
import brooklyn.util.time.Duration;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

/**
 * Implementation of {@link SolrServer}.
 */
public class SolrServerImpl extends SoftwareProcessImpl implements SolrServer {

    private static final Logger log = LoggerFactory.getLogger(SolrServerImpl.class);

    public SolrServerImpl() {
    }
    
    @Override public Integer getSolrPort() { return getAttribute(SolrServer.SOLR_PORT); }

    @Override
    public Class<SolrServerDriver> getDriverInterface() {
        return SolrServerDriver.class;
    }

    @Override
    public void init() {
        super.init();
    }

    private volatile HttpFeed httpFeed;
//    private volatile JmxFeed jmxFeed;
//    private JmxHelper jmxHelper;
//    private ObjectName storageServiceMBean = JmxHelper.createObjectName("org.apache.solr.db:type=StorageService");

    @Override 
    protected void connectSensors() {
        super.connectSensors();

        httpFeed = HttpFeed.builder()
                .entity(this)
                .period(500, TimeUnit.MILLISECONDS)
                .baseUri(String.format("http://%s:%d/solr", getAttribute(HOSTNAME), getSolrPort()))
                .poll(new HttpPollConfig<Boolean>(SERVICE_UP)
                        .onSuccess(HttpValueFunctions.responseCodeEquals(200))
                        .onFailureOrException(Functions.constant(false)))
                .build();

//        jmxHelper = new JmxHelper(this);
//        jmxFeed = JmxFeed.builder()
//                .entity(this)
//                .period(3000, TimeUnit.MILLISECONDS)
//                .helper(jmxHelper)
//                .pollAttribute(new JmxAttributePollConfig<Boolean>(SERVICE_UP_JMX)
//                        .objectName(storageServiceMBean)
//                        .attributeName("Initialized")
//                        .onSuccess(Functions.forPredicate(Predicates.notNull()))
//                        .onException(Functions.constant(false)))
//                .build();
//
//        connectEnrichers();
    }

//    protected void connectEnrichers() {
//        connectEnrichers(Duration.TEN_SECONDS);
//    }
//
//    protected void connectEnrichers(Duration windowPeriod) {
//        JavaAppUtils.connectMXBeanSensors(this);
//        JavaAppUtils.connectJavaAppServerPolicies(this);
//
//        if (windowPeriod!=null) {
//            addEnricher(new RollingTimeWindowMeanEnricher<Double>(this, READS_PER_SECOND_LAST, 
//                    READS_PER_SECOND_IN_WINDOW, windowPeriod));
//            addEnricher(new RollingTimeWindowMeanEnricher<Double>(this, WRITES_PER_SECOND_LAST, 
//                    WRITES_PER_SECOND_IN_WINDOW, windowPeriod));
//        }
//    }

    @Override
    public void disconnectSensors() {
        super.disconnectSensors();

        if (httpFeed != null) httpFeed.stop();
//        if (jmxFeed != null) jmxFeed.stop();
//        if (jmxHelper.isConnected()) jmxHelper.disconnect();
    }
}
