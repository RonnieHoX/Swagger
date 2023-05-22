package com.lx.tester.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lx.enums.PlatFormType;
import com.lx.utils.HttpUtil;
import com.lx.utils.JsonUtil;
import com.lx.utils.Md5Util;
import com.lx.websocket.ProtocolCode;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.reactive.RestQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.UUID;

@Path("/lx-api")
public class CallLxApiServer {

    private static final Logger LOG = LoggerFactory.getLogger(CallLxApiServer.class);

    private Map<Env, LxApiParameter> lxApiParameterMap = Map.of(
            Env.TEST_TRANSFER_CNY, new LxApiParameter(
                    "9971f70336320eb5143f72aa0431d2a7"
                    , "1080636_bagfife"                         // 此欄位目前只有演示環境會使用
//                    , 1080636                                 // LX後台帳號: lxapi001
//                    , 1083726                                 // LX後台帳號: lxapi002
                    , 1100005                                   // LX後台帳號: lxmegaag001
                    , "1080613_chessapilixin"
                    , "http://game8866.qaz411.com"
                    , "")
            , Env.TEST_TRANSFER_KRW, new LxApiParameter(
                    "4c1f4e6fda91034d51c73889d1e3f24c"
                    , ""                                        // 此欄位目前只有演示環境會使用
                    , 1101285                                   // LX後台帳號: krmegaag01
                    , "1101282_chessapilixin"
                    , "http://game8866.qaz411.com"
                    , "")
            , Env.TEST_SEAMLESS, new LxApiParameter(
                    "6f407cbf1ec1dfb04f8979eb9969f101"
                    , "1080636_bagfife"                         // 此欄位目前只有演示環境會使用
                    , 1101032                                   // LX後台帳號: megaswagent01
                    , "1100982_chess800"
                    , "http://game8866.qaz411.com"
                    , "http://43.255.53.30:5501")
            , Env.YANSHI, new LxApiParameter(
                    "8cf0cc1105c890962769227c954c7a95"
                    , "1099478_baiaadj"                         // 網頁登入帳號: monkot01
                    , 1099478
                    , "1097867_haocai"
                    , "http://43.255.53.29:8866",
                    "")
    );

    public enum Env {
        TEST_TRANSFER_CNY, TEST_TRANSFER_KRW, TEST_SEAMLESS, YANSHI
    }

    @Path("/deposit")
    @GET
    public Uni<JsonNode> deposit(@RestQuery Env env, @RestQuery String account, @RestQuery double balance) {
        if (account == null || account.isBlank()) {
            return Uni.createFrom().item(JsonUtil.getInstance().getObjectMapper().createObjectNode()
                    .put("code", ProtocolCode.FAIL.getCode())
                    .put("msg", "account is null"));
        }

        return this.postTransferApi(env, account, "/doTransferDepositTask", balance);
    }

    @Path("/withdraw")
    @GET
    public Uni<JsonNode> withdraw(@RestQuery Env env, @RestQuery String account) {
        if (account == null || account.isBlank()) {
            return Uni.createFrom().item(JsonUtil.getInstance().getObjectMapper().createObjectNode()
                    .put("code", ProtocolCode.FAIL.getCode())
                    .put("msg", "account is null"));
        }

        return this.postTransferApi(env, account, "/queryUserScore", 0)
                .onItem().transform(response -> response.get("data").get("money").asDouble())
                .chain(money -> this.postTransferApi(env, account, "/doTransferWithdrawTask", money));
    }


