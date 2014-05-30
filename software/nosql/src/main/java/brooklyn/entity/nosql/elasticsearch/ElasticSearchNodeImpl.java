package brooklyn.entity.nosql.elasticsearch;

import static com.google.common.base.Preconditions.checkNotNull;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.event.feed.http.JsonFunctions;
import brooklyn.location.access.BrooklynAccessUtils;
import brooklyn.util.http.HttpToolResponse;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.net.HostAndPort;
import com.google.gson.JsonElement;

public class ElasticSearchNodeImpl extends SoftwareProcessImpl implements ElasticSearchNode {
    
    HttpFeed httpFeed;
    
    public ElasticSearchNodeImpl() {
        
    }

    @Override
    public Class<ElasticSearchNodeDriver> getDriverInterface() {
        return ElasticSearchNodeDriver.class;
    }
    
    @Override
    protected void connectSensors() {
        super.connectSensors();
        Integer rawPort = getAttribute(HTTP_PORT);
        checkNotNull(rawPort, "HTTP_PORT sensors not set for %s; is an acceptable port available?", this);
        HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, rawPort);
        Function<JsonElement, String> getNodeId = new Function<JsonElement, String>() {
            @Override public String apply(JsonElement input) {
                return input.getAsJsonObject().entrySet().iterator().next().getKey();
            }
        };
        
        Function<JsonElement, JsonElement> getFirstNodeFromNodes = new Function<JsonElement, JsonElement>() {
            @Override public JsonElement apply(JsonElement input) {
                return input.getAsJsonObject().entrySet().iterator().next().getValue();
            }
        };
        
        Function<HttpToolResponse, JsonElement> getFirstNode = HttpValueFunctions.chain(HttpValueFunctions.jsonContents(), 
            JsonFunctions.walk("nodes"), getFirstNodeFromNodes);
                
        httpFeed = HttpFeed.builder()
            .entity(this)
            .period(1000)
            .baseUri(String.format("http://%s:%s/_nodes/_local/stats", hp.getHostText(), hp.getPort()))
            .poll(new HttpPollConfig<Boolean>(SERVICE_UP)
                .onSuccess(HttpValueFunctions.responseCodeEquals(200))
                .onFailureOrException(Functions.constant(false)))
                .poll(new HttpPollConfig<String>(NODE_ID)
                        .onSuccess(HttpValueFunctions.chain(HttpValueFunctions.jsonContents(), JsonFunctions.walk("nodes"), getNodeId))
                        .onFailureOrException(Functions.constant("")))
            .poll(new HttpPollConfig<String>(NODE_NAME)
                .onSuccess(HttpValueFunctions.chain(getFirstNode, JsonFunctions.walk("name"), JsonFunctions.cast(String.class)))
                .onFailureOrException(Functions.<String>constant(null)))
            .poll(new HttpPollConfig<String>(CLUSTER_NAME)
                .onSuccess(HttpValueFunctions.chain(getFirstNode, JsonFunctions.walk("settings", "cluster", "name"), JsonFunctions.cast(String.class)))
                .onFailureOrException(Functions.<String>constant(null)))
            .poll(new HttpPollConfig<Integer>(DOCUMENT_COUNT)
                .onSuccess(HttpValueFunctions.chain(getFirstNode, JsonFunctions.walk("indices", "docs", "count"), JsonFunctions.cast(Integer.class)))
                .onFailureOrException(Functions.<Integer>constant(null)))
            .poll(new HttpPollConfig<Integer>(STORE_BYTES)
                .onSuccess(HttpValueFunctions.chain(getFirstNode, JsonFunctions.walk("indices", "store", "size_in_bytes"), JsonFunctions.cast(Integer.class)))
                .onFailureOrException(Functions.<Integer>constant(null)))
            .poll(new HttpPollConfig<Integer>(GET_TOTAL)
                .onSuccess(HttpValueFunctions.chain(getFirstNode, JsonFunctions.walk("indices", "get", "total"), JsonFunctions.cast(Integer.class)))
                .onFailureOrException(Functions.<Integer>constant(null)))
            .poll(new HttpPollConfig<Integer>(GET_TIME_IN_MILLIS)
                .onSuccess(HttpValueFunctions.chain(getFirstNode, JsonFunctions.walk("indices", "get", "time_in_millis"), JsonFunctions.cast(Integer.class)))
                .onFailureOrException(Functions.<Integer>constant(null)))
            .poll(new HttpPollConfig<Integer>(SEARCH_QUERY_TOTAL)
                .onSuccess(HttpValueFunctions.chain(getFirstNode, JsonFunctions.walk("indices", "search", "query_total"), JsonFunctions.cast(Integer.class)))
                .onFailureOrException(Functions.<Integer>constant(null)))
            .poll(new HttpPollConfig<Integer>(SEARCH_QUERY_TIME_IN_MILLIS)
                .onSuccess(HttpValueFunctions.chain(getFirstNode, JsonFunctions.walk("indices", "search", "query_time_in_millis"), JsonFunctions.cast(Integer.class)))
                .onFailureOrException(Functions.<Integer>constant(null)))
            .build();
    }
    
    @Override
    protected void disconnectSensors() {
        if (httpFeed != null) {
            httpFeed.stop();
        }
    }
}
