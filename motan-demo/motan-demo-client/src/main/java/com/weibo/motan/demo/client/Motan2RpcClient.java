/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.motan.demo.client;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.proxy.CommonClient;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.ResponseFuture;
import com.weibo.breeze.message.GenericMessage;
import com.weibo.breeze.serializer.CommonSerializer;
import com.weibo.motan.demo.service.MotanDemoService;
import com.weibo.motan.demo.service.PbParamService;
import com.weibo.motan.demo.service.model.User;
import io.grpc.examples.routeguide.Point;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Motan2RpcClient {

    public static void main(String[] args) throws Throwable {
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"classpath:motan2_demo_client.xml"});
        MotanDemoService service;
        // hessian
        service = (MotanDemoService) ctx.getBean("motanDemoReferer");
        print(service);

        // simple serialization
        service = (MotanDemoService) ctx.getBean("motanDemoReferer-simple");
        print(service);

        // breeze serialization
        service = (MotanDemoService) ctx.getBean("motanDemoReferer-breeze");
        print(service);

        // server end async
        service = (MotanDemoService) ctx.getBean("motanDemoReferer-asyncServer");
        print(service);

        // pb serialization
        PbParamService pbService = (PbParamService) ctx.getBean("motanDemoReferer-pb");
        System.out.println(pbService.getFeature(Point.newBuilder().setLatitude(123).setLongitude(456).build()));

        // common client
        CommonClient commonClient = (CommonClient) ctx.getBean("motanDemoReferer-common-client");
        motan2XmlCommonClientDemo(commonClient);
        motan2ApiCommonClientDemo();
        breezeGenericCall(commonClient);

        System.out.println("motan demo is finish.");
        System.exit(0);
    }

    public static void print(MotanDemoService service) throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            try {
                System.out.println(service.hello("motan" + i));
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void motan2XmlCommonClientDemo(CommonClient client) throws Throwable {
        System.out.println(client.call("hello", new Object[]{"a"}, String.class));

        User user = new User(1, "AAA");
        System.out.println(user);

        user = (User) client.call("rename", new Object[]{user, "BBB"}, User.class);
        System.out.println(user);

        ResponseFuture future = (ResponseFuture) client.asyncCall("rename", new Object[]{user, "CCC"}, User.class);
        user = (User) future.getValue();
        System.out.println(user);

        ResponseFuture future2 = (ResponseFuture) client.asyncCall("rename", new Object[]{user, "DDD"}, User.class);
        future2.addListener(future1 -> System.out.println(future1.getValue()));

        Request request = client.buildRequest("rename", new Object[]{user, "EEE"});
        request.setAttachment("a", "a");
        user = (User) client.call(request, User.class);
        System.out.println(user);

        // expect throw exception
//        client.call("rename", new Object[]{null, "FFF"}, void.class);
    }

    public static void motan2ApiCommonClientDemo() throws Throwable {
        RefererConfig<CommonClient> referer = new RefererConfig<>();

        // 设置服务端接口
        referer.setInterface(CommonClient.class);
        referer.setServiceInterface("com.weibo.motan.demo.service.MotanDemoService");

        // 配置服务的group以及版本号
        referer.setGroup("motan-demo-rpc");
        referer.setVersion("1.0");
        referer.setRequestTimeout(1000);
        referer.setAsyncInitConnection(false);

        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();
        registry.setRegProtocol("direct");
        registry.setAddress("127.0.0.1:8001");
        referer.setRegistry(registry);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan2");
        protocol.setName("motan2");
        referer.setProtocol(protocol);

        // 使用服务
        CommonClient client = referer.getRef();
        System.out.println(client.call("hello", new Object[]{"a"}, String.class));
    }

    private static void breezeGenericCall(CommonClient client) throws Throwable {
        GenericMessage genericMessage = new GenericMessage("com.weibo.motan.demo.service.model.User");
        genericMessage.putFields(CommonSerializer.getHash("id"), 1);
        genericMessage.putFields(CommonSerializer.getHash("name"), "AAA");
        User user = (User) client.call("rename", new Object[]{genericMessage, "BBB"}, User.class);
        System.out.println(user);
    }

}