    private Uni<JsonNode> postTransferApi(Env env, String account, String apiPath, double balance) {
        Uni<JsonNode> emitter = Uni.createFrom().emitter(uniEmitter -> {
            try {
                uniEmitter.complete(this.postTransferApi(this.prepareLxApiParameter(env, account), apiPath, balance));
            } catch (NoSuchAlgorithmException e) {
                uniEmitter.fail(e);
            }

        });
        return emitter.runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private JsonNode postTransferApi(LxApiParameter lxApiParameter, String apiPath, double money) throws NoSuchAlgorithmException {
        String dateFormat = "yyyyMMddHHmmssSSS";

        ObjectNode contentNode = JsonUtil.getInstance().getObjectMapper().createObjectNode();
        final String account = lxApiParameter.getAccount();
        contentNode.put("account", account);
        contentNode.put("agent", lxApiParameter.getAgent());
        contentNode.put("companyKey", lxApiParameter.getCompanyKey());
        contentNode.put("money", money);

        final long timestamp = System.currentTimeMillis();
        contentNode.put("orderId", new SimpleDateFormat(dateFormat).format(timestamp) + account);
        contentNode.put("timestamp", timestamp);

        return this.postApi(contentNode, lxApiParameter, apiPath);
    }

    @Path("/login-game")
    @GET
    public Uni<JsonNode> loginGame(@Context RoutingContext context
            , @RestQuery Env env, @RestQuery String account, @RestQuery double balance, @RestQuery int gameId, @RestQuery String languageType, @RestQuery Integer lobbyType) {
        if (account == null || account.isBlank()) {
            return Uni.createFrom().item(JsonUtil.getInstance().getObjectMapper().createObjectNode()
                    .put("code", ProtocolCode.FAIL.getCode())
                    .put("msg", "account is null"));
        }

        String ip = context.get(HttpUtil.REMOTE_ADDRESS);

        Uni<JsonNode> emitter = Uni.createFrom().emitter(uniEmitter -> {
            try {
                uniEmitter.complete(this.postLoginGame(env, account, balance, gameId, ip, languageType, lobbyType));
            } catch (NoSuchAlgorithmException e) {
                uniEmitter.fail(e);
            }

        });
        return emitter.runSubscriptionOn(Infrastructure.getDefaultExecutor());
    }

    private JsonNode postLoginGame(Env env, String account, double money, int gameId, String ip, String languageType, Integer lobbyType) throws NoSuchAlgorithmException {
        final LxApiParameter lxApiParameter = prepareLxApiParameter(env, account);
        String dateFormat = "yyyyMMddHHmmssSSS";

        ObjectNode contentNode = JsonUtil.getInstance().getObjectMapper().createObjectNode();
        contentNode.put("account", lxApiParameter.getAccount());
        contentNode.put("gameId", gameId);
        contentNode.put("agent", lxApiParameter.getAgent());
        contentNode.put("companyKey", lxApiParameter.getCompanyKey());
        contentNode.put("money", money);
        contentNode.put("ip", ip);
        contentNode.put("platform", PlatFormType.Wap.getCode());
        if (languageType != null) {
            contentNode.put("languageType", languageType);
        }
        if (lobbyType != null) {
            contentNode.put("lobbyType", lobbyType);
        }
        if (env == Env.TEST_SEAMLESS) {
            contentNode.put("appUrl", lxApiParameter.getAppUrl());
        }
        contentNode.put("theme", getTheme(env));
        contentNode.put("token", UUID.randomUUID().toString().replace("-", ""));

        final long timestamp = System.currentTimeMillis();
        contentNode.put("orderId", new SimpleDateFormat(dateFormat).format(timestamp) + lxApiParameter.getAccount());
        contentNode.put("timestamp", timestamp);

        return postApi(contentNode, lxApiParameter, "/login");
    }

    private static String getTheme(Env env) {
        switch (env) {
            case TEST_TRANSFER_CNY:
            case TEST_TRANSFER_KRW:
            case TEST_SEAMLESS:
                return "S004";
            case YANSHI:
            default:
                return "S001";
        }
    }

    private JsonNode postApi(JsonNode contentNode, LxApiParameter lxApiParameter, String apiPath) throws NoSuchAlgorithmException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(lxApiParameter.getUrl()).path(apiPath);
        Response response = target.request()
                .header("Authorization", Md5Util.generateMd5(lxApiParameter.getKey(), contentNode.toString()))
                .header("ClientIP", "127.0.0.1")
                .post(Entity.entity(contentNode, MediaType.APPLICATION_JSON_TYPE));
        LOG.info("url: {}, apiPath: {}, request: {}", lxApiParameter.getUrl(), apiPath, contentNode);
        final JsonNode responseJson = response.readEntity(JsonNode.class);
        response.close();
        LOG.info("url: {}, apiPath: {}, request: {}, response: {}", lxApiParameter.getUrl(), apiPath, contentNode, responseJson);

        return responseJson;
    }

    private LxApiParameter prepareLxApiParameter(Env env, String account) {
        final LxApiParameter lxApiParameter = lxApiParameterMap.get(env);

        switch (env) {
            case TEST_TRANSFER_CNY:
            case TEST_TRANSFER_KRW:
            case TEST_SEAMLESS:
                lxApiParameter.setAccount(lxApiParameter.getAgent() + "_" + account);
                break;
            case YANSHI:
            default:
        }

        return lxApiParameter;
    }

}
