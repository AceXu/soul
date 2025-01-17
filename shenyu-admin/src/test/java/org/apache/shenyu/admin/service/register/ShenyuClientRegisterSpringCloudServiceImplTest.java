/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
    
package org.apache.shenyu.admin.service.register;
    
import org.apache.shenyu.admin.model.entity.MetaDataDO;
import org.apache.shenyu.admin.model.entity.SelectorDO;
import org.apache.shenyu.admin.service.impl.MetaDataServiceImpl;
import org.apache.shenyu.common.dto.convert.rule.impl.SpringCloudRuleHandle;
import org.apache.shenyu.common.dto.convert.selector.SpringCloudSelectorHandle;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.register.common.dto.MetaDataRegisterDTO;
import org.apache.shenyu.register.common.dto.URIRegisterDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
    
import java.util.ArrayList;
import java.util.List;
    
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
    
/**
 * Test cases for {@link ShenyuClientRegisterSpringCloudServiceImpl}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public final class ShenyuClientRegisterSpringCloudServiceImplTest {
    
    public static final String HOST = "localhost";
    
    @InjectMocks
    private ShenyuClientRegisterSpringCloudServiceImpl shenyuClientRegisterSpringCloudService;
    
    @Mock
    private MetaDataServiceImpl metaDataService;
    
    @Test
    public void testRpcType() {
        Assert.assertEquals(RpcTypeEnum.SPRING_CLOUD.getName(), shenyuClientRegisterSpringCloudService.rpcType());
    }
    
    @Test
    public void testSelectorHandler() {
        MetaDataRegisterDTO metaDataRegisterDTO = MetaDataRegisterDTO.builder().appName("testSelectorHandler").build();
        Assert.assertEquals("{\"serviceId\":\"testSelectorHandler\",\"gray\":false}", 
                shenyuClientRegisterSpringCloudService.selectorHandler(metaDataRegisterDTO));
    }
    
    @Test
    public void testRuleHandler() {
        Assert.assertEquals(new SpringCloudRuleHandle().toJson(), shenyuClientRegisterSpringCloudService.ruleHandler());
    }
    
    @Test
    public void testRegisterMetadata() {
        MetaDataDO metaDataDO = MetaDataDO.builder().build();
        when(metaDataService.findByPath(any())).thenReturn(metaDataDO);
        MetaDataRegisterDTO metaDataDTO = MetaDataRegisterDTO.builder().contextPath("/contextPath").build();
        shenyuClientRegisterSpringCloudService.registerMetadata(metaDataDTO);
        verify(metaDataService).findByPath("/contextPath/**");
        verify(metaDataService).saveOrUpdateMetaData(metaDataDO, metaDataDTO);
    }
    
    @Test
    public void testBuildHandle() {
        shenyuClientRegisterSpringCloudService = spy(shenyuClientRegisterSpringCloudService);
        
        final String returnStr = "{serviceId:'test1',gray:false,divideUpstreams:[{weight:50,warmup:10,protocol:"
                + "'http://',upstreamHost:'localhost',upstreamUrl:'localhost:8090',status:'true',timestamp:1637909490935}]}";
        final String expected = "{\"serviceId\":\"test1\",\"gray\":false,\"divideUpstreams\":[{\"weight\":50,\"warmup\":10,\"protocol\":"
                + "\"http://\",\"upstreamHost\":\"localhost\",\"upstreamUrl\":\"localhost:8090\",\"status\":true,\"timestamp\":1637909490935}]}";
        final URIRegisterDTO dto1 = URIRegisterDTO.builder().appName("test2")
                .rpcType(RpcTypeEnum.SPRING_CLOUD.getName())
                .host(HOST).port(8090).build();
        final URIRegisterDTO dto2 = URIRegisterDTO.builder().appName("test2")
                .rpcType(RpcTypeEnum.SPRING_CLOUD.getName())
                .host(HOST).port(8091).build();
        
        List<URIRegisterDTO> list = new ArrayList<>();
        list.add(dto1);
        SelectorDO selectorDO = mock(SelectorDO.class);
        doNothing().when(shenyuClientRegisterSpringCloudService).doSubmit(any(), any());
        when(selectorDO.getHandle()).thenReturn(returnStr);
        String actual = shenyuClientRegisterSpringCloudService.buildHandle(list, selectorDO);
        assertEquals(expected, actual);
        SpringCloudSelectorHandle handle = GsonUtils.getInstance().fromJson(actual, SpringCloudSelectorHandle.class);
        assertEquals(handle.getDivideUpstreams().size(), 1);
        
        list.clear();
        list.add(dto1);
        list.add(dto2);
        selectorDO = mock(SelectorDO.class);
        doNothing().when(shenyuClientRegisterSpringCloudService).doSubmit(any(), any());
        when(selectorDO.getHandle()).thenReturn(returnStr);
        actual = shenyuClientRegisterSpringCloudService.buildHandle(list, selectorDO);
        handle = GsonUtils.getInstance().fromJson(actual, SpringCloudSelectorHandle.class);
        assertEquals(handle.getDivideUpstreams().size(), 2);
        
        list.clear();
        list.add(dto1);
        selectorDO = mock(SelectorDO.class);
        doNothing().when(shenyuClientRegisterSpringCloudService).doSubmit(any(), any());
        when(selectorDO.getHandle()).thenReturn("{serviceId:'test1',gray:false,divideUpstreams:[]}");
        actual = shenyuClientRegisterSpringCloudService.buildHandle(list, selectorDO);
        handle = GsonUtils.getInstance().fromJson(actual, SpringCloudSelectorHandle.class);
        assertEquals(handle.getDivideUpstreams().size(), 1);
    }
}
